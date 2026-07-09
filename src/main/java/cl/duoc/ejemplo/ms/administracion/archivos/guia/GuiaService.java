package cl.duoc.ejemplo.ms.administracion.archivos.guia;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import cl.duoc.ejemplo.ms.administracion.archivos.exception.GuiaNotFoundException;
import cl.duoc.ejemplo.ms.administracion.archivos.exception.InvalidFileException;
import cl.duoc.ejemplo.ms.administracion.archivos.guia.dto.GuiaDespachoRequest;
import cl.duoc.ejemplo.ms.administracion.archivos.guia.dto.GuiaMensaje;
import cl.duoc.ejemplo.ms.administracion.archivos.service.AwsS3Service;
import cl.duoc.ejemplo.ms.administracion.archivos.service.EfsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Lógica de negocio de las guías de despacho: CRUD, almacenamiento temporal
 * en EFS, subida a S3 organizada por fecha/transportista y publicación de
 * eventos en RabbitMQ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuiaService {

	private static final DateTimeFormatter KEY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final GuiaDespachoRepository guiaRepository;
	private final AwsS3Service awsS3Service;
	private final EfsService efsService;
	private final GuiaColaProducer guiaColaProducer;

	@Value("${aws.s3.bucket}")
	private String bucket;

	@Transactional
	public GuiaDespacho crear(GuiaDespachoRequest request) {

		GuiaDespacho guia = GuiaDespacho.builder().numeroGuia(request.getNumeroGuia())
				.transportista(request.getTransportista()).fecha(request.getFecha())
				.estado(request.getEstado() != null ? request.getEstado() : "CREADA").build();

		guia = guiaRepository.save(guia);
		log.info("Guía creada con id {}", guia.getId());

		guiaColaProducer.publicarGuia(toMensaje(guia, "CREADA"));

		return guia;
	}

	/**
	 * Guarda el archivo temporalmente en EFS y luego lo sube a S3 con la
	 * estructura {yyyyMMdd}/{transportista}/{archivo}.
	 */
	@Transactional
	public GuiaDespacho subirArchivo(Long id, MultipartFile file) {

		GuiaDespacho guia = obtenerPorId(id);

		if (file == null || file.isEmpty() || file.getOriginalFilename() == null
				|| file.getOriginalFilename().isBlank()) {
			throw new InvalidFileException("Debe adjuntar un archivo válido");
		}

		String key = guia.getFecha().format(KEY_DATE_FORMAT) + "/" + guia.getTransportista() + "/"
				+ file.getOriginalFilename();

		try {
			efsService.saveToEfs(key, file);
			log.info("Guía {} almacenada temporalmente en EFS: {}", id, key);
		} catch (IOException e) {
			throw new InvalidFileException("No fue posible guardar el archivo en EFS: " + e.getMessage());
		}

		awsS3Service.upload(bucket, key, file);

		guia.setNombreArchivo(file.getOriginalFilename());
		guia.setS3Key(key);
		guia.setEstado("SUBIDA_S3");
		guia = guiaRepository.save(guia);

		guiaColaProducer.publicarGuia(toMensaje(guia, "ACTUALIZADA"));

		return guia;
	}

	@Transactional(readOnly = true)
	public byte[] descargar(Long id) {

		GuiaDespacho guia = obtenerPorId(id);

		if (guia.getS3Key() == null) {
			throw new GuiaNotFoundException(id);
		}

		return awsS3Service.downloadAsBytes(bucket, guia.getS3Key());
	}

	@Transactional
	public GuiaDespacho actualizar(Long id, GuiaDespachoRequest request) {

		GuiaDespacho guia = obtenerPorId(id);

		guia.setNumeroGuia(request.getNumeroGuia());
		guia.setTransportista(request.getTransportista());
		guia.setFecha(request.getFecha());
		if (request.getEstado() != null) {
			guia.setEstado(request.getEstado());
		}

		guia = guiaRepository.save(guia);
		log.info("Guía {} actualizada", id);

		guiaColaProducer.publicarGuia(toMensaje(guia, "ACTUALIZADA"));

		return guia;
	}

	@Transactional
	public void eliminar(Long id) {

		GuiaDespacho guia = obtenerPorId(id);

		if (guia.getS3Key() != null) {
			awsS3Service.deleteObject(bucket, guia.getS3Key());
		}

		guiaRepository.delete(guia);
		log.info("Guía {} eliminada", id);
	}

	@Transactional(readOnly = true)
	public List<GuiaDespacho> consultar(String transportista, LocalDate fecha) {

		if (transportista != null && fecha != null) {
			return guiaRepository.findByTransportistaIgnoreCaseAndFecha(transportista, fecha);
		}
		if (transportista != null) {
			return guiaRepository.findByTransportistaIgnoreCase(transportista);
		}
		if (fecha != null) {
			return guiaRepository.findByFecha(fecha);
		}
		return guiaRepository.findAll();
	}

	@Transactional(readOnly = true)
	public GuiaDespacho obtenerPorId(Long id) {
		return guiaRepository.findById(id).orElseThrow(() -> new GuiaNotFoundException(id));
	}

	private GuiaMensaje toMensaje(GuiaDespacho guia, String evento) {

		return GuiaMensaje.builder().guiaId(guia.getId()).numeroGuia(guia.getNumeroGuia())
				.transportista(guia.getTransportista()).fecha(guia.getFecha()).s3Key(guia.getS3Key())
				.estado(guia.getEstado()).evento(evento).timestamp(LocalDateTime.now()).build();
	}
}

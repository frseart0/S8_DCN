package cl.duoc.ejemplo.ms.administracion.archivos.guia;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cl.duoc.ejemplo.ms.administracion.archivos.guia.dto.GuiaDespachoRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/guias")
@RequiredArgsConstructor
public class GuiaController {

	private final GuiaService guiaService;
	private final GuiaColaConsumer guiaColaConsumer;

	/** Crear una guía de despacho */
	@PostMapping
	public ResponseEntity<GuiaDespacho> crear(@Valid @RequestBody GuiaDespachoRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(guiaService.crear(request));
	}

	/** Subir el archivo de la guía a S3 (pasa primero por EFS) */
	@PostMapping("/{id}/archivo")
	public ResponseEntity<GuiaDespacho> subirArchivo(@PathVariable Long id,
			@RequestParam("file") MultipartFile file) {
		return ResponseEntity.status(HttpStatus.CREATED).body(guiaService.subirArchivo(id, file));
	}

	/** Descargar la guía (requiere rol GUIA_LECTOR) */
	@GetMapping("/{id}/descargar")
	public ResponseEntity<byte[]> descargar(@PathVariable Long id) {

		GuiaDespacho guia = guiaService.obtenerPorId(id);
		byte[] bytes = guiaService.descargar(id);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"" + guia.getNombreArchivo() + "\"")
				.contentType(MediaType.APPLICATION_OCTET_STREAM).body(bytes);
	}

	/** Modificar o actualizar una guía */
	@PutMapping("/{id}")
	public ResponseEntity<GuiaDespacho> actualizar(@PathVariable Long id,
			@Valid @RequestBody GuiaDespachoRequest request) {
		return ResponseEntity.ok(guiaService.actualizar(id, request));
	}

	/** Eliminar una guía específica (incluye su archivo en S3) */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {

		guiaService.eliminar(id);
		return ResponseEntity.noContent().build();
	}

	/** Consultar guías por transportista y/o fecha */
	@GetMapping
	public ResponseEntity<List<GuiaDespacho>> consultar(@RequestParam(required = false) String transportista,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
		return ResponseEntity.ok(guiaService.consultar(transportista, fecha));
	}

	/** Obtener una guía por id */
	@GetMapping("/{id}")
	public ResponseEntity<GuiaDespacho> obtener(@PathVariable Long id) {
		return ResponseEntity.ok(guiaService.obtenerPorId(id));
	}

	/**
	 * Consume los mensajes pendientes de la cola 1 y los persiste en Oracle
	 * Cloud (tabla GUIAS_PROCESADAS)
	 */
	@PostMapping("/procesar-cola")
	public ResponseEntity<Map<String, Object>> procesarCola() {

		List<GuiaProcesada> procesadas = guiaColaConsumer.procesarCola();
		return ResponseEntity.ok(Map.of("procesadas", procesadas.size(), "guias", procesadas));
	}
}

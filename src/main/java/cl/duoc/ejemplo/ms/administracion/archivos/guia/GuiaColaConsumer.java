package cl.duoc.ejemplo.ms.administracion.archivos.guia;

import java.util.ArrayList;
import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.ejemplo.ms.administracion.archivos.config.RabbitConfig;
import cl.duoc.ejemplo.ms.administracion.archivos.guia.dto.GuiaMensaje;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumidor bajo demanda (semana 8): vacía la cola 1 y persiste cada mensaje
 * en Oracle Cloud en la tabla GUIAS_PROCESADAS. Los mensajes que no puedan
 * procesarse se derivan a la cola 2 (guias.errores).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuiaColaConsumer {

	private static final long RECEIVE_TIMEOUT_MS = 2000;

	private final RabbitTemplate rabbitTemplate;
	private final GuiaProcesadaRepository guiaProcesadaRepository;
	private final GuiaColaProducer guiaColaProducer;

	@Transactional
	public List<GuiaProcesada> procesarCola() {

		List<GuiaProcesada> procesadas = new ArrayList<>();

		Object recibido;
		while ((recibido = rabbitTemplate.receiveAndConvert(RabbitConfig.QUEUE_GUIAS, RECEIVE_TIMEOUT_MS)) != null) {

			if (!(recibido instanceof GuiaMensaje mensaje)) {
				log.warn("Mensaje con formato desconocido descartado: {}", recibido);
				continue;
			}

			try {
				GuiaProcesada procesada = GuiaProcesada.builder().guiaId(mensaje.getGuiaId())
						.numeroGuia(mensaje.getNumeroGuia()).transportista(mensaje.getTransportista())
						.fecha(mensaje.getFecha()).s3Key(mensaje.getS3Key()).estado(mensaje.getEstado())
						.evento(mensaje.getEvento()).build();

				procesadas.add(guiaProcesadaRepository.save(procesada));
				log.info("Guía {} persistida en GUIAS_PROCESADAS", mensaje.getNumeroGuia());

			} catch (Exception e) {
				log.error("Error al persistir la guía {}: {}", mensaje.getNumeroGuia(), e.getMessage());
				guiaColaProducer.enviarAColaErrores(mensaje, e.getMessage());
			}
		}

		log.info("Procesamiento de cola finalizado: {} mensajes persistidos", procesadas.size());
		return procesadas;
	}
}

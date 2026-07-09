package cl.duoc.ejemplo.ms.administracion.archivos.guia;

import java.time.LocalDateTime;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import cl.duoc.ejemplo.ms.administracion.archivos.config.RabbitConfig;
import cl.duoc.ejemplo.ms.administracion.archivos.guia.dto.GuiaErrorMensaje;
import cl.duoc.ejemplo.ms.administracion.archivos.guia.dto.GuiaMensaje;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Productor RabbitMQ: publica las guías en la cola 1 (guias.cola). Si el
 * envío falla, el mensaje se deriva a la cola 2 (guias.errores) con el
 * detalle del error.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuiaColaProducer {

	private final RabbitTemplate rabbitTemplate;

	public void publicarGuia(GuiaMensaje mensaje) {

		try {
			log.info("Publicando guía {} en la cola {}", mensaje.getNumeroGuia(), RabbitConfig.QUEUE_GUIAS);
			rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_GUIAS, RabbitConfig.RK_GUIAS, mensaje);
		} catch (Exception e) {
			log.error("Fallo al publicar en la cola 1, derivando a la cola de errores: {}", e.getMessage());
			enviarAColaErrores(mensaje, e.getMessage());
		}
	}

	public void enviarAColaErrores(GuiaMensaje mensaje, String error) {

		GuiaErrorMensaje errorMensaje =
				GuiaErrorMensaje.builder().guia(mensaje).error(error).timestamp(LocalDateTime.now()).build();

		try {
			rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_GUIAS, RabbitConfig.RK_ERRORES, errorMensaje);
			log.info("Mensaje de error almacenado en la cola {}", RabbitConfig.QUEUE_ERRORES);
		} catch (Exception e) {
			log.error("No fue posible almacenar el mensaje en la cola de errores: {}", e.getMessage());
		}
	}
}

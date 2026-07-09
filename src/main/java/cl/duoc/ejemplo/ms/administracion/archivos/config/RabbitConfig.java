package cl.duoc.ejemplo.ms.administracion.archivos.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Configuración de RabbitMQ (semana 8): cola 1 para guías y cola 2 para
 * mensajes con errores, unidas a un exchange directo.
 */
@Configuration
public class RabbitConfig {

	public static final String EXCHANGE_GUIAS = "guias.exchange";
	public static final String QUEUE_GUIAS = "guias.cola";
	public static final String QUEUE_ERRORES = "guias.errores";
	public static final String RK_GUIAS = "guias.nueva";
	public static final String RK_ERRORES = "guias.error";

	@Bean
	DirectExchange guiasExchange() {
		return new DirectExchange(EXCHANGE_GUIAS, true, false);
	}

	@Bean
	Queue colaGuias() {
		return new Queue(QUEUE_GUIAS, true);
	}

	@Bean
	Queue colaErrores() {
		return new Queue(QUEUE_ERRORES, true);
	}

	@Bean
	Binding bindingGuias() {
		return BindingBuilder.bind(colaGuias()).to(guiasExchange()).with(RK_GUIAS);
	}

	@Bean
	Binding bindingErrores() {
		return BindingBuilder.bind(colaErrores()).to(guiasExchange()).with(RK_ERRORES);
	}

	@Bean
	Jackson2JsonMessageConverter jsonMessageConverter() {

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		return new Jackson2JsonMessageConverter(mapper);
	}

	@Bean
	RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {

		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(converter);
		return template;
	}
}

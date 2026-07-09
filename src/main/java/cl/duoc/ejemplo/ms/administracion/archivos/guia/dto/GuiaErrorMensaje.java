package cl.duoc.ejemplo.ms.administracion.archivos.guia.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mensaje almacenado en la cola 2 (guias.errores) cuando falla el envío o el
 * procesamiento de una guía.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GuiaErrorMensaje {

	private GuiaMensaje guia;
	private String error;
	private LocalDateTime timestamp;
}

package cl.duoc.ejemplo.ms.administracion.archivos.guia.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mensaje publicado en la cola 1 (guias.cola) cada vez que se crea o
 * actualiza una guía de despacho.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GuiaMensaje {

	private Long guiaId;
	private String numeroGuia;
	private String transportista;
	private LocalDate fecha;
	private String s3Key;
	private String estado;
	private String evento;
	private LocalDateTime timestamp;
}

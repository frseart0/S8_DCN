package cl.duoc.ejemplo.ms.administracion.archivos.guia.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GuiaDespachoRequest {

	@NotBlank(message = "El número de guía es obligatorio")
	private String numeroGuia;

	@NotBlank(message = "El transportista es obligatorio")
	private String transportista;

	@NotNull(message = "La fecha es obligatoria")
	private LocalDate fecha;

	private String estado;
}

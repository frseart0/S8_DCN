package cl.duoc.ejemplo.ms.administracion.archivos.guia;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Guía consumida desde la cola 1 de RabbitMQ y persistida en Oracle Cloud.
 * Tabla distinta a GUIAS_DESPACHO (requisito de la semana 8).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "GUIAS_PROCESADAS")
public class GuiaProcesada {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "GUIA_ID")
	private Long guiaId;

	@Column(name = "NUMERO_GUIA")
	private String numeroGuia;

	@Column(name = "TRANSPORTISTA")
	private String transportista;

	@Column(name = "FECHA")
	private LocalDate fecha;

	@Column(name = "S3_KEY")
	private String s3Key;

	@Column(name = "ESTADO")
	private String estado;

	/** Evento que originó el mensaje (CREADA, ACTUALIZADA) */
	@Column(name = "EVENTO")
	private String evento;

	@Column(name = "FECHA_PROCESAMIENTO")
	private LocalDateTime fechaProcesamiento;

	@PrePersist
	void prePersist() {
		if (fechaProcesamiento == null) {
			fechaProcesamiento = LocalDateTime.now();
		}
	}
}

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
 * Guía de despacho. Tabla usada por el CRUD principal (sumativas anteriores).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "GUIAS_DESPACHO")
public class GuiaDespacho {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "NUMERO_GUIA", nullable = false)
	private String numeroGuia;

	@Column(name = "TRANSPORTISTA", nullable = false)
	private String transportista;

	@Column(name = "FECHA", nullable = false)
	private LocalDate fecha;

	@Column(name = "NOMBRE_ARCHIVO")
	private String nombreArchivo;

	/** Clave del objeto en S3: {yyyyMMdd}/{transportista}/{archivo} */
	@Column(name = "S3_KEY")
	private String s3Key;

	@Column(name = "ESTADO")
	private String estado;

	@Column(name = "FECHA_CREACION")
	private LocalDateTime fechaCreacion;

	@PrePersist
	void prePersist() {
		if (fechaCreacion == null) {
			fechaCreacion = LocalDateTime.now();
		}
		if (estado == null) {
			estado = "CREADA";
		}
	}
}

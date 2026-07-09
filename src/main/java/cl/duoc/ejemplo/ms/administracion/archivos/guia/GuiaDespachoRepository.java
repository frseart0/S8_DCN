package cl.duoc.ejemplo.ms.administracion.archivos.guia;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GuiaDespachoRepository extends JpaRepository<GuiaDespacho, Long> {

	List<GuiaDespacho> findByTransportistaIgnoreCase(String transportista);

	List<GuiaDespacho> findByFecha(LocalDate fecha);

	List<GuiaDespacho> findByTransportistaIgnoreCaseAndFecha(String transportista, LocalDate fecha);
}

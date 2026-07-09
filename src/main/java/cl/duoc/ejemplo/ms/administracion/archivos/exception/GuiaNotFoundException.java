package cl.duoc.ejemplo.ms.administracion.archivos.exception;

public class GuiaNotFoundException extends RuntimeException {

	public GuiaNotFoundException(Long id) {
		super("No existe la guía de despacho con id: " + id);
	}
}

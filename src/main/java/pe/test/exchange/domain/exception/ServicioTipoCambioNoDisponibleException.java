package pe.test.exchange.domain.exception;

public class ServicioTipoCambioNoDisponibleException extends RuntimeException {

    public ServicioTipoCambioNoDisponibleException() {
        super("El servicio de tipo de cambio no está disponible, intente nuevamente más tarde");
    }
}

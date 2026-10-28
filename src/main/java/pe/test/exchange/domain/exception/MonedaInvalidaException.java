package pe.test.exchange.domain.exception;

public class MonedaInvalidaException extends RuntimeException {

    public MonedaInvalidaException(String moneda) {
        super("La moneda '%s' no es válida o no está soportada".formatted(moneda));
    }
}

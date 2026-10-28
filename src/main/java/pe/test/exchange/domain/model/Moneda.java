package pe.test.exchange.domain.model;

import pe.test.exchange.domain.exception.MonedaInvalidaException;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Código de moneda ISO 4217: 3 letras, siempre en mayúsculas. El dominio solo valida el formato;
 * si la moneda está soportada o no lo decide el proveedor del tipo de cambio.
 */
public record Moneda(String codigo) {

    private static final Pattern FORMATO = Pattern.compile("[A-Z]{3}");

    public Moneda {
        Objects.requireNonNull(codigo, "codigo");
        String normalizado = codigo.toUpperCase(Locale.ROOT);
        if (!FORMATO.matcher(normalizado).matches()) {
            throw new MonedaInvalidaException(codigo);
        }
        codigo = normalizado;
    }

    public static Moneda de(String codigo) {
        return new Moneda(codigo);
    }

    @Override
    public String toString() {
        return codigo;
    }
}

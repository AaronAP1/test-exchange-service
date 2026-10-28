package pe.test.exchange.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cantidad de dinero: nunca negativa y con {@value #DECIMALES} decimales como máximo. Puede ser cero porque un
 * monto convertido puede redondear a 0.00; que el monto a convertir sea mayor a cero es regla de {@link Conversion}.
 */
public record Monto(BigDecimal valor) {

    public static final int DECIMALES = 2;

    public Monto {
        if (valor == null || valor.signum() < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo");
        }
        if (valor.stripTrailingZeros().scale() > DECIMALES) {
            throw new IllegalArgumentException("El monto admite como máximo " + DECIMALES + " decimales");
        }
    }

    public static Monto de(BigDecimal valor) {
        return new Monto(valor);
    }

    /** Aplica el tipo de cambio y redondea a {@value #DECIMALES} decimales con HALF_UP. */
    public Monto convertir(TipoCambio tipoCambio) {
        return new Monto(valor.multiply(tipoCambio.valor()).setScale(DECIMALES, RoundingMode.HALF_UP));
    }

    public boolean esCero() {
        return valor.signum() == 0;
    }

    // Igualdad por valor numérico: 100 y 100.00 son el mismo monto
    @Override
    public boolean equals(Object otro) {
        return otro instanceof Monto monto && valor.compareTo(monto.valor) == 0;
    }

    @Override
    public int hashCode() {
        return valor.stripTrailingZeros().hashCode();
    }
}

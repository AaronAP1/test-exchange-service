package pe.test.exchange.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cuántas unidades de la moneda destino vale una unidad de la moneda origen. Siempre es positivo y tiene
 * como máximo {@value #DECIMALES} decimales, así el valor que se aplica es el mismo que se guarda y se responde.
 */
public record TipoCambio(BigDecimal valor) {

    public static final int DECIMALES = 10;

    public TipoCambio {
        if (valor != null && valor.scale() > DECIMALES) {
            valor = valor.setScale(DECIMALES, RoundingMode.HALF_UP);
        }
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("El tipo de cambio debe ser mayor a cero");
        }
    }

    public static TipoCambio de(BigDecimal valor) {
        return new TipoCambio(valor);
    }

    // Igualdad por valor numérico: 3.4486 y 3.44860 son el mismo tipo de cambio
    @Override
    public boolean equals(Object otro) {
        return otro instanceof TipoCambio tipoCambio && valor.compareTo(tipoCambio.valor) == 0;
    }

    @Override
    public int hashCode() {
        return valor.stripTrailingZeros().hashCode();
    }
}

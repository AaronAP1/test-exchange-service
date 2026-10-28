package pe.test.exchange.application.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ConversionRequest(
        @NotNull(message = "es obligatorio")
        @Positive(message = "debe ser mayor a cero")
        @Digits(integer = 17, fraction = 2, message = "admite como máximo 17 enteros y 2 decimales")
        BigDecimal monto,

        @NotNull(message = "es obligatoria")
        @Pattern(regexp = CODIGO_MONEDA, message = "debe ser un código de moneda de 3 letras (ej. USD)")
        String monedaOrigen,

        @NotNull(message = "es obligatoria")
        @Pattern(regexp = CODIGO_MONEDA, message = "debe ser un código de moneda de 3 letras (ej. PEN)")
        String monedaDestino
) {
    private static final String CODIGO_MONEDA = "[A-Za-z]{3}";
}

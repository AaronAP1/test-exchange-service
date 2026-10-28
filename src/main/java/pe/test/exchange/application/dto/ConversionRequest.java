package pe.test.exchange.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Datos de la conversión a realizar")
public record ConversionRequest(
        @Schema(description = "Monto a convertir, mayor a cero y con 2 decimales como máximo",
                example = "100", minimum = "0.01")
        @NotNull(message = "es obligatorio")
        @Positive(message = "debe ser mayor a cero")
        @Digits(integer = 17, fraction = 2, message = "admite como máximo 17 enteros y 2 decimales")
        BigDecimal monto,

        @Schema(description = "Código ISO 4217 de la moneda de origen", example = "USD")
        @NotNull(message = "es obligatoria")
        @Pattern(regexp = CODIGO_MONEDA, message = "debe ser un código de moneda de 3 letras (ej. USD)")
        String monedaOrigen,

        @Schema(description = "Código ISO 4217 de la moneda de destino", example = "PEN")
        @NotNull(message = "es obligatoria")
        @Pattern(regexp = CODIGO_MONEDA, message = "debe ser un código de moneda de 3 letras (ej. PEN)")
        String monedaDestino
) {
    private static final String CODIGO_MONEDA = "[A-Za-z]{3}";
}

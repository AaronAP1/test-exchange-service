package pe.test.exchange.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.test.exchange.domain.model.Conversion;

import java.math.BigDecimal;

@Schema(description = "Resultado de la conversión")
public record ConversionResponse(
        @Schema(description = "Monto original", example = "100")
        BigDecimal monto,

        @Schema(description = "Monto convertido, redondeado a 2 decimales (HALF_UP)", example = "344.86")
        BigDecimal montoConvertido,

        @Schema(description = "Código ISO 4217 de la moneda de origen", example = "USD")
        String monedaOrigen,

        @Schema(description = "Código ISO 4217 de la moneda de destino", example = "PEN")
        String monedaDestino,

        @Schema(description = "Tipo de cambio aplicado (1 monedaOrigen = tipoCambio monedaDestino)", example = "3.4486")
        BigDecimal tipoCambio
) {
    public static ConversionResponse desde(Conversion conversion) {
        return new ConversionResponse(
                conversion.getMonto().valor(),
                conversion.getMontoConvertido().valor(),
                conversion.getMonedaOrigen().codigo(),
                conversion.getMonedaDestino().codigo(),
                conversion.getTipoCambio().valor()
        );
    }
}

package pe.test.exchange.application.dto;

import pe.test.exchange.domain.model.Conversion;

import java.math.BigDecimal;

public record ConversionResponse(
        BigDecimal monto,
        BigDecimal montoConvertido,
        String monedaOrigen,
        String monedaDestino,
        BigDecimal tipoCambio
) {
    public static ConversionResponse desde(Conversion conversion) {
        return new ConversionResponse(
                conversion.getMonto(),
                conversion.getMontoConvertido(),
                conversion.getMonedaOrigen(),
                conversion.getMonedaDestino(),
                conversion.getTipoCambio()
        );
    }
}

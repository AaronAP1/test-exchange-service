package pe.test.exchange.infrastructure.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import pe.test.exchange.domain.model.TipoCambio;

import java.math.BigDecimal;

@Converter(autoApply = true)
public class TipoCambioConverter implements AttributeConverter<TipoCambio, BigDecimal> {

    @Override
    public BigDecimal convertToDatabaseColumn(TipoCambio tipoCambio) {
        return tipoCambio == null ? null : tipoCambio.valor();
    }

    @Override
    public TipoCambio convertToEntityAttribute(BigDecimal valor) {
        return valor == null ? null : TipoCambio.de(valor);
    }
}

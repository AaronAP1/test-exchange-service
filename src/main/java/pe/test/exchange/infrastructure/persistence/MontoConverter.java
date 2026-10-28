package pe.test.exchange.infrastructure.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import pe.test.exchange.domain.model.Monto;

import java.math.BigDecimal;

@Converter(autoApply = true)
public class MontoConverter implements AttributeConverter<Monto, BigDecimal> {

    @Override
    public BigDecimal convertToDatabaseColumn(Monto monto) {
        return monto == null ? null : monto.valor();
    }

    @Override
    public Monto convertToEntityAttribute(BigDecimal valor) {
        return valor == null ? null : Monto.de(valor);
    }
}

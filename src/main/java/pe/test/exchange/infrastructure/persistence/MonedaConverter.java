package pe.test.exchange.infrastructure.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import pe.test.exchange.domain.model.Moneda;

// autoApply: Conversion usa Moneda sin referenciar este converter, así el dominio no depende de infraestructura
@Converter(autoApply = true)
public class MonedaConverter implements AttributeConverter<Moneda, String> {

    @Override
    public String convertToDatabaseColumn(Moneda moneda) {
        return moneda == null ? null : moneda.codigo();
    }

    @Override
    public Moneda convertToEntityAttribute(String codigo) {
        return codigo == null ? null : Moneda.de(codigo);
    }
}

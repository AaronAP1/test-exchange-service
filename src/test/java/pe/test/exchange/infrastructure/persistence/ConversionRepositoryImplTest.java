package pe.test.exchange.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.Moneda;
import pe.test.exchange.domain.model.Monto;
import pe.test.exchange.domain.model.TipoCambio;
import pe.test.exchange.domain.repository.ConversionRepository;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ConversionRepositoryImpl.class)
class ConversionRepositoryImplTest {

    @Autowired
    private ConversionRepository conversionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void guardaLaConversionConTodosSusDatos() {
        Conversion encontrada = guardarYReleer(Conversion.crear(Monto.de(new BigDecimal("100")), Moneda.de("USD"),
                Moneda.de("PEN"), TipoCambio.de(new BigDecimal("3.4486"))));

        assertThat(encontrada.getMonto().valor()).isEqualByComparingTo("100");
        assertThat(encontrada.getMonedaOrigen()).isEqualTo(Moneda.de("USD"));
        assertThat(encontrada.getMonedaDestino()).isEqualTo(Moneda.de("PEN"));
        assertThat(encontrada.getTipoCambio().valor()).isEqualByComparingTo("3.4486");
        assertThat(encontrada.getMontoConvertido().valor()).isEqualByComparingTo("344.86");
        assertThat(encontrada.getFechaOperacion()).isNotNull();
    }

    @Test
    void guardaElTipoDeCambioConTodosSusDecimales() {
        Conversion encontrada = guardarYReleer(Conversion.crear(Monto.de(new BigDecimal("1000000")), Moneda.de("VND"),
                Moneda.de("USD"), TipoCambio.de(new BigDecimal("0.0000394117"))));

        assertThat(encontrada.getTipoCambio().valor()).isEqualByComparingTo("0.0000394117");
    }

    @Test
    void guardaElMontoMaximoConUnTipoDeCambioAlto() {
        // 17 enteros, el máximo que acepta la API, por un tipo de cambio de 6 cifras (como KWD -> LBP)
        Conversion encontrada = guardarYReleer(Conversion.crear(Monto.de(new BigDecimal("99999999999999999.99")),
                Moneda.de("KWD"), Moneda.de("LBP"), TipoCambio.de(new BigDecimal("291000"))));

        assertThat(encontrada.getMontoConvertido().valor()).isEqualByComparingTo("29099999999999999997090.00");
    }

    private Conversion guardarYReleer(Conversion conversion) {
        Long id = conversionRepository.guardar(conversion).getId();
        entityManager.flush();
        entityManager.clear();
        return entityManager.find(Conversion.class, id);
    }
}

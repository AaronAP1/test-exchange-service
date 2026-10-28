package pe.test.exchange.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import pe.test.exchange.domain.model.Conversion;
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
        Conversion guardada = conversionRepository.guardar(
                Conversion.crear(new BigDecimal("100"), "USD", "PEN", new BigDecimal("3.4486")));
        entityManager.flush();
        entityManager.clear();

        Conversion encontrada = entityManager.find(Conversion.class, guardada.getId());

        assertThat(encontrada.getMonto()).isEqualByComparingTo("100");
        assertThat(encontrada.getMonedaOrigen()).isEqualTo("USD");
        assertThat(encontrada.getMonedaDestino()).isEqualTo("PEN");
        assertThat(encontrada.getTipoCambio()).isEqualByComparingTo("3.4486");
        assertThat(encontrada.getMontoConvertido()).isEqualByComparingTo("344.86");
        assertThat(encontrada.getFechaOperacion()).isNotNull();
    }
}

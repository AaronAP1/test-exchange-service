package pe.test.exchange.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversionTest {

    private static final BigDecimal TIPO_CAMBIO = new BigDecimal("3.4486");

    @Test
    void calculaElMontoConvertidoConDosDecimales() {
        Conversion conversion = Conversion.crear(new BigDecimal("100"), "USD", "PEN", TIPO_CAMBIO);

        assertThat(conversion.getMonto()).isEqualByComparingTo("100");
        assertThat(conversion.getMonedaOrigen()).isEqualTo("USD");
        assertThat(conversion.getMonedaDestino()).isEqualTo("PEN");
        assertThat(conversion.getTipoCambio()).isEqualByComparingTo("3.4486");
        assertThat(conversion.getMontoConvertido()).isEqualByComparingTo("344.86");
        assertThat(conversion.getMontoConvertido().scale()).isEqualTo(2);
    }

    @Test
    void redondeaHaciaArribaDesdeLaMitad() {
        Conversion conversion = Conversion.crear(BigDecimal.ONE, "USD", "EUR", new BigDecimal("0.125"));

        assertThat(conversion.getMontoConvertido()).isEqualByComparingTo("0.13");
    }

    @Test
    void registraLaFechaDeLaOperacion() {
        LocalDateTime antes = LocalDateTime.now();

        Conversion conversion = Conversion.crear(BigDecimal.TEN, "USD", "PEN", TIPO_CAMBIO);

        assertThat(conversion.getFechaOperacion()).isBetween(antes, LocalDateTime.now());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-10"})
    void rechazaMontosNoPositivos(String monto) {
        assertThatThrownBy(() -> Conversion.crear(new BigDecimal(monto), "USD", "PEN", TIPO_CAMBIO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("monto");
    }

    @Test
    void rechazaUnTipoDeCambioNoPositivo() {
        assertThatThrownBy(() -> Conversion.crear(BigDecimal.TEN, "USD", "PEN", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tipoCambio");
    }
}

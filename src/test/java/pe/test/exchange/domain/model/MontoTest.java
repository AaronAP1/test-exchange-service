package pe.test.exchange.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MontoTest {

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "-10"})
    void rechazaMontosNegativos(String valor) {
        assertThatThrownBy(() -> monto(valor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativo");
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.123", "0.001"})
    void rechazaMasDeDosDecimales(String valor) {
        assertThatThrownBy(() -> monto(valor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 decimales");
    }

    @Test
    void aceptaCerosALaDerechaMasAllaDelSegundoDecimal() {
        // 10.120 vale lo mismo que 10.12: la regla es sobre el valor, no sobre cómo se escribe
        assertThat(monto("10.120").valor()).isEqualByComparingTo("10.12");
    }

    @Test
    void convierteRedondeandoHaciaArribaDesdeLaMitad() {
        Monto convertido = monto("1").convertir(TipoCambio.de(new BigDecimal("0.125")));

        assertThat(convertido.valor()).isEqualTo(new BigDecimal("0.13"));
    }

    @Test
    void unMontoConvertidoPuedeRedondearACero() {
        Monto convertido = monto("0.01").convertir(TipoCambio.de(new BigDecimal("0.0000394117")));

        assertThat(convertido.esCero()).isTrue();
    }

    @Test
    void dosMontosConElMismoValorSonIgualesAunqueCambieLaEscala() {
        assertThat(monto("100"))
                .isEqualTo(monto("100.00"))
                .hasSameHashCodeAs(monto("100.00"));
    }

    private static Monto monto(String valor) {
        return Monto.de(new BigDecimal(valor));
    }
}

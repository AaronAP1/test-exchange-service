package pe.test.exchange.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.test.exchange.domain.exception.MonedaInvalidaException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonedaTest {

    @Test
    void normalizaElCodigoAMayusculas() {
        assertThat(Moneda.de("usd").codigo()).isEqualTo("USD");
    }

    @Test
    void dosMonedasConElMismoCodigoSonIguales() {
        assertThat(Moneda.de("pen"))
                .isEqualTo(Moneda.de("PEN"))
                .hasSameHashCodeAs(Moneda.de("PEN"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "US", "USDD", "U$D", "123"})
    void rechazaCodigosQueNoSonDeTresLetras(String codigo) {
        assertThatThrownBy(() -> Moneda.de(codigo))
                .isInstanceOf(MonedaInvalidaException.class)
                .hasMessageContaining("'" + codigo + "'");
    }
}

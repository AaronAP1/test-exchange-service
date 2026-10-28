package pe.test.exchange.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TipoCambioTest {

    @ParameterizedTest
    @ValueSource(strings = {"0", "-3.4486"})
    void rechazaValoresNoPositivos(String valor) {
        assertThatThrownBy(() -> TipoCambio.de(new BigDecimal(valor)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tipo de cambio");
    }

    @Test
    void conservaLosDecimalesQueTrae() {
        // equals de BigDecimal compara también la escala: 3.4486 no se convierte en 3.4486000000
        assertThat(TipoCambio.de(new BigDecimal("3.4486")).valor()).isEqualTo(new BigDecimal("3.4486"));
    }

    @Test
    void limitaElValorADiezDecimales() {
        assertThat(TipoCambio.de(new BigDecimal("0.000039411764705882")).valor())
                .isEqualTo(new BigDecimal("0.0000394118"));
    }

    @Test
    void dosTiposDeCambioConElMismoValorSonIgualesAunqueCambieLaEscala() {
        assertThat(TipoCambio.de(new BigDecimal("3.4486")))
                .isEqualTo(TipoCambio.de(new BigDecimal("3.44860")))
                .hasSameHashCodeAs(TipoCambio.de(new BigDecimal("3.44860")));
    }
}

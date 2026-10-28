package pe.test.exchange.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversionTest {

    private static final Moneda USD = Moneda.de("USD");
    private static final Moneda PEN = Moneda.de("PEN");
    private static final TipoCambio TIPO_CAMBIO = TipoCambio.de(new BigDecimal("3.4486"));

    @Test
    void calculaElMontoConvertidoConDosDecimales() {
        Conversion conversion = Conversion.crear(monto("100"), USD, PEN, TIPO_CAMBIO);

        assertThat(conversion.getMonto()).isEqualTo(monto("100"));
        assertThat(conversion.getMonedaOrigen()).isEqualTo(USD);
        assertThat(conversion.getMonedaDestino()).isEqualTo(PEN);
        assertThat(conversion.getTipoCambio()).isEqualTo(TIPO_CAMBIO);
        assertThat(conversion.getMontoConvertido().valor()).isEqualByComparingTo("344.86");
        assertThat(conversion.getMontoConvertido().valor().scale()).isEqualTo(2);
    }

    @Test
    void usaTodosLosDecimalesDeUnTipoDeCambioPequeno() {
        Conversion conversion = Conversion.crear(monto("1000000"), Moneda.de("VND"), USD,
                TipoCambio.de(new BigDecimal("0.0000394117")));

        assertThat(conversion.getMontoConvertido()).isEqualTo(monto("39.41"));
    }

    @Test
    void registraLaFechaDeLaOperacion() {
        LocalDateTime antes = LocalDateTime.now();

        Conversion conversion = Conversion.crear(monto("10"), USD, PEN, TIPO_CAMBIO);

        assertThat(conversion.getFechaOperacion()).isBetween(antes, LocalDateTime.now());
    }

    @Test
    void rechazaConvertirUnMontoDeCero() {
        assertThatThrownBy(() -> Conversion.crear(monto("0"), USD, PEN, TIPO_CAMBIO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayor a cero");
    }

    private static Monto monto(String valor) {
        return Monto.de(new BigDecimal(valor));
    }
}

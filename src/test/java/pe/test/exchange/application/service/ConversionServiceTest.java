package pe.test.exchange.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import pe.test.exchange.application.dto.ConversionRequest;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.domain.exception.MonedaInvalidaException;
import pe.test.exchange.domain.exception.ServicioTipoCambioNoDisponibleException;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.Moneda;
import pe.test.exchange.domain.model.TipoCambio;
import pe.test.exchange.domain.repository.ConversionRepository;
import pe.test.exchange.domain.service.ExchangeRateProvider;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversionServiceTest {

    private static final Moneda USD = Moneda.de("USD");
    private static final Moneda PEN = Moneda.de("PEN");
    private static final TipoCambio TIPO_CAMBIO = TipoCambio.de(new BigDecimal("3.4486"));

    @Mock
    private ExchangeRateProvider exchangeRateProvider;

    @Mock
    private ConversionRepository conversionRepository;

    @InjectMocks
    private ConversionService conversionService;

    @Test
    void convierteGuardaYRespondeLaConversion() {
        when(exchangeRateProvider.obtenerTipoCambio(USD, PEN)).thenReturn(TIPO_CAMBIO);
        when(conversionRepository.guardar(any(Conversion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        ConversionResponse response = conversionService.convertir(
                new ConversionRequest(new BigDecimal("100"), "USD", "PEN"));

        assertThat(response.monto()).isEqualByComparingTo("100");
        assertThat(response.montoConvertido()).isEqualByComparingTo("344.86");
        assertThat(response.monedaOrigen()).isEqualTo("USD");
        assertThat(response.monedaDestino()).isEqualTo("PEN");
        assertThat(response.tipoCambio()).isEqualByComparingTo("3.4486");

        ArgumentCaptor<Conversion> guardada = ArgumentCaptor.forClass(Conversion.class);
        verify(conversionRepository).guardar(guardada.capture());
        assertThat(guardada.getValue().getMontoConvertido().valor()).isEqualByComparingTo("344.86");
        assertThat(guardada.getValue().getFechaOperacion()).isNotNull();
    }

    @Test
    void normalizaLasMonedasAMayusculas() {
        when(exchangeRateProvider.obtenerTipoCambio(USD, PEN)).thenReturn(TIPO_CAMBIO);
        when(conversionRepository.guardar(any(Conversion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        ConversionResponse response = conversionService.convertir(
                new ConversionRequest(BigDecimal.TEN, "usd", "Pen"));

        assertThat(response.monedaOrigen()).isEqualTo("USD");
        assertThat(response.monedaDestino()).isEqualTo("PEN");
    }

    @Test
    void noConsultaAlProveedorSiElMontoEsInvalido() {
        // El DTO ya lo rechaza en el controlador; el dominio lo protege igual si el caso de uso se llama desde otro lado
        assertThatThrownBy(() -> conversionService.convertir(
                new ConversionRequest(new BigDecimal("10.123"), "USD", "PEN")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(exchangeRateProvider, conversionRepository);
    }

    @Test
    void noGuardaNadaSiLaMonedaEsInvalida() {
        when(exchangeRateProvider.obtenerTipoCambio(USD, Moneda.de("XYZ"))).thenThrow(new MonedaInvalidaException("XYZ"));

        assertThatThrownBy(() -> conversionService.convertir(new ConversionRequest(BigDecimal.TEN, "USD", "XYZ")))
                .isInstanceOf(MonedaInvalidaException.class);
        verifyNoInteractions(conversionRepository);
    }

    @Test
    void noGuardaNadaSiElServicioDeTipoDeCambioNoEstaDisponible() {
        when(exchangeRateProvider.obtenerTipoCambio(USD, PEN)).thenThrow(new ServicioTipoCambioNoDisponibleException());

        assertThatThrownBy(() -> conversionService.convertir(new ConversionRequest(BigDecimal.TEN, "USD", "PEN")))
                .isInstanceOf(ServicioTipoCambioNoDisponibleException.class);
        verifyNoInteractions(conversionRepository);
    }

    @Test
    void propagaLosErroresAlGuardarLaConversion() {
        when(exchangeRateProvider.obtenerTipoCambio(USD, PEN)).thenReturn(TIPO_CAMBIO);
        when(conversionRepository.guardar(any(Conversion.class)))
                .thenThrow(new DataAccessResourceFailureException("La base de datos no responde"));

        assertThatThrownBy(() -> conversionService.convertir(new ConversionRequest(BigDecimal.TEN, "USD", "PEN")))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }
}

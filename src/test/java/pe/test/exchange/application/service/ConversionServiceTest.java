package pe.test.exchange.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.test.exchange.application.dto.ConversionRequest;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.domain.exception.MonedaInvalidaException;
import pe.test.exchange.domain.model.Conversion;
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

    @Mock
    private ExchangeRateProvider exchangeRateProvider;

    @Mock
    private ConversionRepository conversionRepository;

    @InjectMocks
    private ConversionService conversionService;

    @Test
    void convierteGuardaYRespondeLaConversion() {
        when(exchangeRateProvider.obtenerTipoCambio("USD", "PEN")).thenReturn(new BigDecimal("3.4486"));
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
        assertThat(guardada.getValue().getMontoConvertido()).isEqualByComparingTo("344.86");
        assertThat(guardada.getValue().getFechaOperacion()).isNotNull();
    }

    @Test
    void normalizaLasMonedasAMayusculas() {
        when(exchangeRateProvider.obtenerTipoCambio("USD", "PEN")).thenReturn(new BigDecimal("3.4486"));
        when(conversionRepository.guardar(any(Conversion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        ConversionResponse response = conversionService.convertir(
                new ConversionRequest(BigDecimal.TEN, "usd", "Pen"));

        assertThat(response.monedaOrigen()).isEqualTo("USD");
        assertThat(response.monedaDestino()).isEqualTo("PEN");
    }

    @Test
    void noGuardaNadaSiLaMonedaEsInvalida() {
        when(exchangeRateProvider.obtenerTipoCambio("USD", "XYZ")).thenThrow(new MonedaInvalidaException("XYZ"));

        assertThatThrownBy(() -> conversionService.convertir(new ConversionRequest(BigDecimal.TEN, "USD", "XYZ")))
                .isInstanceOf(MonedaInvalidaException.class);
        verifyNoInteractions(conversionRepository);
    }
}

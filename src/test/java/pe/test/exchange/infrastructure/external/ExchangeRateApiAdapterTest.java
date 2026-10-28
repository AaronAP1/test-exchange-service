package pe.test.exchange.infrastructure.external;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.test.exchange.domain.exception.MonedaInvalidaException;
import pe.test.exchange.domain.exception.ServicioTipoCambioNoDisponibleException;
import pe.test.exchange.infrastructure.external.dto.ExchangeRateApiResponse;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateApiAdapterTest {

    private static final String API_KEY = "test-key";

    @Mock
    private ExchangeRateApiClient client;

    private ExchangeRateApiAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ExchangeRateApiAdapter(client, API_KEY);
    }

    @Test
    void devuelveElTipoDeCambioDeLaMonedaDestino() {
        when(client.getLatestRates(API_KEY, "USD")).thenReturn(exitosa(Map.of("PEN", new BigDecimal("3.4486"))));

        assertThat(adapter.obtenerTipoCambio("USD", "PEN")).isEqualByComparingTo("3.4486");
    }

    @Test
    void lanzaMonedaInvalidaSiLaMonedaDestinoNoExiste() {
        when(client.getLatestRates(API_KEY, "USD")).thenReturn(exitosa(Map.of("PEN", new BigDecimal("3.4486"))));

        assertThatThrownBy(() -> adapter.obtenerTipoCambio("USD", "XYZ"))
                .isInstanceOf(MonedaInvalidaException.class)
                .hasMessageContaining("XYZ");
    }

    @Test
    void lanzaMonedaInvalidaSiLaApiNoSoportaLaMonedaOrigen() {
        when(client.getLatestRates(API_KEY, "XYZ")).thenReturn(error("unsupported-code"));

        assertThatThrownBy(() -> adapter.obtenerTipoCambio("XYZ", "PEN"))
                .isInstanceOf(MonedaInvalidaException.class)
                .hasMessageContaining("XYZ");
    }

    @Test
    void lanzaServicioNoDisponibleSiLaApiRespondeConOtroError() {
        when(client.getLatestRates(API_KEY, "USD")).thenReturn(error("quota-reached"));

        assertThatThrownBy(() -> adapter.obtenerTipoCambio("USD", "PEN"))
                .isInstanceOf(ServicioTipoCambioNoDisponibleException.class);
    }

    @Test
    void lanzaServicioNoDisponibleSiFallaLaLlamada() {
        when(client.getLatestRates(API_KEY, "USD")).thenThrow(FeignException.class);

        assertThatThrownBy(() -> adapter.obtenerTipoCambio("USD", "PEN"))
                .isInstanceOf(ServicioTipoCambioNoDisponibleException.class);
    }

    @Test
    void lanzaServicioNoDisponibleSiLaApiRespondeSinCuerpo() {
        when(client.getLatestRates(API_KEY, "USD")).thenReturn(null);

        assertThatThrownBy(() -> adapter.obtenerTipoCambio("USD", "PEN"))
                .isInstanceOf(ServicioTipoCambioNoDisponibleException.class);
    }

    @Test
    void noLlamaALaApiSiNoHayApiKeyConfigurada() {
        adapter = new ExchangeRateApiAdapter(client, "");

        assertThatThrownBy(() -> adapter.obtenerTipoCambio("USD", "PEN"))
                .isInstanceOf(ServicioTipoCambioNoDisponibleException.class);
        verifyNoInteractions(client);
    }

    private static ExchangeRateApiResponse exitosa(Map<String, BigDecimal> tasas) {
        return new ExchangeRateApiResponse("success", "USD", tasas, null);
    }

    private static ExchangeRateApiResponse error(String tipoError) {
        return new ExchangeRateApiResponse("error", null, null, tipoError);
    }
}

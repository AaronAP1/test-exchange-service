package pe.test.exchange;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.test.exchange.domain.model.Moneda;
import pe.test.exchange.infrastructure.external.ExchangeRateApiClient;
import pe.test.exchange.infrastructure.external.dto.ExchangeRateApiResponse;
import pe.test.exchange.infrastructure.persistence.ConversionJpaRepository;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "exchange-rate.api.key=test-key")
@AutoConfigureMockMvc
class ConversionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConversionJpaRepository conversionJpaRepository;

    @MockitoBean
    private ExchangeRateApiClient exchangeRateApiClient;

    @BeforeEach
    void limpiarBaseDeDatos() {
        conversionJpaRepository.deleteAll();
    }

    @Test
    void convierteYPersisteLaOperacion() throws Exception {
        when(exchangeRateApiClient.getLatestRates("test-key", "USD")).thenReturn(new ExchangeRateApiResponse(
                "success", "USD", Map.of("USD", BigDecimal.ONE, "PEN", new BigDecimal("3.4486")), null));

        mockMvc.perform(post("/api/conversion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto": 100, "monedaOrigen": "USD", "monedaDestino": "PEN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoConvertido").value(344.86))
                .andExpect(jsonPath("$.tipoCambio").value(3.4486));

        assertThat(conversionJpaRepository.findAll()).singleElement().satisfies(conversion -> {
            assertThat(conversion.getMonto().valor()).isEqualByComparingTo("100");
            assertThat(conversion.getMonedaOrigen()).isEqualTo(Moneda.de("USD"));
            assertThat(conversion.getMonedaDestino()).isEqualTo(Moneda.de("PEN"));
            assertThat(conversion.getTipoCambio().valor()).isEqualByComparingTo("3.4486");
            assertThat(conversion.getMontoConvertido().valor()).isEqualByComparingTo("344.86");
            assertThat(conversion.getFechaOperacion()).isNotNull();
        });
    }

    @Test
    void noPersisteNadaSiLaMonedaNoEsSoportada() throws Exception {
        when(exchangeRateApiClient.getLatestRates("test-key", "XYZ"))
                .thenReturn(new ExchangeRateApiResponse("error", null, null, "unsupported-code"));

        mockMvc.perform(post("/api/conversion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto": 100, "monedaOrigen": "XYZ", "monedaDestino": "PEN"}
                                """))
                .andExpect(status().isBadRequest());

        assertThat(conversionJpaRepository.count()).isZero();
    }

    @Test
    void respondeServiceUnavailableSinPersistirSiLaApiDevuelveUnTipoDeCambioInvalido() throws Exception {
        when(exchangeRateApiClient.getLatestRates("test-key", "USD")).thenReturn(new ExchangeRateApiResponse(
                "success", "USD", Map.of("USD", BigDecimal.ONE, "PEN", BigDecimal.ZERO), null));

        mockMvc.perform(post("/api/conversion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto": 100, "monedaOrigen": "USD", "monedaDestino": "PEN"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));

        assertThat(conversionJpaRepository.count()).isZero();
    }
}

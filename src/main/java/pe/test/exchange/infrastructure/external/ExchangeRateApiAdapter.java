package pe.test.exchange.infrastructure.external;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import pe.test.exchange.domain.exception.MonedaInvalidaException;
import pe.test.exchange.domain.exception.ServicioTipoCambioNoDisponibleException;
import pe.test.exchange.domain.service.ExchangeRateProvider;
import pe.test.exchange.infrastructure.external.dto.ExchangeRateApiResponse;

import java.math.BigDecimal;

@Slf4j
@Component
public class ExchangeRateApiAdapter implements ExchangeRateProvider {

    private static final String RESULTADO_EXITOSO = "success";
    private static final String MONEDA_NO_SOPORTADA = "unsupported-code";

    private final ExchangeRateApiClient client;
    private final String apiKey;

    public ExchangeRateApiAdapter(ExchangeRateApiClient client,
                                  @Value("${exchange-rate.api.key}") String apiKey) {
        this.client = client;
        this.apiKey = apiKey;
    }

    @Override
    public BigDecimal obtenerTipoCambio(String monedaOrigen, String monedaDestino) {
        BigDecimal tipoCambio = consultarTasas(monedaOrigen).conversionRates().get(monedaDestino);
        if (tipoCambio == null) {
            throw new MonedaInvalidaException(monedaDestino);
        }
        return tipoCambio;
    }

    private ExchangeRateApiResponse consultarTasas(String monedaOrigen) {
        if (!StringUtils.hasText(apiKey)) {
            log.error("No se configuró la API key de ExchangeRate-API (variable EXCHANGE_API_KEY)");
            throw new ServicioTipoCambioNoDisponibleException();
        }

        ExchangeRateApiResponse response;
        try {
            response = client.getLatestRates(apiKey, monedaOrigen);
        } catch (FeignException ex) {
            // El mensaje de FeignException incluye la URL con la API key, por eso solo se registra el status
            log.error("Falló la llamada a ExchangeRate-API (status {})", ex.status());
            throw new ServicioTipoCambioNoDisponibleException();
        }

        if (response == null) {
            log.error("ExchangeRate-API respondió sin cuerpo");
            throw new ServicioTipoCambioNoDisponibleException();
        }
        if (MONEDA_NO_SOPORTADA.equals(response.errorType())) {
            throw new MonedaInvalidaException(monedaOrigen);
        }
        if (!RESULTADO_EXITOSO.equals(response.result()) || response.conversionRates() == null) {
            log.error("ExchangeRate-API respondió con error: {}", response.errorType());
            throw new ServicioTipoCambioNoDisponibleException();
        }
        return response;
    }
}

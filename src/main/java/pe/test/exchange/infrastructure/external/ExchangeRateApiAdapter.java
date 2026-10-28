package pe.test.exchange.infrastructure.external;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import pe.test.exchange.domain.exception.MonedaInvalidaException;
import pe.test.exchange.domain.exception.ServicioTipoCambioNoDisponibleException;
import pe.test.exchange.domain.model.Moneda;
import pe.test.exchange.domain.model.TipoCambio;
import pe.test.exchange.domain.service.ExchangeRateProvider;
import pe.test.exchange.infrastructure.external.dto.ExchangeRateApiResponse;

import java.math.BigDecimal;

@Slf4j
@Component
public class ExchangeRateApiAdapter implements ExchangeRateProvider {

    static final String RESILIENCIA = "exchangeRateApi";

    private static final String RESULTADO_EXITOSO = "success";
    private static final String MONEDA_NO_SOPORTADA = "unsupported-code";

    private final ExchangeRateApiClient client;
    private final String apiKey;

    public ExchangeRateApiAdapter(ExchangeRateApiClient client,
                                  @Value("${exchange-rate.api.key}") String apiKey) {
        this.client = client;
        this.apiKey = apiKey;
    }

    // Retry envuelve al CircuitBreaker: cada intento cuenta para el circuito y, si está abierto, no se reintenta.
    // Las FeignException llegan sin traducir hasta aquí para que Resilience4j decida; el fallback las traduce.
    @Override
    @Retry(name = RESILIENCIA, fallbackMethod = "servicioNoDisponible")
    @CircuitBreaker(name = RESILIENCIA)
    public TipoCambio obtenerTipoCambio(Moneda monedaOrigen, Moneda monedaDestino) {
        if (!StringUtils.hasText(apiKey)) {
            log.error("No se configuró la API key de ExchangeRate-API (variable EXCHANGE_API_KEY)");
            throw new ServicioTipoCambioNoDisponibleException();
        }

        ExchangeRateApiResponse response = client.getLatestRates(apiKey, monedaOrigen.codigo());
        validarRespuesta(response, monedaOrigen);

        BigDecimal tasa = response.conversionRates().get(monedaDestino.codigo());
        if (tasa == null) {
            throw new MonedaInvalidaException(monedaDestino.codigo());
        }
        // Una tasa así es un error del proveedor, no del cliente: se responde 503, no 400 ni 500
        if (tasa.signum() <= 0) {
            log.error("ExchangeRate-API devolvió un tipo de cambio no positivo para {} -> {}: {}",
                    monedaOrigen, monedaDestino, tasa);
            throw new ServicioTipoCambioNoDisponibleException();
        }
        return TipoCambio.de(tasa);
    }

    private void validarRespuesta(ExchangeRateApiResponse response, Moneda monedaOrigen) {
        if (response == null) {
            log.error("ExchangeRate-API respondió sin cuerpo");
            throw new ServicioTipoCambioNoDisponibleException();
        }
        if (MONEDA_NO_SOPORTADA.equals(response.errorType())) {
            throw new MonedaInvalidaException(monedaOrigen.codigo());
        }
        if (!RESULTADO_EXITOSO.equals(response.result())) {
            log.error("ExchangeRate-API respondió con error: {}", response.errorType());
            throw new ServicioTipoCambioNoDisponibleException();
        }
        // Sin tasas no se puede saber si la moneda destino existe: no se culpa al cliente
        if (CollectionUtils.isEmpty(response.conversionRates())) {
            log.error("ExchangeRate-API respondió sin tipos de cambio");
            throw new ServicioTipoCambioNoDisponibleException();
        }
    }

    private TipoCambio servicioNoDisponible(Moneda monedaOrigen, Moneda monedaDestino, FeignException ex) {
        // El mensaje de FeignException incluye la URL con la API key, por eso solo se registra el status
        log.error("Falló la llamada a ExchangeRate-API (status {})", ex.status());
        throw new ServicioTipoCambioNoDisponibleException();
    }

    private TipoCambio servicioNoDisponible(Moneda monedaOrigen, Moneda monedaDestino, CallNotPermittedException ex) {
        log.warn("Circuito abierto hacia ExchangeRate-API: se rechaza la llamada sin consultar la API");
        throw new ServicioTipoCambioNoDisponibleException();
    }
}

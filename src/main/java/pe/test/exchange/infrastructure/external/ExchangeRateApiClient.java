package pe.test.exchange.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import pe.test.exchange.infrastructure.external.dto.ExchangeRateApiResponse;

// dismiss404: la API responde 404 con un JSON de error (ej. "unsupported-code") que se decodifica en el DTO
@FeignClient(name = "exchangeRateApi", url = "${exchange-rate.api.base-url}", dismiss404 = true)
public interface ExchangeRateApiClient {

    @GetMapping("/{apiKey}/latest/{baseCurrency}")
    ExchangeRateApiResponse getLatestRates(
            @PathVariable("apiKey") String apiKey,
            @PathVariable("baseCurrency") String baseCurrency
    );
}

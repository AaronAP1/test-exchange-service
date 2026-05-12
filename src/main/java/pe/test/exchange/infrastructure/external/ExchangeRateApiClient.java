package pe.test.exchange.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import pe.test.exchange.infrastructure.external.dto.ExchangeRateApiResponse;

@FeignClient(name = "exchangeRateApi", url = "${exchange-rate.api.base-url}")
public interface ExchangeRateApiClient {

    @GetMapping("/{apiKey}/latest/{baseCurrency}")
    ExchangeRateApiResponse getLatestRates(
            @PathVariable("apiKey") String apiKey,
            @PathVariable("baseCurrency") String baseCurrency
    );
}

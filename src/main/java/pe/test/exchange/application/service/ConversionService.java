package pe.test.exchange.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.test.exchange.application.dto.ConversionRequest;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.repository.ConversionRepository;
import pe.test.exchange.domain.service.ExchangeRateProvider;

import java.math.BigDecimal;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ConversionService {

    private final ExchangeRateProvider exchangeRateProvider;
    private final ConversionRepository conversionRepository;

    public ConversionResponse convertir(ConversionRequest request) {
        String monedaOrigen = normalizar(request.monedaOrigen());
        String monedaDestino = normalizar(request.monedaDestino());

        BigDecimal tipoCambio = exchangeRateProvider.obtenerTipoCambio(monedaOrigen, monedaDestino);
        Conversion conversion = Conversion.crear(request.monto(), monedaOrigen, monedaDestino, tipoCambio);

        return ConversionResponse.desde(conversionRepository.guardar(conversion));
    }

    private static String normalizar(String moneda) {
        return moneda.toUpperCase(Locale.ROOT);
    }
}

package pe.test.exchange.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.test.exchange.application.dto.ConversionRequest;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.Moneda;
import pe.test.exchange.domain.model.TipoCambio;
import pe.test.exchange.domain.repository.ConversionRepository;
import pe.test.exchange.domain.service.ExchangeRateProvider;

@Service
@RequiredArgsConstructor
public class ConversionService {

    private final ExchangeRateProvider exchangeRateProvider;
    private final ConversionRepository conversionRepository;

    public ConversionResponse convertir(ConversionRequest request) {
        Moneda monedaOrigen = Moneda.de(request.monedaOrigen());
        Moneda monedaDestino = Moneda.de(request.monedaDestino());

        TipoCambio tipoCambio = exchangeRateProvider.obtenerTipoCambio(monedaOrigen, monedaDestino);
        Conversion conversion = Conversion.crear(request.monto(), monedaOrigen, monedaDestino, tipoCambio);

        return ConversionResponse.desde(conversionRepository.guardar(conversion));
    }
}

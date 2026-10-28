package pe.test.exchange.domain.service;

import pe.test.exchange.domain.model.Moneda;
import pe.test.exchange.domain.model.TipoCambio;

public interface ExchangeRateProvider {

    TipoCambio obtenerTipoCambio(Moneda monedaOrigen, Moneda monedaDestino);
}

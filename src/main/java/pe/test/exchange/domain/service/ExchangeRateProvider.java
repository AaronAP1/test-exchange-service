package pe.test.exchange.domain.service;

import java.math.BigDecimal;

public interface ExchangeRateProvider {

    BigDecimal obtenerTipoCambio(String monedaOrigen, String monedaDestino);
}

package pe.test.exchange.infrastructure.external;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pe.test.exchange.domain.exception.ServicioTipoCambioNoDisponibleException;
import pe.test.exchange.domain.model.Moneda;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba el adaptador con ExchangeRate-API caída: la URL base apunta a un puerto local donde no escucha nadie.
 */
@SpringBootTest(properties = {
        "exchange-rate.api.key=test-key",
        "spring.cloud.openfeign.client.config.default.connect-timeout=500",
        "resilience4j.retry.instances.exchangeRateApi.wait-duration=10ms"
})
class ExchangeRateApiAdapterProveedorCaidoTest {

    @Autowired
    private ExchangeRateApiAdapter adapter;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @DynamicPropertySource
    static void apuntarAUnPuertoSinServidor(DynamicPropertyRegistry registry) {
        int puerto = puertoLibre();
        registry.add("exchange-rate.api.base-url", () -> "http://localhost:" + puerto + "/v6");
    }

    @Test
    void reintentaYRespondeServicioNoDisponibleSiNoHayConexionConLaApi() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(ExchangeRateApiAdapter.RESILIENCIA);
        circuitBreaker.reset();

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(Moneda.de("USD"), Moneda.de("PEN")))
                .isInstanceOf(ServicioTipoCambioNoDisponibleException.class);

        // Sin conexión es un fallo transitorio: se hicieron los 3 intentos y todos cuentan para el circuito
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(3);
    }

    private static int puertoLibre() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}

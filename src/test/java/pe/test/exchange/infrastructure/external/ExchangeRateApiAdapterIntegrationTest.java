package pe.test.exchange.infrastructure.external;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pe.test.exchange.domain.exception.MonedaInvalidaException;
import pe.test.exchange.domain.exception.ServicioTipoCambioNoDisponibleException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba el adaptador con el cliente Feign real contra un servidor HTTP local que imita
 * las respuestas de ExchangeRate-API (incluido el 404 con "unsupported-code").
 */
@SpringBootTest(properties = "exchange-rate.api.key=test-key")
class ExchangeRateApiAdapterIntegrationTest {

    private static final HttpServer SERVIDOR = iniciarServidor();

    private static volatile int statusRespuesta;
    private static volatile String cuerpoRespuesta;
    private static volatile String rutaSolicitada;

    @Autowired
    private ExchangeRateApiAdapter adapter;

    @DynamicPropertySource
    static void configurarUrlBase(DynamicPropertyRegistry registry) {
        registry.add("exchange-rate.api.base-url",
                () -> "http://localhost:" + SERVIDOR.getAddress().getPort() + "/v6");
    }

    @AfterAll
    static void detenerServidor() {
        SERVIDOR.stop(0);
    }

    @Test
    void obtieneElTipoDeCambioDesdeLaApi() {
        responder(200, """
                {"result":"success","base_code":"USD","conversion_rates":{"USD":1,"PEN":3.4486}}
                """);

        assertThat(adapter.obtenerTipoCambio("USD", "PEN")).isEqualByComparingTo("3.4486");
        assertThat(rutaSolicitada).isEqualTo("/v6/test-key/latest/USD");
    }

    @Test
    void traduceEl404DeMonedaNoSoportadaAMonedaInvalida() {
        responder(404, """
                {"result":"error","error-type":"unsupported-code"}
                """);

        assertThatThrownBy(() -> adapter.obtenerTipoCambio("XYZ", "PEN"))
                .isInstanceOf(MonedaInvalidaException.class)
                .hasMessageContaining("XYZ");
    }

    @Test
    void traduceUnaKeyInvalidaAServicioNoDisponible() {
        responder(403, """
                {"result":"error","error-type":"invalid-key"}
                """);

        assertThatThrownBy(() -> adapter.obtenerTipoCambio("USD", "PEN"))
                .isInstanceOf(ServicioTipoCambioNoDisponibleException.class);
    }

    @Test
    void traduceUnErrorDelServidorAServicioNoDisponible() {
        responder(500, "{}");

        assertThatThrownBy(() -> adapter.obtenerTipoCambio("USD", "PEN"))
                .isInstanceOf(ServicioTipoCambioNoDisponibleException.class);
    }

    private static void responder(int status, String cuerpo) {
        statusRespuesta = status;
        cuerpoRespuesta = cuerpo;
    }

    private static HttpServer iniciarServidor() {
        try {
            HttpServer servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            servidor.createContext("/", intercambio -> {
                rutaSolicitada = intercambio.getRequestURI().getPath();
                byte[] cuerpo = cuerpoRespuesta.getBytes(StandardCharsets.UTF_8);
                intercambio.getResponseHeaders().add("Content-Type", "application/json");
                intercambio.sendResponseHeaders(statusRespuesta, cuerpo.length);
                try (OutputStream salida = intercambio.getResponseBody()) {
                    salida.write(cuerpo);
                }
            });
            servidor.start();
            return servidor;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}

package pe.test.exchange.infrastructure.external;

import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pe.test.exchange.domain.exception.MonedaInvalidaException;
import pe.test.exchange.domain.exception.ServicioTipoCambioNoDisponibleException;
import pe.test.exchange.domain.model.Moneda;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba el adaptador con el cliente Feign real y Resilience4j activo contra un servidor HTTP local
 * que imita las respuestas de ExchangeRate-API (incluido el 404 con "unsupported-code").
 */
@SpringBootTest(properties = {
        "exchange-rate.api.key=test-key",
        "spring.cloud.openfeign.client.config.default.read-timeout=300",
        "resilience4j.retry.instances.exchangeRateApi.wait-duration=10ms"
})
class ExchangeRateApiAdapterIntegrationTest {

    private static final String RESPUESTA_EXITOSA = """
            {"result":"success","base_code":"USD","conversion_rates":{"USD":1,"PEN":3.4486}}
            """;

    private static final Moneda USD = Moneda.de("USD");
    private static final Moneda PEN = Moneda.de("PEN");
    private static final Moneda XYZ = Moneda.de("XYZ");

    private static final ConcurrentLinkedDeque<Respuesta> RESPUESTAS = new ConcurrentLinkedDeque<>();
    private static final AtomicInteger PETICIONES = new AtomicInteger();
    private static volatile String rutaSolicitada;

    private static final HttpServer SERVIDOR = iniciarServidor();

    @Autowired
    private ExchangeRateApiAdapter adapter;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @DynamicPropertySource
    static void configurarUrlBase(DynamicPropertyRegistry registry) {
        registry.add("exchange-rate.api.base-url",
                () -> "http://localhost:" + SERVIDOR.getAddress().getPort() + "/v6");
    }

    @AfterAll
    static void detenerServidor() {
        SERVIDOR.stop(0);
    }

    @BeforeEach
    void reiniciar() {
        circuitBreaker().reset();
        PETICIONES.set(0);
        RESPUESTAS.clear();
    }

    @Test
    void obtieneElTipoDeCambioDesdeLaApi() {
        responder(new Respuesta(200, RESPUESTA_EXITOSA));

        assertThat(adapter.obtenerTipoCambio(USD, PEN).valor()).isEqualByComparingTo("3.4486");
        assertThat(rutaSolicitada).isEqualTo("/v6/test-key/latest/USD");
        assertThat(PETICIONES).hasValue(1);
    }

    @Test
    void traduceEl404DeMonedaNoSoportadaSinReintentar() {
        responder(new Respuesta(404, """
                {"result":"error","error-type":"unsupported-code"}
                """));

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(XYZ, PEN))
                .isInstanceOf(MonedaInvalidaException.class)
                .hasMessageContaining("XYZ");
        assertThat(PETICIONES).hasValue(1);
    }

    @Test
    void noReintentaUnaKeyInvalida() {
        responder(new Respuesta(403, """
                {"result":"error","error-type":"invalid-key"}
                """));

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD, PEN))
                .isInstanceOf(ServicioTipoCambioNoDisponibleException.class);
        assertThat(PETICIONES).hasValue(1);
    }

    @Test
    void traduceUnaRespuestaQueNoEsJsonSinReintentar() {
        responder(new Respuesta(200, "<html><body>En mantenimiento</body></html>"));

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD, PEN))
                .isInstanceOf(ServicioTipoCambioNoDisponibleException.class);
        assertThat(PETICIONES).hasValue(1);
    }

    @Test
    void reintentaLosErroresDelServidorAntesDeDarsePorVencido() {
        responder(new Respuesta(500, "{}"));

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD, PEN))
                .isInstanceOf(ServicioTipoCambioNoDisponibleException.class);
        assertThat(PETICIONES).hasValue(3);
    }

    @Test
    void reintentaLosTimeouts() {
        responder(new Respuesta(200, RESPUESTA_EXITOSA, 600));

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD, PEN))
                .isInstanceOf(ServicioTipoCambioNoDisponibleException.class);
        assertThat(PETICIONES).hasValue(3);
    }

    @Test
    void seRecuperaSiElSegundoIntentoFunciona() {
        responder(new Respuesta(503, "{}"), new Respuesta(200, RESPUESTA_EXITOSA));

        assertThat(adapter.obtenerTipoCambio(USD, PEN).valor()).isEqualByComparingTo("3.4486");
        assertThat(PETICIONES).hasValue(2);
    }

    @Test
    void abreElCircuitoTrasFallosRepetidosYDejaDeLlamarALaApi() {
        responder(new Respuesta(500, "{}"));
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD, PEN))
                    .isInstanceOf(ServicioTipoCambioNoDisponibleException.class);
        }
        assertThat(circuitBreaker().getState()).isEqualTo(CircuitBreaker.State.OPEN);
        int peticionesAntes = PETICIONES.get();

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD, PEN))
                .isInstanceOf(ServicioTipoCambioNoDisponibleException.class);
        assertThat(PETICIONES).hasValue(peticionesAntes);
    }

    @Test
    void cierraElCircuitoSiLasLlamadasDePruebaFuncionan() {
        abrirYPasarASemiabierto();
        responder(new Respuesta(200, RESPUESTA_EXITOSA));

        for (int i = 0; i < 2; i++) {
            assertThat(adapter.obtenerTipoCambio(USD, PEN).valor()).isEqualByComparingTo("3.4486");
        }

        assertThat(circuitBreaker().getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(PETICIONES).hasValue(2);
    }

    @Test
    void reabreElCircuitoSiLasLlamadasDePruebaFallan() {
        abrirYPasarASemiabierto();
        responder(new Respuesta(500, "{}"));

        assertThatThrownBy(() -> adapter.obtenerTipoCambio(USD, PEN))
                .isInstanceOf(ServicioTipoCambioNoDisponibleException.class);

        // Los 2 primeros intentos agotan las llamadas de prueba y reabren el circuito: el 3.º ya no llega a la API
        assertThat(circuitBreaker().getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(PETICIONES).hasValue(2);
    }

    @Test
    void unaMonedaInvalidaNoCuentaComoFalloDelCircuito() {
        responder(new Respuesta(404, """
                {"result":"error","error-type":"unsupported-code"}
                """));
        for (int i = 0; i < 6; i++) {
            assertThatThrownBy(() -> adapter.obtenerTipoCambio(XYZ, PEN))
                    .isInstanceOf(MonedaInvalidaException.class);
        }

        assertThat(circuitBreaker().getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker().getMetrics().getNumberOfFailedCalls()).isZero();
    }

    private CircuitBreaker circuitBreaker() {
        return circuitBreakerRegistry.circuitBreaker(ExchangeRateApiAdapter.RESILIENCIA);
    }

    /** Equivale a que el circuito se abrió y ya pasaron los 30 s de espera. */
    private void abrirYPasarASemiabierto() {
        circuitBreaker().transitionToOpenState();
        circuitBreaker().transitionToHalfOpenState();
    }

    /** Encola las respuestas en orden; la última se repite para las peticiones siguientes. */
    private static void responder(Respuesta... respuestas) {
        RESPUESTAS.addAll(List.of(respuestas));
    }

    private static Respuesta siguienteRespuesta() {
        return RESPUESTAS.size() > 1 ? RESPUESTAS.poll() : RESPUESTAS.peek();
    }

    private static HttpServer iniciarServidor() {
        try {
            HttpServer servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            servidor.setExecutor(Executors.newCachedThreadPool());
            servidor.createContext("/", intercambio -> {
                PETICIONES.incrementAndGet();
                rutaSolicitada = intercambio.getRequestURI().getPath();
                Respuesta respuesta = siguienteRespuesta();
                try {
                    Thread.sleep(respuesta.demoraMs());
                    byte[] cuerpo = respuesta.cuerpo().getBytes(StandardCharsets.UTF_8);
                    intercambio.getResponseHeaders().add("Content-Type", "application/json");
                    intercambio.sendResponseHeaders(respuesta.status(), cuerpo.length);
                    try (OutputStream salida = intercambio.getResponseBody()) {
                        salida.write(cuerpo);
                    }
                } catch (IOException | InterruptedException ex) {
                    // El cliente ya cortó la conexión por timeout
                } finally {
                    intercambio.close();
                }
            });
            servidor.start();
            return servidor;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private record Respuesta(int status, String cuerpo, long demoraMs) {
        Respuesta(int status, String cuerpo) {
            this(status, cuerpo, 0);
        }
    }
}

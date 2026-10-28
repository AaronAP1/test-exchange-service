package pe.test.exchange.application.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.application.service.ConversionService;
import pe.test.exchange.domain.exception.MonedaInvalidaException;
import pe.test.exchange.domain.exception.ServicioTipoCambioNoDisponibleException;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversionController.class)
class ConversionControllerTest {

    private static final String URL = "/api/conversion";
    private static final String SOLICITUD_VALIDA = """
            {"monto": 100, "monedaOrigen": "USD", "monedaDestino": "PEN"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConversionService conversionService;

    @Test
    void devuelveLaConversion() throws Exception {
        when(conversionService.convertir(any())).thenReturn(new ConversionResponse(
                new BigDecimal("100"), new BigDecimal("344.86"), "USD", "PEN", new BigDecimal("3.4486")));

        enviar(SOLICITUD_VALIDA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monto").value(100))
                .andExpect(jsonPath("$.montoConvertido").value(344.86))
                .andExpect(jsonPath("$.monedaOrigen").value("USD"))
                .andExpect(jsonPath("$.monedaDestino").value("PEN"))
                .andExpect(jsonPath("$.tipoCambio").value(3.4486));
    }

    @Test
    void rechazaUnaSolicitudConDatosInvalidos() throws Exception {
        enviar("""
                {"monto": -5, "monedaOrigen": "US"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value("La solicitud contiene datos inválidos"))
                .andExpect(jsonPath("$.detalles", contains(
                        "monedaDestino: es obligatoria",
                        "monedaOrigen: debe ser un código de moneda de 3 letras (ej. USD)",
                        "monto: debe ser mayor a cero")));

        verifyNoInteractions(conversionService);
    }

    @Test
    void rechazaUnJsonMalFormado() throws Exception {
        enviar("{\"monto\": \"cien\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El cuerpo de la solicitud no es un JSON válido"));
    }

    @Test
    void respondeBadRequestSiLaMonedaNoEsSoportada() throws Exception {
        when(conversionService.convertir(any())).thenThrow(new MonedaInvalidaException("XYZ"));

        enviar(SOLICITUD_VALIDA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La moneda 'XYZ' no es válida o no está soportada"));
    }

    @Test
    void respondeServiceUnavailableSiElServicioExternoFalla() throws Exception {
        when(conversionService.convertir(any())).thenThrow(new ServicioTipoCambioNoDisponibleException());

        enviar(SOLICITUD_VALIDA)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void conservaElStatusDeLosErroresPropiosDeSpring() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    private ResultActions enviar(String cuerpo) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }
}

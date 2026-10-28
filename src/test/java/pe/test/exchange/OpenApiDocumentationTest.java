package pe.test.exchange;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    private static final String OPERACION = "$.paths['/api/conversion'].post";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicaLaEspecificacionDelEndpointDeConversion() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("API de Tipo de Cambio"))
                .andExpect(jsonPath(OPERACION + ".summary").value("Convierte un monto entre dos monedas"))
                .andExpect(jsonPath(OPERACION + ".responses['200']").exists())
                .andExpect(jsonPath(OPERACION + ".responses['400']").exists())
                .andExpect(jsonPath(OPERACION + ".responses['503']").exists())
                .andExpect(jsonPath("$.components.schemas.ConversionRequest.required",
                        containsInAnyOrder("monto", "monedaOrigen", "monedaDestino")))
                .andExpect(jsonPath("$.components.schemas.ConversionRequest.properties.monedaOrigen.pattern")
                        .value("[A-Za-z]{3}"))
                .andExpect(jsonPath("$.components.schemas.ConversionRequest.properties.monto.minimum")
                        .value(0.01));
    }

    @Test
    void sirveLaInterfazDeSwagger() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}

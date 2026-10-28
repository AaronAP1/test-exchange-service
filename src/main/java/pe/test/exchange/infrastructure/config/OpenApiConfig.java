package pe.test.exchange.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI exchangeServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("API de Tipo de Cambio")
                .version("v1")
                .description("""
                        Convierte montos entre monedas con el tipo de cambio vigente de ExchangeRate-API \
                        y registra cada operación. Las consultas a la API externa usan Retry y Circuit Breaker."""));
    }
}

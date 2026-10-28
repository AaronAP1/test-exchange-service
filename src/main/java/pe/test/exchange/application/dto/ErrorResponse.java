package pe.test.exchange.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Respuesta de error")
public record ErrorResponse(
        @Schema(description = "Fecha y hora del error", example = "2026-10-27T20:28:19.085")
        LocalDateTime timestamp,

        @Schema(description = "Código HTTP", example = "400")
        int status,

        @Schema(description = "Descripción del error", example = "La solicitud contiene datos inválidos")
        String mensaje,

        @Schema(description = "Errores por campo; solo aparece cuando hay errores de validación",
                example = "[\"monto: debe ser mayor a cero\"]")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<String> detalles
) {
    public static ErrorResponse de(int status, String mensaje, List<String> detalles) {
        return new ErrorResponse(LocalDateTime.now(), status, mensaje, detalles);
    }
}

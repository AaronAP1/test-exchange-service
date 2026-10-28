package pe.test.exchange.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String mensaje,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<String> detalles
) {
    public static ErrorResponse de(int status, String mensaje, List<String> detalles) {
        return new ErrorResponse(LocalDateTime.now(), status, mensaje, detalles);
    }
}

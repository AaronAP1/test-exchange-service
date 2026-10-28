package pe.test.exchange.application.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.test.exchange.application.dto.ErrorResponse;
import pe.test.exchange.domain.exception.MonedaInvalidaException;
import pe.test.exchange.domain.exception.ServicioTipoCambioNoDisponibleException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .toList();
        return construir(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos", detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarCuerpoIlegible(HttpMessageNotReadableException ex) {
        return construir(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON válido", List.of());
    }

    @ExceptionHandler(MonedaInvalidaException.class)
    public ResponseEntity<ErrorResponse> manejarMonedaInvalida(MonedaInvalidaException ex) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(ServicioTipoCambioNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> manejarServicioNoDisponible(ServicioTipoCambioNoDisponibleException ex) {
        return construir(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), List.of());
    }

    // El caso de uso responde después de guardar: si el guardado falla, el cliente no recibe una conversión sin registrar
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> manejarErrorDePersistencia(DataAccessException ex) {
        log.error("No se pudo registrar la conversión", ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo registrar la conversión, intente nuevamente más tarde", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarInesperado(Exception ex) {
        // Errores propios de Spring MVC (404, 405, 415...) conservan su status original
        if (ex instanceof org.springframework.web.ErrorResponse errorWeb) {
            return construir(errorWeb.getStatusCode(), errorWeb.getBody().getDetail(), List.of());
        }
        log.error("Error inesperado al procesar la solicitud", ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado", List.of());
    }

    private static ResponseEntity<ErrorResponse> construir(HttpStatusCode status, String mensaje, List<String> detalles) {
        return ResponseEntity.status(status).body(ErrorResponse.de(status.value(), mensaje, detalles));
    }
}

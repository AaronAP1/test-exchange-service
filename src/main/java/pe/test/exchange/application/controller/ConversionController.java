package pe.test.exchange.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.test.exchange.application.dto.ConversionRequest;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.application.dto.ErrorResponse;
import pe.test.exchange.application.service.ConversionService;

@Tag(name = "Conversión", description = "Conversión de montos entre monedas")
@RestController
@RequestMapping("/api/conversion")
@RequiredArgsConstructor
public class ConversionController {

    private final ConversionService conversionService;

    @Operation(
            summary = "Convierte un monto entre dos monedas",
            description = "Obtiene el tipo de cambio vigente de ExchangeRate-API, calcula el monto convertido "
                    + "(2 decimales, redondeo HALF_UP) y registra la operación. Los códigos de moneda "
                    + "se aceptan en mayúsculas o minúsculas.")
    @ApiResponse(responseCode = "200", description = "Conversión realizada y registrada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos, JSON mal formado o moneda no soportada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                    @ExampleObject(name = "Datos inválidos", value = """
                            {"timestamp": "2026-10-27T20:28:19.085", "status": 400,
                             "mensaje": "La solicitud contiene datos inválidos",
                             "detalles": ["monedaDestino: es obligatoria", "monto: debe ser mayor a cero"]}"""),
                    @ExampleObject(name = "Moneda no soportada", value = """
                            {"timestamp": "2026-10-27T20:28:18.679", "status": 400,
                             "mensaje": "La moneda 'XYZ' no es válida o no está soportada"}""")
            }))
    @ApiResponse(responseCode = "503",
            description = "Servicio de tipo de cambio no disponible: API caída, timeout o circuito abierto",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
            @ExampleObject(value = """
                    {"timestamp": "2026-10-27T20:56:21.423", "status": 503,
                     "mensaje": "El servicio de tipo de cambio no está disponible, intente nuevamente más tarde"}""")))
    @ApiResponse(responseCode = "500", description = "No se pudo registrar la conversión o error inesperado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public ConversionResponse convertir(@Valid @RequestBody ConversionRequest request) {
        return conversionService.convertir(request);
    }
}

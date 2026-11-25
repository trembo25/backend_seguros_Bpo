package cl.duocuc.seguros.analista.controller;

import cl.duocuc.seguros.analista.dto.*;
import cl.duocuc.seguros.analista.security.TokenService;
import cl.duocuc.seguros.analista.service.AnalistaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Todo este servicio es solo para ANALISTA
@Tag(name = "Analista", description = "Panel, filtro de siniestros, liquidadores y reasignacion (solo ANALISTA)")
@RestController
@RequestMapping("/analista")
public class AnalistaController {

    private final AnalistaService servicio;
    private final TokenService tokens;

    public AnalistaController(AnalistaService servicio, TokenService tokens) {
        this.servicio = servicio;
        this.tokens = tokens;
    }

    @Operation(summary = "Panel del analista",
            description = "Metricas de todos los siniestros, lista filtrada por estado y liquidadores con su carga. estado: TODOS, EN_REVISION, DOCUMENTACION_PENDIENTE, EN_LIQUIDACION, LIQUIDADO o RECHAZADO")
    @ApiResponse(responseCode = "200", description = "Panel")
    @ApiResponse(responseCode = "400", description = "Estado invalido")
    @ApiResponse(responseCode = "403", description = "El token no es de un analista")
    @GetMapping("/panel")
    public PanelResponse panel(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth,
                               @RequestParam(required = false, defaultValue = "TODOS") String estado) {
        tokens.exigirAnalista(auth);
        return servicio.panel(auth, estado);
    }

    @Operation(summary = "Listar siniestros, con filtro opcional por estado")
    @ApiResponse(responseCode = "200", description = "Lista con el nombre del liquidador asignado")
    @ApiResponse(responseCode = "400", description = "Estado invalido")
    @GetMapping("/siniestros")
    public List<SiniestroAnalista> siniestros(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth,
                                              @RequestParam(required = false, defaultValue = "TODOS") String estado) {
        tokens.exigirAnalista(auth);
        return servicio.siniestros(auth, estado);
    }

    @Operation(summary = "Detalle de un siniestro")
    @ApiResponse(responseCode = "200", description = "Detalle con documentos")
    @ApiResponse(responseCode = "404", description = "Siniestro no encontrado")
    @GetMapping("/siniestros/{numero}")
    public SiniestroDetalleRemoto detalle(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth,
                                          @PathVariable String numero) {
        tokens.exigirAnalista(auth);
        return servicio.detalle(auth, numero);
    }

    @Operation(summary = "Liquidadores con su carga de trabajo")
    @ApiResponse(responseCode = "200", description = "Lista de liquidadores")
    @GetMapping("/liquidadores")
    public List<LiquidadorResumen> liquidadores(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth) {
        tokens.exigirAnalista(auth);
        return servicio.liquidadores(auth);
    }

    @Operation(summary = "Asignar el siniestro a otro liquidador", description = "Body: {\"idLiquidador\": 5}")
    @ApiResponse(responseCode = "200", description = "Siniestro reasignado")
    @ApiResponse(responseCode = "400", description = "Falta idLiquidador o no es un liquidador")
    @ApiResponse(responseCode = "404", description = "Siniestro no encontrado")
    @ApiResponse(responseCode = "409", description = "El siniestro ya esta cerrado")
    @PutMapping("/siniestros/{numero}/asignar")
    public SiniestroAnalista asignar(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth,
                                     @PathVariable String numero,
                                     @Valid @RequestBody AsignarRequest r) {
        tokens.exigirAnalista(auth);
        return servicio.asignar(auth, numero, r.idLiquidador());
    }
}

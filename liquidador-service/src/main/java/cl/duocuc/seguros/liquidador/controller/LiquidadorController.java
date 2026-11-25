package cl.duocuc.seguros.liquidador.controller;

import cl.duocuc.seguros.liquidador.dto.*;
import cl.duocuc.seguros.liquidador.model.EstadoSiniestro;
import cl.duocuc.seguros.liquidador.security.TokenService;
import cl.duocuc.seguros.liquidador.service.LiquidadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Liquidador", description = "Siniestros asignados al liquidador de la sesion (solo LIQUIDADOR)")
@RestController
@RequestMapping("/liquidador/siniestros")
public class LiquidadorController {

    private final LiquidadorService servicio;
    private final TokenService tokens;

    public LiquidadorController(LiquidadorService servicio, TokenService tokens) {
        this.servicio = servicio;
        this.tokens = tokens;
    }

    @Operation(summary = "Listar mis siniestros, con filtro opcional por estado")
    @ApiResponse(responseCode = "200", description = "Lista de siniestros asignados")
    @ApiResponse(responseCode = "403", description = "El token no es de un liquidador")
    @GetMapping
    public List<SiniestroResumen> listar(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth,
                                         @RequestParam(required = false) EstadoSiniestro estado) {
        return servicio.listar(tokens.exigirLiquidador(auth).idUsuario(), estado);
    }

    @Operation(summary = "Detalle de un siniestro asignado")
    @ApiResponse(responseCode = "200", description = "Detalle con documentos")
    @ApiResponse(responseCode = "403", description = "No esta asignado a este liquidador")
    @ApiResponse(responseCode = "404", description = "Siniestro no encontrado")
    @GetMapping("/{numero}")
    public SiniestroDetalle detalle(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth,
                                    @PathVariable String numero) {
        return servicio.detalle(tokens.exigirLiquidador(auth).idUsuario(), numero);
    }

    @Operation(summary = "Crear un siniestro a nombre de un asegurado")
    @ApiResponse(responseCode = "201", description = "Siniestro creado")
    @ApiResponse(responseCode = "400", description = "El usuario no es asegurado")
    @ApiResponse(responseCode = "404", description = "El asegurado no existe")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SiniestroDetalle crear(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth,
                                  @Valid @RequestBody CrearSiniestroRequest r) {
        return servicio.crear(tokens.exigirLiquidador(auth).idUsuario(), auth, r);
    }

    @Operation(summary = "Cambiar el estado del siniestro")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "409", description = "El siniestro ya esta cerrado")
    @PutMapping("/{numero}/estado")
    public SiniestroDetalle cambiarEstado(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth,
                                          @PathVariable String numero,
                                          @Valid @RequestBody CambiarEstadoRequest r) {
        return servicio.cambiarEstado(tokens.exigirLiquidador(auth).idUsuario(), numero, r.estado());
    }

    @Operation(summary = "Solicitar un documento al asegurado (pasa a DOCUMENTACION_PENDIENTE)")
    @ApiResponse(responseCode = "201", description = "Documento solicitado")
    @ApiResponse(responseCode = "409", description = "El siniestro ya esta cerrado")
    @PostMapping("/{numero}/documentos")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentoResponse solicitarDocumento(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth,
                                                @PathVariable String numero,
                                                @Valid @RequestBody SolicitarDocumentoRequest r) {
        return servicio.solicitarDocumento(tokens.exigirLiquidador(auth).idUsuario(), numero, r);
    }

    @Operation(summary = "Marcar un documento como recibido")
    @ApiResponse(responseCode = "200", description = "Documento recibido")
    @ApiResponse(responseCode = "404", description = "Documento no encontrado")
    @PutMapping("/{numero}/documentos/{idDocumento}/recibido")
    public SiniestroDetalle marcarRecibido(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth,
                                           @PathVariable String numero,
                                           @PathVariable Long idDocumento) {
        return servicio.marcarRecibido(tokens.exigirLiquidador(auth).idUsuario(), numero, idDocumento);
    }

    @Operation(summary = "Eliminar una solicitud de documento")
    @ApiResponse(responseCode = "200", description = "Documento eliminado")
    @ApiResponse(responseCode = "404", description = "Documento no encontrado")
    @DeleteMapping("/{numero}/documentos/{idDocumento}")
    public SiniestroDetalle eliminarDocumento(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth,
                                              @PathVariable String numero,
                                              @PathVariable Long idDocumento) {
        return servicio.eliminarDocumento(tokens.exigirLiquidador(auth).idUsuario(), numero, idDocumento);
    }
}

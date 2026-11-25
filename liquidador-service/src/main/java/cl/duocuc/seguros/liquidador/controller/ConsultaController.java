package cl.duocuc.seguros.liquidador.controller;

import cl.duocuc.seguros.liquidador.dto.AsignarLiquidadorRequest;
import cl.duocuc.seguros.liquidador.dto.SiniestroAnalista;
import cl.duocuc.seguros.liquidador.dto.SiniestroDetalle;
import cl.duocuc.seguros.liquidador.dto.SiniestroResumen;
import cl.duocuc.seguros.liquidador.model.EstadoSiniestro;
import cl.duocuc.seguros.liquidador.security.TokenService;
import cl.duocuc.seguros.liquidador.service.ConsultaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// v2: endpoints que consumen usuario-service (asegurado) y analista-service (analista) con Feign.
// El token viaja tal cual, asi que aqui tambien se valida el rol.
@Tag(name = "Consultas", description = "Lecturas para asegurado y analista, y reasignacion de liquidador")
@RestController
@RequestMapping("/liquidador/consultas")
public class ConsultaController {

    private final ConsultaService servicio;
    private final TokenService tokens;

    public ConsultaController(ConsultaService servicio, TokenService tokens) {
        this.servicio = servicio;
        this.tokens = tokens;
    }

    @Operation(summary = "Siniestros del asegurado de la sesion (solo ASEGURADO)")
    @ApiResponse(responseCode = "200", description = "Lista (puede venir vacia)")
    @ApiResponse(responseCode = "403", description = "El token no es de un asegurado")
    @GetMapping("/asegurado/siniestros")
    public List<SiniestroResumen> delAsegurado(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth) {
        return servicio.siniestrosDelAsegurado(tokens.exigirAsegurado(auth).idUsuario());
    }

    @Operation(summary = "Detalle de un siniestro propio (solo ASEGURADO)")
    @ApiResponse(responseCode = "200", description = "Detalle con documentos solicitados")
    @ApiResponse(responseCode = "403", description = "El siniestro no es del asegurado")
    @ApiResponse(responseCode = "404", description = "Siniestro no encontrado")
    @GetMapping("/asegurado/siniestros/{numero}")
    public SiniestroDetalle detalleAsegurado(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth,
                                             @PathVariable String numero) {
        return servicio.detalleDelAsegurado(tokens.exigirAsegurado(auth).idUsuario(), numero);
    }

    @Operation(summary = "Todos los siniestros, con filtro opcional por estado (solo ANALISTA)")
    @ApiResponse(responseCode = "200", description = "Lista de siniestros con su liquidador")
    @ApiResponse(responseCode = "403", description = "El token no es de un analista")
    @GetMapping("/analista/siniestros")
    public List<SiniestroAnalista> todos(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth,
                                         @RequestParam(required = false) EstadoSiniestro estado) {
        tokens.exigirAnalista(auth);
        return servicio.todos(estado);
    }

    @Operation(summary = "Detalle de cualquier siniestro (solo ANALISTA)")
    @ApiResponse(responseCode = "200", description = "Detalle")
    @ApiResponse(responseCode = "404", description = "Siniestro no encontrado")
    @GetMapping("/analista/siniestros/{numero}")
    public SiniestroDetalle detalleAnalista(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth,
                                            @PathVariable String numero) {
        tokens.exigirAnalista(auth);
        return servicio.detalleAnalista(numero);
    }

    @Operation(summary = "Reasignar el siniestro a otro liquidador (solo ANALISTA)")
    @ApiResponse(responseCode = "200", description = "Siniestro reasignado")
    @ApiResponse(responseCode = "400", description = "El usuario indicado no es liquidador")
    @ApiResponse(responseCode = "404", description = "Siniestro o liquidador no encontrado")
    @ApiResponse(responseCode = "409", description = "El siniestro ya esta cerrado")
    @PutMapping("/analista/siniestros/{numero}/liquidador")
    public SiniestroAnalista reasignar(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth,
                                       @PathVariable String numero,
                                       @Valid @RequestBody AsignarLiquidadorRequest r) {
        tokens.exigirAnalista(auth);
        return servicio.reasignar(auth, numero, r.idLiquidador());
    }
}

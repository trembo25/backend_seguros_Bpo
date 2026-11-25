package cl.duocuc.seguros.usuario.controller;

import cl.duocuc.seguros.usuario.dto.PerfilResponse;
import cl.duocuc.seguros.usuario.dto.ResumenAseguradoResponse;
import cl.duocuc.seguros.usuario.dto.SiniestroDetalle;
import cl.duocuc.seguros.usuario.dto.SiniestroResumen;
import cl.duocuc.seguros.usuario.security.TokenService;
import cl.duocuc.seguros.usuario.service.AseguradoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Todo este servicio es solo para ASEGURADO: primero se valida el rol del token y luego se consulta
@Tag(name = "Asegurado", description = "Perfil y siniestros del asegurado de la sesion (solo ASEGURADO)")
@RestController
@RequestMapping("/asegurado")
public class AseguradoController {

    private final AseguradoService servicio;
    private final TokenService tokens;

    public AseguradoController(AseguradoService servicio, TokenService tokens) {
        this.servicio = servicio;
        this.tokens = tokens;
    }

    @Operation(summary = "Mi perfil")
    @ApiResponse(responseCode = "200", description = "Datos del asegurado")
    @ApiResponse(responseCode = "401", description = "Falta el token o es invalido")
    @ApiResponse(responseCode = "403", description = "El token no es de un asegurado")
    @GetMapping("/perfil")
    public PerfilResponse perfil(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth) {
        tokens.exigirAsegurado(auth);
        return servicio.perfil(auth);
    }

    @Operation(summary = "Resumen para la pantalla de inicio", description = "Perfil, total de siniestros, conteo por estado y documentos pendientes")
    @ApiResponse(responseCode = "200", description = "Resumen")
    @GetMapping("/resumen")
    public ResumenAseguradoResponse resumen(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth) {
        tokens.exigirAsegurado(auth);
        return servicio.resumen(auth);
    }

    @Operation(summary = "Mis siniestros")
    @ApiResponse(responseCode = "200", description = "Lista (puede venir vacia)")
    @ApiResponse(responseCode = "502", description = "liquidador-service no responde")
    @GetMapping("/siniestros")
    public List<SiniestroResumen> siniestros(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth) {
        tokens.exigirAsegurado(auth);
        return servicio.misSiniestros(auth);
    }

    @Operation(summary = "Detalle de uno de mis siniestros", description = "Incluye los documentos que pidio el liquidador")
    @ApiResponse(responseCode = "200", description = "Detalle")
    @ApiResponse(responseCode = "403", description = "El siniestro no es del asegurado")
    @ApiResponse(responseCode = "404", description = "Siniestro no encontrado")
    @GetMapping("/siniestros/{numero}")
    public SiniestroDetalle detalle(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String auth,
                                    @PathVariable String numero) {
        tokens.exigirAsegurado(auth);
        return servicio.detalle(auth, numero);
    }
}

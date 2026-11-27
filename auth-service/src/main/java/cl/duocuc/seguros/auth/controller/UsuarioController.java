package cl.duocuc.seguros.auth.controller;

import cl.duocuc.seguros.auth.dto.UsuarioResponse;
import cl.duocuc.seguros.auth.exception.ApiException;
import cl.duocuc.seguros.auth.service.AuthService;
import cl.duocuc.seguros.auth.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Lo usan los otros microservicios (con Feign) para obtener datos de usuarios.
@Tag(name = "Usuarios", description = "Consultas que usan los otros microservicios")
@RestController
@RequestMapping("/auth/usuarios")
public class UsuarioController {

    private final AuthService auth;
    private final JwtService jwt;

    public UsuarioController(AuthService auth, JwtService jwt) {
        this.auth = auth;
        this.jwt = jwt;
    }

    @Operation(summary = "Buscar usuario por id",
            description = "Un ASEGURADO solo puede verse a si mismo; LIQUIDADOR y ANALISTA pueden ver a cualquiera")
    @ApiResponse(responseCode = "200", description = "Usuario encontrado")
    @ApiResponse(responseCode = "403", description = "Asegurado intentando ver a otro usuario")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    @GetMapping("/{id}")
    public UsuarioResponse porId(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable Long id) {
        Long idToken = jwt.idDesdeHeader(authorization);
        String rol = jwt.rolDesdeHeader(authorization);
        if (!"LIQUIDADOR".equals(rol) && !"ANALISTA".equals(rol) && !idToken.equals(id)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No tienes permiso para ver a otro usuario");
        }
        return auth.buscar(id);
    }

    @Operation(summary = "Listar usuarios por rol (solo ANALISTA)",
            description = "Ejemplo: ?rol=LIQUIDADOR. Lo usa analista-service para reasignar siniestros")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios con ese rol")
    @ApiResponse(responseCode = "403", description = "El usuario no es ANALISTA")
    @GetMapping
    public List<UsuarioResponse> porRol(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization,
                                        @RequestParam String rol) {
        if (!"ANALISTA".equals(jwt.rolDesdeHeader(authorization))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo un analista puede listar usuarios");
        }
        return auth.listarPorRol(rol);
    }
    @GetMapping("/pendientes")
    public List<UsuarioResponse> pendientes(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!"ANALISTA".equals(jwt.rolDesdeHeader(authorization))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo un analista puede ver pendientes");
        }
        return auth.listarPendientes();
    }

    @PutMapping("/{id}/aprobar")
    public UsuarioResponse aprobar(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable Long id) {
        if (!"ANALISTA".equals(jwt.rolDesdeHeader(authorization))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo un analista puede aprobar usuarios");
        }
        return auth.aprobarUsuario(id);
    }
}

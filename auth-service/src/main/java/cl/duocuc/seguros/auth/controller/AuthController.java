package cl.duocuc.seguros.auth.controller;

import cl.duocuc.seguros.auth.dto.*;
import cl.duocuc.seguros.auth.service.AuthService;
import cl.duocuc.seguros.auth.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticacion", description = "Registro, login y recuperacion de contrasena")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService auth;
    private final JwtService jwt;

    public AuthController(AuthService auth, JwtService jwt) {
        this.auth = auth;
        this.jwt = jwt;
    }

    @Operation(summary = "Registrar un asegurado", description = "Todo registro publico queda con rol ASEGURADO")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos o contrasena debil")
    @ApiResponse(responseCode = "409", description = "Correo o RUT ya registrado")
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registro(@Valid @RequestBody RegistroRequest r) {
        return auth.registrar(r);
    }

    @Operation(summary = "Iniciar sesion", description = "Devuelve el JWT y los datos del usuario")
    @ApiResponse(responseCode = "200", description = "Login correcto")
    @ApiResponse(responseCode = "401", description = "Correo o contrasena incorrectos")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest r) {
        return auth.login(r);
    }

    @Operation(summary = "Recuperar paso 1: validar respuestas de seguridad",
            description = "Si las 3 respuestas coinciden entrega un resetToken de 10 minutos")
    @ApiResponse(responseCode = "200", description = "Respuestas correctas")
    @ApiResponse(responseCode = "401", description = "Respuestas incorrectas")
    @PostMapping("/recuperar/validar")
    public ResetTokenResponse validar(@Valid @RequestBody ValidarRespuestasRequest r) {
        return auth.validarRespuestas(r);
    }

    @Operation(summary = "Recuperar paso 2: guardar la nueva contrasena con el resetToken")
    @ApiResponse(responseCode = "200", description = "Contrasena actualizada")
    @ApiResponse(responseCode = "401", description = "resetToken invalido o expirado")
    @PostMapping("/recuperar/nueva-contrasena")
    public MensajeResponse nueva(@Valid @RequestBody NuevaContrasenaRequest r) {
        return auth.nuevaContrasena(r);
    }

    @Operation(summary = "Cambiar contrasena (usuario con sesion)")
    @ApiResponse(responseCode = "200", description = "Contrasena actualizada")
    @ApiResponse(responseCode = "401", description = "Falta el token o la contrasena actual no coincide")
    @PutMapping("/cambiar-contrasena")
    public MensajeResponse cambiar(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization,
                                   @Valid @RequestBody CambiarContrasenaRequest r) {
        return auth.cambiarContrasena(jwt.idDesdeHeader(authorization), r);
    }

    @Operation(summary = "Datos del usuario de la sesion")
    @ApiResponse(responseCode = "200", description = "Usuario del token")
    @ApiResponse(responseCode = "401", description = "Falta el token o es invalido")
    @GetMapping("/me")
    public UsuarioResponse me(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization) {
        return auth.buscar(jwt.idDesdeHeader(authorization));
    }
}

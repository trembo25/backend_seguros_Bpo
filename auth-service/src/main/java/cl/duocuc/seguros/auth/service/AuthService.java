package cl.duocuc.seguros.auth.service;

import cl.duocuc.seguros.auth.dto.*;
import cl.duocuc.seguros.auth.exception.ApiException;
import cl.duocuc.seguros.auth.model.Rol;
import cl.duocuc.seguros.auth.model.Usuario;
import cl.duocuc.seguros.auth.repository.RolRepository;
import cl.duocuc.seguros.auth.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UsuarioRepository usuarios, RolRepository roles,
                       PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    // Las respuestas de seguridad se comparan sin importar mayúsculas ni espacios sobrantes
    public static String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
    }

    // Misma regla que usa la app: 8 caracteres, 1 mayúscula, 1 número y 1 carácter especial
    private void validarContrasena(String clave) {
        boolean ok = clave.length() >= 8
                && clave.chars().anyMatch(Character::isUpperCase)
                && clave.chars().anyMatch(Character::isDigit)
                && clave.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
        if (!ok) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "La contraseña debe tener al menos 8 caracteres, 1 mayúscula, 1 número y 1 carácter especial");
        }
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest r) {
        String correo = r.correo().trim().toLowerCase(Locale.ROOT);
        String rut = r.rut().trim();
        if (usuarios.existsByCorreo(correo) || usuarios.existsByRut(rut)) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo o RUT ingresado ya está registrado");
        }
        validarContrasena(r.contrasena());

        // Todo registro público es ASEGURADO. Liquidadores y analistas los crea la empresa.
        Rol rol = roles.findByNombre("ASEGURADO")
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Falta el rol ASEGURADO"));

        Usuario u = new Usuario(r.nombre().trim(), rut, correo, encoder.encode(r.contrasena()), rol);
        u.setRespuestas(
                encoder.encode(normalizar(r.respuesta1())),
                encoder.encode(normalizar(r.respuesta2())),
                encoder.encode(normalizar(r.respuesta3())));
        return UsuarioResponse.de(usuarios.save(u));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest r) {
        Usuario u = usuarios.findByCorreo(r.correo().trim().toLowerCase(Locale.ROOT))
                .filter(Usuario::isActivo)
                .filter(x -> encoder.matches(r.contrasena(), x.getClaveHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos"));
        return new LoginResponse(jwt.generarToken(u), UsuarioResponse.de(u));
    }

    // Paso 1 de recuperar: si las 3 respuestas coinciden, entrega un token corto
    @Transactional(readOnly = true)
    public ResetTokenResponse validarRespuestas(ValidarRespuestasRequest r) {
        Usuario u = usuarios.findByCorreo(r.correo().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Las respuestas no coinciden"));
        boolean ok = u.getRespuesta1Hash() != null
                && encoder.matches(normalizar(r.respuesta1()), u.getRespuesta1Hash())
                && encoder.matches(normalizar(r.respuesta2()), u.getRespuesta2Hash())
                && encoder.matches(normalizar(r.respuesta3()), u.getRespuesta3Hash());
        if (!ok) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Las respuestas no coinciden");
        }
        return new ResetTokenResponse(jwt.generarTokenReset(u));
    }

    // Paso 2 de recuperar: con el token corto se fija la nueva contraseña
    @Transactional
    public MensajeResponse nuevaContrasena(NuevaContrasenaRequest r) {
        Long id = jwt.idDesdeTokenReset(r.resetToken());
        Usuario u = usuarios.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        validarContrasena(r.nuevaContrasena());
        u.setClaveHash(encoder.encode(r.nuevaContrasena()));
        return new MensajeResponse("Contraseña actualizada");
    }

    // Cambiar contraseña desde el panel lateral (sesión iniciada)
    @Transactional
    public MensajeResponse cambiarContrasena(Long idUsuario, CambiarContrasenaRequest r) {
        Usuario u = usuarios.findById(idUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (!encoder.matches(r.contrasenaActual(), u.getClaveHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta");
        }
        validarContrasena(r.nuevaContrasena());
        u.setClaveHash(encoder.encode(r.nuevaContrasena()));
        return new MensajeResponse("Contraseña actualizada");
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) {
        return usuarios.findById(id)
                .map(UsuarioResponse::de)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    // Lista los usuarios activos de un rol (por ejemplo LIQUIDADOR), ordenados por nombre
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarPorRol(String rol) {
        return usuarios.findByRol_NombreAndActivoTrueOrderByNombreAsc(rol.trim().toUpperCase(Locale.ROOT))
                .stream().map(UsuarioResponse::de).toList();
    }
}

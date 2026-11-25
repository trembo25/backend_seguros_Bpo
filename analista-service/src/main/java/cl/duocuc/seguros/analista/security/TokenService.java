package cl.duocuc.seguros.analista.security;

import cl.duocuc.seguros.analista.exception.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

// Solo LEE los tokens que emite auth-service (mismo secreto). No genera tokens.
@Service
public class TokenService {

    private final SecretKey clave;

    public TokenService(@Value("${app.jwt.secret}") String secreto) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    public Sesion leerSesion(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Falta el token de autorización");
        }
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(clave).build()
                    .parseSignedClaims(authorization.substring(7)).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token inválido o expirado");
        }
        if (!"sesion".equals(claims.get("tipo", String.class))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }
        return new Sesion(Long.valueOf(claims.getSubject()),
                claims.get("rol", String.class), claims.get("nombre", String.class));
    }

    // Devuelve la sesión solo si el usuario es LIQUIDADOR
    public Sesion exigirLiquidador(String authorization) {
        Sesion s = leerSesion(authorization);
        if (!"LIQUIDADOR".equals(s.rol())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo un liquidador puede realizar esta acción");
        }
        return s;
    }

    // Devuelve la sesion solo si el usuario es ASEGURADO
    public Sesion exigirAsegurado(String authorization) {
        return exigirRol(authorization, "ASEGURADO", "Solo un asegurado puede realizar esta acción");
    }

    // Devuelve la sesion solo si el usuario es ANALISTA
    public Sesion exigirAnalista(String authorization) {
        return exigirRol(authorization, "ANALISTA", "Solo un analista puede realizar esta acción");
    }

    private Sesion exigirRol(String authorization, String rol, String mensaje) {
        Sesion s = leerSesion(authorization);
        if (!rol.equals(s.rol())) {
            throw new ApiException(HttpStatus.FORBIDDEN, mensaje);
        }
        return s;
    }
}

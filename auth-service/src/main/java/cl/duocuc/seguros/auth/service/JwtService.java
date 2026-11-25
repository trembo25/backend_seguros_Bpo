package cl.duocuc.seguros.auth.service;

import cl.duocuc.seguros.auth.exception.ApiException;
import cl.duocuc.seguros.auth.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey clave;
    private final long minutosSesion;
    private final long minutosReset;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
                      @Value("${app.jwt.expiracion-minutos}") long minutosSesion,
                      @Value("${app.jwt.expiracion-reset-minutos}") long minutosReset) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.minutosSesion = minutosSesion;
        this.minutosReset = minutosReset;
    }

    // Token de sesión: lleva el id, el rol y el nombre (los otros servicios lo usan para autorizar)
    public String generarToken(Usuario u) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(u.getId()))
                .claim("rol", u.getRol().getNombre())
                .claim("nombre", u.getNombre())
                .claim("tipo", "sesion")
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(minutosSesion, ChronoUnit.MINUTES)))
                .signWith(clave)
                .compact();
    }

    // Token corto para el paso "nueva contraseña" después de responder las preguntas
    public String generarTokenReset(Usuario u) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(u.getId()))
                .claim("tipo", "reset")
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(minutosReset, ChronoUnit.MINUTES)))
                .signWith(clave)
                .compact();
    }

    public Claims leer(String token) {
        try {
            return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token inválido o expirado");
        }
    }

    // Lee el id de usuario desde el header "Authorization: Bearer xxx" (solo tokens de sesión)
    public Long idDesdeHeader(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Falta el token de autorización");
        }
        Claims claims = leer(authorization.substring(7));
        if (!"sesion".equals(claims.get("tipo", String.class))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }
        return Long.valueOf(claims.getSubject());
    }

    // Lee el rol desde el header (solo tokens de sesión)
    public String rolDesdeHeader(String authorization) {
        idDesdeHeader(authorization); // valida formato, firma, expiración y tipo
        return leer(authorization.substring(7)).get("rol", String.class);
    }

    // Lee el id de usuario desde un token de recuperación
    public Long idDesdeTokenReset(String token) {
        Claims claims = leer(token);
        if (!"reset".equals(claims.get("tipo", String.class))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token de recuperación inválido");
        }
        return Long.valueOf(claims.getSubject());
    }
}

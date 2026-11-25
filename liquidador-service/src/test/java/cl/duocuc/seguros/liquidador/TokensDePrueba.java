package cl.duocuc.seguros.liquidador;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;

// Genera tokens como lo haría auth-service, para probar sin levantar ese servicio
public final class TokensDePrueba {

    public static final String SECRETO = "secreto-de-prueba-con-mas-de-32-caracteres!!";

    private TokensDePrueba() {}

    public static String token(long id, String rol, String tipo, String secreto, long minutos) {
        return Jwts.builder()
                .subject(String.valueOf(id))
                .claim("rol", rol)
                .claim("nombre", "Usuario " + id)
                .claim("tipo", tipo)
                .expiration(new Date(System.currentTimeMillis() + minutos * 60_000))
                .signWith(Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public static String bearer(long id, String rol) {
        return "Bearer " + token(id, rol, "sesion", SECRETO, 60);
    }
}

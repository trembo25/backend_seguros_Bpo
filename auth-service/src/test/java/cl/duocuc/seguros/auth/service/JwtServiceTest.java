package cl.duocuc.seguros.auth.service;

import cl.duocuc.seguros.auth.exception.ApiException;
import cl.duocuc.seguros.auth.model.Rol;
import cl.duocuc.seguros.auth.model.Usuario;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRETO = "secreto-de-prueba-con-mas-de-32-caracteres!!";

    private JwtService jwt;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        jwt = new JwtService(SECRETO, 60, 10);
        usuario = new Usuario("Camila Rojas", "15842371-5", "camila@correo.cl", "hash", new Rol("ASEGURADO"));
        ReflectionTestUtils.setField(usuario, "id", 7L);
    }

    @Test
    void tokenDeSesionLlevaIdRolYNombre() {
        Claims claims = jwt.leer(jwt.generarToken(usuario));

        assertEquals("7", claims.getSubject());
        assertEquals("ASEGURADO", claims.get("rol", String.class));
        assertEquals("Camila Rojas", claims.get("nombre", String.class));
        assertEquals("sesion", claims.get("tipo", String.class));
    }

    @Test
    void idDesdeHeaderAceptaBearerValido() {
        String header = "Bearer " + jwt.generarToken(usuario);
        assertEquals(7L, jwt.idDesdeHeader(header));
    }

    @Test
    void idDesdeHeaderRechazaHeaderAusente() {
        ApiException e = assertThrows(ApiException.class, () -> jwt.idDesdeHeader(null));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }

    @Test
    void idDesdeHeaderRechazaTokenDeRecuperacion() {
        String header = "Bearer " + jwt.generarTokenReset(usuario);
        ApiException e = assertThrows(ApiException.class, () -> jwt.idDesdeHeader(header));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }

    @Test
    void idDesdeTokenResetAceptaTokenDeRecuperacion() {
        assertEquals(7L, jwt.idDesdeTokenReset(jwt.generarTokenReset(usuario)));
    }

    @Test
    void idDesdeTokenResetRechazaTokenDeSesion() {
        String token = jwt.generarToken(usuario);
        assertThrows(ApiException.class, () -> jwt.idDesdeTokenReset(token));
    }

    @Test
    void rechazaTokenFirmadoConOtraClave() {
        JwtService otro = new JwtService("otra-clave-distinta-de-mas-de-32-caracteres!!", 60, 10);
        String token = otro.generarToken(usuario);
        assertThrows(ApiException.class, () -> jwt.leer(token));
    }

    @Test
    void rechazaTokenExpirado() {
        JwtService vencido = new JwtService(SECRETO, -1, -1);
        String token = vencido.generarToken(usuario);
        assertThrows(ApiException.class, () -> jwt.leer(token));
    }

    @Test
    void rechazaTextoQueNoEsToken() {
        assertThrows(ApiException.class, () -> jwt.leer("esto-no-es-un-jwt"));
    }

    @Test
    void rolDesdeHeaderDevuelveElRolDelToken() {
        assertEquals("ASEGURADO", jwt.rolDesdeHeader("Bearer " + jwt.generarToken(usuario)));
    }

    @Test
    void rolDesdeHeaderRechazaTokenDeRecuperacion() {
        String header = "Bearer " + jwt.generarTokenReset(usuario);
        assertThrows(ApiException.class, () -> jwt.rolDesdeHeader(header));
    }
}

package cl.duocuc.seguros.liquidador.service;

import cl.duocuc.seguros.liquidador.TokensDePrueba;
import cl.duocuc.seguros.liquidador.exception.ApiException;
import cl.duocuc.seguros.liquidador.security.Sesion;
import cl.duocuc.seguros.liquidador.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static cl.duocuc.seguros.liquidador.TokensDePrueba.SECRETO;
import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private final TokenService tokens = new TokenService(SECRETO);

    @Test
    void leeLaSesionDeUnTokenValido() {
        Sesion s = tokens.leerSesion(TokensDePrueba.bearer(4, "LIQUIDADOR"));

        assertEquals(4L, s.idUsuario());
        assertEquals("LIQUIDADOR", s.rol());
        assertEquals("Usuario 4", s.nombre());
    }

    @Test
    void rechazaHeaderAusenteOSinBearer() {
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ApiException.class, () -> tokens.leerSesion(null)).getStatus());
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ApiException.class, () -> tokens.leerSesion("abc")).getStatus());
    }

    @Test
    void rechazaTokenFirmadoConOtroSecreto() {
        String falso = "Bearer " + TokensDePrueba.token(4, "LIQUIDADOR", "sesion",
                "otro-secreto-distinto-de-mas-de-32-caracteres!!", 60);
        assertThrows(ApiException.class, () -> tokens.leerSesion(falso));
    }

    @Test
    void rechazaTokenExpirado() {
        String vencido = "Bearer " + TokensDePrueba.token(4, "LIQUIDADOR", "sesion", SECRETO, -5);
        assertThrows(ApiException.class, () -> tokens.leerSesion(vencido));
    }

    @Test
    void rechazaTokenDeRecuperacion() {
        String reset = "Bearer " + TokensDePrueba.token(4, "LIQUIDADOR", "reset", SECRETO, 10);
        assertThrows(ApiException.class, () -> tokens.leerSesion(reset));
    }

    @Test
    void exigirLiquidadorAceptaLiquidador() {
        assertEquals(4L, tokens.exigirLiquidador(TokensDePrueba.bearer(4, "LIQUIDADOR")).idUsuario());
    }

    @Test
    void exigirLiquidadorRechazaOtrosRolesConForbidden() {
        for (String rol : new String[]{"ASEGURADO", "ANALISTA"}) {
            ApiException e = assertThrows(ApiException.class,
                    () -> tokens.exigirLiquidador(TokensDePrueba.bearer(1, rol)));
            assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
        }
    }
}

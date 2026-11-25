package cl.duocuc.seguros.usuario.controller;

import cl.duocuc.seguros.usuario.TokensDePrueba;
import cl.duocuc.seguros.usuario.dto.PerfilResponse;
import cl.duocuc.seguros.usuario.dto.SiniestroDetalle;
import cl.duocuc.seguros.usuario.dto.SiniestroResumen;
import cl.duocuc.seguros.usuario.exception.ApiException;
import cl.duocuc.seguros.usuario.exception.ManejadorErrores;
import cl.duocuc.seguros.usuario.security.TokenService;
import cl.duocuc.seguros.usuario.service.AseguradoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Prueba el controlador sin levantar Spring: TokenService real + servicio simulado
@ExtendWith(MockitoExtension.class)
class AseguradoControllerTest {

    @Mock AseguradoService servicio;

    private MockMvc mvc;

    private static final String ASEG = TokensDePrueba.bearer(1, "ASEGURADO");
    private static final String LIQ = TokensDePrueba.bearer(4, "LIQUIDADOR");

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders
                .standaloneSetup(new AseguradoController(servicio, new TokenService(TokensDePrueba.SECRETO)))
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @Test
    void perfilDelAsegurado() throws Exception {
        when(servicio.perfil(ASEG)).thenReturn(new PerfilResponse(1L, "Camila", "1-9", "c@c.cl", "ASEGURADO"));

        mvc.perform(get("/asegurado/perfil").header("Authorization", ASEG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Camila"));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/asegurado/perfil"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Falta el token de autorización"));
        verifyNoInteractions(servicio);
    }

    @Test
    void tokenFalsoDevuelve401() throws Exception {
        mvc.perform(get("/asegurado/siniestros").header("Authorization", "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void liquidadorNoPuedeEntrarAlServicioDelAsegurado() throws Exception {
        mvc.perform(get("/asegurado/siniestros").header("Authorization", LIQ))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Solo un asegurado puede realizar esta acción"));
        verifyNoInteractions(servicio);
    }

    @Test
    void listaMisSiniestros() throws Exception {
        when(servicio.misSiniestros(ASEG)).thenReturn(List.of(
                new SiniestroResumen("SIN-1", "Camila", "Choque", "EN_REVISION", LocalDate.of(2026, 11, 1))));

        mvc.perform(get("/asegurado/siniestros").header("Authorization", ASEG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].estado").value("EN_REVISION"));
    }

    @Test
    void detalleDeUnSiniestro() throws Exception {
        when(servicio.detalle(ASEG, "SIN-1")).thenReturn(new SiniestroDetalle("SIN-1", 1L, "Camila", "Choque",
                "desc", "EN_REVISION", LocalDate.of(2026, 11, 1), List.of()));

        mvc.perform(get("/asegurado/siniestros/SIN-1").header("Authorization", ASEG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("SIN-1"));
    }

    @Test
    void detalleAjenoDevuelve403ConMensaje() throws Exception {
        when(servicio.detalle(ASEG, "SIN-9"))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN, "El siniestro no pertenece a este asegurado"));

        mvc.perform(get("/asegurado/siniestros/SIN-9").header("Authorization", ASEG))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("El siniestro no pertenece a este asegurado"));
    }
}

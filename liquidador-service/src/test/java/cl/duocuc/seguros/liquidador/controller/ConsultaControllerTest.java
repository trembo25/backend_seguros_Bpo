package cl.duocuc.seguros.liquidador.controller;

import cl.duocuc.seguros.liquidador.TokensDePrueba;
import cl.duocuc.seguros.liquidador.dto.SiniestroAnalista;
import cl.duocuc.seguros.liquidador.dto.SiniestroResumen;
import cl.duocuc.seguros.liquidador.exception.ManejadorErrores;
import cl.duocuc.seguros.liquidador.model.EstadoSiniestro;
import cl.duocuc.seguros.liquidador.security.TokenService;
import cl.duocuc.seguros.liquidador.service.ConsultaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Prueba las reglas de rol de los endpoints v2 con un TokenService real
@ExtendWith(MockitoExtension.class)
class ConsultaControllerTest {

    @Mock ConsultaService servicio;

    private MockMvc mvc;

    private static final String ASEG = TokensDePrueba.bearer(1, "ASEGURADO");
    private static final String ANA = TokensDePrueba.bearer(6, "ANALISTA");
    private static final String LIQ = TokensDePrueba.bearer(4, "LIQUIDADOR");

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders
                .standaloneSetup(new ConsultaController(servicio, new TokenService(TokensDePrueba.SECRETO)))
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @Test
    void aseguradoListaSusSiniestrosConElIdDelToken() throws Exception {
        when(servicio.siniestrosDelAsegurado(1L)).thenReturn(List.of(
                new SiniestroResumen("SIN-1", "Camila", "Choque", "EN_REVISION", LocalDate.of(2026, 11, 1))));

        mvc.perform(get("/liquidador/consultas/asegurado/siniestros").header("Authorization", ASEG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value("SIN-1"));
    }

    @Test
    void liquidadorNoPuedeUsarLaConsultaDeAsegurado() throws Exception {
        mvc.perform(get("/liquidador/consultas/asegurado/siniestros").header("Authorization", LIQ))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Solo un asegurado puede realizar esta acción"));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/liquidador/consultas/analista/siniestros"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void analistaFiltraPorEstado() throws Exception {
        when(servicio.todos(EstadoSiniestro.EN_REVISION)).thenReturn(List.of(
                new SiniestroAnalista("SIN-1", 1L, "Camila", 4L, "Choque", "EN_REVISION", LocalDate.of(2026, 11, 1), 0)));

        mvc.perform(get("/liquidador/consultas/analista/siniestros").param("estado", "EN_REVISION").header("Authorization", ANA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idLiquidador").value(4));
    }

    @Test
    void estadoInexistenteDevuelve400() throws Exception {
        mvc.perform(get("/liquidador/consultas/analista/siniestros").param("estado", "OTRO").header("Authorization", ANA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void aseguradoNoPuedeVerTodosLosSiniestros() throws Exception {
        mvc.perform(get("/liquidador/consultas/analista/siniestros").header("Authorization", ASEG))
                .andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test
    void analistaReasignaLiquidador() throws Exception {
        when(servicio.reasignar(eq(ANA), eq("SIN-1"), eq(5L))).thenReturn(
                new SiniestroAnalista("SIN-1", 1L, "Camila", 5L, "Choque", "EN_REVISION", LocalDate.of(2026, 11, 1), 0));

        mvc.perform(put("/liquidador/consultas/analista/siniestros/SIN-1/liquidador").header("Authorization", ANA)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"idLiquidador\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idLiquidador").value(5));
    }

    @Test
    void reasignarSinIdDevuelve400() throws Exception {
        mvc.perform(put("/liquidador/consultas/analista/siniestros/SIN-1/liquidador").header("Authorization", ANA)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El idLiquidador es obligatorio"));
        verify(servicio, never()).reasignar(any(), any(), anyLong());
    }
}

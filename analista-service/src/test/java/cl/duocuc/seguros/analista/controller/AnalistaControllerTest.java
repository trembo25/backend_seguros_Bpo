package cl.duocuc.seguros.analista.controller;

import cl.duocuc.seguros.analista.TokensDePrueba;
import cl.duocuc.seguros.analista.dto.*;
import cl.duocuc.seguros.analista.exception.ApiException;
import cl.duocuc.seguros.analista.exception.ManejadorErrores;
import cl.duocuc.seguros.analista.security.TokenService;
import cl.duocuc.seguros.analista.service.AnalistaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Controlador del analista sin levantar Spring: TokenService real + servicio simulado
@ExtendWith(MockitoExtension.class)
class AnalistaControllerTest {

    @Mock AnalistaService servicio;

    private MockMvc mvc;

    private static final String ANA = TokensDePrueba.bearer(6, "ANALISTA");
    private static final String ASEG = TokensDePrueba.bearer(1, "ASEGURADO");

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders
                .standaloneSetup(new AnalistaController(servicio, new TokenService(TokensDePrueba.SECRETO)))
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    private SiniestroAnalista fila(Long idLiquidador, String nombre) {
        return new SiniestroAnalista("SIN-1", 1L, "Camila", idLiquidador, nombre, "Choque", "EN_REVISION",
                LocalDate.of(2026, 11, 1), 0);
    }

    @Test
    void panelUsaTodosPorDefecto() throws Exception {
        when(servicio.panel(ANA, "TODOS")).thenReturn(new PanelResponse(
                new MetricasPanel(1, 1, 0, Map.of("EN_REVISION", 1L)), List.of(fila(4L, "Paulina")), List.of()));

        mvc.perform(get("/analista/panel").header("Authorization", ANA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricas.total").value(1))
                .andExpect(jsonPath("$.siniestros[0].nombreLiquidador").value("Paulina"));
    }

    @Test
    void aseguradoNoPuedeVerElPanel() throws Exception {
        mvc.perform(get("/analista/panel").header("Authorization", ASEG))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Solo un analista puede realizar esta acción"));
        verifyNoInteractions(servicio);
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/analista/liquidadores"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void siniestrosConEstadoInvalidoDevuelve400() throws Exception {
        when(servicio.siniestros(ANA, "CERRADO")).thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "Estado inválido"));

        mvc.perform(get("/analista/siniestros").param("estado", "CERRADO").header("Authorization", ANA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Estado inválido"));
    }

    @Test
    void listaLiquidadores() throws Exception {
        when(servicio.liquidadores(ANA)).thenReturn(List.of(new LiquidadorResumen(4L, "Paulina", "p@a.cl", 2, 3)));

        mvc.perform(get("/analista/liquidadores").header("Authorization", ANA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].siniestrosActivos").value(2));
    }

    @Test
    void asignarDevuelveElSiniestroActualizado() throws Exception {
        when(servicio.asignar(ANA, "SIN-1", 5L)).thenReturn(fila(5L, "Matias"));

        mvc.perform(put("/analista/siniestros/SIN-1/asignar").header("Authorization", ANA)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"idLiquidador\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idLiquidador").value(5))
                .andExpect(jsonPath("$.nombreLiquidador").value("Matias"));
    }

    @Test
    void asignarSinLiquidadorDevuelve400() throws Exception {
        mvc.perform(put("/analista/siniestros/SIN-1/asignar").header("Authorization", ANA)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Debe seleccionar un liquidador"));
        verify(servicio, never()).asignar(any(), any(), anyLong());
    }
}

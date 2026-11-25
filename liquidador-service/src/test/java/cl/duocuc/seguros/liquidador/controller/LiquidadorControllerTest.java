package cl.duocuc.seguros.liquidador.controller;

import cl.duocuc.seguros.liquidador.TokensDePrueba;
import cl.duocuc.seguros.liquidador.dto.*;
import cl.duocuc.seguros.liquidador.exception.ApiException;
import cl.duocuc.seguros.liquidador.exception.ManejadorErrores;
import cl.duocuc.seguros.liquidador.model.EstadoSiniestro;
import cl.duocuc.seguros.liquidador.security.TokenService;
import cl.duocuc.seguros.liquidador.service.LiquidadorService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LiquidadorControllerTest {

    @Mock LiquidadorService servicio;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        // TokenService real: así se prueba también la regla "solo LIQUIDADOR"
        mvc = MockMvcBuilders
                .standaloneSetup(new LiquidadorController(servicio, new TokenService(TokensDePrueba.SECRETO)))
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    private static final String LIQ = TokensDePrueba.bearer(4, "LIQUIDADOR");

    private SiniestroDetalle detalle(String estado) {
        return new SiniestroDetalle("SIN-2026-0001", 1L, "Camila Rojas", "Choque", "desc",
                estado, LocalDate.of(2026, 11, 1), List.of());
    }

    @Test
    void listarDevuelveElPanelDelLiquidador() throws Exception {
        when(servicio.listar(4L, null)).thenReturn(List.of(
                new SiniestroResumen("SIN-2026-0001", "Camila Rojas", "Choque", "EN_REVISION", LocalDate.of(2026, 11, 1))));

        mvc.perform(get("/liquidador/siniestros").header("Authorization", LIQ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value("SIN-2026-0001"))
                .andExpect(jsonPath("$[0].estado").value("EN_REVISION"));
    }

    @Test
    void listarAceptaFiltroPorEstado() throws Exception {
        when(servicio.listar(4L, EstadoSiniestro.LIQUIDADO)).thenReturn(List.of());

        mvc.perform(get("/liquidador/siniestros?estado=LIQUIDADO").header("Authorization", LIQ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listarSinTokenDevuelve401() throws Exception {
        mvc.perform(get("/liquidador/siniestros"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void listarConRolAseguradoDevuelve403() throws Exception {
        mvc.perform(get("/liquidador/siniestros").header("Authorization", TokensDePrueba.bearer(1, "ASEGURADO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void detalleDevuelveElSiniestro() throws Exception {
        when(servicio.detalle(4L, "SIN-2026-0001")).thenReturn(detalle("EN_REVISION"));

        mvc.perform(get("/liquidador/siniestros/SIN-2026-0001").header("Authorization", LIQ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreAsegurado").value("Camila Rojas"));
    }

    @Test
    void detalleAjenoDevuelve403() throws Exception {
        when(servicio.detalle(4L, "SIN-X")).thenThrow(new ApiException(HttpStatus.FORBIDDEN, "El siniestro no está asignado a este liquidador"));

        mvc.perform(get("/liquidador/siniestros/SIN-X").header("Authorization", LIQ))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("El siniestro no está asignado a este liquidador"));
    }

    @Test
    void crearDevuelve201() throws Exception {
        when(servicio.crear(eq(4L), eq(LIQ), any())).thenReturn(detalle("EN_REVISION"));

        mvc.perform(post("/liquidador/siniestros").header("Authorization", LIQ)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idAsegurado\":1,\"tipo\":\"Choque\",\"descripcion\":\"desc\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value("SIN-2026-0001"));
    }

    @Test
    void crearSinTipoDevuelve400() throws Exception {
        mvc.perform(post("/liquidador/siniestros").header("Authorization", LIQ)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idAsegurado\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El tipo de siniestro es obligatorio"));
    }

    @Test
    void cambiarEstadoDevuelveElSiniestroActualizado() throws Exception {
        when(servicio.cambiarEstado(4L, "SIN-2026-0001", EstadoSiniestro.LIQUIDADO)).thenReturn(detalle("LIQUIDADO"));

        mvc.perform(put("/liquidador/siniestros/SIN-2026-0001/estado").header("Authorization", LIQ)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"LIQUIDADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("LIQUIDADO"));
    }

    @Test
    void cambiarEstadoConValorInvalidoDevuelve400() throws Exception {
        mvc.perform(put("/liquidador/siniestros/SIN-2026-0001/estado").header("Authorization", LIQ)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INVENTADO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void solicitarDocumentoDevuelve201() throws Exception {
        when(servicio.solicitarDocumento(eq(4L), eq("SIN-2026-0001"), any()))
                .thenReturn(new DocumentoResponse(1L, "Parte policial", "Adjuntalo", false, LocalDate.of(2026, 11, 2)));

        mvc.perform(post("/liquidador/siniestros/SIN-2026-0001/documentos").header("Authorization", LIQ)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Parte policial\",\"mensaje\":\"Adjuntalo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recibido").value(false));
    }

    @Test
    void solicitarDocumentoSinNombreDevuelve400() throws Exception {
        mvc.perform(post("/liquidador/siniestros/SIN-2026-0001/documentos").header("Authorization", LIQ)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mensaje\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El nombre del documento es obligatorio"));
    }

    @Test
    void marcarRecibidoDevuelveElSiniestro() throws Exception {
        when(servicio.marcarRecibido(4L, "SIN-2026-0001", 1L)).thenReturn(detalle("EN_REVISION"));

        mvc.perform(put("/liquidador/siniestros/SIN-2026-0001/documentos/1/recibido").header("Authorization", LIQ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_REVISION"));
    }

    @Test
    void eliminarDocumentoDevuelveElSiniestro() throws Exception {
        when(servicio.eliminarDocumento(4L, "SIN-2026-0001", 1L)).thenReturn(detalle("EN_REVISION"));

        mvc.perform(delete("/liquidador/siniestros/SIN-2026-0001/documentos/1").header("Authorization", LIQ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_REVISION"));
    }

    @Test
    void eliminarDocumentoConRolAseguradoDevuelve403() throws Exception {
        mvc.perform(delete("/liquidador/siniestros/SIN-2026-0001/documentos/1")
                        .header("Authorization", TokensDePrueba.bearer(1, "ASEGURADO")))
                .andExpect(status().isForbidden());
    }
}

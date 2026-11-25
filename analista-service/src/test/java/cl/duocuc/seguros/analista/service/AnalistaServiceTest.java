package cl.duocuc.seguros.analista.service;

import cl.duocuc.seguros.analista.client.AuthClient;
import cl.duocuc.seguros.analista.client.LiquidadorClient;
import cl.duocuc.seguros.analista.dto.*;
import cl.duocuc.seguros.analista.exception.ApiException;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Logica del analista con los clientes Feign simulados
@ExtendWith(MockitoExtension.class)
class AnalistaServiceTest {

    private static final String TOKEN = "Bearer analista";

    @Mock AuthClient auth;
    @Mock LiquidadorClient liquidador;

    private AnalistaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new AnalistaService(auth, liquidador);
    }

    private static final List<UsuarioRemoto> LIQUIDADORES = List.of(
            new UsuarioRemoto(4L, "Paulina Fernandez", "2-7", "p@a.cl", "LIQUIDADOR"),
            new UsuarioRemoto(5L, "Matias Gallardo", "3-5", "m@a.cl", "LIQUIDADOR"));

    private SiniestroRemoto s(String numero, Long idLiquidador, String estado) {
        return new SiniestroRemoto(numero, 1L, "Camila", idLiquidador, "Choque", estado, LocalDate.of(2026, 11, 1), 0);
    }

    private FeignException feign(int status, String cuerpo) {
        Request req = Request.create(Request.HttpMethod.GET, "/x", Map.of(), null, StandardCharsets.UTF_8, new RequestTemplate());
        return FeignException.errorStatus("x", feign.Response.builder().status(status).reason("x").request(req)
                .headers(Map.of()).body(cuerpo, StandardCharsets.UTF_8).build());
    }

    @Test
    void panelCalculaMetricasYCargaDeCadaLiquidador() {
        when(liquidador.siniestros(TOKEN, null)).thenReturn(List.of(
                s("SIN-1", 4L, "EN_REVISION"), s("SIN-2", 4L, "LIQUIDADO"),
                s("SIN-3", 5L, "DOCUMENTACION_PENDIENTE"), s("SIN-4", 5L, "RECHAZADO")));
        when(auth.usuariosPorRol(TOKEN, "LIQUIDADOR")).thenReturn(LIQUIDADORES);

        PanelResponse p = servicio.panel(TOKEN, "TODOS");

        assertEquals(4, p.metricas().total());
        assertEquals(2, p.metricas().activos());
        assertEquals(2, p.metricas().cerrados());
        assertEquals(1L, p.metricas().porEstado().get("EN_REVISION"));
        assertEquals(0L, p.metricas().porEstado().get("EN_LIQUIDACION"));
        assertEquals(4, p.siniestros().size());
        assertEquals("Paulina Fernandez", p.siniestros().get(0).nombreLiquidador());
        assertEquals(1, p.liquidadores().get(0).siniestrosActivos());
        assertEquals(2, p.liquidadores().get(0).siniestrosTotales());
    }

    @Test
    void panelFiltraLaListaPeroLasMetricasSonDeTodos() {
        when(liquidador.siniestros(TOKEN, null)).thenReturn(List.of(
                s("SIN-1", 4L, "EN_REVISION"), s("SIN-2", 4L, "LIQUIDADO")));
        when(auth.usuariosPorRol(TOKEN, "LIQUIDADOR")).thenReturn(LIQUIDADORES);

        PanelResponse p = servicio.panel(TOKEN, "liquidado");

        assertEquals(2, p.metricas().total());
        assertEquals(1, p.siniestros().size());
        assertEquals("SIN-2", p.siniestros().get(0).numero());
    }

    @Test
    void estadoInvalidoDevuelve400SinLlamarAOtrosServicios() {
        ApiException e = assertThrows(ApiException.class, () -> servicio.siniestros(TOKEN, "CERRADO"));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        verifyNoInteractions(liquidador, auth);
    }

    @Test
    void siniestrosConFiltroLoEnviaALiquidador() {
        when(liquidador.siniestros(TOKEN, "EN_REVISION")).thenReturn(List.of(s("SIN-1", 5L, "EN_REVISION")));
        when(auth.usuariosPorRol(TOKEN, "LIQUIDADOR")).thenReturn(LIQUIDADORES);

        List<SiniestroAnalista> lista = servicio.siniestros(TOKEN, "en_revision");

        assertEquals("Matias Gallardo", lista.get(0).nombreLiquidador());
    }

    @Test
    void liquidadoresSinSiniestrosTienenCeroDeCarga() {
        when(liquidador.siniestros(TOKEN, null)).thenReturn(List.of());
        when(auth.usuariosPorRol(TOKEN, "LIQUIDADOR")).thenReturn(LIQUIDADORES);

        List<LiquidadorResumen> lista = servicio.liquidadores(TOKEN);

        assertEquals(2, lista.size());
        assertEquals(0, lista.get(1).siniestrosTotales());
    }

    @Test
    void asignarAUnLiquidadorValido() {
        when(auth.usuariosPorRol(TOKEN, "LIQUIDADOR")).thenReturn(LIQUIDADORES);
        when(liquidador.reasignar(eq(TOKEN), eq("SIN-1"), any())).thenReturn(s("SIN-1", 5L, "EN_REVISION"));

        SiniestroAnalista r = servicio.asignar(TOKEN, "SIN-1", 5L);

        assertEquals(5L, r.idLiquidador());
        assertEquals("Matias Gallardo", r.nombreLiquidador());
        verify(liquidador).reasignar(TOKEN, "SIN-1", new AsignarRequest(5L));
    }

    @Test
    void asignarAQuienNoEsLiquidadorDevuelve400() {
        when(auth.usuariosPorRol(TOKEN, "LIQUIDADOR")).thenReturn(LIQUIDADORES);

        ApiException e = assertThrows(ApiException.class, () -> servicio.asignar(TOKEN, "SIN-1", 1L));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        verify(liquidador, never()).reasignar(any(), any(), any());
    }

    @Test
    void asignarSiniestroCerradoMantiene409DeLiquidador() {
        when(auth.usuariosPorRol(TOKEN, "LIQUIDADOR")).thenReturn(LIQUIDADORES);
        when(liquidador.reasignar(eq(TOKEN), eq("SIN-2"), any()))
                .thenThrow(feign(409, "{\"error\":\"El siniestro ya está cerrado\"}"));

        ApiException e = assertThrows(ApiException.class, () -> servicio.asignar(TOKEN, "SIN-2", 5L));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals("El siniestro ya está cerrado", e.getMessage());
    }

    @Test
    void authCaidoDevuelve502() {
        when(auth.usuariosPorRol(TOKEN, "LIQUIDADOR")).thenThrow(feign(500, ""));

        ApiException e = assertThrows(ApiException.class, () -> servicio.liquidadores(TOKEN));
        assertEquals(HttpStatus.BAD_GATEWAY, e.getStatus());
    }
}

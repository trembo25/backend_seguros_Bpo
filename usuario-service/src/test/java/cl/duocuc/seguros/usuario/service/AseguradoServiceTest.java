package cl.duocuc.seguros.usuario.service;

import cl.duocuc.seguros.usuario.client.AuthClient;
import cl.duocuc.seguros.usuario.client.LiquidadorClient;
import cl.duocuc.seguros.usuario.dto.*;
import cl.duocuc.seguros.usuario.exception.ApiException;
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
import static org.mockito.Mockito.*;

// Prueba la logica del servicio con los clientes Feign simulados (no hace falta levantar nada)
@ExtendWith(MockitoExtension.class)
class AseguradoServiceTest {

    private static final String TOKEN = "Bearer xyz";

    @Mock AuthClient auth;
    @Mock LiquidadorClient liquidador;

    private AseguradoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new AseguradoService(auth, liquidador);
    }

    private FeignException feign(int status, String cuerpo) {
        Request req = Request.create(Request.HttpMethod.GET, "/x", Map.of(), null, StandardCharsets.UTF_8, new RequestTemplate());
        return FeignException.errorStatus("x", feign.Response.builder().status(status).reason("x").request(req)
                .headers(Map.of()).body(cuerpo, StandardCharsets.UTF_8).build());
    }

    private SiniestroResumen fila(String numero, String estado) {
        return new SiniestroResumen(numero, "Camila", "Choque", estado, LocalDate.of(2026, 11, 1));
    }

    @Test
    void perfilDevuelveLoQueEntregaAuth() {
        when(auth.me(TOKEN)).thenReturn(new PerfilResponse(1L, "Camila", "1-9", "c@c.cl", "ASEGURADO"));

        assertEquals("Camila", servicio.perfil(TOKEN).nombre());
    }

    @Test
    void perfilConAuthCaidoDevuelve502() {
        when(auth.me(TOKEN)).thenThrow(feign(503, ""));

        ApiException e = assertThrows(ApiException.class, () -> servicio.perfil(TOKEN));
        assertEquals(HttpStatus.BAD_GATEWAY, e.getStatus());
        assertEquals("No se pudo consultar auth-service", e.getMessage());
    }

    @Test
    void misSiniestrosReenviaElToken() {
        when(liquidador.misSiniestros(TOKEN)).thenReturn(List.of(fila("SIN-1", "EN_REVISION")));

        assertEquals(1, servicio.misSiniestros(TOKEN).size());
        verify(liquidador).misSiniestros(TOKEN);
    }

    @Test
    void detalleAjenoMantiene403YElMensajeOriginal() {
        when(liquidador.detalle(TOKEN, "SIN-9"))
                .thenThrow(feign(403, "{\"error\":\"El siniestro no pertenece a este asegurado\"}"));

        ApiException e = assertThrows(ApiException.class, () -> servicio.detalle(TOKEN, "SIN-9"));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
        assertEquals("El siniestro no pertenece a este asegurado", e.getMessage());
    }

    @Test
    void detalleInexistenteDevuelve404() {
        when(liquidador.detalle(TOKEN, "NO")).thenThrow(feign(404, "{\"error\":\"Siniestro no encontrado\"}"));

        ApiException e = assertThrows(ApiException.class, () -> servicio.detalle(TOKEN, "NO"));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    @Test
    void resumenCuentaPorEstadoYDocumentosPendientes() {
        when(auth.me(TOKEN)).thenReturn(new PerfilResponse(1L, "Camila", "1-9", "c@c.cl", "ASEGURADO"));
        when(liquidador.misSiniestros(TOKEN)).thenReturn(List.of(
                fila("SIN-1", "EN_REVISION"), fila("SIN-2", "DOCUMENTACION_PENDIENTE"), fila("SIN-3", "EN_REVISION")));
        when(liquidador.detalle(TOKEN, "SIN-2")).thenReturn(new SiniestroDetalle("SIN-2", 1L, "Camila", "Robo", "d",
                "DOCUMENTACION_PENDIENTE", LocalDate.of(2026, 11, 1), List.of(
                new DocumentoResponse(1L, "Parte policial", "m", false, LocalDate.of(2026, 11, 2)),
                new DocumentoResponse(2L, "Fotos", "m", true, LocalDate.of(2026, 11, 2)))));

        ResumenAseguradoResponse r = servicio.resumen(TOKEN);

        assertEquals(3, r.totalSiniestros());
        assertEquals(2L, r.porEstado().get("EN_REVISION"));
        assertEquals(0L, r.porEstado().get("LIQUIDADO"));
        assertEquals(1, r.documentosPendientes());
    }
}

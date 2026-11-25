package cl.duocuc.seguros.liquidador.service;

import cl.duocuc.seguros.liquidador.client.AuthClient;
import cl.duocuc.seguros.liquidador.client.UsuarioRemoto;
import cl.duocuc.seguros.liquidador.dto.SiniestroAnalista;
import cl.duocuc.seguros.liquidador.dto.SiniestroDetalle;
import cl.duocuc.seguros.liquidador.dto.SiniestroResumen;
import cl.duocuc.seguros.liquidador.exception.ApiException;
import cl.duocuc.seguros.liquidador.model.EstadoSiniestro;
import cl.duocuc.seguros.liquidador.model.Siniestro;
import cl.duocuc.seguros.liquidador.repository.SiniestroRepository;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Pruebas de las consultas v2 (asegurado y analista) sin base de datos
@ExtendWith(MockitoExtension.class)
class ConsultaServiceTest {

    private static final String TOKEN = "Bearer analista";

    @Mock SiniestroRepository siniestros;
    @Mock AuthClient auth;

    private ConsultaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ConsultaService(siniestros, auth);
    }

    private Siniestro siniestro(String numero, Long idAsegurado, Long idLiquidador, EstadoSiniestro estado) {
        Siniestro s = new Siniestro(numero, idAsegurado, "Camila Rojas", idLiquidador, "Choque", "desc");
        ReflectionTestUtils.setField(s, "id", 10L);
        s.setEstado(estado);
        return s;
    }

    private FeignException feign(int status) {
        Request req = Request.create(Request.HttpMethod.GET, "/x", Map.of(), null, StandardCharsets.UTF_8, new RequestTemplate());
        return FeignException.errorStatus("usuarioPorId",
                feign.Response.builder().status(status).reason("x").request(req).headers(Map.of()).build());
    }

    // ---------- asegurado ----------

    @Test
    void aseguradoSoloRecibeSusSiniestros() {
        when(siniestros.findByIdAseguradoOrderByFechaCreacionDescIdDesc(1L))
                .thenReturn(List.of(siniestro("SIN-1", 1L, 4L, EstadoSiniestro.EN_REVISION)));

        List<SiniestroResumen> lista = servicio.siniestrosDelAsegurado(1L);

        assertEquals(1, lista.size());
        assertEquals("SIN-1", lista.get(0).numero());
    }

    @Test
    void aseguradoVeElDetalleDeSuSiniestro() {
        when(siniestros.findByNumero("SIN-1")).thenReturn(Optional.of(siniestro("SIN-1", 1L, 4L, EstadoSiniestro.EN_REVISION)));

        SiniestroDetalle d = servicio.detalleDelAsegurado(1L, "SIN-1");

        assertEquals(1L, d.idAsegurado());
    }

    @Test
    void aseguradoNoPuedeVerSiniestroAjeno() {
        when(siniestros.findByNumero("SIN-1")).thenReturn(Optional.of(siniestro("SIN-1", 2L, 4L, EstadoSiniestro.EN_REVISION)));

        ApiException e = assertThrows(ApiException.class, () -> servicio.detalleDelAsegurado(1L, "SIN-1"));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }

    @Test
    void siniestroInexistenteDevuelve404() {
        when(siniestros.findByNumero("NO")).thenReturn(Optional.empty());

        ApiException e = assertThrows(ApiException.class, () -> servicio.detalleDelAsegurado(1L, "NO"));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    // ---------- analista ----------

    @Test
    void analistaSinFiltroVeTodos() {
        when(siniestros.findAllByOrderByFechaCreacionDescIdDesc()).thenReturn(List.of(
                siniestro("SIN-1", 1L, 4L, EstadoSiniestro.EN_REVISION),
                siniestro("SIN-2", 2L, 5L, EstadoSiniestro.LIQUIDADO)));

        List<SiniestroAnalista> lista = servicio.todos(null);

        assertEquals(2, lista.size());
        assertEquals(5L, lista.get(1).idLiquidador());
        verify(siniestros, never()).findByEstadoOrderByFechaCreacionDescIdDesc(any());
    }

    @Test
    void analistaConFiltroUsaElEstado() {
        when(siniestros.findByEstadoOrderByFechaCreacionDescIdDesc(EstadoSiniestro.RECHAZADO)).thenReturn(List.of());

        assertTrue(servicio.todos(EstadoSiniestro.RECHAZADO).isEmpty());
    }

    @Test
    void reasignarCambiaElLiquidador() {
        Siniestro s = siniestro("SIN-1", 1L, 4L, EstadoSiniestro.EN_REVISION);
        when(siniestros.findByNumero("SIN-1")).thenReturn(Optional.of(s));
        when(auth.usuarioPorId(TOKEN, 5L)).thenReturn(new UsuarioRemoto(5L, "Matias", "1-9", "m@a.cl", "LIQUIDADOR"));

        SiniestroAnalista r = servicio.reasignar(TOKEN, "SIN-1", 5L);

        assertEquals(5L, r.idLiquidador());
        assertEquals(5L, s.getIdLiquidador());
    }

    @Test
    void reasignarAUnAseguradoDevuelve400() {
        when(siniestros.findByNumero("SIN-1")).thenReturn(Optional.of(siniestro("SIN-1", 1L, 4L, EstadoSiniestro.EN_REVISION)));
        when(auth.usuarioPorId(TOKEN, 1L)).thenReturn(new UsuarioRemoto(1L, "Camila", "1-9", "c@c.cl", "ASEGURADO"));

        ApiException e = assertThrows(ApiException.class, () -> servicio.reasignar(TOKEN, "SIN-1", 1L));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }

    @Test
    void reasignarSiniestroCerradoDevuelve409() {
        when(siniestros.findByNumero("SIN-1")).thenReturn(Optional.of(siniestro("SIN-1", 1L, 4L, EstadoSiniestro.LIQUIDADO)));

        ApiException e = assertThrows(ApiException.class, () -> servicio.reasignar(TOKEN, "SIN-1", 5L));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        verifyNoInteractions(auth);
    }

    @Test
    void reasignarALiquidadorInexistenteDevuelve404() {
        when(siniestros.findByNumero("SIN-1")).thenReturn(Optional.of(siniestro("SIN-1", 1L, 4L, EstadoSiniestro.EN_REVISION)));
        when(auth.usuarioPorId(TOKEN, 99L)).thenThrow(feign(404));

        ApiException e = assertThrows(ApiException.class, () -> servicio.reasignar(TOKEN, "SIN-1", 99L));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    @Test
    void reasignarConAuthCaidoDevuelve502() {
        when(siniestros.findByNumero("SIN-1")).thenReturn(Optional.of(siniestro("SIN-1", 1L, 4L, EstadoSiniestro.EN_REVISION)));
        when(auth.usuarioPorId(TOKEN, 5L)).thenThrow(feign(503));

        ApiException e = assertThrows(ApiException.class, () -> servicio.reasignar(TOKEN, "SIN-1", 5L));
        assertEquals(HttpStatus.BAD_GATEWAY, e.getStatus());
    }
}

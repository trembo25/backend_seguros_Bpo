package cl.duocuc.seguros.liquidador.service;

import cl.duocuc.seguros.liquidador.client.AuthClient;
import cl.duocuc.seguros.liquidador.client.UsuarioRemoto;
import cl.duocuc.seguros.liquidador.dto.*;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiquidadorServiceTest {

    private static final Long LIQUIDADOR = 4L;
    private static final Long OTRO_LIQUIDADOR = 5L;

    @Mock SiniestroRepository siniestros;
    @Mock AuthClient auth;

    private LiquidadorService servicio;

    @BeforeEach
    void preparar() {
        servicio = new LiquidadorService(siniestros, auth);
    }

    private Siniestro siniestro(String numero, Long idLiquidador, EstadoSiniestro estado) {
        Siniestro s = new Siniestro(numero, 1L, "Camila Rojas", idLiquidador, "Choque", "desc");
        ReflectionTestUtils.setField(s, "id", 10L);
        s.setEstado(estado);
        return s;
    }

    private void existe(Siniestro s) {
        when(siniestros.findByNumero(s.getNumero())).thenReturn(Optional.of(s));
    }

    private FeignException feign(int status) {
        Request req = Request.create(Request.HttpMethod.GET, "/x", Map.of(), null, StandardCharsets.UTF_8, new RequestTemplate());
        return FeignException.errorStatus("usuarioPorId",
                feign.Response.builder().status(status).reason("x").request(req).headers(Map.of()).build());
    }

    // ---------- listar / detalle ----------

    @Test
    void listarSinFiltroDevuelveTodosLosDelLiquidador() {
        when(siniestros.findByIdLiquidadorOrderByFechaCreacionDescIdDesc(LIQUIDADOR))
                .thenReturn(List.of(siniestro("SIN-1", LIQUIDADOR, EstadoSiniestro.EN_REVISION)));

        List<SiniestroResumen> lista = servicio.listar(LIQUIDADOR, null);

        assertEquals(1, lista.size());
        assertEquals("EN_REVISION", lista.get(0).estado());
    }

    @Test
    void listarConFiltroUsaElEstado() {
        when(siniestros.findByIdLiquidadorAndEstadoOrderByFechaCreacionDescIdDesc(LIQUIDADOR, EstadoSiniestro.LIQUIDADO))
                .thenReturn(List.of());

        assertTrue(servicio.listar(LIQUIDADOR, EstadoSiniestro.LIQUIDADO).isEmpty());
        verify(siniestros, never()).findByIdLiquidadorOrderByFechaCreacionDescIdDesc(any());
    }

    @Test
    void detalleDeSiniestroPropio() {
        existe(siniestro("SIN-1", LIQUIDADOR, EstadoSiniestro.EN_REVISION));

        SiniestroDetalle d = servicio.detalle(LIQUIDADOR, "SIN-1");

        assertEquals("SIN-1", d.numero());
        assertEquals("Camila Rojas", d.nombreAsegurado());
    }

    @Test
    void detalleDeSiniestroAjenoEsForbidden() {
        existe(siniestro("SIN-1", OTRO_LIQUIDADOR, EstadoSiniestro.EN_REVISION));

        ApiException e = assertThrows(ApiException.class, () -> servicio.detalle(LIQUIDADOR, "SIN-1"));

        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }

    @Test
    void detalleDeSiniestroInexistenteEsNotFound() {
        when(siniestros.findByNumero("SIN-X")).thenReturn(Optional.empty());

        ApiException e = assertThrows(ApiException.class, () -> servicio.detalle(LIQUIDADOR, "SIN-X"));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    // ---------- crear ----------

    @Test
    void crearValidaAlAseguradoYAsignaNumero() {
        when(auth.usuarioPorId("Bearer t", 1L)).thenReturn(new UsuarioRemoto(1L, "Camila Rojas", "1-9", "c@c.cl", "ASEGURADO"));
        when(siniestros.save(any(Siniestro.class))).thenAnswer(i -> {
            Siniestro s = i.getArgument(0);
            ReflectionTestUtils.setField(s, "id", 12L);
            return s;
        });

        SiniestroDetalle d = servicio.crear(LIQUIDADOR, "Bearer t", new CrearSiniestroRequest(1L, " Choque ", "desc"));

        assertEquals(LiquidadorService.generarNumero(12L), d.numero());
        assertEquals("Camila Rojas", d.nombreAsegurado());
        assertEquals("Choque", d.tipo());
        assertEquals("EN_REVISION", d.estado());
    }

    @Test
    void crearRechazaSiElUsuarioNoEsAsegurado() {
        when(auth.usuarioPorId(any(), eq(4L))).thenReturn(new UsuarioRemoto(4L, "Paulina", "1-1", "p@a.cl", "LIQUIDADOR"));

        ApiException e = assertThrows(ApiException.class,
                () -> servicio.crear(LIQUIDADOR, "Bearer t", new CrearSiniestroRequest(4L, "Choque", null)));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        verify(siniestros, never()).save(any());
    }

    @Test
    void crearRechazaAseguradoInexistente() {
        when(auth.usuarioPorId(any(), eq(99L))).thenThrow(feign(404));

        ApiException e = assertThrows(ApiException.class,
                () -> servicio.crear(LIQUIDADOR, "Bearer t", new CrearSiniestroRequest(99L, "Choque", null)));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    @Test
    void crearDevuelveBadGatewaySiAuthFalla() {
        when(auth.usuarioPorId(any(), any())).thenThrow(feign(500));

        ApiException e = assertThrows(ApiException.class,
                () -> servicio.crear(LIQUIDADOR, "Bearer t", new CrearSiniestroRequest(1L, "Choque", null)));

        assertEquals(HttpStatus.BAD_GATEWAY, e.getStatus());
    }

    @Test
    void generarNumeroTieneFormatoConRelleno() {
        assertTrue(LiquidadorService.generarNumero(7L).matches("SIN-\\d{4}-0007"));
    }

    // ---------- cambiar estado ----------

    @Test
    void cambiarEstadoActualizaElSiniestro() {
        existe(siniestro("SIN-1", LIQUIDADOR, EstadoSiniestro.EN_REVISION));

        SiniestroDetalle d = servicio.cambiarEstado(LIQUIDADOR, "SIN-1", EstadoSiniestro.EN_LIQUIDACION);

        assertEquals("EN_LIQUIDACION", d.estado());
    }

    @Test
    void cambiarEstadoDeSiniestroCerradoEsConflicto() {
        existe(siniestro("SIN-1", LIQUIDADOR, EstadoSiniestro.LIQUIDADO));

        ApiException e = assertThrows(ApiException.class,
                () -> servicio.cambiarEstado(LIQUIDADOR, "SIN-1", EstadoSiniestro.EN_REVISION));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void cambiarEstadoDeSiniestroAjenoEsForbidden() {
        existe(siniestro("SIN-1", OTRO_LIQUIDADOR, EstadoSiniestro.EN_REVISION));

        assertThrows(ApiException.class,
                () -> servicio.cambiarEstado(LIQUIDADOR, "SIN-1", EstadoSiniestro.LIQUIDADO));
    }

    // ---------- documentos ----------

    @Test
    void solicitarDocumentoDejaElSiniestroConDocumentacionPendiente() {
        Siniestro s = siniestro("SIN-1", LIQUIDADOR, EstadoSiniestro.EN_REVISION);
        existe(s);

        DocumentoResponse d = servicio.solicitarDocumento(LIQUIDADOR, "SIN-1",
                new SolicitarDocumentoRequest(" Parte policial ", "Adjuntalo por favor"));

        assertEquals("Parte policial", d.nombre());
        assertFalse(d.recibido());
        assertEquals(EstadoSiniestro.DOCUMENTACION_PENDIENTE, s.getEstado());
        assertEquals(1, s.getDocumentos().size());
    }

    @Test
    void solicitarDocumentoEnSiniestroCerradoEsConflicto() {
        existe(siniestro("SIN-1", LIQUIDADOR, EstadoSiniestro.RECHAZADO));

        ApiException e = assertThrows(ApiException.class, () -> servicio.solicitarDocumento(LIQUIDADOR, "SIN-1",
                new SolicitarDocumentoRequest("Foto", null)));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void marcarUnicoDocumentoRecibidoVuelveAEnRevision() {
        Siniestro s = siniestro("SIN-1", LIQUIDADOR, EstadoSiniestro.DOCUMENTACION_PENDIENTE);
        ReflectionTestUtils.setField(s.solicitarDocumento("Parte policial", null), "id", 1L);
        existe(s);

        SiniestroDetalle d = servicio.marcarRecibido(LIQUIDADOR, "SIN-1", 1L);

        assertEquals("EN_REVISION", d.estado());
        assertTrue(d.documentos().get(0).recibido());
    }

    @Test
    void conOtroDocumentoPendienteSigueEnDocumentacionPendiente() {
        Siniestro s = siniestro("SIN-1", LIQUIDADOR, EstadoSiniestro.DOCUMENTACION_PENDIENTE);
        ReflectionTestUtils.setField(s.solicitarDocumento("Parte policial", null), "id", 1L);
        ReflectionTestUtils.setField(s.solicitarDocumento("Fotos", null), "id", 2L);
        existe(s);

        SiniestroDetalle d = servicio.marcarRecibido(LIQUIDADOR, "SIN-1", 1L);

        assertEquals("DOCUMENTACION_PENDIENTE", d.estado());
    }

    @Test
    void marcarDocumentoInexistenteEsNotFound() {
        existe(siniestro("SIN-1", LIQUIDADOR, EstadoSiniestro.DOCUMENTACION_PENDIENTE));

        ApiException e = assertThrows(ApiException.class, () -> servicio.marcarRecibido(LIQUIDADOR, "SIN-1", 99L));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    @Test
    void eliminarUnicoDocumentoPendienteVuelveAEnRevision() {
        Siniestro s = siniestro("SIN-1", LIQUIDADOR, EstadoSiniestro.DOCUMENTACION_PENDIENTE);
        ReflectionTestUtils.setField(s.solicitarDocumento("Parte policial", null), "id", 1L);
        existe(s);

        SiniestroDetalle d = servicio.eliminarDocumento(LIQUIDADOR, "SIN-1", 1L);

        assertTrue(d.documentos().isEmpty());
        assertEquals("EN_REVISION", d.estado());
    }

    @Test
    void eliminarDocumentoInexistenteEsNotFound() {
        existe(siniestro("SIN-1", LIQUIDADOR, EstadoSiniestro.EN_REVISION));

        ApiException e = assertThrows(ApiException.class, () -> servicio.eliminarDocumento(LIQUIDADOR, "SIN-1", 99L));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }
}

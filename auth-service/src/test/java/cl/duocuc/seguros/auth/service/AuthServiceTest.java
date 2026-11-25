package cl.duocuc.seguros.auth.service;

import cl.duocuc.seguros.auth.dto.*;
import cl.duocuc.seguros.auth.exception.ApiException;
import cl.duocuc.seguros.auth.model.Rol;
import cl.duocuc.seguros.auth.model.Usuario;
import cl.duocuc.seguros.auth.repository.RolRepository;
import cl.duocuc.seguros.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UsuarioRepository usuarios;
    @Mock RolRepository roles;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private JwtService jwt;
    private AuthService servicio;
    private Rol rolAsegurado;

    @BeforeEach
    void preparar() {
        jwt = new JwtService("secreto-de-prueba-con-mas-de-32-caracteres!!", 60, 10);
        servicio = new AuthService(usuarios, roles, encoder, jwt);
        rolAsegurado = new Rol("ASEGURADO");
    }

    private Usuario usuarioConClave(String clave) {
        Usuario u = new Usuario("Camila Rojas", "15842371-5", "camila@correo.cl", encoder.encode(clave), rolAsegurado);
        ReflectionTestUtils.setField(u, "id", 1L);
        u.setRespuestas(encoder.encode("rocky"), encoder.encode("santiago"), encoder.encode("matrix"));
        return u;
    }

    private RegistroRequest registro(String clave) {
        return new RegistroRequest("  Test Uno ", " 1-9 ", "  TEST@Correo.CL ", clave, "Rocky", "Santiago", "Matrix");
    }

    // ---------- registro ----------

    @Test
    void registrarGuardaUsuarioNormalizadoYConHashes() {
        when(usuarios.existsByCorreo("test@correo.cl")).thenReturn(false);
        when(usuarios.existsByRut("1-9")).thenReturn(false);
        when(roles.findByNombre("ASEGURADO")).thenReturn(Optional.of(rolAsegurado));
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponse resp = servicio.registrar(registro("Abcdef1!"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertEquals("Test Uno", guardado.getNombre());
        assertEquals("test@correo.cl", guardado.getCorreo());
        assertNotEquals("Abcdef1!", guardado.getClaveHash());
        assertTrue(encoder.matches("Abcdef1!", guardado.getClaveHash()));
        assertTrue(encoder.matches("rocky", guardado.getRespuesta1Hash()));
        assertEquals("ASEGURADO", resp.rol());
    }

    @Test
    void registrarRechazaCorreoDuplicado() {
        when(usuarios.existsByCorreo("test@correo.cl")).thenReturn(true);

        ApiException e = assertThrows(ApiException.class, () -> servicio.registrar(registro("Abcdef1!")));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        verify(usuarios, never()).save(any());
    }

    @Test
    void registrarRechazaRutDuplicado() {
        when(usuarios.existsByCorreo(any())).thenReturn(false);
        when(usuarios.existsByRut("1-9")).thenReturn(true);

        ApiException e = assertThrows(ApiException.class, () -> servicio.registrar(registro("Abcdef1!")));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void registrarRechazaContrasenasDebiles() {
        String[] debiles = {"Ab1!", "abcdefg1!", "Abcdefgh!", "Abcdefg12"};
        for (String clave : debiles) {
            ApiException e = assertThrows(ApiException.class, () -> servicio.registrar(registro(clave)),
                    "Debía rechazar: " + clave);
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        }
        verify(usuarios, never()).save(any());
    }

    @Test
    void registrarFallaSiFaltaElRolAsegurado() {
        when(roles.findByNombre("ASEGURADO")).thenReturn(Optional.empty());

        ApiException e = assertThrows(ApiException.class, () -> servicio.registrar(registro("Abcdef1!")));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatus());
    }

    // ---------- login ----------

    @Test
    void loginCorrectoEntregaTokenYUsuario() {
        when(usuarios.findByCorreo("camila@correo.cl")).thenReturn(Optional.of(usuarioConClave("Clave123!")));

        LoginResponse resp = servicio.login(new LoginRequest(" Camila@Correo.cl ", "Clave123!"));

        assertEquals(1L, jwt.idDesdeHeader("Bearer " + resp.token()));
        assertEquals("camila@correo.cl", resp.usuario().correo());
    }

    @Test
    void loginRechazaContrasenaIncorrecta() {
        when(usuarios.findByCorreo("camila@correo.cl")).thenReturn(Optional.of(usuarioConClave("Clave123!")));

        ApiException e = assertThrows(ApiException.class,
                () -> servicio.login(new LoginRequest("camila@correo.cl", "otra")));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }

    @Test
    void loginRechazaCorreoInexistente() {
        when(usuarios.findByCorreo("nadie@correo.cl")).thenReturn(Optional.empty());

        ApiException e = assertThrows(ApiException.class,
                () -> servicio.login(new LoginRequest("nadie@correo.cl", "Clave123!")));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }

    @Test
    void loginRechazaUsuarioInactivo() {
        Usuario u = usuarioConClave("Clave123!");
        ReflectionTestUtils.setField(u, "activo", false);
        when(usuarios.findByCorreo("camila@correo.cl")).thenReturn(Optional.of(u));

        assertThrows(ApiException.class,
                () -> servicio.login(new LoginRequest("camila@correo.cl", "Clave123!")));
    }

    // ---------- recuperar contraseña ----------

    @Test
    void validarRespuestasIgnoraMayusculasYEspacios() {
        when(usuarios.findByCorreo("camila@correo.cl")).thenReturn(Optional.of(usuarioConClave("Clave123!")));

        ResetTokenResponse resp = servicio.validarRespuestas(
                new ValidarRespuestasRequest("camila@correo.cl", "  ROCKY ", "Santiago", "MATRIX"));

        assertEquals(1L, jwt.idDesdeTokenReset(resp.resetToken()));
    }

    @Test
    void validarRespuestasRechazaRespuestaIncorrecta() {
        when(usuarios.findByCorreo("camila@correo.cl")).thenReturn(Optional.of(usuarioConClave("Clave123!")));

        ApiException e = assertThrows(ApiException.class, () -> servicio.validarRespuestas(
                new ValidarRespuestasRequest("camila@correo.cl", "rocky", "valparaiso", "matrix")));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }

    @Test
    void validarRespuestasRechazaUsuarioSinRespuestasGuardadas() {
        Usuario sinRespuestas = new Usuario("X", "2-7", "x@correo.cl", "hash", rolAsegurado);
        when(usuarios.findByCorreo("x@correo.cl")).thenReturn(Optional.of(sinRespuestas));

        assertThrows(ApiException.class, () -> servicio.validarRespuestas(
                new ValidarRespuestasRequest("x@correo.cl", "a", "b", "c")));
    }

    @Test
    void nuevaContrasenaActualizaElHash() {
        Usuario u = usuarioConClave("Clave123!");
        when(usuarios.findById(1L)).thenReturn(Optional.of(u));
        String reset = jwt.generarTokenReset(u);

        MensajeResponse resp = servicio.nuevaContrasena(new NuevaContrasenaRequest(reset, "Nueva456#"));

        assertEquals("Contraseña actualizada", resp.mensaje());
        assertTrue(encoder.matches("Nueva456#", u.getClaveHash()));
    }

    @Test
    void nuevaContrasenaRechazaTokenDeSesion() {
        Usuario u = usuarioConClave("Clave123!");
        String sesion = jwt.generarToken(u);

        assertThrows(ApiException.class,
                () -> servicio.nuevaContrasena(new NuevaContrasenaRequest(sesion, "Nueva456#")));
    }

    @Test
    void nuevaContrasenaRechazaClaveDebil() {
        Usuario u = usuarioConClave("Clave123!");
        when(usuarios.findById(1L)).thenReturn(Optional.of(u));
        String reset = jwt.generarTokenReset(u);

        ApiException e = assertThrows(ApiException.class,
                () -> servicio.nuevaContrasena(new NuevaContrasenaRequest(reset, "debil")));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }

    // ---------- cambiar contraseña ----------

    @Test
    void cambiarContrasenaConClaveActualCorrecta() {
        Usuario u = usuarioConClave("Clave123!");
        when(usuarios.findById(1L)).thenReturn(Optional.of(u));

        servicio.cambiarContrasena(1L, new CambiarContrasenaRequest("Clave123!", "Nueva456#"));

        assertTrue(encoder.matches("Nueva456#", u.getClaveHash()));
    }

    @Test
    void cambiarContrasenaRechazaClaveActualIncorrecta() {
        Usuario u = usuarioConClave("Clave123!");
        when(usuarios.findById(1L)).thenReturn(Optional.of(u));

        ApiException e = assertThrows(ApiException.class,
                () -> servicio.cambiarContrasena(1L, new CambiarContrasenaRequest("mala", "Nueva456#")));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertTrue(encoder.matches("Clave123!", u.getClaveHash()));
    }

    // ---------- buscar ----------

    @Test
    void buscarDevuelveElUsuario() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuarioConClave("Clave123!")));

        UsuarioResponse resp = servicio.buscar(1L);

        assertEquals("Camila Rojas", resp.nombre());
    }

    @Test
    void buscarLanzaNotFoundSiNoExiste() {
        when(usuarios.findById(99L)).thenReturn(Optional.empty());

        ApiException e = assertThrows(ApiException.class, () -> servicio.buscar(99L));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    @Test
    void normalizarQuitaEspaciosYMayusculas() {
        assertEquals("hola mundo", AuthService.normalizar("  HoLa Mundo "));
        assertEquals("", AuthService.normalizar(null));
    }

    // ---------- v2: listar por rol ----------

    @Test
    void listarPorRolNormalizaElNombreDelRol() {
        Usuario u = usuarioConClave("Abcdef1!");
        when(usuarios.findByRol_NombreAndActivoTrueOrderByNombreAsc("LIQUIDADOR")).thenReturn(List.of(u));

        List<UsuarioResponse> lista = servicio.listarPorRol(" liquidador ");

        assertEquals(1, lista.size());
        assertEquals("Camila Rojas", lista.get(0).nombre());
    }
}

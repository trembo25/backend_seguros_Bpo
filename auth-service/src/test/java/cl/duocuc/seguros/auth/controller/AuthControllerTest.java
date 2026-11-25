package cl.duocuc.seguros.auth.controller;

import cl.duocuc.seguros.auth.dto.*;
import cl.duocuc.seguros.auth.exception.ApiException;
import cl.duocuc.seguros.auth.exception.ManejadorErrores;
import cl.duocuc.seguros.auth.service.AuthService;
import cl.duocuc.seguros.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Prueba de los controladores sin levantar Spring ni la base de datos
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock AuthService auth;
    @Mock JwtService jwt;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders
                .standaloneSetup(new AuthController(auth, jwt), new UsuarioController(auth, jwt))
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    private static final String REGISTRO_OK = """
            {"nombre":"Test Uno","rut":"1-9","correo":"t@t.cl","contrasena":"Abcdef1!",
             "respuesta1":"a","respuesta2":"b","respuesta3":"c"}""";

    @Test
    void registroDevuelve201() throws Exception {
        when(auth.registrar(any())).thenReturn(new UsuarioResponse(7L, "Test Uno", "1-9", "t@t.cl", "ASEGURADO"));

        mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON).content(REGISTRO_OK))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.rol").value("ASEGURADO"));
    }

    @Test
    void registroConCamposVaciosDevuelve400ConMensaje() throws Exception {
        mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"rut\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void registroConJsonInvalidoDevuelve400() throws Exception {
        mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON).content("no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void registroDuplicadoDevuelve409() throws Exception {
        when(auth.registrar(any())).thenThrow(new ApiException(HttpStatus.CONFLICT, "El correo o RUT ingresado ya está registrado"));

        mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON).content(REGISTRO_OK))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El correo o RUT ingresado ya está registrado"));
    }

    @Test
    void loginCorrectoDevuelveToken() throws Exception {
        UsuarioResponse u = new UsuarioResponse(1L, "Camila", "1-9", "c@c.cl", "ASEGURADO");
        when(auth.login(any())).thenReturn(new LoginResponse("token-123", u));

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"c@c.cl\",\"contrasena\":\"Clave123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-123"))
                .andExpect(jsonPath("$.usuario.nombre").value("Camila"));
    }

    @Test
    void loginIncorrectoDevuelve401() throws Exception {
        when(auth.login(any())).thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos"));

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"c@c.cl\",\"contrasena\":\"mala\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Correo o contraseña incorrectos"));
    }

    @Test
    void loginSinContrasenaDevuelve400() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"c@c.cl\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La contraseña es obligatoria"));
    }

    @Test
    void recuperarValidarDevuelveResetToken() throws Exception {
        when(auth.validarRespuestas(any())).thenReturn(new ResetTokenResponse("reset-abc"));

        mvc.perform(post("/auth/recuperar/validar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"c@c.cl\",\"respuesta1\":\"a\",\"respuesta2\":\"b\",\"respuesta3\":\"c\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken").value("reset-abc"));
    }

    @Test
    void nuevaContrasenaDevuelveMensaje() throws Exception {
        when(auth.nuevaContrasena(any())).thenReturn(new MensajeResponse("Contraseña actualizada"));

        mvc.perform(post("/auth/recuperar/nueva-contrasena").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resetToken\":\"reset-abc\",\"nuevaContrasena\":\"Nueva456#\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Contraseña actualizada"));
    }

    @Test
    void meUsaElIdDelToken() throws Exception {
        when(jwt.idDesdeHeader("Bearer abc")).thenReturn(1L);
        when(auth.buscar(1L)).thenReturn(new UsuarioResponse(1L, "Camila", "1-9", "c@c.cl", "ASEGURADO"));

        mvc.perform(get("/auth/me").header("Authorization", "Bearer abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("c@c.cl"));
    }

    @Test
    void meSinTokenDevuelve401() throws Exception {
        when(jwt.idDesdeHeader(null)).thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "Falta el token de autorización"));

        mvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Falta el token de autorización"));
    }

    @Test
    void cambiarContrasenaDevuelve200() throws Exception {
        when(jwt.idDesdeHeader("Bearer abc")).thenReturn(1L);
        when(auth.cambiarContrasena(any(), any())).thenReturn(new MensajeResponse("Contraseña actualizada"));

        mvc.perform(put("/auth/cambiar-contrasena").header("Authorization", "Bearer abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contrasenaActual\":\"Clave123!\",\"nuevaContrasena\":\"Nueva456#\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Contraseña actualizada"));
    }

    @Test
    void liquidadorPuedeVerACualquierUsuario() throws Exception {
        when(jwt.idDesdeHeader("Bearer abc")).thenReturn(4L);
        when(jwt.rolDesdeHeader("Bearer abc")).thenReturn("LIQUIDADOR");
        when(auth.buscar(1L)).thenReturn(new UsuarioResponse(1L, "Camila", "1-9", "c@c.cl", "ASEGURADO"));

        mvc.perform(get("/auth/usuarios/1").header("Authorization", "Bearer abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ASEGURADO"));
    }

    @Test
    void aseguradoPuedeVerSusPropiosDatos() throws Exception {
        when(jwt.idDesdeHeader("Bearer abc")).thenReturn(1L);
        when(jwt.rolDesdeHeader("Bearer abc")).thenReturn("ASEGURADO");
        when(auth.buscar(1L)).thenReturn(new UsuarioResponse(1L, "Camila", "1-9", "c@c.cl", "ASEGURADO"));

        mvc.perform(get("/auth/usuarios/1").header("Authorization", "Bearer abc"))
                .andExpect(status().isOk());
    }

    @Test
    void aseguradoNoPuedeVerAOtroUsuario() throws Exception {
        when(jwt.idDesdeHeader("Bearer abc")).thenReturn(1L);
        when(jwt.rolDesdeHeader("Bearer abc")).thenReturn("ASEGURADO");

        mvc.perform(get("/auth/usuarios/4").header("Authorization", "Bearer abc"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No tienes permiso para ver a otro usuario"));
    }

    @Test
    void usuarioPorIdSinTokenDevuelve401() throws Exception {
        when(jwt.idDesdeHeader(null)).thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "Falta el token de autorización"));

        mvc.perform(get("/auth/usuarios/1"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- v2: listar usuarios por rol (lo usa analista-service) ----------

    @Test
    void analistaPuedeListarLiquidadores() throws Exception {
        when(jwt.rolDesdeHeader("Bearer abc")).thenReturn("ANALISTA");
        when(auth.listarPorRol("LIQUIDADOR")).thenReturn(List.of(
                new UsuarioResponse(4L, "Paulina Fernandez", "2-7", "p@a.cl", "LIQUIDADOR"),
                new UsuarioResponse(5L, "Matias Gallardo", "3-5", "m@a.cl", "LIQUIDADOR")));

        mvc.perform(get("/auth/usuarios").param("rol", "LIQUIDADOR").header("Authorization", "Bearer abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(4));
    }

    @Test
    void liquidadorNoPuedeListarUsuarios() throws Exception {
        when(jwt.rolDesdeHeader("Bearer abc")).thenReturn("LIQUIDADOR");

        mvc.perform(get("/auth/usuarios").param("rol", "LIQUIDADOR").header("Authorization", "Bearer abc"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Solo un analista puede listar usuarios"));
    }
}

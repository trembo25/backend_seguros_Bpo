package cl.duocuc.seguros.auth.config;

import cl.duocuc.seguros.auth.model.Rol;
import cl.duocuc.seguros.auth.model.Usuario;
import cl.duocuc.seguros.auth.repository.RolRepository;
import cl.duocuc.seguros.auth.repository.UsuarioRepository;
import cl.duocuc.seguros.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

// Carga los roles y los usuarios de ejemplo la primera vez (los mismos de la app Android)
@Component
public class DatosIniciales implements CommandLineRunner {

    private final RolRepository roles;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    @Value("${app.seed-demo:true}")
    private boolean seedDemo;

    public DatosIniciales(RolRepository roles, UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.roles = roles;
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        // El orden importa: ASEGURADO=1, LIQUIDADOR=2, ANALISTA=3 (igual que en la app)
        for (String nombre : List.of("ASEGURADO", "LIQUIDADOR", "ANALISTA")) {
            if (roles.findByNombre(nombre).isEmpty()) {
                roles.save(new Rol(nombre));
            }
        }
        if (!seedDemo || usuarios.count() > 0) return;

        crear("Camila Rojas Fuentes", "15842371-5", "camila.rojas@correo.ejemplo", "ASEGURADO");
        crear("Jorge Pizarro Contreras", "9418552-1", "jorge.pizarro@correo.ejemplo", "ASEGURADO");
        crear("Valentina Soto Muñoz", "19205884-8", "vale.soto@correo.ejemplo", "ASEGURADO");
        crear("Paulina Fernández Ruiz", "11111111-1", "paulina.fernandez@andes-bpo.ejemplo", "LIQUIDADOR");
        crear("Matías Gallardo Silva", "12222222-2", "matias.gallardo@andes-bpo.ejemplo", "LIQUIDADOR");
        crear("Natalia Reyes Campos", "13333333-3", "natalia.reyes@andes-bpo.ejemplo", "ANALISTA");
    }

    private void crear(String nombre, String rut, String correo, String rol) {
        Rol r = roles.findByNombre(rol).orElseThrow();
        Usuario u = new Usuario(nombre, rut, correo, encoder.encode("Clave123"), r);
        u.setEstado("ACTIVO");
        // Respuestas de seguridad de ejemplo: "demo" en las tres preguntas
        String demo = encoder.encode(AuthService.normalizar("demo"));
        u.setRespuestas(demo, demo, demo);
        usuarios.save(u);
    }
}

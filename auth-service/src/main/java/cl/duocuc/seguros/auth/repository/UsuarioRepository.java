package cl.duocuc.seguros.auth.repository;

import cl.duocuc.seguros.auth.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
    boolean existsByRut(String rut);
    List<Usuario> findByRol_NombreAndActivoTrueOrderByNombreAsc(String nombreRol);
    List<Usuario> findByEstadoOrderByNombreAsc(String estado);
}

package cl.duocuc.seguros.analista.client;

import cl.duocuc.seguros.analista.dto.UsuarioRemoto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// Feign busca "auth-service" en Eureka. El token del analista se reenvia tal cual.
@FeignClient(name = "auth-service")
public interface AuthClient {

    @GetMapping("/auth/usuarios")
    List<UsuarioRemoto> usuariosPorRol(@RequestHeader("Authorization") String authorization,
                                       @RequestParam("rol") String rol);

    @GetMapping("/auth/usuarios/pendientes")
    List<UsuarioRemoto> usuariosPendientes(@RequestHeader("Authorization") String authorization);

    @PutMapping("/auth/usuarios/{id}/aprobar")
    UsuarioRemoto aprobarUsuario(@RequestHeader("Authorization") String authorization,
                                 @PathVariable("id") Long id);
}

package cl.duocuc.seguros.liquidador.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

// Consulta a auth-service. Sin "url": Feign lo busca por nombre en Eureka (balanceado).
@FeignClient(name = "auth-service")
public interface AuthClient {

    @GetMapping("/auth/usuarios/{id}")
    UsuarioRemoto usuarioPorId(@RequestHeader("Authorization") String authorization,
                               @PathVariable("id") Long id);
}

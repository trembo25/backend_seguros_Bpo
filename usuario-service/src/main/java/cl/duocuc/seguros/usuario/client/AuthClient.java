package cl.duocuc.seguros.usuario.client;

import cl.duocuc.seguros.usuario.dto.PerfilResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

// Feign busca "auth-service" en Eureka. El token del asegurado se reenvia tal cual.
@FeignClient(name = "auth-service")
public interface AuthClient {

    @GetMapping("/auth/me")
    PerfilResponse me(@RequestHeader("Authorization") String authorization);
}

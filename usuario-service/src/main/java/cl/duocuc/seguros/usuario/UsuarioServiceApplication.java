package cl.duocuc.seguros.usuario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

// Servicio del ASEGURADO. No tiene base de datos: consulta a auth-service y liquidador-service con Feign.
@SpringBootApplication
@EnableFeignClients
public class UsuarioServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UsuarioServiceApplication.class, args);
    }
}

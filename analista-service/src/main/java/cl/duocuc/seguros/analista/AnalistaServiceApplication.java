package cl.duocuc.seguros.analista;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

// Servicio del ANALISTA (reemplaza al admin-service). No tiene base de datos:
// lee usuarios de auth-service y siniestros de liquidador-service con Feign.
@SpringBootApplication
@EnableFeignClients
public class AnalistaServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnalistaServiceApplication.class, args);
    }
}

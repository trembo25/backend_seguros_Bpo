package cl.duocuc.seguros.liquidador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class LiquidadorServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LiquidadorServiceApplication.class, args);
        System.out.println("liquidador activo");
    }
}

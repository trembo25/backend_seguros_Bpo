package cl.duocuc.seguros.usuario.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configuracion de Swagger: titulo, boton "Authorize" para pegar el JWT y servidor relativo
// (el servidor "/" hace que "Try it out" funcione igual entrando directo o por el gateway)
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("usuario-service").version("2.0.0")
                        .description("Servicio del ASEGURADO: perfil y consulta de sus propios siniestros"))
                .addServersItem(new Server().url("/"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}

package cl.duocuc.seguros.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// Comprueba que el gateway arranca (sin Eureka) y que cada prefijo apunta al servicio correcto por nombre
@SpringBootTest(properties = "eureka.client.enabled=false")
class RutasGatewayTest {

    @Autowired Environment env;

    private void ruta(int i, String servicio, String path) {
        String base = "spring.cloud.gateway.server.webmvc.routes[" + i + "].";
        assertEquals(servicio, env.getProperty(base + "id"));
        assertEquals("lb://" + servicio, env.getProperty(base + "uri"));
        assertEquals("Path=" + path, env.getProperty(base + "predicates[0]"));
    }

    @Test
    void elContextoArranca() {
        assertNotNull(env);
    }

    @Test
    void cadaPrefijoApuntaASuServicioEnEureka() {
        ruta(0, "auth-service", "/auth/**");
        ruta(1, "usuario-service", "/asegurado/**");
        ruta(2, "liquidador-service", "/liquidador/**");
        ruta(3, "analista-service", "/analista/**");
    }

    @Test
    void swaggerUnificadoTieneLosCuatroServicios() {
        assertEquals("/auth/v3/api-docs", env.getProperty("springdoc.swagger-ui.urls[0].url"));
        assertEquals("/asegurado/v3/api-docs", env.getProperty("springdoc.swagger-ui.urls[1].url"));
        assertEquals("/liquidador/v3/api-docs", env.getProperty("springdoc.swagger-ui.urls[2].url"));
        assertEquals("/analista/v3/api-docs", env.getProperty("springdoc.swagger-ui.urls[3].url"));
    }
}

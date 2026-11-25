package cl.duocuc.seguros.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Prueba real de enrutamiento: un servidor falso hace de "auth-service" y "analista-service".
// En vez de Eureka se usa el descubrimiento simple de Spring Cloud (misma resolucion lb://).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EnrutamientoGatewayTest {

    static final HttpServer FALSO = iniciarFalso();

    static HttpServer iniciarFalso() {
        try {
            HttpServer s = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            s.createContext("/", ex -> {
                String auth = ex.getRequestHeaders().getFirst("Authorization");
                byte[] body = ("{\"ruta\":\"" + ex.getRequestURI().getPath() + "\",\"token\":\"" + auth + "\"}")
                        .getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().add("Content-Type", "application/json");
                ex.sendResponseHeaders(200, body.length);
                ex.getResponseBody().write(body);
                ex.close();
            });
            s.start();
            return s;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        String url = "http://localhost:" + FALSO.getAddress().getPort();
        r.add("eureka.client.enabled", () -> "false");
        r.add("spring.cloud.discovery.client.simple.instances.auth-service[0].uri", () -> url);
        r.add("spring.cloud.discovery.client.simple.instances.analista-service[0].uri", () -> url);
    }

    @AfterAll
    static void detener() {
        FALSO.stop(0);
    }

    @LocalServerPort int puerto;

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + path))
                        .header("Authorization", "Bearer abc").GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void reenviaAuthConLaRutaCompletaYElToken() throws Exception {
        HttpResponse<String> r = get("/auth/me");

        assertEquals(200, r.statusCode());
        assertEquals("{\"ruta\":\"/auth/me\",\"token\":\"Bearer abc\"}", r.body());
    }

    @Test
    void reenviaAnalista() throws Exception {
        HttpResponse<String> r = get("/analista/panel");

        assertEquals(200, r.statusCode());
        assertEquals("{\"ruta\":\"/analista/panel\",\"token\":\"Bearer abc\"}", r.body());
    }

    @Test
    void rutaDesconocidaDevuelve404() throws Exception {
        assertEquals(404, get("/otra/cosa").statusCode());
    }
}

package cl.duocuc.seguros.eureka;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

// Levanta Eureka en un puerto libre y comprueba que responde
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EurekaServerApplicationTest {

    @LocalServerPort int puerto;
    @Autowired Environment env;

    @Test
    void noSeRegistraASiMismo() {
        assertEquals("false", env.getProperty("eureka.client.register-with-eureka"));
        assertEquals("false", env.getProperty("eureka.client.fetch-registry"));
    }

    @Test
    void elRegistroDeAplicacionesResponde() throws Exception {
        HttpResponse<String> r = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/eureka/apps"))
                        .header("Accept", "application/json").GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("applications"));
    }

    @Test
    void elPanelWebResponde() throws Exception {
        HttpResponse<String> r = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, r.statusCode());
    }
}

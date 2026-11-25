package cl.duocuc.seguros.analista.exception;

import feign.FeignException;
import org.springframework.http.HttpStatus;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Convierte un error de Feign en un ApiException con el mismo formato {"error": "..."}.
// Si el otro servicio respondio 401/403/404/400/409 se devuelve ese mismo codigo y su mensaje;
// si no respondio (caido, timeout, 5xx) se devuelve 502 Bad Gateway.
public final class ErroresRemotos {

    private static final Pattern MENSAJE = Pattern.compile("\"error\"\\s*:\\s*\"([^\"]*)\"");

    private ErroresRemotos() {}

    public static ApiException traducir(FeignException e, String servicio) {
        HttpStatus status = HttpStatus.resolve(e.status());
        if (status == null || status.is5xxServerError()) {
            return new ApiException(HttpStatus.BAD_GATEWAY, "No se pudo consultar " + servicio);
        }
        return new ApiException(status, mensaje(e, "Error al consultar " + servicio));
    }

    static String mensaje(FeignException e, String porDefecto) {
        String cuerpo = e.contentUTF8();
        if (cuerpo == null) return porDefecto;
        Matcher m = MENSAJE.matcher(cuerpo);
        return m.find() ? m.group(1) : porDefecto;
    }
}

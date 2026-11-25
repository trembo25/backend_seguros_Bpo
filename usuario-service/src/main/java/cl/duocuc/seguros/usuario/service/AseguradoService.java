package cl.duocuc.seguros.usuario.service;

import cl.duocuc.seguros.usuario.client.AuthClient;
import cl.duocuc.seguros.usuario.client.LiquidadorClient;
import cl.duocuc.seguros.usuario.dto.*;
import cl.duocuc.seguros.usuario.exception.ErroresRemotos;
import feign.FeignException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AseguradoService {

    private static final List<String> ESTADOS = List.of(
            "EN_REVISION", "DOCUMENTACION_PENDIENTE", "EN_LIQUIDACION", "LIQUIDADO", "RECHAZADO");

    private final AuthClient auth;
    private final LiquidadorClient liquidador;

    public AseguradoService(AuthClient auth, LiquidadorClient liquidador) {
        this.auth = auth;
        this.liquidador = liquidador;
    }

    public PerfilResponse perfil(String authorization) {
        try {
            return auth.me(authorization);
        } catch (FeignException e) {
            throw ErroresRemotos.traducir(e, "auth-service");
        }
    }

    public List<SiniestroResumen> misSiniestros(String authorization) {
        try {
            return liquidador.misSiniestros(authorization);
        } catch (FeignException e) {
            throw ErroresRemotos.traducir(e, "liquidador-service");
        }
    }

    public SiniestroDetalle detalle(String authorization, String numero) {
        try {
            return liquidador.detalle(authorization, numero);
        } catch (FeignException e) {
            throw ErroresRemotos.traducir(e, "liquidador-service");
        }
    }

    // Junta perfil + conteo por estado + documentos pendientes (para la pantalla de inicio)
    public ResumenAseguradoResponse resumen(String authorization) {
        PerfilResponse perfil = perfil(authorization);
        List<SiniestroResumen> lista = misSiniestros(authorization);

        Map<String, Long> porEstado = new LinkedHashMap<>();
        ESTADOS.forEach(e -> porEstado.put(e, 0L));
        lista.forEach(s -> porEstado.merge(s.estado(), 1L, Long::sum));

        int pendientes = lista.stream()
                .filter(s -> "DOCUMENTACION_PENDIENTE".equals(s.estado()))
                .mapToInt(s -> (int) detalle(authorization, s.numero()).documentos().stream()
                        .filter(d -> !d.recibido()).count())
                .sum();
        return new ResumenAseguradoResponse(perfil, lista.size(), porEstado, pendientes);
    }
}

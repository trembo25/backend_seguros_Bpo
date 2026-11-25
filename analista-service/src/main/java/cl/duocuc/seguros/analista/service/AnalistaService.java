package cl.duocuc.seguros.analista.service;

import cl.duocuc.seguros.analista.client.AuthClient;
import cl.duocuc.seguros.analista.client.LiquidadorClient;
import cl.duocuc.seguros.analista.dto.*;
import cl.duocuc.seguros.analista.exception.ApiException;
import cl.duocuc.seguros.analista.exception.ErroresRemotos;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalistaService {

    // Mismos estados que liquidador-service
    public static final List<String> ESTADOS = List.of(
            "EN_REVISION", "DOCUMENTACION_PENDIENTE", "EN_LIQUIDACION", "LIQUIDADO", "RECHAZADO");
    private static final Set<String> CERRADOS = Set.of("LIQUIDADO", "RECHAZADO");

    private final AuthClient auth;
    private final LiquidadorClient liquidador;

    public AnalistaService(AuthClient auth, LiquidadorClient liquidador) {
        this.auth = auth;
        this.liquidador = liquidador;
    }

    // ---------- panel ----------

    // Metricas de TODOS los siniestros + la lista filtrada + los liquidadores con su carga
    public PanelResponse panel(String authorization, String estado) {
        String filtro = normalizarEstado(estado);
        List<SiniestroRemoto> todos = remotos(authorization, null);
        List<UsuarioRemoto> liquidadores = liquidadoresRemotos(authorization);
        Map<Long, String> nombres = nombres(liquidadores);

        Map<String, Long> porEstado = new LinkedHashMap<>();
        ESTADOS.forEach(e -> porEstado.put(e, 0L));
        todos.forEach(s -> porEstado.merge(s.estado(), 1L, Long::sum));
        long cerrados = todos.stream().filter(s -> CERRADOS.contains(s.estado())).count();
        MetricasPanel metricas = new MetricasPanel(todos.size(), todos.size() - cerrados, cerrados, porEstado);

        List<SiniestroAnalista> filtrados = todos.stream()
                .filter(s -> filtro == null || filtro.equals(s.estado()))
                .map(s -> SiniestroAnalista.de(s, nombres.get(s.idLiquidador())))
                .toList();
        return new PanelResponse(metricas, filtrados, resumenLiquidadores(liquidadores, todos));
    }

    // ---------- siniestros ----------

    public List<SiniestroAnalista> siniestros(String authorization, String estado) {
        List<SiniestroRemoto> lista = remotos(authorization, normalizarEstado(estado));
        Map<Long, String> nombres = nombres(liquidadoresRemotos(authorization));
        return lista.stream().map(s -> SiniestroAnalista.de(s, nombres.get(s.idLiquidador()))).toList();
    }

    public SiniestroDetalleRemoto detalle(String authorization, String numero) {
        try {
            return liquidador.detalle(authorization, numero);
        } catch (FeignException e) {
            throw ErroresRemotos.traducir(e, "liquidador-service");
        }
    }

    // ---------- liquidadores ----------

    public List<LiquidadorResumen> liquidadores(String authorization) {
        return resumenLiquidadores(liquidadoresRemotos(authorization), remotos(authorization, null));
    }

    // ---------- asignar ----------

    // Primero se comprueba aqui que el id sea de un liquidador (mensaje claro para la app);
    // liquidador-service lo vuelve a validar antes de guardar.
    public SiniestroAnalista asignar(String authorization, String numero, Long idLiquidador) {
        Map<Long, String> nombres = nombres(liquidadoresRemotos(authorization));
        if (!nombres.containsKey(idLiquidador)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El usuario indicado no es un liquidador");
        }
        try {
            SiniestroRemoto s = liquidador.reasignar(authorization, numero, new AsignarRequest(idLiquidador));
            return SiniestroAnalista.de(s, nombres.get(s.idLiquidador()));
        } catch (FeignException e) {
            throw ErroresRemotos.traducir(e, "liquidador-service");
        }
    }

    // ---------- ayudas ----------

    static String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank() || "TODOS".equalsIgnoreCase(estado.trim())) return null;
        String e = estado.trim().toUpperCase(Locale.ROOT);
        if (!ESTADOS.contains(e)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Estado inválido. Usa uno de: TODOS, " + String.join(", ", ESTADOS));
        }
        return e;
    }

    private List<SiniestroRemoto> remotos(String authorization, String estado) {
        try {
            return liquidador.siniestros(authorization, estado);
        } catch (FeignException e) {
            throw ErroresRemotos.traducir(e, "liquidador-service");
        }
    }

    private List<UsuarioRemoto> liquidadoresRemotos(String authorization) {
        try {
            return auth.usuariosPorRol(authorization, "LIQUIDADOR");
        } catch (FeignException e) {
            throw ErroresRemotos.traducir(e, "auth-service");
        }
    }

    private static Map<Long, String> nombres(List<UsuarioRemoto> liquidadores) {
        return liquidadores.stream().collect(Collectors.toMap(UsuarioRemoto::id, UsuarioRemoto::nombre, (a, b) -> a));
    }

    private static List<LiquidadorResumen> resumenLiquidadores(List<UsuarioRemoto> liquidadores, List<SiniestroRemoto> todos) {
        Map<Long, List<SiniestroRemoto>> porLiquidador = todos.stream()
                .collect(Collectors.groupingBy(SiniestroRemoto::idLiquidador));
        return liquidadores.stream().map(u -> {
            List<SiniestroRemoto> suyos = porLiquidador.getOrDefault(u.id(), List.of());
            long activos = suyos.stream().filter(s -> !CERRADOS.contains(s.estado())).count();
            return new LiquidadorResumen(u.id(), u.nombre(), u.correo(), activos, suyos.size());
        }).toList();
    }
}

package cl.duocuc.seguros.analista.dto;

import java.util.Map;

// activos = EN_REVISION + DOCUMENTACION_PENDIENTE + EN_LIQUIDACION; cerrados = LIQUIDADO + RECHAZADO
public record MetricasPanel(long total, long activos, long cerrados, Map<String, Long> porEstado) {}

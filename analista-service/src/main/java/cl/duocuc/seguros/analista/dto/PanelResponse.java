package cl.duocuc.seguros.analista.dto;

import java.util.List;

public record PanelResponse(MetricasPanel metricas, List<SiniestroAnalista> siniestros, List<LiquidadorResumen> liquidadores) {}

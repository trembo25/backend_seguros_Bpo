package cl.duocuc.seguros.analista.dto;

// Liquidador con su carga de trabajo (siniestros abiertos y total asignado)
public record LiquidadorResumen(Long id, String nombre, String correo, long siniestrosActivos, long siniestrosTotales) {}

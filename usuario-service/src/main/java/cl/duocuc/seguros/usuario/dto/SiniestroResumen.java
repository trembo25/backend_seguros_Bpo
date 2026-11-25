package cl.duocuc.seguros.usuario.dto;

import java.time.LocalDate;

// Fila de "Mis siniestros" (viene de liquidador-service)
public record SiniestroResumen(String numero, String nombreAsegurado, String tipo, String estado, LocalDate fechaCreacion) {}

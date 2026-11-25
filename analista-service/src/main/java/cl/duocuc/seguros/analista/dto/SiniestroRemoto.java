package cl.duocuc.seguros.analista.dto;

import java.time.LocalDate;

// Siniestro tal como lo entrega liquidador-service (/liquidador/consultas/analista/...)
public record SiniestroRemoto(String numero, Long idAsegurado, String nombreAsegurado, Long idLiquidador,
                              String tipo, String estado, LocalDate fechaCreacion, int documentosPendientes) {}

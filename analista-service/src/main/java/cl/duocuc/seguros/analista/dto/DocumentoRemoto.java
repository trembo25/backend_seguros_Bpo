package cl.duocuc.seguros.analista.dto;

import java.time.LocalDate;

public record DocumentoRemoto(Long id, String nombre, String mensaje, boolean recibido, LocalDate fechaSolicitud) {}

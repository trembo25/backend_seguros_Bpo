package cl.duocuc.seguros.usuario.dto;

import java.time.LocalDate;

public record DocumentoResponse(Long id, String nombre, String mensaje, boolean recibido, LocalDate fechaSolicitud) {}

package cl.duocuc.seguros.usuario.dto;

import java.time.LocalDate;
import java.util.List;

// Detalle de un siniestro con los documentos que pidio el liquidador
public record SiniestroDetalle(String numero, Long idAsegurado, String nombreAsegurado, String tipo,
                               String descripcion, String estado, LocalDate fechaCreacion,
                               List<DocumentoResponse> documentos) {}

package cl.duocuc.seguros.analista.dto;

import java.time.LocalDate;
import java.util.List;

public record SiniestroDetalleRemoto(String numero, Long idAsegurado, String nombreAsegurado, String tipo,
                                     String descripcion, String estado, LocalDate fechaCreacion,
                                     List<DocumentoRemoto> documentos) {}

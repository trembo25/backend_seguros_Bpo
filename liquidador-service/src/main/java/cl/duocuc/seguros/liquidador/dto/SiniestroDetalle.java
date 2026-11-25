package cl.duocuc.seguros.liquidador.dto;

import cl.duocuc.seguros.liquidador.model.Siniestro;

import java.time.LocalDate;
import java.util.List;

public record SiniestroDetalle(String numero, Long idAsegurado, String nombreAsegurado, String tipo,
                               String descripcion, String estado, LocalDate fechaCreacion,
                               List<DocumentoResponse> documentos) {

    public static SiniestroDetalle de(Siniestro s) {
        return new SiniestroDetalle(s.getNumero(), s.getIdAsegurado(), s.getNombreAsegurado(), s.getTipo(),
                s.getDescripcion(), s.getEstado().name(), s.getFechaCreacion(),
                s.getDocumentos().stream().map(DocumentoResponse::de).toList());
    }
}

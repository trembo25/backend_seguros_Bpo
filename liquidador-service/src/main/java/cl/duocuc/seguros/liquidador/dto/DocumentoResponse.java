package cl.duocuc.seguros.liquidador.dto;

import cl.duocuc.seguros.liquidador.model.DocumentoSolicitado;

import java.time.LocalDate;

public record DocumentoResponse(Long id, String nombre, String mensaje, boolean recibido, LocalDate fechaSolicitud) {

    public static DocumentoResponse de(DocumentoSolicitado d) {
        return new DocumentoResponse(d.getId(), d.getNombre(), d.getMensaje(), d.isRecibido(), d.getFechaSolicitud());
    }
}

package cl.duocuc.seguros.liquidador.dto;

import cl.duocuc.seguros.liquidador.model.Siniestro;

import java.time.LocalDate;

// Fila del panel del liquidador
public record SiniestroResumen(String numero, String nombreAsegurado, String tipo, String estado, LocalDate fechaCreacion) {

    public static SiniestroResumen de(Siniestro s) {
        return new SiniestroResumen(s.getNumero(), s.getNombreAsegurado(), s.getTipo(),
                s.getEstado().name(), s.getFechaCreacion());
    }
}

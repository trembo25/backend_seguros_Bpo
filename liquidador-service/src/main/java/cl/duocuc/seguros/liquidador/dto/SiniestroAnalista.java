package cl.duocuc.seguros.liquidador.dto;

import cl.duocuc.seguros.liquidador.model.Siniestro;

import java.time.LocalDate;

// Fila que ve el analista: incluye a que liquidador esta asignado
public record SiniestroAnalista(String numero, Long idAsegurado, String nombreAsegurado, Long idLiquidador,
                                String tipo, String estado, LocalDate fechaCreacion, int documentosPendientes) {

    public static SiniestroAnalista de(Siniestro s) {
        int pendientes = (int) s.getDocumentos().stream().filter(d -> !d.isRecibido()).count();
        return new SiniestroAnalista(s.getNumero(), s.getIdAsegurado(), s.getNombreAsegurado(), s.getIdLiquidador(),
                s.getTipo(), s.getEstado().name(), s.getFechaCreacion(), pendientes);
    }
}

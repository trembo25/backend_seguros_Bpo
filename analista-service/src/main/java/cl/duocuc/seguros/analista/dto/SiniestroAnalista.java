package cl.duocuc.seguros.analista.dto;

import java.time.LocalDate;

// Fila del panel del analista: igual al remoto pero con el nombre del liquidador
public record SiniestroAnalista(String numero, Long idAsegurado, String nombreAsegurado, Long idLiquidador,
                                String nombreLiquidador, String tipo, String estado, LocalDate fechaCreacion,
                                int documentosPendientes) {

    public static SiniestroAnalista de(SiniestroRemoto s, String nombreLiquidador) {
        return new SiniestroAnalista(s.numero(), s.idAsegurado(), s.nombreAsegurado(), s.idLiquidador(),
                nombreLiquidador, s.tipo(), s.estado(), s.fechaCreacion(), s.documentosPendientes());
    }
}

package cl.duocuc.seguros.liquidador.config;

import cl.duocuc.seguros.liquidador.model.EstadoSiniestro;
import cl.duocuc.seguros.liquidador.model.Siniestro;
import cl.duocuc.seguros.liquidador.repository.SiniestroRepository;
import cl.duocuc.seguros.liquidador.service.LiquidadorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Siniestros de ejemplo. Los ids de usuario son los de los usuarios demo de auth-service:
// asegurados 1 (Camila), 2 (Jorge), 3 (Vale); liquidadores 4 (Paulina) y 5 (Matias).
@Component
public class DatosIniciales implements CommandLineRunner {

    private final SiniestroRepository siniestros;
    private final boolean activo;

    public DatosIniciales(SiniestroRepository siniestros, @Value("${app.seed-demo:false}") boolean activo) {
        this.siniestros = siniestros;
        this.activo = activo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!activo || siniestros.count() > 0) return;

        crear(1L, "Camila Rojas Fuentes", 4L, "Choque", "Choque leve en estacionamiento", EstadoSiniestro.EN_REVISION);
        crear(2L, "Jorge Pizarro", 4L, "Robo", "Robo de accesorios del vehículo", EstadoSiniestro.DOCUMENTACION_PENDIENTE)
                .solicitarDocumento("Parte policial", "Adjunta el parte policial del robo");
        crear(3L, "Vale Soto", 5L, "Incendio", "Daño por incendio en cocina", EstadoSiniestro.EN_LIQUIDACION);
    }

    private Siniestro crear(Long idAsegurado, String nombre, Long idLiquidador, String tipo, String desc, EstadoSiniestro estado) {
        Siniestro s = siniestros.save(new Siniestro("TMP-" + nombre, idAsegurado, nombre, idLiquidador, tipo, desc));
        s.setNumero(LiquidadorService.generarNumero(s.getId()));
        s.setEstado(estado);
        return s;
    }
}

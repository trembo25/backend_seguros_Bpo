package cl.duocuc.seguros.liquidador.repository;

import cl.duocuc.seguros.liquidador.model.EstadoSiniestro;
import cl.duocuc.seguros.liquidador.model.Siniestro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SiniestroRepository extends JpaRepository<Siniestro, Long> {
    List<Siniestro> findByIdLiquidadorOrderByFechaCreacionDescIdDesc(Long idLiquidador);
    List<Siniestro> findByIdLiquidadorAndEstadoOrderByFechaCreacionDescIdDesc(Long idLiquidador, EstadoSiniestro estado);
    Optional<Siniestro> findByNumero(String numero);

    // v2: consultas del asegurado (solo los suyos) y del analista (todos)
    List<Siniestro> findByIdAseguradoOrderByFechaCreacionDescIdDesc(Long idAsegurado);
    List<Siniestro> findAllByOrderByFechaCreacionDescIdDesc();
    List<Siniestro> findByEstadoOrderByFechaCreacionDescIdDesc(EstadoSiniestro estado);
}

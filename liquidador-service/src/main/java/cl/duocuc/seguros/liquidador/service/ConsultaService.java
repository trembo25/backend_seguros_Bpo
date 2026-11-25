package cl.duocuc.seguros.liquidador.service;

import cl.duocuc.seguros.liquidador.client.AuthClient;
import cl.duocuc.seguros.liquidador.client.UsuarioRemoto;
import cl.duocuc.seguros.liquidador.dto.SiniestroAnalista;
import cl.duocuc.seguros.liquidador.dto.SiniestroDetalle;
import cl.duocuc.seguros.liquidador.dto.SiniestroResumen;
import cl.duocuc.seguros.liquidador.exception.ApiException;
import cl.duocuc.seguros.liquidador.model.EstadoSiniestro;
import cl.duocuc.seguros.liquidador.model.Siniestro;
import cl.duocuc.seguros.liquidador.repository.SiniestroRepository;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// v2: lecturas para el asegurado (via usuario-service) y para el analista (via analista-service).
// liquidador-service sigue siendo el UNICO dueno de la tabla siniestros.
@Service
public class ConsultaService {

    private final SiniestroRepository siniestros;
    private final AuthClient auth;

    public ConsultaService(SiniestroRepository siniestros, AuthClient auth) {
        this.siniestros = siniestros;
        this.auth = auth;
    }

    // ---------- asegurado ----------

    @Transactional(readOnly = true)
    public List<SiniestroResumen> siniestrosDelAsegurado(Long idAsegurado) {
        return siniestros.findByIdAseguradoOrderByFechaCreacionDescIdDesc(idAsegurado)
                .stream().map(SiniestroResumen::de).toList();
    }

    // Un asegurado solo puede ver el detalle de un siniestro que sea suyo
    @Transactional(readOnly = true)
    public SiniestroDetalle detalleDelAsegurado(Long idAsegurado, String numero) {
        Siniestro s = buscar(numero);
        if (!s.getIdAsegurado().equals(idAsegurado)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "El siniestro no pertenece a este asegurado");
        }
        return SiniestroDetalle.de(s);
    }

    // ---------- analista ----------

    @Transactional(readOnly = true)
    public List<SiniestroAnalista> todos(EstadoSiniestro estado) {
        List<Siniestro> lista = estado == null
                ? siniestros.findAllByOrderByFechaCreacionDescIdDesc()
                : siniestros.findByEstadoOrderByFechaCreacionDescIdDesc(estado);
        return lista.stream().map(SiniestroAnalista::de).toList();
    }

    @Transactional(readOnly = true)
    public SiniestroDetalle detalleAnalista(String numero) {
        return SiniestroDetalle.de(buscar(numero));
    }

    // Reasigna el siniestro a otro liquidador (se valida contra auth-service que sea LIQUIDADOR)
    @Transactional
    public SiniestroAnalista reasignar(String authorization, String numero, Long idLiquidador) {
        Siniestro s = buscar(numero);
        if (s.getEstado() == EstadoSiniestro.LIQUIDADO || s.getEstado() == EstadoSiniestro.RECHAZADO) {
            throw new ApiException(HttpStatus.CONFLICT, "El siniestro ya está cerrado");
        }
        UsuarioRemoto u;
        try {
            u = auth.usuarioPorId(authorization, idLiquidador);
        } catch (FeignException.NotFound e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "El liquidador no existe");
        } catch (FeignException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "No se pudo consultar auth-service");
        }
        if (!"LIQUIDADOR".equals(u.rol())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El usuario indicado no es un liquidador");
        }
        s.setIdLiquidador(idLiquidador);
        return SiniestroAnalista.de(s);
    }

    private Siniestro buscar(String numero) {
        return siniestros.findByNumero(numero)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Siniestro no encontrado"));
    }
}

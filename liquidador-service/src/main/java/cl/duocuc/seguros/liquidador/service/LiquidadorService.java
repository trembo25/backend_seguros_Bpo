package cl.duocuc.seguros.liquidador.service;

import cl.duocuc.seguros.liquidador.client.AuthClient;
import cl.duocuc.seguros.liquidador.client.UsuarioRemoto;
import cl.duocuc.seguros.liquidador.dto.*;
import cl.duocuc.seguros.liquidador.exception.ApiException;
import cl.duocuc.seguros.liquidador.model.DocumentoSolicitado;
import cl.duocuc.seguros.liquidador.model.EstadoSiniestro;
import cl.duocuc.seguros.liquidador.model.Siniestro;
import cl.duocuc.seguros.liquidador.repository.SiniestroRepository;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
public class LiquidadorService {

    private final SiniestroRepository siniestros;
    private final AuthClient auth;

    public LiquidadorService(SiniestroRepository siniestros, AuthClient auth) {
        this.siniestros = siniestros;
        this.auth = auth;
    }

    @Transactional(readOnly = true)
    public List<SiniestroResumen> listar(Long idLiquidador, EstadoSiniestro estado) {
        List<Siniestro> lista = estado == null
                ? siniestros.findByIdLiquidadorOrderByFechaCreacionDescIdDesc(idLiquidador)
                : siniestros.findByIdLiquidadorAndEstadoOrderByFechaCreacionDescIdDesc(idLiquidador, estado);
        return lista.stream().map(SiniestroResumen::de).toList();
    }

    @Transactional(readOnly = true)
    public SiniestroDetalle detalle(Long idLiquidador, String numero) {
        return SiniestroDetalle.de(propio(idLiquidador, numero));
    }

    // El liquidador abre un siniestro a nombre de un asegurado (se valida contra auth-service)
    @Transactional
    public SiniestroDetalle crear(Long idLiquidador, String authorization, CrearSiniestroRequest r) {
        UsuarioRemoto asegurado = buscarAsegurado(authorization, r.idAsegurado());

        Siniestro s = siniestros.save(new Siniestro("TMP-" + UUID.randomUUID(), asegurado.id(),
                asegurado.nombre(), idLiquidador, r.tipo().trim(), r.descripcion()));
        s.setNumero(generarNumero(s.getId()));
        return SiniestroDetalle.de(s);
    }

    public static String generarNumero(Long id) {
        return "SIN-" + Year.now().getValue() + "-" + String.format("%04d", id);
    }

    private UsuarioRemoto buscarAsegurado(String authorization, Long id) {
        UsuarioRemoto u;
        try {
            u = auth.usuarioPorId(authorization, id);
        } catch (FeignException.NotFound e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "El asegurado no existe");
        } catch (FeignException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "No se pudo consultar auth-service");
        }
        if (!"ASEGURADO".equals(u.rol())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El usuario indicado no es un asegurado");
        }
        return u;
    }

    @Transactional
    public SiniestroDetalle cambiarEstado(Long idLiquidador, String numero, EstadoSiniestro nuevo) {
        Siniestro s = propio(idLiquidador, numero);
        if (s.getEstado() == EstadoSiniestro.LIQUIDADO || s.getEstado() == EstadoSiniestro.RECHAZADO) {
            throw new ApiException(HttpStatus.CONFLICT, "El siniestro ya está cerrado");
        }
        s.setEstado(nuevo);
        return SiniestroDetalle.de(s);
    }

    // Pide un documento al asegurado: el siniestro pasa a DOCUMENTACION_PENDIENTE
    @Transactional
    public DocumentoResponse solicitarDocumento(Long idLiquidador, String numero, SolicitarDocumentoRequest r) {
        Siniestro s = propio(idLiquidador, numero);
        if (s.getEstado() == EstadoSiniestro.LIQUIDADO || s.getEstado() == EstadoSiniestro.RECHAZADO) {
            throw new ApiException(HttpStatus.CONFLICT, "El siniestro ya está cerrado");
        }
        DocumentoSolicitado d = s.solicitarDocumento(r.nombre().trim(), r.mensaje());
        s.setEstado(EstadoSiniestro.DOCUMENTACION_PENDIENTE);
        siniestros.flush();
        return DocumentoResponse.de(d);
    }

    // Marca un documento como recibido; si no queda ninguno pendiente, vuelve a EN_REVISION
    @Transactional
    public SiniestroDetalle marcarRecibido(Long idLiquidador, String numero, Long idDocumento) {
        Siniestro s = propio(idLiquidador, numero);
        DocumentoSolicitado d = s.getDocumentos().stream()
                .filter(x -> x.getId().equals(idDocumento))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Documento no encontrado"));
        d.setRecibido(true);
        if (s.getEstado() == EstadoSiniestro.DOCUMENTACION_PENDIENTE && !s.tieneDocumentosPendientes()) {
            s.setEstado(EstadoSiniestro.EN_REVISION);
        }
        return SiniestroDetalle.de(s);
    }

    // Elimina una solicitud de documento (por ejemplo, si se pidió por error)
    @Transactional
    public SiniestroDetalle eliminarDocumento(Long idLiquidador, String numero, Long idDocumento) {
        Siniestro s = propio(idLiquidador, numero);
        boolean existe = s.getDocumentos().removeIf(x -> x.getId().equals(idDocumento));
        if (!existe) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Documento no encontrado");
        }
        if (s.getEstado() == EstadoSiniestro.DOCUMENTACION_PENDIENTE && !s.tieneDocumentosPendientes()) {
            s.setEstado(EstadoSiniestro.EN_REVISION);
        }
        return SiniestroDetalle.de(s);
    }

    // Un liquidador solo ve y modifica los siniestros que tiene asignados
    private Siniestro propio(Long idLiquidador, String numero) {
        Siniestro s = siniestros.findByNumero(numero)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Siniestro no encontrado"));
        if (!s.getIdLiquidador().equals(idLiquidador)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "El siniestro no está asignado a este liquidador");
        }
        return s;
    }
}

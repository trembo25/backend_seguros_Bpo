package cl.duocuc.seguros.usuario.client;

import cl.duocuc.seguros.usuario.dto.SiniestroDetalle;
import cl.duocuc.seguros.usuario.dto.SiniestroResumen;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

// liquidador-service es el dueno de los siniestros; aqui solo se leen los del asegurado
@FeignClient(name = "liquidador-service")
public interface LiquidadorClient {

    @GetMapping("/liquidador/consultas/asegurado/siniestros")
    List<SiniestroResumen> misSiniestros(@RequestHeader("Authorization") String authorization);

    @GetMapping("/liquidador/consultas/asegurado/siniestros/{numero}")
    SiniestroDetalle detalle(@RequestHeader("Authorization") String authorization,
                             @PathVariable("numero") String numero);
}

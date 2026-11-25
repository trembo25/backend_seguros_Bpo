package cl.duocuc.seguros.analista.client;

import cl.duocuc.seguros.analista.dto.AsignarRequest;
import cl.duocuc.seguros.analista.dto.SiniestroDetalleRemoto;
import cl.duocuc.seguros.analista.dto.SiniestroRemoto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// liquidador-service es el unico dueno de los siniestros
@FeignClient(name = "liquidador-service")
public interface LiquidadorClient {

    @GetMapping("/liquidador/consultas/analista/siniestros")
    List<SiniestroRemoto> siniestros(@RequestHeader("Authorization") String authorization,
                                     @RequestParam(value = "estado", required = false) String estado);

    @GetMapping("/liquidador/consultas/analista/siniestros/{numero}")
    SiniestroDetalleRemoto detalle(@RequestHeader("Authorization") String authorization,
                                   @PathVariable("numero") String numero);

    @PutMapping("/liquidador/consultas/analista/siniestros/{numero}/liquidador")
    SiniestroRemoto reasignar(@RequestHeader("Authorization") String authorization,
                              @PathVariable("numero") String numero,
                              @RequestBody AsignarRequest body);
}

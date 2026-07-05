package br.com.fiscalizacao.client;

import br.com.fiscalizacao.dto.OcorrenciaGeoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "ms-geo", url = "${ms-geo.url}")
public interface GeoClient {

    @PostMapping("/api/geo/ocorrencias")
    void registrarNoMapa(@RequestBody OcorrenciaGeoDTO dto);

    @PatchMapping("/api/geo/ocorrencias/{id}/apoiar")
    void atualizarApoio(@PathVariable("id") Long id, @RequestBody Map<String, Integer> body);
}

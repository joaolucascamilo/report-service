package br.com.fiscalizacao.client;

import br.com.fiscalizacao.dto.OcorrenciaGeoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ms-geo", url = "http://localhost:8084")
public interface GeoClient {

    @PostMapping("/api/geo/ocorrencias")
    void registrarNoMapa(@RequestBody OcorrenciaGeoDTO dto);
}

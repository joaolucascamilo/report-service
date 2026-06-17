package br.com.fiscalizacao.client;

import br.com.fiscalizacao.dto.OcorrenciaPriorizacaoDTO;
import br.com.fiscalizacao.dto.PrioridadeResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ms-priorizacao", url = "${ms-priorizacao.url}")
public interface PriorizacaoClient {

    @PostMapping("/api/priorizacao/calcular")
    PrioridadeResponseDTO calcularPrioridade(@RequestBody OcorrenciaPriorizacaoDTO dto);

    @PutMapping("/api/priorizacao/recalcular/{ocorrenciaId}")
    PrioridadeResponseDTO recalcularPrioridade(
            @PathVariable Long ocorrenciaId,
            @RequestBody OcorrenciaPriorizacaoDTO dto
    );
}

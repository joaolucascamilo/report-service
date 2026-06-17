package br.com.fiscalizacao.dto;

import br.com.fiscalizacao.enums.NivelPrioridade;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrioridadeResponseDTO {

    private Long ocorrenciaId;
    private NivelPrioridade nivelPrioridade;
    private Integer scoreCalculado;
    private String justificativa;     // explica por que recebeu aquela prioridade
}

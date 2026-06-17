package br.com.fiscalizacao.dto;

import br.com.fiscalizacao.entity.Endereco;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OcorrenciaRequest {

    private EnderecoRequest enderecoOcorrencia;
    private Integer tipoOcorrencia;
    private List<FotoOcorrenciaRequest> fotoOcorrencia;
    private Double latitude;
    private Double longitude;

}

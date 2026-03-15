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

    private Long usuarioId;
    private Endereco enderecoOcorrencia;
    private Integer tipoOcorrencia;
    private List<FotoOcorrenciaRequest> fotoOcorrencia;

}

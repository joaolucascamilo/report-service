package br.com.fiscalizacao.dto;

import br.com.fiscalizacao.entity.Endereco;
import br.com.fiscalizacao.entity.FotoOcorrencia;
import br.com.fiscalizacao.entity.Usuario;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class OcorrenciaRequest {

    private Usuario usuario;
    private Endereco enderecoOcorrencia;
    private String status;
    private Integer tipoOcorrencia;
    private List<FotoOcorrenciaRequest> fotoOcorrencia;

}

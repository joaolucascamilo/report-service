package br.com.fiscalizacao.dto;

import br.com.fiscalizacao.entity.Endereco;
import br.com.fiscalizacao.entity.Usuario;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class OcorrenciaResponse {

    private Long id;
    private LocalDateTime data;
    private String status;
    private String nomeUsuario;
    private EnderecoResponse endereco;

    public OcorrenciaResponse(Long id, LocalDateTime data, String status, String nomeUsuario) {
        this.id = id;
        this.data = data;
        this.status = status;
        this.nomeUsuario = nomeUsuario;
    }

    public OcorrenciaResponse() {

    }
}

package br.com.fiscalizacao.dto;

import br.com.fiscalizacao.entity.Endereco;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EnderecoResponse {
    private String rua;
    private Integer numero;
    private String bairro;
    private String cidade;
    private String estado;
    private Integer cep;

    public EnderecoResponse() {}
}
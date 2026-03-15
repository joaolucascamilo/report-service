package br.com.fiscalizacao.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnderecoResponse {
    private String rua;
    private Integer numero;
    private String bairro;
    private String cidade;
    private String estado;
    private Integer cep;

}
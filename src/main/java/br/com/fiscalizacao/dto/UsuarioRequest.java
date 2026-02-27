package br.com.fiscalizacao.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioRequest {
    private String nome;
    private String email;
    private Integer telefone;
}

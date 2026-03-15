package br.com.fiscalizacao.enums;

public enum StatusOcorrencia {

    REGISTRADO(1, "Registrado"),
    CANCELADO(2, "Cancelado"),
    RESOLVIDO(3, "Resolvido"),
    EM_PROCEDIMENTO(4, "Em Procedimento");

    private final int codigo;
    private final String descricao;

    // Construtor do Enum
    StatusOcorrencia(int codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    // Getters para acessar os valores
    public int getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    // Método utilitário para converter um número (ex: 1) de volta para o Enum (REGISTRADO)
    // Isso será muito útil quando o frontend enviar apenas o número no JSON
    public static StatusOcorrencia valueOf(int codigo) {
        for (StatusOcorrencia status : StatusOcorrencia.values()) {
            if (status.getCodigo() == codigo) {
                return status;
            }
        }
        throw new IllegalArgumentException("Código de status de ocorrência inválido: " + codigo);
    }
}
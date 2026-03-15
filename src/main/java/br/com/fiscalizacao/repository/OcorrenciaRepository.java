package br.com.fiscalizacao.repository;

import br.com.fiscalizacao.entity.Ocorrencia;
import br.com.fiscalizacao.enums.StatusOcorrencia;
import br.com.fiscalizacao.enums.TipoOcorrencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long> {

    @Query("SELECT COUNT(o) > 0 FROM Ocorrencia o " +
            "WHERE o.tipo = :tipo " +
            "AND o.status IN :statusAbertos " +
            "AND o.endereco.cep = :cep " +
            "AND o.endereco.rua = :rua")
    boolean existeOcorrenciaPendenteNoLocal(
            @Param("tipo") TipoOcorrencia tipo,
            @Param("statusAbertos") List<StatusOcorrencia> statusAbertos,
            @Param("cep") Integer cep,
            @Param("rua") String rua
    );
}

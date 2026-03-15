package br.com.fiscalizacao.service;

import br.com.fiscalizacao.dto.EnderecoResponse;
import br.com.fiscalizacao.dto.FotoOcorrenciaRequest;
import br.com.fiscalizacao.dto.OcorrenciaRequest;
import br.com.fiscalizacao.dto.OcorrenciaResponse;
import br.com.fiscalizacao.entity.Endereco;
import br.com.fiscalizacao.entity.FotoOcorrencia;
import br.com.fiscalizacao.entity.Ocorrencia;
import br.com.fiscalizacao.entity.Usuario;
import br.com.fiscalizacao.enums.StatusOcorrencia;
import br.com.fiscalizacao.enums.TipoOcorrencia;
import br.com.fiscalizacao.repository.OcorrenciaRepository;
import br.com.fiscalizacao.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OcorrenciaService {

    @Autowired
    private OcorrenciaRepository ocorrenciaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    // Injeção de dependência via construtor
    public OcorrenciaService(OcorrenciaRepository repository) {
        this.ocorrenciaRepository = repository;
    }

    @Transactional
    public OcorrenciaResponse registrar(OcorrenciaRequest ocorrenciaRequest) {
        // 1. Buscar o usuário no banco de dados pelo ID enviado no Request
        Usuario usuario;
        if (ocorrenciaRequest.getUsuarioId() != null) {
            usuario = usuarioRepository.findById(ocorrenciaRequest.getUsuarioId())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        } else {
            throw new RuntimeException("Usuário ou ID nulo");
        }

        validaDuplicidadeOcorrencia(ocorrenciaRequest);

        Endereco endereco = converteEnderecoRequestParaEndereco(ocorrenciaRequest);

        Ocorrencia ocorrencia = Ocorrencia.builder()
                .data(LocalDateTime.now())
                .endereco(endereco)
                .usuario(usuario)
                .status(StatusOcorrencia.REGISTRADO)
                .tipo(TipoOcorrencia.valueOf(ocorrenciaRequest.getTipoOcorrencia()))
                .build();

        //Lidar com as fotos, se a lista não for nula ou vazia
        if (ocorrenciaRequest.getFotoOcorrencia() != null && !ocorrenciaRequest.getFotoOcorrencia().isEmpty()) {
            for (FotoOcorrenciaRequest fotoReq : ocorrenciaRequest.getFotoOcorrencia()) {
                // Instancia a entidade usando o construtor da sua classe
                FotoOcorrencia foto = new FotoOcorrencia(
                        fotoReq.getNomeArquivo(),
                        fotoReq.getUrl(),
                        fotoReq.getBucket()
                );
                // Usa o seu helper method para amarrar os dois lados do relacionamento
                ocorrencia.adicionarFoto(foto);
            }
        }

        Ocorrencia ocorrenciaSalva = ocorrenciaRepository.save(ocorrencia);

        return converterParaResponse(ocorrenciaSalva);
    }

    private void validaDuplicidadeOcorrencia(OcorrenciaRequest ocorrenciaRequest) {
        TipoOcorrencia tipoDaNovaOcorrencia = TipoOcorrencia.valueOf(ocorrenciaRequest.getTipoOcorrencia());

        List<StatusOcorrencia> statusAbertos = List.of(
                StatusOcorrencia.REGISTRADO,
                StatusOcorrencia.EM_PROCEDIMENTO
        );

        // Verifica no banco de dados se já existe duplicidade
        boolean isDuplicado = ocorrenciaRepository.existeOcorrenciaPendenteNoLocal(
                tipoDaNovaOcorrencia,
                statusAbertos,
                ocorrenciaRequest.getEnderecoOcorrencia().getCep(),
                ocorrenciaRequest.getEnderecoOcorrencia().getRua()
        );

        if (isDuplicado) {
            throw new IllegalArgumentException("Já existe um problema deste tipo reportado neste endereço que ainda está aguardando solução.");
        }
    }

    private static Endereco converteEnderecoRequestParaEndereco(OcorrenciaRequest ocorrenciaRequest) {
        Endereco endereco = new Endereco();
        endereco.setCep(ocorrenciaRequest.getEnderecoOcorrencia().getCep());
        endereco.setRua(ocorrenciaRequest.getEnderecoOcorrencia().getRua());
        endereco.setNumero(ocorrenciaRequest.getEnderecoOcorrencia().getNumero());
        endereco.setBairro(ocorrenciaRequest.getEnderecoOcorrencia().getBairro());
        endereco.setCidade(ocorrenciaRequest.getEnderecoOcorrencia().getCidade());
        endereco.setEstado(ocorrenciaRequest.getEnderecoOcorrencia().getEstado());
        return endereco;
    }

    public List<OcorrenciaResponse> listarTodas() {
        List<Ocorrencia> ocorrencias = ocorrenciaRepository.findAll();
        return ocorrencias.stream()
                .map(this::converterParaResponse)
                .toList();
    }

    public OcorrenciaResponse buscarPorId(Long id) {
        Ocorrencia ocorrencia = ocorrenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ocorrência não encontrada com o ID: " + id));
        return converterParaResponse(ocorrencia);
    }

    public OcorrenciaResponse atualizarStatus(Long id, Integer codigoStatus) {

        // Converte o número que veio do frontend para o Enum correspondente
        StatusOcorrencia novoStatusEnum = StatusOcorrencia.valueOf(codigoStatus);

        Ocorrencia ocorrenciaAtualizada = ocorrenciaRepository.findById(id).map(ocorrencia -> {
            ocorrencia.setStatus(novoStatusEnum);
            return ocorrenciaRepository.save(ocorrencia);
        }).orElseThrow(() -> new RuntimeException("Ocorrência não encontrada com o ID: " + id));

        // Passa a entidade atualizada pelo método de conversão e devolve o DTO
        return converterParaResponse(ocorrenciaAtualizada);
    }

    public void deletar(Long id) {
        ocorrenciaRepository.deleteById(id);
    }

    // Método utilitário privado para manter o código limpo
    private OcorrenciaResponse converterParaResponse(Ocorrencia ocorrencia) {
        OcorrenciaResponse response = new OcorrenciaResponse();
        response.setId(ocorrencia.getId());
        response.setData(ocorrencia.getData());
        response.setStatus(ocorrencia.getStatus().getCodigo());

        // Aqui evitamos expor todos os dados do usuário, mandando apenas o nome
        response.setNomeUsuario(ocorrencia.getUsuario().getNome());
        List<OcorrenciaResponse.FotoResponse> fotosResponse = null;
        if (ocorrencia.getFotos() != null && !ocorrencia.getFotos().isEmpty()) {
            fotosResponse = ocorrencia.getFotos().stream()
                    .map(this::converterParaFotoResponse) // Chama aquele método que criamos para cada foto da lista
                    .toList();
        }
        response.setFotos(fotosResponse);

        // Convertendo o endereço
        EnderecoResponse endRes = new EnderecoResponse();
        endRes.setCep(ocorrencia.getEndereco().getCep());
        endRes.setRua(ocorrencia.getEndereco().getRua());
        endRes.setNumero(ocorrencia.getEndereco().getNumero());
        endRes.setBairro(ocorrencia.getEndereco().getBairro());
        endRes.setCidade(ocorrencia.getEndereco().getCidade());
        endRes.setEstado(ocorrencia.getEndereco().getEstado());

        response.setEndereco(endRes);

        return response;
    }

    // Método auxiliar para converter uma única FotoOcorrencia (Entidade) para FotoResponse (DTO)
    private OcorrenciaResponse.FotoResponse converterParaFotoResponse(FotoOcorrencia foto) {
        if (foto == null) return null;

        return OcorrenciaResponse.FotoResponse.builder()
                .id(foto.getId())
                .url(foto.getUrl())
                .build();
    }
}

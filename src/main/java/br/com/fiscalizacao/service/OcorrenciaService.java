package br.com.fiscalizacao.service;

import br.com.fiscalizacao.dto.EnderecoResponse;
import br.com.fiscalizacao.dto.FotoOcorrenciaRequest;
import br.com.fiscalizacao.dto.OcorrenciaRequest;
import br.com.fiscalizacao.dto.OcorrenciaResponse;
import br.com.fiscalizacao.entity.Endereco;
import br.com.fiscalizacao.entity.FotoOcorrencia;
import br.com.fiscalizacao.entity.Ocorrencia;
import br.com.fiscalizacao.entity.Usuario;
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
        if (ocorrenciaRequest.getUsuario() != null && ocorrenciaRequest.getUsuario().getId() != null) {
            usuario = usuarioRepository.findById(ocorrenciaRequest.getUsuario().getId())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        } else {
            throw new RuntimeException("Usuário ou ID nulo");
        }

        Endereco endereco = converteEnderecoRequestParaEndereco(ocorrenciaRequest);

        Ocorrencia ocorrencia = new Ocorrencia(LocalDateTime.now(), endereco, usuario);
        ocorrencia.setStatus("REGISTRADO");

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

    public List<Ocorrencia> listarTodas() {
        return ocorrenciaRepository.findAll();
    }

    public Optional<Ocorrencia> buscarPorId(Long id) {
        return ocorrenciaRepository.findById(id);
    }

    public Ocorrencia atualizarStatus(Long id, String novoStatus) {
        return ocorrenciaRepository.findById(id).map(ocorrencia -> {
            ocorrencia.setStatus(novoStatus);
            return ocorrenciaRepository.save(ocorrencia);
        }).orElseThrow(() -> new RuntimeException("Ocorrência não encontrada!"));
    }

    public void deletar(Long id) {
        ocorrenciaRepository.deleteById(id);
    }

    // Método utilitário privado para manter o código limpo
    private OcorrenciaResponse converterParaResponse(Ocorrencia ocorrencia) {
        OcorrenciaResponse response = new OcorrenciaResponse();
        response.setId(ocorrencia.getId());
        response.setData(ocorrencia.getData());
        response.setStatus(ocorrencia.getStatus());

        // Aqui evitamos expor todos os dados do usuário, mandando apenas o nome
        response.setNomeUsuario(ocorrencia.getUsuario().getNome());

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
}

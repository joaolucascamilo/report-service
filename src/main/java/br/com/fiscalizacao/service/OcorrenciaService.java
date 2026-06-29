package br.com.fiscalizacao.service;

import br.com.fiscalizacao.client.GeoClient;
import br.com.fiscalizacao.client.PriorizacaoClient;
import br.com.fiscalizacao.client.UserClient;
import br.com.fiscalizacao.dto.*;
import br.com.fiscalizacao.entity.Endereco;
import br.com.fiscalizacao.entity.FotoOcorrencia;
import br.com.fiscalizacao.entity.Ocorrencia;
import br.com.fiscalizacao.enums.StatusOcorrencia;
import br.com.fiscalizacao.enums.TipoOcorrencia;
import br.com.fiscalizacao.exception.ConflictException;
import br.com.fiscalizacao.repository.OcorrenciaRepository;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OcorrenciaService {

    @Autowired
    private OcorrenciaRepository ocorrenciaRepository;

    private final UserClient userClient;

    private final GeoClient geoClient;

    private final PriorizacaoClient  priorizacaoClient;

    private final S3Service s3Service;

    public OcorrenciaService(OcorrenciaRepository repository, UserClient userClient, GeoClient geoClient, PriorizacaoClient priorizacaoClient, S3Service s3Service) {
        this.ocorrenciaRepository = repository;
        this.userClient = userClient;
        this.geoClient = geoClient;
        this.priorizacaoClient = priorizacaoClient;
        this.s3Service = s3Service;
    }

    @Transactional
    public OcorrenciaResponse registrar(OcorrenciaRequest ocorrenciaRequest, Long usuarioId) {
        validaDuplicidadeOcorrencia(ocorrenciaRequest);

        Endereco endereco = converteEnderecoRequestParaEndereco(ocorrenciaRequest);

        Ocorrencia ocorrencia = Ocorrencia.builder()
                .data(LocalDateTime.now())
                .endereco(endereco)
                .usuarioId(usuarioId)
                .status(StatusOcorrencia.REGISTRADO)
                .tipo(TipoOcorrencia.valueOf(ocorrenciaRequest.getTipoOcorrencia()))
                .detalhes(ocorrenciaRequest.getDetalhes())
                .latitude(ocorrenciaRequest.getLatitude())
                .longitude(ocorrenciaRequest.getLongitude())
                .quantidadeDenuncias(1)
                .build();

        if (ocorrenciaRequest.getFotoOcorrencia() != null && !ocorrenciaRequest.getFotoOcorrencia().isEmpty()) {
            for (FotoOcorrenciaRequest fotoReq : ocorrenciaRequest.getFotoOcorrencia()) {
                FotoOcorrencia foto = new FotoOcorrencia(
                        fotoReq.getNomeArquivo(),
                        fotoReq.getUrl(),
                        fotoReq.getBucket()
                );
                ocorrencia.adicionarFoto(foto);
            }
        }

        Ocorrencia ocorrenciaSalva = ocorrenciaRepository.save(ocorrencia);

        // Monta o DTO para o ms-geo
        OcorrenciaGeoDTO geoDTO = new OcorrenciaGeoDTO();
        geoDTO.setId(ocorrenciaSalva.getId());
        geoDTO.setCategoria(ocorrenciaSalva.getTipo().getDescricao());
        geoDTO.setStatus(StatusOcorrencia.REGISTRADO.getDescricao());
        geoDTO.setQuantidadeDenuncias(1);
        geoDTO.setDataCriacao(ocorrenciaSalva.getData());

        // Decide o caminho: coordenadas diretas OU endereço para geocodificar
        if (ocorrenciaRequest.getLatitude() != null && ocorrenciaRequest.getLongitude() != null) {
            geoDTO.setLatitude(ocorrenciaRequest.getLatitude());
            geoDTO.setLongitude(ocorrenciaRequest.getLongitude());
        } else {
            // Passa o endereço para o ms-geo geocodificar via Nominatim
            EnderecoRequest end = ocorrenciaRequest.getEnderecoOcorrencia();
            geoDTO.setRua(end.getRua());
            geoDTO.setBairro(end.getBairro());
            geoDTO.setCidade(end.getCidade());
            geoDTO.setEstado(end.getEstado());
            geoDTO.setPais("Brasil");
        }

        try {
            geoClient.registrarNoMapa(geoDTO);
        } catch (Exception e) {
            // Não deixa a ocorrência falhar se o ms-geo estiver fora do ar
            System.err.println("Erro ao notificar ms-geo: " + e.getMessage());
        }

        // Após salvar a ocorrência
        OcorrenciaPriorizacaoDTO priorizacaoDTO = new OcorrenciaPriorizacaoDTO(
                ocorrenciaSalva.getId(),
                ocorrenciaSalva.getTipo().name(),
                1,
                ocorrenciaSalva.getData(),
                ocorrenciaRequest.getLatitude(),
                ocorrenciaRequest.getLongitude()
        );

        try {
            PrioridadeResponseDTO prioridade = priorizacaoClient.calcularPrioridade(priorizacaoDTO);
            ocorrenciaSalva.setNivelPrioridade(prioridade.getNivelPrioridade().name());
            ocorrenciaRepository.save(ocorrenciaSalva);
        } catch (Exception e) {
            System.err.println("Erro ao calcular prioridade: " + e.getMessage());
        }

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
        EnderecoRequest endReq = ocorrenciaRequest.getEnderecoOcorrencia();
        Endereco endereco = new Endereco();
        endereco.setCep(endReq.getCep());
        endereco.setRua(endReq.getRua());
        endereco.setNumero(endReq.getNumero());
        endereco.setBairro(endReq.getBairro());
        endereco.setCidade(endReq.getCidade());
        endereco.setEstado(endReq.getEstado());
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

    public OcorrenciaResponse atualizarStatus(Long id, Integer codigoStatus, String tokenAgente) {

        StatusOcorrencia novoStatusEnum = StatusOcorrencia.valueOf(codigoStatus);

        Ocorrencia ocorrenciaAtualizada = ocorrenciaRepository.findById(id).map(ocorrencia -> {
            ocorrencia.setStatus(novoStatusEnum);
            return ocorrenciaRepository.save(ocorrencia);
        }).orElseThrow(() -> new RuntimeException("Ocorrência não encontrada com o ID: " + id));

        // Recalcula prioridade sempre que o status muda
        // (tempo passou, denúncias podem ter aumentado)
        try {
            OcorrenciaPriorizacaoDTO dto = new OcorrenciaPriorizacaoDTO(
                    ocorrenciaAtualizada.getId(),
                    ocorrenciaAtualizada.getTipo().name(),
                    ocorrenciaAtualizada.getQuantidadeDenuncias() != null
                            ? ocorrenciaAtualizada.getQuantidadeDenuncias() : 1,
                    ocorrenciaAtualizada.getData(),
                    ocorrenciaAtualizada.getLatitude(),
                    ocorrenciaAtualizada.getLongitude()
            );
            PrioridadeResponseDTO prioridade = priorizacaoClient.recalcularPrioridade(
                    ocorrenciaAtualizada.getId(), dto
            );
            ocorrenciaAtualizada.setNivelPrioridade(prioridade.getNivelPrioridade().name());
            ocorrenciaRepository.save(ocorrenciaAtualizada);
        } catch (Exception e) {
            System.err.println("Erro ao recalcular prioridade: " + e.getMessage());
        }

        // Pontua o cidadão se resolvido
        if (codigoStatus.equals(StatusOcorrencia.RESOLVIDO.getCodigo())) {
            try {
                PontuacaoRequestDTO pontuacao = new PontuacaoRequestDTO(
                        ocorrenciaAtualizada.getUsuarioId(),
                        50,
                        "Ocorrência ID " + ocorrenciaAtualizada.getId() + " resolvida pela prefeitura."
                );
                userClient.pontuarUsuario(pontuacao, tokenAgente);
            } catch (Exception e) {
                System.err.println("Erro ao atribuir pontos: " + e.getMessage());
            }
        }

        return converterParaResponse(ocorrenciaAtualizada);
    }

    @Transactional
    public OcorrenciaResponse apoiar(Long id, Long usuarioId) {
        Ocorrencia ocorrencia = ocorrenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ocorrência não encontrada com o ID: " + id));

        if (ocorrencia.getApoiadores().contains(usuarioId)) {
            throw new ConflictException("Você já apoiou esta ocorrência.");
        }

        ocorrencia.getApoiadores().add(usuarioId);
        int novaQuantidade = (ocorrencia.getQuantidadeDenuncias() != null ? ocorrencia.getQuantidadeDenuncias() : 1) + 1;
        ocorrencia.setQuantidadeDenuncias(novaQuantidade);
        ocorrenciaRepository.save(ocorrencia);

        try {
            geoClient.atualizarApoio(id, Map.of("quantidadeDenuncias", novaQuantidade));
        } catch (Exception e) {
            System.err.println("Erro ao notificar ms-geo sobre apoio: " + e.getMessage());
        }

        try {
            OcorrenciaPriorizacaoDTO dto = new OcorrenciaPriorizacaoDTO(
                    ocorrencia.getId(),
                    ocorrencia.getTipo().name(),
                    novaQuantidade,
                    ocorrencia.getData(),
                    ocorrencia.getLatitude(),
                    ocorrencia.getLongitude()
            );
            PrioridadeResponseDTO prioridade = priorizacaoClient.recalcularPrioridade(ocorrencia.getId(), dto);
            ocorrencia.setNivelPrioridade(prioridade.getNivelPrioridade().name());
            ocorrenciaRepository.save(ocorrencia);
        } catch (Exception e) {
            System.err.println("Erro ao recalcular prioridade após apoio: " + e.getMessage());
        }

        return converterParaResponse(ocorrencia);
    }

    public void deletar(Long id) {
        Ocorrencia ocorrencia = ocorrenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ocorrência não encontrada com o ID: " + id));

        if (ocorrencia.getFotos() != null) {
            ocorrencia.getFotos().forEach(foto -> s3Service.deletar(foto.getUrl()));
        }

        ocorrenciaRepository.deleteById(id);
    }

    // Método utilitário privado para manter o código limpo
    private OcorrenciaResponse converterParaResponse(Ocorrencia ocorrencia) {
        OcorrenciaResponse response = new OcorrenciaResponse();
        response.setId(ocorrencia.getId());
        response.setData(ocorrencia.getData());
        response.setStatus(ocorrencia.getStatus().getCodigo());
        response.setTipo(ocorrencia.getTipo().getCodigo());
        response.setDescricaoTipo(ocorrencia.getTipo().getDescricao());
        response.setNivelPrioridade(ocorrencia.getNivelPrioridade());
        response.setDetalhes(ocorrencia.getDetalhes());
        response.setQuantidadeDenuncias(ocorrencia.getQuantidadeDenuncias());

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
                .url(s3Service.gerarUrlLeitura(foto.getUrl()))
                .build();
    }

    public Optional<OcorrenciaResponse> verificarDuplicidade(String tipoStr, String rua, String bairro) {
        TipoOcorrencia tipo = TipoOcorrencia.valueOf(tipoStr);
        List<StatusOcorrencia> statusAbertos = List.of(StatusOcorrencia.REGISTRADO, StatusOcorrencia.EM_PROCEDIMENTO);

        return ocorrenciaRepository
                .findAtivasPorTipoEEndereco(tipo, statusAbertos, rua, bairro)
                .stream()
                .findFirst()
                .map(this::converterParaResponse);
    }

    public @Nullable List<OcorrenciaResponse> listarPorUsuarioId(Long usuarioId) {
        List<Ocorrencia> ocorrencias = ocorrenciaRepository.findByUsuarioId(usuarioId);
        return ocorrencias.stream()
                .map(this::converterParaResponse)
                .toList();
    }
}

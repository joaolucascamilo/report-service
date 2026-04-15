package br.com.fiscalizacao.controller;

import br.com.fiscalizacao.dto.OcorrenciaRequest;
import br.com.fiscalizacao.dto.OcorrenciaResponse;
import br.com.fiscalizacao.service.OcorrenciaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ocorrencias")
public class OcorrenciaController {

    private final OcorrenciaService service;

    public OcorrenciaController(OcorrenciaService service) {
        this.service = service;
    }

    /**
     * Pega o ID do usuário diretamente do Token JWT que foi interceptado pelo SecurityFilter
     */
    private Long getUsuarioIdAutenticado() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /**
     * ROTAS DO CIDADÃO (Exige perfil ROLE_CIDADAO no SecurityConfig)
     */

    @PostMapping
    public ResponseEntity<OcorrenciaResponse> registrarOcorrencia(@RequestBody OcorrenciaRequest ocorrencia) {
        OcorrenciaResponse novaOcorrencia = service.registrar(ocorrencia);
        return new ResponseEntity<>(novaOcorrencia, HttpStatus.CREATED);
    }

    @GetMapping("/minhas")
    public ResponseEntity<List<OcorrenciaResponse>> listarMinhasOcorrencias() {
        Long usuarioId = getUsuarioIdAutenticado();

        return ResponseEntity.ok(service.listarPorUsuarioId(usuarioId));
    }

    /**
     * ROTAS DA PREFEITURA / AGENTE (Exige perfil ROLE_AGENTE_PREFEITURA)
     */

    @GetMapping
    public ResponseEntity<List<OcorrenciaResponse>> listarOcorrencias() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OcorrenciaResponse> atualizarStatus(@PathVariable Long id, @RequestBody Integer codigoStatus) {
        try {
            OcorrenciaResponse ocorrenciaAtualizada = service.atualizarStatus(id, codigoStatus);
            return ResponseEntity.ok(ocorrenciaAtualizada);

        } catch (IllegalArgumentException e) {
            // Cai aqui se o frontend mandar um código de status que não existe no Enum (ex: 99)
            return ResponseEntity.badRequest().build();

        } catch (RuntimeException e) {
            // Cai aqui se o ID da ocorrência não for encontrado no banco
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarOcorrencia(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * ROTAS PÚBLICAS / COMPARTILHADAS
     */

    @GetMapping("/mapa")
    public ResponseEntity<List<OcorrenciaResponse>> listarParaMapa() {
        // Rota pública para alimentar o mapa do frontend sem exigir login (se desejar)
        // O ideal é que o service retorne um DTO simplificado apenas com Latitude, Longitude e Status
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OcorrenciaResponse> buscarPorId(@PathVariable Long id) {
        try {
            OcorrenciaResponse response = service.buscarPorId(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

package br.com.fiscalizacao.controller;

import br.com.fiscalizacao.dto.OcorrenciaRequest;
import br.com.fiscalizacao.dto.OcorrenciaResponse;
import br.com.fiscalizacao.entity.Ocorrencia;
import br.com.fiscalizacao.service.OcorrenciaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ocorrencias")
public class OcorrenciaController {

    private final OcorrenciaService service;

    public OcorrenciaController(OcorrenciaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OcorrenciaResponse> registrarOcorrencia(@RequestBody OcorrenciaRequest ocorrencia) {
        OcorrenciaResponse novaOcorrencia = service.registrar(ocorrencia);
        return new ResponseEntity<>(novaOcorrencia, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Ocorrencia>> listarOcorrencias() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ocorrencia> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Ocorrencia> atualizarStatus(@PathVariable Long id, @RequestBody String novoStatus) {
        try {
            Ocorrencia ocorrenciaAtualizada = service.atualizarStatus(id, novoStatus);
            return ResponseEntity.ok(ocorrenciaAtualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarOcorrencia(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

package br.com.fiscalizacao.exception;

import br.com.fiscalizacao.dto.ErroResponse;
import br.com.fiscalizacao.exception.ConflictException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice //Escuta todas as requições que são chamadas no sistema
public class ResourceExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {

        ErroResponse erro = ErroResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .erro("Requisição Inválida")
                .mensagem(e.getMessage())
                .caminho(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErroResponse> handleConflictException(ConflictException e, HttpServletRequest request) {

        ErroResponse erro = ErroResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .erro("Conflito")
                .mensagem(e.getMessage())
                .caminho(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro);
    }

}

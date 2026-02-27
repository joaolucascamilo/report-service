package br.com.fiscalizacao.repository;

import br.com.fiscalizacao.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}

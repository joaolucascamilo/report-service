package br.com.fiscalizacao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity //Avisa ao Spring Boot para desligar a segurança padrão (que bloqueia tudo) e usar as regras que a classe vai definir.
public class SecurityConfig {

    private final SecurityFilter securityFilter;

    public SecurityConfig(SecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Desabilitamos CSRF pois o JWT já protege contra isso
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // Rotas exclusivas da Prefeitura
                        .requestMatchers(HttpMethod.GET, "/api/ocorrencias/todas").hasRole("AGENTE_PREFEITURA")
                        .requestMatchers(HttpMethod.PUT, "/api/ocorrencias/{id}/status").hasRole("AGENTE_PREFEITURA")

                        // Rotas exclusivas do Cidadão
                        .requestMatchers(HttpMethod.POST, "/api/ocorrencias").hasRole("CIDADAO")
                        .requestMatchers(HttpMethod.GET, "/api/ocorrencias/minhas").hasRole("CIDADAO")

                        // Mapa público (opcional: se qualquer pessoa puder ver o mapa sem logar)
                        .requestMatchers(HttpMethod.GET, "/api/ocorrencias/mapa").permitAll()

                        .anyRequest().authenticated()
                )
        // Coloca o nosso filtro antes do filtro padrão do Spring
        .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Bean responsável por criptografar as senhas antes de salvar no banco
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

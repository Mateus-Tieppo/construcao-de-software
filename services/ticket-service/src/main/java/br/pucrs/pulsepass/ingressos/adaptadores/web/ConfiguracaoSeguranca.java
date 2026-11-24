package br.pucrs.pulsepass.ingressos.adaptadores.web;

import br.pucrs.pulsepass.commons.seguranca.PapeisKeycloak;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** Cliente vê os próprios ingressos; operador valida QR; admin vê estatísticas. */
@Configuration
public class ConfiguracaoSeguranca {

  @Bean
  public SecurityFilterChain filtros(HttpSecurity http) throws Exception {
    http.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a
            .requestMatchers("/actuator/health/**", "/actuator/prometheus", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
            .requestMatchers("/validacoes/**").hasRole("OPERADOR")
            .requestMatchers("/ingressos/estatisticas").hasRole("ADMIN")
            .requestMatchers("/ingressos/**").hasRole("CLIENTE")
            .anyRequest().authenticated())
        .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(new PapeisKeycloak())));
    return http.build();
  }
}

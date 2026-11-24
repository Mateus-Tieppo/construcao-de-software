package br.pucrs.pulsepass.inventario.adaptadores.web;

import br.pucrs.pulsepass.commons.seguranca.PapeisKeycloak;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** Cada serviço revalida o JWT do Keycloak (RNF18) e aplica RBAC por rota. */
@Configuration
public class ConfiguracaoSeguranca {

  @Bean
  public SecurityFilterChain filtros(HttpSecurity http) throws Exception {
    http.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a
            .requestMatchers("/actuator/health/**", "/actuator/prometheus", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/eventos/**").permitAll()
            .requestMatchers("/internal/**").permitAll()
            .requestMatchers("/organizador/**").hasRole("ORGANIZADOR")
            .anyRequest().authenticated())
        .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(new PapeisKeycloak())));
    return http.build();
  }
}

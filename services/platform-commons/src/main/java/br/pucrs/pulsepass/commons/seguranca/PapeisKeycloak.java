package br.pucrs.pulsepass.commons.seguranca;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Converte os papéis do realm do Keycloak (claim realm_access.roles) em authorities ROLE_*.
 * Papéis do enunciado: cliente, organizador, operador, admin (RBAC).
 */
public final class PapeisKeycloak implements Converter<Jwt, AbstractAuthenticationToken> {

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    return new JwtAuthenticationToken(jwt, extrair(jwt), jwt.getSubject());
  }

  public static Collection<GrantedAuthority> extrair(Jwt jwt) {
    List<GrantedAuthority> autoridades = new ArrayList<>();
    Object realm = jwt.getClaims().get("realm_access");
    if (realm instanceof Map<?, ?> mapa && mapa.get("roles") instanceof Collection<?> papeis) {
      for (Object papel : papeis) {
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + papel.toString().toUpperCase()));
      }
    }
    return autoridades;
  }
}

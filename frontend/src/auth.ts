import Keycloak from 'keycloak-js';

/** Login OIDC + PKCE no Keycloak (US01). O access token fica só em memória. */
export const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8180',
  realm: 'pulsepass',
  clientId: 'pulsepass-spa',
});

export async function iniciarAuth(): Promise<boolean> {
  try {
    return await keycloak.init({ onLoad: 'check-sso', pkceMethod: 'S256', checkLoginIframe: false });
  } catch {
    return false;
  }
}

export function temPapel(papel: 'cliente' | 'organizador' | 'operador' | 'admin'): boolean {
  return keycloak.hasRealmRole(papel);
}

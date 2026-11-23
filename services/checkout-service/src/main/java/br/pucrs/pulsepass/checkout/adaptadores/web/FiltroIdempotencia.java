package br.pucrs.pulsepass.checkout.adaptadores.web;

import br.pucrs.pulsepass.checkout.adaptadores.persistencia.IdempotenciaRepositorio;
import br.pucrs.pulsepass.checkout.dominio.RegraIdempotencia;
import br.pucrs.pulsepass.checkout.dominio.RegraNegocioException;
import br.pucrs.pulsepass.commons.seguranca.ErroApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Suporte nativo a Idempotency-Key (enunciado; RNF05; Pacote 2 seção 1.5.4).
 * POST em /pedidos exige a chave. Mesma chave + mesmo corpo: devolve a resposta gravada.
 * Mesma chave + corpo diferente: 422. Primeira requisição ainda em andamento: 409.
 */
public class FiltroIdempotencia extends OncePerRequestFilter {

  public static final String CABECALHO = "Idempotency-Key";

  private final IdempotenciaRepositorio repo;
  private final ObjectMapper mapper;

  public FiltroIdempotencia(IdempotenciaRepositorio repo, ObjectMapper mapper) {
    this.repo = repo;
    this.mapper = mapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest req) {
    return !HttpMethod.POST.matches(req.getMethod()) || !req.getRequestURI().startsWith("/pedidos");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
      throws ServletException, IOException {
    String chave = req.getHeader(CABECALHO);
    try {
      RegraIdempotencia.validarChave(chave);
    } catch (RegraNegocioException e) {
      erro(resp, 400, e.codigo(), e.getMessage());
      return;
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String usuario = auth == null ? "anonimo" : auth.getName();
    String escopo = req.getMethod() + " " + req.getRequestURI();
    CorpoEmCache corpo = new CorpoEmCache(req);
    String hash = RegraIdempotencia.hash(req.getMethod(), req.getRequestURI(), corpo.corpo());

    if (!repo.registrar(usuario, escopo, chave, hash)) {
      Optional<IdempotenciaRepositorio.Registro> existente = repo.buscar(usuario, escopo, chave);
      if (existente.isEmpty()) {
        erro(resp, 409, "IDEMPOTENCY_KEY_EM_USO", "Tente novamente em instantes");
        return;
      }
      IdempotenciaRepositorio.Registro r = existente.get();
      switch (RegraIdempotencia.decidir(r.hash(), hash, r.concluido())) {
        case CORPO_DIFERENTE -> erro(resp, 422, "IDEMPOTENCY_KEY_REUTILIZADA",
            "A mesma Idempotency-Key foi usada com outra requisição");
        case EM_ANDAMENTO -> erro(resp, 409, "REQUISICAO_EM_ANDAMENTO", "A requisição original ainda está em processamento");
        default -> {
          resp.setStatus(r.status());
          resp.setContentType(r.contentType());
          resp.setHeader("Idempotent-Replayed", "true");
          resp.getOutputStream().write(r.corpo() == null ? new byte[0] : r.corpo());
        }
      }
      return;
    }

    ContentCachingResponseWrapper resposta = new ContentCachingResponseWrapper(resp);
    try {
      chain.doFilter(corpo, resposta);
    } catch (IOException | ServletException | RuntimeException e) {
      repo.descartar(usuario, escopo, chave);
      throw e;
    }
    if (resposta.getStatus() >= 500) {
      repo.descartar(usuario, escopo, chave);
    } else {
      repo.concluir(usuario, escopo, chave, resposta.getStatus(),
          resposta.getContentType() == null ? MediaType.APPLICATION_JSON_VALUE : resposta.getContentType(),
          resposta.getContentAsByteArray());
    }
    resposta.copyBodyToResponse();
  }

  private void erro(HttpServletResponse resp, int status, String codigo, String mensagem) throws IOException {
    resp.setStatus(status);
    resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
    resp.setCharacterEncoding("UTF-8");
    mapper.writeValue(resp.getOutputStream(), ErroApi.de(codigo, mensagem));
  }
}

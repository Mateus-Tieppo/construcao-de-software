package br.pucrs.pulsepass.checkout.adaptadores.web;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Lê o corpo uma vez (para o hash da Idempotency-Key) e o entrega de novo ao controller. */
class CorpoEmCache extends HttpServletRequestWrapper {

  private final byte[] corpo;

  CorpoEmCache(HttpServletRequest req) throws IOException {
    super(req);
    this.corpo = req.getInputStream().readAllBytes();
  }

  byte[] corpo() {
    return corpo;
  }

  @Override
  public ServletInputStream getInputStream() {
    ByteArrayInputStream in = new ByteArrayInputStream(corpo);
    return new ServletInputStream() {
      @Override
      public boolean isFinished() {
        return in.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener listener) {
        throw new UnsupportedOperationException();
      }

      @Override
      public int read() {
        return in.read();
      }
    };
  }

  @Override
  public BufferedReader getReader() {
    return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
  }
}

package br.pucrs.pulsepass.inventario.dominio;

import java.util.ArrayList;
import java.util.List;

/** Gera os rótulos dos assentos de um setor (fila em letra + número), usado na criação do mapa (US11). */
public final class MapaAssentos {

  public static final int MAX_FILAS = 26;
  public static final int MAX_POR_FILA = 100;

  private MapaAssentos() {
  }

  public static List<String> rotulos(int filas, int assentosPorFila) {
    if (filas < 1 || filas > MAX_FILAS || assentosPorFila < 1 || assentosPorFila > MAX_POR_FILA) {
      throw new RegraNegocioException("MAPA_INVALIDO",
          "Use de 1 a " + MAX_FILAS + " filas e de 1 a " + MAX_POR_FILA + " assentos por fila");
    }
    List<String> rotulos = new ArrayList<>(filas * assentosPorFila);
    for (int f = 0; f < filas; f++) {
      char letra = (char) ('A' + f);
      for (int n = 1; n <= assentosPorFila; n++) {
        rotulos.add(letra + "-" + n);
      }
    }
    return rotulos;
  }
}

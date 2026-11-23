/** Funções puras de apresentação (testadas com Vitest). */

export function reais(centavos: number | null | undefined): string {
  if (centavos == null) return '—';
  return (centavos / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

/** Tempo restante da reserva no formato mm:ss; 00:00 quando expirou. */
export function contagemRegressiva(expiraEm: string | null | undefined, agora: number = Date.now()): string {
  if (!expiraEm) return '--:--';
  const restante = Math.max(0, Math.floor((new Date(expiraEm).getTime() - agora) / 1000));
  const m = Math.floor(restante / 60).toString().padStart(2, '0');
  const s = (restante % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
}

/** Ordena rótulos "A-10", "A-2", "B-1" por fila e número. */
export function compararAssentos(a: string, b: string): number {
  const [fa, na] = a.split('-');
  const [fb, nb] = b.split('-');
  return fa === fb ? Number(na) - Number(nb) : fa.localeCompare(fb);
}

export function agruparPorFila<T extends { rotulo: string }>(assentos: T[]): [string, T[]][] {
  const filas = new Map<string, T[]>();
  for (const a of [...assentos].sort((x, y) => compararAssentos(x.rotulo, y.rotulo))) {
    const fila = a.rotulo.split('-')[0];
    filas.set(fila, [...(filas.get(fila) ?? []), a]);
  }
  return [...filas.entries()];
}

export const LIMITE_ASSENTOS = 6;

export function alternarSelecao(selecionados: string[], id: string): string[] {
  if (selecionados.includes(id)) return selecionados.filter((s) => s !== id);
  if (selecionados.length >= LIMITE_ASSENTOS) return selecionados;
  return [...selecionados, id];
}

import { describe, expect, it } from 'vitest';
import { agruparPorFila, alternarSelecao, compararAssentos, contagemRegressiva, LIMITE_ASSENTOS, reais } from './formatos';

describe('formatos', () => {
  it('formata centavos em reais', () => {
    expect(reais(25000)).toContain('250,00');
    expect(reais(null)).toBe('—');
  });

  it('conta o tempo restante da reserva', () => {
    const agora = Date.parse('2026-11-23T20:00:00Z');
    expect(contagemRegressiva('2026-11-23T20:10:00Z', agora)).toBe('10:00');
    expect(contagemRegressiva('2026-11-23T20:00:05Z', agora)).toBe('00:05');
    expect(contagemRegressiva('2026-11-23T19:59:00Z', agora)).toBe('00:00');
    expect(contagemRegressiva(null, agora)).toBe('--:--');
  });

  it('ordena e agrupa assentos por fila', () => {
    expect(['A-10', 'B-1', 'A-2'].sort(compararAssentos)).toEqual(['A-2', 'A-10', 'B-1']);
    const filas = agruparPorFila([{ rotulo: 'B-1' }, { rotulo: 'A-2' }, { rotulo: 'A-1' }]);
    expect(filas.map(([f, a]) => [f, a.map((x) => x.rotulo)])).toEqual([['A', ['A-1', 'A-2']], ['B', ['B-1']]]);
  });

  it('limita a seleção a 6 assentos', () => {
    let sel: string[] = [];
    for (let i = 0; i < 8; i++) sel = alternarSelecao(sel, `s${i}`);
    expect(sel).toHaveLength(LIMITE_ASSENTOS);
    expect(alternarSelecao(sel, 's0')).not.toContain('s0');
  });
});

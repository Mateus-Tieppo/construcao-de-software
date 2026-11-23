import { keycloak } from '../auth';

export type EventoResumo = { id: string; nome: string; localCidade: string; inicio: string; precoMinimoCentavos: number | null; disponiveis: number };
export type Setor = { id: string; nome: string; capacidade: number; disponiveis: number; precoCentavos: number | null; lote: string | null };
export type EventoDetalhe = { id: string; nome: string; descricao: string; localCidade: string; inicio: string; setores: Setor[] };
export type Assento = { id: string; rotulo: string; estado: 'DISPONIVEL' | 'RESERVADO' | 'VENDIDO' | 'BLOQUEADO' };
export type ItemPedido = { id: string; assentoId: string; precoCentavos: number; descricao: string };
export type Pedido = {
  id: string; eventoId: string; estado: string; motivoExpiracao: string | null; totalCentavos: number;
  expiraEm: string | null; itens: ItemPedido[]; ultimoPagamento: string | null; mensagem: string | null;
};
export type Ingresso = { id: string; pedidoId: string; setorAssento: string; estado: string; emitidoEm: string | null; codigoQr: string | null };
export type Validacao = {
  resultado: 'LIBERADO' | 'JA_UTILIZADO' | 'INVALIDO' | 'NAO_AUTORIZADO'; setorAssento: string | null; titular: string | null; utilizadoEm: string | null;
};

export class ErroApi extends Error {
  constructor(public status: number, public codigo: string, mensagem: string) {
    super(mensagem);
  }
}

async function chamar<T>(caminho: string, init: RequestInit = {}): Promise<T> {
  if (keycloak.authenticated) await keycloak.updateToken(30).catch(() => keycloak.login());
  const headers = new Headers(init.headers);
  if (keycloak.token) headers.set('Authorization', `Bearer ${keycloak.token}`);
  if (init.body) headers.set('Content-Type', 'application/json');
  const resp = await fetch(`/api${caminho}`, { ...init, headers });
  if (!resp.ok) {
    const corpo = await resp.json().catch(() => ({}));
    throw new ErroApi(resp.status, corpo.codigo ?? String(resp.status), corpo.mensagem ?? resp.statusText);
  }
  return resp.status === 204 ? (undefined as T) : resp.json();
}

/** POST com Idempotency-Key estável por intenção: repetir o clique não duplica o pedido (US22). */
function postIdempotente<T>(caminho: string, chave: string, corpo?: unknown): Promise<T> {
  return chamar<T>(caminho, { method: 'POST', headers: { 'Idempotency-Key': chave }, body: corpo ? JSON.stringify(corpo) : undefined });
}

export const api = {
  eventos: () => chamar<EventoResumo[]>('/eventos'),
  evento: (id: string) => chamar<EventoDetalhe>(`/eventos/${id}`),
  assentos: (eventoId: string, setorId: string) => chamar<Assento[]>(`/eventos/${eventoId}/setores/${setorId}/assentos`),
  criarPedido: (chave: string, eventoId: string, assentos: string[]) => postIdempotente<Pedido>('/pedidos', chave, { eventoId, assentos }),
  pedido: (id: string) => chamar<Pedido>(`/pedidos/${id}`),
  pedidos: () => chamar<Pedido[]>('/pedidos'),
  pagar: (id: string, chave: string, cartaoFinal: string) => postIdempotente<Pedido>(`/pedidos/${id}/pagamento`, chave, { cartaoFinal }),
  cancelar: (id: string, chave: string) => postIdempotente<Pedido>(`/pedidos/${id}/cancelamento`, chave),
  ingressos: () => chamar<Ingresso[]>('/ingressos'),
  validar: (codigoQr: string) => chamar<Validacao>('/validacoes', { method: 'POST', body: JSON.stringify({ codigoQr }) }),
  async baixarPdf(id: string) {
    await keycloak.updateToken(30);
    const resp = await fetch(`/api/ingressos/${id}/pdf`, { headers: { Authorization: `Bearer ${keycloak.token}` } });
    if (!resp.ok) throw new ErroApi(resp.status, 'PDF', 'PDF ainda não disponível');
    const url = URL.createObjectURL(await resp.blob());
    const a = document.createElement('a');
    a.href = url;
    a.download = `ingresso-${id}.pdf`;
    a.click();
    URL.revokeObjectURL(url);
  },
};

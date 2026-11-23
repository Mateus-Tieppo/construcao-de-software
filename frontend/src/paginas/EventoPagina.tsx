import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, Assento, ErroApi, EventoDetalhe } from '../api/cliente';
import { keycloak } from '../auth';
import { agruparPorFila, alternarSelecao, LIMITE_ASSENTOS, reais } from '../formatos';

/** US15 + US17 + US18: detalhe do evento, mapa de assentos e reserva atômica. */
export default function EventoPagina() {
  const { id = '' } = useParams();
  const navegar = useNavigate();
  const [evento, setEvento] = useState<EventoDetalhe | null>(null);
  const [setorId, setSetorId] = useState<string | null>(null);
  const [assentos, setAssentos] = useState<Assento[]>([]);
  const [selecionados, setSelecionados] = useState<string[]>([]);
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  // uma chave por intenção de compra: cliques repetidos reaproveitam a mesma (Idempotency-Key)
  const [chave, setChave] = useState(() => crypto.randomUUID());

  useEffect(() => {
    api.evento(id).then((e) => {
      setEvento(e);
      setSetorId(e.setores[0]?.id ?? null);
    }).catch((e: Error) => setErro(e.message));
  }, [id]);

  const carregarMapa = useCallback(() => {
    if (setorId) api.assentos(id, setorId).then(setAssentos).catch((e: Error) => setErro(e.message));
  }, [id, setorId]);

  useEffect(() => {
    setSelecionados([]);
    carregarMapa();
  }, [carregarMapa]);

  const setor = evento?.setores.find((s) => s.id === setorId);
  const filas = useMemo(() => agruparPorFila(assentos), [assentos]);

  async function reservar() {
    if (!keycloak.authenticated) {
      keycloak.login();
      return;
    }
    setEnviando(true);
    setErro(null);
    try {
      const pedido = await api.criarPedido(chave, id, selecionados);
      navegar(`/pedidos/${pedido.id}`);
    } catch (e) {
      const err = e as ErroApi;
      setErro(err.codigo === 'ASSENTO_INDISPONIVEL' ? 'Alguém reservou um desses assentos antes de você. Escolha outro.' : err.message);
      setChave(crypto.randomUUID());
      carregarMapa();
    } finally {
      setEnviando(false);
    }
  }

  if (!evento) return erro ? <p className="erro">{erro}</p> : <p>Carregando…</p>;
  return (
    <section>
      <h1>{evento.nome}</h1>
      <p>{evento.localCidade} · {new Date(evento.inicio).toLocaleString('pt-BR')}</p>
      {evento.descricao && <p className="discreto">{evento.descricao}</p>}
      <div className="abas">
        {evento.setores.map((s) => (
          <button key={s.id} className={s.id === setorId ? '' : 'secundario'} onClick={() => setSetorId(s.id)}>
            {s.nome} · {reais(s.precoCentavos)} · {s.disponiveis} livres
          </button>
        ))}
      </div>
      <div className="palco">PALCO</div>
      <div className="mapa">
        {filas.map(([fila, lista]) => (
          <div key={fila} className="fila">
            <span className="rotulo-fila">{fila}</span>
            {lista.map((a) => (
              <button
                key={a.id}
                title={a.rotulo}
                disabled={a.estado !== 'DISPONIVEL'}
                className={`assento ${a.estado.toLowerCase()} ${selecionados.includes(a.id) ? 'selecionado' : ''}`}
                onClick={() => setSelecionados((s) => alternarSelecao(s, a.id))}
              >
                {a.rotulo.split('-')[1]}
              </button>
            ))}
          </div>
        ))}
      </div>
      <p className="legenda">
        <span className="assento disponivel">·</span> livre <span className="assento selecionado">·</span> selecionado
        <span className="assento reservado">·</span> reservado <span className="assento vendido">·</span> vendido
      </p>
      {erro && <p className="erro">{erro}</p>}
      <div className="barra-acao">
        <span>{selecionados.length}/{LIMITE_ASSENTOS} assentos · {reais((setor?.precoCentavos ?? 0) * selecionados.length)}</span>
        <button disabled={selecionados.length === 0 || enviando} onClick={reservar}>
          {keycloak.authenticated ? 'Reservar por 10 minutos' : 'Entrar para reservar'}
        </button>
      </div>
    </section>
  );
}

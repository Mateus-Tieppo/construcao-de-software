import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, Pedido } from '../api/cliente';
import { contagemRegressiva, reais } from '../formatos';

const ROTULOS: Record<string, string> = {
  CRIADO: 'Criado', RESERVADO: 'Reservado', AGUARDANDO: 'Aguardando pagamento', PAGO: 'Pago', EXPIRADO: 'Expirado',
};

/** US21 + US22: pedido com FSM, contagem de 10 min, pagamento e cancelamento com Idempotency-Key. */
export default function PedidoPagina() {
  const { id = '' } = useParams();
  const [pedido, setPedido] = useState<Pedido | null>(null);
  const [agora, setAgora] = useState(Date.now());
  const [cartao, setCartao] = useState('4242');
  const [erro, setErro] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState(false);
  const [chavePagamento, setChavePagamento] = useState(() => crypto.randomUUID());

  useEffect(() => {
    api.pedido(id).then(setPedido).catch((e: Error) => setErro(e.message));
  }, [id]);

  useEffect(() => {
    const t = setInterval(() => setAgora(Date.now()), 1000);
    return () => clearInterval(t);
  }, []);

  // atualiza o estado periodicamente (expiração chega por Kafka; WebSocket previsto no Pacote 4)
  useEffect(() => {
    if (!pedido || pedido.estado === 'PAGO' || pedido.estado === 'EXPIRADO') return;
    const t = setInterval(() => api.pedido(id).then(setPedido).catch(() => undefined), 5000);
    return () => clearInterval(t);
  }, [id, pedido]);

  async function pagar() {
    setOcupado(true);
    setErro(null);
    try {
      const p = await api.pagar(id, chavePagamento, cartao);
      setPedido(p);
      if (p.estado !== 'PAGO') setChavePagamento(crypto.randomUUID()); // nova tentativa = nova chave
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setOcupado(false);
    }
  }

  async function cancelar() {
    setOcupado(true);
    try {
      setPedido(await api.cancelar(id, `cancelar-${id}`));
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setOcupado(false);
    }
  }

  if (!pedido) return erro ? <p className="erro">{erro}</p> : <p>Carregando pedido…</p>;
  const tempo = contagemRegressiva(pedido.expiraEm, agora);
  const aberto = pedido.estado === 'RESERVADO';
  return (
    <section className="estreito">
      <h1>Pedido</h1>
      <p className={`estado estado-${pedido.estado.toLowerCase()}`}>
        {ROTULOS[pedido.estado] ?? pedido.estado}
        {pedido.motivoExpiracao && ` (${pedido.motivoExpiracao.toLowerCase().replace('_', ' ')})`}
      </p>
      {aberto && <p className="cronometro">Sua reserva expira em <strong>{tempo}</strong></p>}
      <ul className="itens">
        {pedido.itens.map((i) => <li key={i.id}>{i.descricao} <span>{reais(i.precoCentavos)}</span></li>)}
      </ul>
      <p className="total">Total: {reais(pedido.totalCentavos)}</p>
      {pedido.mensagem && <p className="aviso">{pedido.mensagem}</p>}
      {erro && <p className="erro">{erro}</p>}
      {aberto && (
        <div className="pagamento">
          <label>
            Final do cartão (simulado; 0002 é recusado)
            <input value={cartao} maxLength={4} onChange={(e) => setCartao(e.target.value.replace(/\D/g, ''))} />
          </label>
          <button disabled={ocupado || tempo === '00:00'} onClick={pagar}>Pagar {reais(pedido.totalCentavos)}</button>
          <button className="secundario" disabled={ocupado} onClick={cancelar}>Cancelar reserva</button>
        </div>
      )}
      {pedido.estado === 'PAGO' && <p>Pagamento confirmado. <Link to="/ingressos">Ver meus ingressos</Link> (a emissão leva alguns segundos).</p>}
      {pedido.estado === 'EXPIRADO' && (
        <p>
          {pedido.motivoExpiracao === 'TIMEOUT' ? 'Sua reserva expirou. ' : ''}
          <Link to={`/eventos/${pedido.eventoId}`}>Escolher outros assentos</Link>
        </p>
      )}
    </section>
  );
}

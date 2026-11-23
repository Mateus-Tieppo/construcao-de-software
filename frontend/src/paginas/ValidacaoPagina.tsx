import { FormEvent, useState } from 'react';
import { api, Validacao } from '../api/cliente';
import { keycloak, temPapel } from '../auth';

const MENSAGENS: Record<Validacao['resultado'], string> = {
  LIBERADO: 'Entrada liberada',
  JA_UTILIZADO: 'Ingresso já utilizado',
  INVALIDO: 'QR inválido',
  NAO_AUTORIZADO: 'Operador não autorizado para este evento',
};

/** US29: validação de QR na entrada (papel operador). Leitura por câmera prevista no Pacote 4. */
export default function ValidacaoPagina() {
  const [codigo, setCodigo] = useState('');
  const [resultado, setResultado] = useState<Validacao | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  if (!keycloak.authenticated || !temPapel('operador')) {
    return <p>Esta área é do operador de entrada. <button onClick={() => keycloak.login()}>Entrar</button></p>;
  }

  async function validar(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    try {
      setResultado(await api.validar(codigo.trim()));
    } catch (err) {
      setErro((err as Error).message);
    }
  }

  return (
    <section className="estreito">
      <h1>Validar entrada</h1>
      <form onSubmit={validar} className="pagamento">
        <label>
          Conteúdo do QR Code
          <textarea rows={3} value={codigo} onChange={(e) => setCodigo(e.target.value)} placeholder="PP1.…" />
        </label>
        <button disabled={!codigo.trim()}>Validar</button>
      </form>
      {erro && <p className="erro">{erro}</p>}
      {resultado && (
        <div className={`resultado resultado-${resultado.resultado.toLowerCase()}`}>
          <strong>{MENSAGENS[resultado.resultado]}</strong>
          {resultado.setorAssento && <p>{resultado.setorAssento} · {resultado.titular}</p>}
        </div>
      )}
    </section>
  );
}

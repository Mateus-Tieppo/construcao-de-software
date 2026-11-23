// Gera pacote-3/Pacote3_Grupo9.pdf: checkpoint + documentação dos Pacotes 1 e 2 atualizada.
// Uso: cd pacote-3 && npm ci && npm run pdf
// Requer Google Chrome instalado (playwright-core usa o canal "chrome"; não baixa navegador).
// Variável opcional CHROME_PATH aponta outro executável do Chrome/Chromium.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { marked } from 'marked';
import { chromium } from 'playwright-core';

const DIR = path.dirname(fileURLToPath(import.meta.url));
const OUT_PDF = process.env.DOCS ? path.join(DIR, 'build', 'preview.pdf') : path.join(DIR, 'Pacote3_Grupo9.pdf');
const OUT_HTML = path.join(DIR, 'build', 'Pacote3_Grupo9.html');
const PARTE = (t, sub) => ({ parte: t, sub });
const ALL_FILES = [
  PARTE('Parte A', 'Checkpoint do desenvolvimento'),
  '01-estado-desenvolvimento.md',
  '02-registro-atualizacoes.md',
  PARTE('Parte B', 'Pacote 1 atualizado: processo e requisitos'),
  'documentacao/pacote-1/01-processo-desenvolvimento-ia.md',
  'documentacao/pacote-1/02-documento-visao.md',
  'documentacao/pacote-1/03-backlog-story-mapping.md',
  'documentacao/pacote-1/04-requisitos-nao-funcionais.md',
  'documentacao/pacote-1/05-mockups-mapa-navegacao.md',
  PARTE('Parte C', 'Pacote 2 atualizado: arquitetura e infraestrutura'),
  'documentacao/pacote-2/01-arquitetura-c4.md',
  'documentacao/pacote-2/02-modelo-dados-conceitual.md',
  'documentacao/pacote-2/03-mensageria-kafka.md',
  'documentacao/pacote-2/04-pipeline-desenvolvimento.md',
  'documentacao/pacote-2/05-processo-gcs.md',
  'documentacao/pacote-2/adr/ADR-007-implementacao-checkpoint.md',
];
// DOCS=01-arquitetura-c4.md permite pré-visualizar um documento só.
const FILES = process.env.DOCS ? process.env.DOCS.split(',') : ALL_FILES;

const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
const renderer = new marked.Renderer();
// Diagramas largos começam com a linha "%% paisagem" e vão para uma página A4 deitada.
renderer.code = (code, lang) =>
  lang === 'mermaid'
    ? `<div class="fig${/^%% paisagem/.test(code) ? ' wide' : ''}"><pre class="mermaid">${esc(code)}</pre></div>`
    : `<pre><code>${esc(code)}</code></pre>`;
// Links relativos para outros arquivos do repositório viram texto no PDF.
renderer.link = (href, title, text) =>
  /^https?:/.test(href) ? `<a href="${href}">${text}</a>` : `<span class="rel">${text}</span>`;
// Imagens (mockups do Pacote 1) entram embutidas no HTML.
renderer.image = (href, title, text) => {
  const b = fs.readFileSync(path.resolve(docAtual, href)).toString('base64');
  return `<img alt="${text}" src="data:image/png;base64,${b}">`;
};
marked.use({ renderer });

// A legenda (parágrafo "Figura N...") logo após um diagrama em paisagem entra na mesma página.
const keepCaption = (h) =>
  h.replace(/(<h([234])[^>]*>(?:(?!<\/?h[1-6])[\s\S])*?<\/h\2>)\s*<div class="fig wide">/g, '<div class="fig wide">$1')
   .replace(/(<div class="fig wide">[\s\S]*?<\/pre>)<\/div>\s*(<p><em>Figura[\s\S]*?<\/p>)/g, '$1$2</div>');
let docAtual = DIR;
const body = FILES.map((f) => {
  if (typeof f !== 'string') return `<section class="parte"><h1>${f.parte}</h1><h2>${f.sub}</h2></section>`;
  docAtual = path.dirname(path.join(DIR, f));
  return `<section class="doc">${keepCaption(marked.parse(fs.readFileSync(path.join(DIR, f), 'utf8')))}</section>`;
}).join('\n');
const mermaidJs = fs.readFileSync(path.join(DIR, 'node_modules/mermaid/dist/mermaid.min.js'), 'utf8');

const html = `<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><title>Pacote 3 Grupo 9</title>
<style>
@page { size: A4; margin: 15mm 13mm 17mm; }
body { font: 10pt/1.45 "Helvetica Neue", Arial, sans-serif; color:#1a1a1a; }
.capa { height: 255mm; display:flex; flex-direction:column; justify-content:center; text-align:center; page-break-after: always; }
.capa h1 { font-size: 28pt; margin: 10px 0; border: none; } .capa h2 { font-size: 15pt; font-weight: 500; color:#444; border:none; }
.capa .inst { font-size: 11pt; color:#333; } .capa .grp { margin-top: 26mm; font-size: 12pt; }
.sumario { page-break-after: always; } .sumario li { margin: 6px 0; font-size: 12pt; }
.doc { page-break-before: always; }
h1 { font-size: 18pt; border-bottom: 2px solid #1f6feb; padding-bottom: 4px; }
h2 { font-size: 13.5pt; margin-top: 18px; color:#0b3d91; } h3 { font-size: 11.3pt; margin-top: 14px; }
h2, h3, h4 { page-break-after: avoid; }
table { border-collapse: collapse; width: 100%; margin: 8px 0; font-size: 8.4pt; }
tr { page-break-inside: avoid; }
th, td { border: 1px solid #bbb; padding: 3px 5px; vertical-align: top; text-align: left; }
th { background: #e8f0fe; }
blockquote { border-left: 4px solid #9cbcf5; margin: 8px 0; padding: 4px 10px; color:#333; background:#f6f9ff; }
pre { background:#f5f5f7; padding: 7px; font-size: 8.2pt; white-space: pre-wrap; word-break: break-word; border-radius: 4px; page-break-inside: avoid; }
.fig { page-break-inside: avoid; text-align:center; margin: 6px 0 12px; }
pre.mermaid { background: none; padding: 0; }
pre.mermaid svg { max-width: 100% !important; max-height: 225mm; height: auto; }
@page paisagem { size: A4 landscape; margin: 12mm 13mm 15mm; }
.fig.wide { page: paisagem; page-break-before: always; page-break-after: always; margin:0; }
.fig.wide pre.mermaid { width: 270mm; height: 160mm; margin: 0 auto; }
.fig.wide h2, .fig.wide h3 { margin: 0 0 2mm; text-align: left; }
.fig.wide p { margin: 3mm 0 0; text-align: left; }
.fig.wide pre.mermaid svg { width: 100% !important; height: 100% !important; max-width: none !important; max-height: none !important; }
code { font-size: 8.6pt; } .rel { font-style: italic; }
.parte { page-break-before: always; height: 230mm; display:flex; flex-direction:column; justify-content:center; text-align:center; }
.parte h1 { border: none; font-size: 26pt; color:#0b3d91; } .parte h2 { font-weight: 500; color:#444; }
p:has(img) { break-inside: avoid; } img { max-width: 100%; max-height: 200mm; border: 1px solid #ccc; display:block; margin: 6px auto 14px; }
</style></head><body>
<div class="capa">
 <div class="inst">Pontifícia Universidade Católica do Rio Grande do Sul · Escola Politécnica</div>
 <div class="inst">46504-04 Construção de Software · Turma 330 · 2026/2</div>
 <div class="inst">Prof. Michael da Costa Mora</div>
 <h1 style="margin-top:28mm">PulsePass</h1>
 <h2>Plataforma Distribuída de Bilhetagem, Reserva e Acesso para Megaeventos</h2>
 <h2><b>Pacote 3: Checkpoint do Desenvolvimento</b></h2>
 <div class="grp"><b>Grupo 9</b><br>Gabriel Gerhardt de Marque<br>Mateus Granja de Andrade Madalosso Tieppo<br>Filipe Tesche Roman</div>
 <div class="inst" style="margin-top:12mm">Repositório: <b>https://github.com/Mateus-Tieppo/construcao-de-software</b> (código na raiz; documentação em <code>pacote-3/</code>)</div>
 <div class="inst" style="margin-top:8mm">Porto Alegre, 23 de novembro de 2026</div>
</div>
<div class="sumario"><h1>Sumário</h1>
<p><b>Parte A: Checkpoint do desenvolvimento</b></p><ol>
<li>Checkpoint do desenvolvimento (estado do código, como executar, evidências)</li>
<li>Registro de atualizações da documentação</li></ol>
<p><b>Parte B: Pacote 1 atualizado</b></p><ol>
<li>Processo de desenvolvimento suportado por IA</li><li>Documento de Visão do Produto</li>
<li>Backlog com Épicos e Histórias (Story Mapping)</li><li>Requisitos não-funcionais</li>
<li>Mockups e mapa de navegação</li></ol>
<p><b>Parte C: Pacote 2 atualizado</b></p><ol>
<li>Arquitetura no modelo C4</li><li>Modelo de Dados Conceitual</li><li>Estrutura de mensageria do Kafka</li>
<li>Pipeline de desenvolvimento</li><li>Processo de GCS</li><li>ADR-007: decisões de implementação do checkpoint</li></ol>
<p><b>Repositório:</b> https://github.com/Mateus-Tieppo/construcao-de-software · <b>Pull Request do checkpoint:</b> https://github.com/Mateus-Tieppo/construcao-de-software/pull/3</p>
<p><b>Convenção de rótulos.</b> <b>[Enunciado]</b> exigência do professor; <b>[Decisão do grupo, Pacote N]</b> escolha registrada no pacote indicado; <b>[Proposta para revisão do grupo]</b> detalhe ainda não revisado pelo grupo; <b>[Atualização Pacote 3]</b> trecho novo desta entrega; <b>[Pendente, Pacote 4]</b> ainda não implementado.</p></div>
${body}
<script>${mermaidJs}</script>
<script>
mermaid.initialize({ startOnLoad:false, theme:'neutral', securityLevel:'loose',
  flowchart:{ useMaxWidth:true, htmlLabels:true, curve:'basis' }, sequence:{ useMaxWidth:true, wrap:true },
  er:{ useMaxWidth:true }, state:{ useMaxWidth:true } });
mermaid.run({ querySelector:'pre.mermaid' })
  .then(() => { document.title = 'READY'; })
  .catch((e) => { document.title = 'ERROR'; document.body.insertAdjacentHTML('afterbegin','<p id="mmerr">MERMAID ERR '+e+'</p>'); });
</script></body></html>`;

fs.mkdirSync(path.dirname(OUT_HTML), { recursive: true });
fs.writeFileSync(OUT_HTML, html);

const launch = process.env.CHROME_PATH ? { executablePath: process.env.CHROME_PATH } : { channel: 'chrome' };
const browser = await chromium.launch({ ...launch, args: ['--no-sandbox'] });
const page = await browser.newPage();
await page.goto('file://' + OUT_HTML);
await page.waitForFunction(() => document.title === 'READY' || document.title === 'ERROR', null, { timeout: 120000 });
if ((await page.title()) === 'ERROR') {
  console.error(await page.textContent('#mmerr'));
  await browser.close();
  process.exit(1);
}
const errs = await page.$$eval('pre.mermaid', (els) => els.filter((e) => !e.querySelector('svg')).length);
if (errs) { console.error(`${errs} diagrama(s) sem SVG`); await browser.close(); process.exit(1); }
await page.pdf({
  path: OUT_PDF, preferCSSPageSize: true, printBackground: true, displayHeaderFooter: true,
  headerTemplate: '<span></span>',
  footerTemplate: '<div style="font-size:7.5pt;color:#666;width:100%;padding:0 13mm;display:flex;justify-content:space-between"><span>PulsePass · Pacote 3 · Grupo 9</span><span><span class="pageNumber"></span> / <span class="totalPages"></span></span></div>',
  margin: { top: '15mm', bottom: '17mm', left: '13mm', right: '13mm' },
});
await browser.close();
console.log('PDF gerado:', OUT_PDF);

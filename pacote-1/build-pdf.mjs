import fs from 'fs'; import path from 'path'; import { marked } from 'marked';
const P = process.argv[2], OUT = process.argv[3];
const files = ['01-processo-desenvolvimento-ia.md','02-documento-visao.md','03-backlog-story-mapping.md','04-requisitos-nao-funcionais.md','05-mockups-mapa-navegacao.md'];
const renderer = new marked.Renderer();
renderer.code = (code, lang) => lang === 'mermaid'
  ? `<pre class="mermaid">${code.replace(/</g,'&lt;').replace(/>/g,'&gt;')}</pre>`
  : `<pre><code>${code.replace(/&/g,'&amp;').replace(/</g,'&lt;')}</code></pre>`;
renderer.image = (href, title, text) => {
  const b = fs.readFileSync(path.join(P, href)).toString('base64');
  return `<img alt="${text}" src="data:image/png;base64,${b}">`;
};
marked.use({ renderer });
const body = files.map(f => `<section class="doc">${marked.parse(fs.readFileSync(path.join(P,f),'utf8'))}</section>`).join('\n');
const mm = fs.readFileSync(new URL('./node_modules/mermaid/dist/mermaid.min.js', import.meta.url), 'utf8');
const html = `<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><title>Pacote 1 — Grupo 9</title>
<style>
@page { size: A4; margin: 16mm 14mm; }
body { font: 10.5pt/1.45 -apple-system, "Helvetica Neue", Arial, sans-serif; color:#1a1a1a; }
.capa { height: 260mm; display:flex; flex-direction:column; justify-content:center; text-align:center; page-break-after: always; }
.capa h1 { font-size: 26pt; margin: 10px 0; } .capa h2 { font-size: 15pt; font-weight: 500; color:#444; }
.capa .inst { font-size: 11pt; color:#333; } .capa .grp { margin-top: 30mm; font-size: 12pt; }
.sumario { page-break-after: always; } .sumario li { margin: 6px 0; font-size: 12pt; }
.doc { page-break-before: always; }
h1 { font-size: 18pt; border-bottom: 2px solid #6b3df5; padding-bottom: 4px; }
h2 { font-size: 13.5pt; margin-top: 18px; color:#2b1a7a; } h3 { font-size: 11.5pt; }
h2, h3 { page-break-after: avoid; }
table { border-collapse: collapse; width: 100%; margin: 8px 0; font-size: 8.6pt; page-break-inside: auto; }
tr { page-break-inside: avoid; }
th, td { border: 1px solid #bbb; padding: 4px 5px; vertical-align: top; text-align: left; }
th { background: #eeeaff; }
blockquote { border-left: 4px solid #c9bdfc; margin: 8px 0; padding: 4px 10px; color:#444; background:#faf9ff; }
pre { background:#f5f5f7; padding: 8px; font-size: 8.5pt; white-space: pre-wrap; border-radius: 4px; }
pre.mermaid svg { max-width: min(100%, 170mm) !important; } pre.mermaid { background: none; text-align:center; page-break-inside: avoid; }
pre.mermaid svg { max-width: 100% !important; max-height: 200mm; height: auto; }
p:has(img) { break-inside: avoid; } img { max-width: 100%; border: 1px solid #ccc; page-break-inside: avoid; display:block; margin: 6px auto 14px; }
code { font-size: 9pt; }
</style></head><body>
<div class="capa">
 <div class="inst">Pontifícia Universidade Católica do Rio Grande do Sul — Escola Politécnica</div>
 <div class="inst">46504-04 Construção de Software — Turma 330 — 2026/2</div>
 <div class="inst">Prof. Michael da Costa Mora</div>
 <h1 style="margin-top:30mm">PulsePass</h1>
 <h2>Plataforma Distribuída de Bilhetagem, Reserva e Acesso para Megaeventos</h2>
 <h2><b>Pacote 1 — Processo de Desenvolvimento e Requisitos</b></h2>
 <div class="grp"><b>Grupo 9</b><br>Gabriel Gerhardt de Marque<br>Mateus Granja de Andrade Madalosso Tieppo<br>Filipe Tesche Roman</div>
 <div class="inst" style="margin-top:14mm">Repositório: <b>https://github.com/Mateus-Tieppo/construcao-de-software</b></div>
 <div class="inst" style="margin-top:8mm">Porto Alegre, 14 de outubro de 2026</div>
</div>
<div class="sumario"><h1>Sumário</h1><ol>
<li>Descrição do Processo de Desenvolvimento Suportado por IA</li><li>Documento de Visão do Produto</li>
<li>Backlog com Épicos e Histórias de Usuários no Formato de Story Mapping</li><li>Requisitos Não-Funcionais</li>
<li>Mockups (medium res) e Mapa de Navegação do Front-end</li></ol>
<p>Os arquivos-fonte (Markdown, diagramas Mermaid e mockups HTML) são versionados no repositório do grupo e serão atualizados nos Pacotes 3 e 4.</p></div>
${body}
<script>${mm}</script>
<script>mermaid.initialize({startOnLoad:false, theme:'neutral', flowchart:{useMaxWidth:false, htmlLabels:true}, state:{useMaxWidth:false}, securityLevel:'loose'});
mermaid.run({querySelector:'pre.mermaid'}).then(()=>{document.title='READY'}).catch(e=>{document.body.insertAdjacentHTML('afterbegin','<p style=color:red>MERMAID ERR '+e+'</p>')});</script>
</body></html>`;
fs.writeFileSync(OUT, html);
console.log('html ok', html.length);

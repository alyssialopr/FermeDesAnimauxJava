/**
 * Fabrique une documentation Swagger autonome : un seul fichier HTML, sans reseau,
 * sans serveur. Swagger UI et le contrat OpenAPI sont embarques dedans, il suffit
 * de l'ouvrir dans un navigateur.
 *
 *   node generer-doc.mjs              lit le contrat sur l'API qui tourne
 *   node generer-doc.mjs --hors-ligne reutilise le openapi.json deja genere
 *
 * Sortie : docs/api.html et docs/openapi.json
 */

import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const require = createRequire(import.meta.url);
const ICI = path.dirname(fileURLToPath(import.meta.url));
const SOURCE_API = process.env.API_URL ?? 'http://localhost:8080/v3/api-docs';
const FICHIER_SPEC = path.join(ICI, 'openapi.json');
const FICHIER_HTML = path.join(ICI, 'api.html');

const specification = await recupererSpecification();
const versions = {
  api: specification.info?.version ?? '?',
  swaggerUi: require('swagger-ui-dist/package.json').version,
};

const dist = path.dirname(require.resolve('swagger-ui-dist/swagger-ui-bundle.js'));
const css = lire(path.join(dist, 'swagger-ui.css'));
const bundle = lire(path.join(dist, 'swagger-ui-bundle.js'));
const preset = lire(path.join(dist, 'swagger-ui-standalone-preset.js'));

fs.writeFileSync(FICHIER_SPEC, JSON.stringify(specification, null, 2) + '\n');
fs.writeFileSync(FICHIER_HTML, page());

const poids = (Math.round(fs.statSync(FICHIER_HTML).size / 1024) / 1024).toFixed(2);
console.log(`docs/openapi.json  ${compterOperations()} operations`);
console.log(`docs/api.html      ${poids} Mo, autonome (Swagger UI ${versions.swaggerUi})`);

// ---------------------------------------------------------------------------

async function recupererSpecification() {
  const horsLigne = process.argv.includes('--hors-ligne');

  if (!horsLigne) {
    try {
      const reponse = await fetch(SOURCE_API);
      if (reponse.ok) {
        console.log(`contrat lu sur ${SOURCE_API}`);
        return await reponse.json();
      }
      console.warn(`${SOURCE_API} a repondu ${reponse.status}`);
    } catch {
      console.warn(`${SOURCE_API} injoignable (l'API tourne-t-elle ?)`);
    }
  }

  if (fs.existsSync(FICHIER_SPEC)) {
    console.log('contrat repris depuis docs/openapi.json');
    return JSON.parse(lire(FICHIER_SPEC));
  }

  console.error("Aucun contrat disponible : lance la stack (docker compose up -d) puis relance.");
  process.exit(1);
}

function lire(fichier) {
  return fs.readFileSync(fichier, 'utf8');
}

function compterOperations() {
  return Object.values(specification.paths ?? {})
    .reduce((total, chemin) => total + Object.keys(chemin).length, 0);
}

/** Neutralise les sequences qui fermeraient prematurement la balise script. */
function pourScript(texte) {
  return texte.replaceAll('</script', '<\\/script');
}

/**
 * Remplace les $ref internes par les schemas correspondants.
 *
 * Ouvert depuis un fichier local, Swagger UI resout les references par rapport a
 * l'URL de la page (file://…/api.html) et echoue. En aplatissant le contrat, plus
 * aucune reference n'est a resoudre. Le openapi.json ecrit a cote, lui, garde ses
 * $ref pour les autres outils.
 */
function aplatir(document) {
  const enCours = new Set();

  const resoudre = (noeud) => {
    if (Array.isArray(noeud)) {
      return noeud.map(resoudre);
    }
    if (noeud === null || typeof noeud !== 'object') {
      return noeud;
    }

    const reference = noeud.$ref;
    if (typeof reference === 'string' && reference.startsWith('#/')) {
      // Reference circulaire : on la laisse telle quelle pour ne pas boucler.
      if (enCours.has(reference)) {
        return { ...noeud };
      }
      const cible = reference.slice(2).split('/').reduce((valeur, cle) => valeur?.[cle], document);
      if (cible === undefined) {
        console.warn(`reference introuvable, laissee en l'etat : ${reference}`);
        return { ...noeud };
      }
      enCours.add(reference);
      const resolu = resoudre(cible);
      enCours.delete(reference);

      const { $ref: _ignore, ...reste } = noeud;
      return { ...resolu, ...reste };
    }

    return Object.fromEntries(Object.entries(noeud).map(([cle, valeur]) => [cle, resoudre(valeur)]));
  };

  return resoudre(document);
}

function page() {
  return `<!doctype html>
<html lang="fr">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>API Ferme des Animaux — documentation</title>
<link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>🐄</text></svg>" />
<style>${css}</style>
<style>
  body { margin: 0; background: #fafafa; }
  .bandeau {
    padding: 14px 22px;
    background: #1b2f1d;
    color: #eef3ea;
    font: 14px/1.5 "Segoe UI", system-ui, sans-serif;
    display: flex;
    flex-wrap: wrap;
    gap: 6px 18px;
    align-items: baseline;
  }
  .bandeau b { font-size: 15px; }
  .bandeau span { color: #a8c89a; }
  .bandeau code {
    background: rgba(255,255,255,.12);
    padding: 1px 6px;
    border-radius: 5px;
    font-size: 13px;
  }
  .swagger-ui .topbar { display: none; }
</style>
</head>
<body>
<div class="bandeau">
  <b>🐄 API Ferme des Animaux ${versions.api}</b>
  <span>Documentation hors-ligne — ce fichier fonctionne seul, sans serveur.</span>
  <span>Pour essayer les requetes : <code>docker compose up -d</code> puis <code>http://localhost:8080/swagger-ui.html</code></span>
</div>

<div id="swagger"></div>

<script>${pourScript(bundle)}</script>
<script>${pourScript(preset)}</script>
<script>
  const contrat = ${pourScript(JSON.stringify(aplatir(specification)))};

  window.ui = SwaggerUIBundle({
    spec: contrat,
    dom_id: '#swagger',
    deepLinking: true,
    docExpansion: 'list',
    defaultModelsExpandDepth: 2,
    defaultModelRendering: 'model',
    tryItOutEnabled: false,
    // Pas de bouton "Try it out" : ouvert depuis un fichier local, le navigateur
    // ne peut pas appeler l'API. La version servie par l'API, elle, le permet.
    supportedSubmitMethods: [],
    presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
    layout: 'BaseLayout',
  });
</script>
</body>
</html>
`;
}

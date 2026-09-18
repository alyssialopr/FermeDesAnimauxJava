/**
 * Client de l'API de la ferme.
 *
 * Toutes les requetes passent par /api : en production c'est nginx qui relaie vers
 * le conteneur de l'API, en developpement c'est le proxy de Vite. Pas de CORS, pas
 * d'URL en dur.
 */

const BASE = '/api';
const CLES = 'ferme.cles';

/**
 * Les cles d'eleveur connues de ce navigateur. Elles ne quittent jamais la
 * machine autrement que dans l'en-tete Authorization, et le serveur n'en garde
 * qu'une empreinte.
 */
function clesConnues() {
  try {
    return JSON.parse(localStorage.getItem(CLES)) ?? {};
  } catch {
    return {};
  }
}

export function definirCle(eleveurId, cle) {
  const cles = clesConnues();
  cles[eleveurId] = cle;
  localStorage.setItem(CLES, JSON.stringify(cles));
}

export function oublierCle(eleveurId) {
  const cles = clesConnues();
  delete cles[eleveurId];
  localStorage.setItem(CLES, JSON.stringify(cles));
}

export function cleDe(eleveurId) {
  return clesConnues()[eleveurId] ?? null;
}

/** L'eleveur au nom duquel on joue : ses requetes portent sa cle. */
let eleveurCourant = null;

export function jouerAvec(eleveurId) {
  eleveurCourant = eleveurId;
}

function entetes(corps) {
  const enTetes = {};
  if (corps) {
    enTetes['Content-Type'] = 'application/json';
  }
  const cle = eleveurCourant == null ? null : cleDe(eleveurCourant);
  if (cle) {
    enTetes.Authorization = `Bearer ${eleveurCourant}.${cle}`;
  }
  return enTetes;
}

/** Erreur metier renvoyee par l'API (RFC 7807). */
export class ErreurFerme extends Error {
  constructor(message, statut, titre) {
    super(message);
    this.name = 'ErreurFerme';
    this.statut = statut;
    this.titre = titre;
  }
}

async function requete(chemin, options = {}) {
  let reponse;
  try {
    reponse = await fetch(BASE + chemin, {
      headers: entetes(options.corps),
      method: options.methode ?? 'GET',
      body: options.corps ? JSON.stringify(options.corps) : undefined,
    });
  } catch {
    throw new ErreurFerme("La ferme ne repond pas (API injoignable).", 0, 'Hors ligne');
  }

  if (reponse.status === 204) {
    return null;
  }

  const texte = await reponse.text();
  const donnees = texte ? JSON.parse(texte) : null;

  if (!reponse.ok) {
    if (reponse.status === 429) {
      throw new ErreurFerme('Trop de requetes, laisse souffler la ferme.', 429, 'Trop de requetes');
    }
    const detail = donnees?.detail ?? donnees?.title ?? `Erreur ${reponse.status}`;
    const champs = donnees?.champs;
    const message = champs
      ? `${detail} ${Object.entries(champs).map(([c, m]) => `${c} : ${m}`).join(', ')}`
      : detail;
    throw new ErreurFerme(message, reponse.status, donnees?.title);
  }

  return donnees;
}

export const api = {
  // --- catalogue ----------------------------------------------------------
  listerEspeces: () => requete('/especes'),

  // --- eleveurs -----------------------------------------------------------
  listerEleveurs: () => requete('/eleveurs'),
  eleveur: (id) => requete(`/eleveurs/${id}`),
  creerEleveur: (prenom) => requete('/eleveurs', { methode: 'POST', corps: { prenom } }),
  classement: () => requete('/eleveurs/classement'),
  mouvements: (eleveurId) => requete(`/eleveurs/${eleveurId}/mouvements`),

  // --- animaux ------------------------------------------------------------
  listerAnimaux: () => requete('/animaux'),
  creerAnimal: (animal) => requete('/animaux', { methode: 'POST', corps: animal }),
  deplacerAnimal: (id, enclos) =>
    requete(`/animaux/${id}/enclos`, { methode: 'PATCH', corps: { enclos } }),

  // --- actions de l'eleveur ----------------------------------------------
  action: (eleveurId, animalId, action) =>
    requete(`/eleveurs/${eleveurId}/animaux/${animalId}/${action}`, { methode: 'POST' }),
};

/** Les actions exposees par l'API, dans l'ordre d'affichage de la fiche. */
export const ACTIONS = {
  repas: { libelle: 'Nourrir', emoji: '🌾' },
  soin: { libelle: 'Soigner', emoji: '💊' },
  balade: { libelle: 'Balade', emoji: '🚶' },
  recolte: { libelle: 'Récolter', emoji: '🪣' },
  achat: { libelle: 'Acheter', emoji: '💰' },
  vente: { libelle: 'Vendre', emoji: '🏷️' },
};

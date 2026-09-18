/**
 * Client de l'API de la ferme.
 *
 * Toutes les requetes passent par /api : en production c'est nginx qui relaie vers
 * le conteneur de l'API, en developpement c'est le proxy de Vite. Pas de CORS, pas
 * d'URL en dur.
 */

const BASE = '/api';

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
      headers: options.corps ? { 'Content-Type': 'application/json' } : undefined,
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

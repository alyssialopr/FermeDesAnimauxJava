/**
 * Point d'entree du jeu : relie l'API de la ferme a la scene Three.js.
 *
 * Aucune donnee de jeu n'est inventee cote navigateur — les animaux, les eleveurs,
 * l'argent et le resultat des actions viennent tous de l'API, donc de PostgreSQL.
 * Le navigateur ne garde que l'eleveur choisi.
 */

import * as THREE from 'three';
import './ui/styles.css';
import { api, cleDe, definirCle, ErreurFerme, jouerAvec, oublierCle } from './api.js';
import { AnimalVisuel } from './scene/animaux.js';
import {
  cadrerFerme,
  creerEnclos,
  creerMarche,
  creerMonde,
  POSITION_MARCHE,
  TAILLE_ENCLOS,
  positionEnclos,
} from './scene/monde.js';
import { eurosSigne, Hud } from './ui/hud.js';

const CLE_ELEVEUR = 'ferme.eleveur';
const PERIODE_RAFRAICHISSEMENT = 5_000;

const monde = creerMonde(document.getElementById('scene'));
const hud = new Hud();
const horloge = new THREE.Clock();
const rayon = new THREE.Raycaster();
const pointeur = new THREE.Vector2();

const etat = {
  eleveurId: Number(localStorage.getItem(CLE_ELEVEUR)) || null,
  eleveurs: [],
  animaux: [],
  selection: null,
};

const visuels = new Map();
const enclosAffiches = new Map();

/** Le joueur a bouge la camera : on ne la recadre plus automatiquement. */
let cameraApprivoisee = false;
monde.controles.addEventListener('start', () => {
  cameraApprivoisee = true;
});

/** Recentrage progressif sur l'animal selectionne. */
let focus = null;

// -------------------------------------------------------------------- donnees

function eleveurCourant() {
  return etat.eleveurs.find((eleveur) => eleveur.id === etat.eleveurId) ?? null;
}

async function rafraichir({ silencieux = true } = {}) {
  try {
    const [animaux, eleveurs] = await Promise.all([api.listerAnimaux(), api.listerEleveurs()]);
    etat.animaux = animaux;
    etat.eleveurs = eleveurs;

    if (etat.eleveurId && !eleveurs.some((eleveur) => eleveur.id === etat.eleveurId)) {
      changerEleveur(null);
    }

    hud.majEleveurs(eleveurs, etat.eleveurId);
    synchroniserScene();
    majCompteurs();
    majFiche();
    majMarche();
    await majCompte();
    return true;
  } catch (erreur) {
    if (!silencieux) {
      signaler(erreur);
    }
    return false;
  }
}

function majCompteurs() {
  const eleveur = eleveurCourant();
  hud.majCompteurs({
    solde: eleveur?.solde ?? null,
    fortune: eleveur?.fortune ?? null,
    troupeau: etat.animaux.filter(
      (animal) => animal.eleveurId === etat.eleveurId && animal.etat === 'LIBRE',
    ).length,
  });
}

function majFiche() {
  if (etat.selection == null) {
    hud.masquerFiche();
    return;
  }
  const animal = etat.animaux.find((candidat) => candidat.id === etat.selection);
  if (!animal) {
    selectionner(null);
    return;
  }
  hud.afficherFiche(animal, { eleveurId: etat.eleveurId });
}

function majMarche() {
  const aVendre = etat.animaux
    .filter((animal) => animal.eleveurId == null && animal.etat === 'LIBRE')
    .sort((a, b) => Number(a.prix) - Number(b.prix));

  hud.majMarche(aVendre, { solde: eleveurCourant()?.solde, eleveurId: etat.eleveurId });
}

async function majCompte() {
  try {
    hud.majClassement(await api.classement(), etat.eleveurId);
    const releveLisible = etat.eleveurId != null && cleDe(etat.eleveurId);
    hud.majMouvements(releveLisible ? await api.mouvements(etat.eleveurId) : []);
  } catch {
    // Le classement et le releve sont du confort : leur echec ne casse pas la partie.
  }
}

// ---------------------------------------------------------------------- scene

/** Les animaux presents a la ferme : les vendus, morts ou disparus l'ont quittee. */
function animauxVisibles() {
  return etat.animaux.filter((animal) => animal.etat === 'LIBRE');
}

function synchroniserScene() {
  const presents = animauxVisibles();
  majEnclos(presents);

  const identifiants = new Set(presents.map((animal) => animal.id));

  // Depart des animaux qui ne sont plus a la ferme.
  for (const [id, visuel] of visuels) {
    if (!identifiants.has(id) && !visuel.effet) {
      visuel.partir(new THREE.Vector3(POSITION_MARCHE.x, 0, POSITION_MARCHE.z - 24));
    }
  }

  for (const animal of presents) {
    let visuel = visuels.get(animal.id);

    if (!visuel) {
      visuel = new AnimalVisuel(animal);
      const zone = zoneDe(animal);
      visuel.definirZone(zone.centre, zone.rayon);
      visuel.placer(visuel.positionAleatoireDansZone());
      monde.groupeAnimaux.add(visuel.groupe);
      visuels.set(animal.id, visuel);
    } else {
      const zone = zoneDe(animal);
      if (!visuel.zone.centre.equals(zone.centre)) {
        visuel.definirZone(zone.centre, zone.rayon);
      }
      visuel.majDonnees(animal);
    }

    visuel.selectionner(animal.id === etat.selection);
  }
}

/** Ou vit cet animal : dans son enclos s'il a un proprietaire, au marche sinon. */
function zoneDe(animal) {
  if (animal.eleveurId == null) {
    return { centre: POSITION_MARCHE.clone(), rayon: 8.5 };
  }
  const position = enclosAffiches.get(animal.enclos);
  return position
    ? { centre: position.clone(), rayon: TAILLE_ENCLOS / 2 - 1.6 }
    : { centre: POSITION_MARCHE.clone(), rayon: 6 };
}

/** Construit une parcelle cloturee par enclos existant, dans l'ordre alphabetique. */
function majEnclos(animaux) {
  const noms = [...new Set(animaux.filter((a) => a.eleveurId != null).map((a) => a.enclos))].sort();

  const inchange = noms.length === enclosAffiches.size
    && noms.every((nom) => enclosAffiches.has(nom));
  if (inchange) {
    return;
  }

  monde.groupeEnclos.clear();
  enclosAffiches.clear();

  noms.forEach((nom, rang) => {
    const position = positionEnclos(rang);
    enclosAffiches.set(nom, position);
    monde.groupeEnclos.add(creerEnclos(nom, position));
  });

  monde.groupeEnclos.add(creerMarche(POSITION_MARCHE));

  // La ferme vient de changer de taille : on la recadre, sauf si le joueur a
  // deja pris la main sur la camera.
  if (!cameraApprivoisee) {
    cadrerFerme(monde, [...enclosAffiches.values()]);
  }

  // Les animaux deja en scene rejoignent leur nouvelle parcelle.
  for (const [id, visuel] of visuels) {
    const animal = etat.animaux.find((candidat) => candidat.id === id);
    if (animal) {
      const zone = zoneDe(animal);
      visuel.definirZone(zone.centre, zone.rayon);
    }
  }
}

// -------------------------------------------------------------------- actions

function signaler(erreur) {
  const message = erreur instanceof ErreurFerme ? erreur.message : 'Une erreur est survenue.';
  hud.toast(message, 'erreur');
  hud.journal(message, 'erreur');
}

/** Ajoute le mouvement d'argent au message quand il y en a un. */
function messageAvecMontant(resultat) {
  const montant = Number(resultat.montant ?? 0);
  return montant === 0 ? resultat.message : `${resultat.message} ${eurosSigne(montant)}`;
}

async function lancerAction(action, animalIdForce = null, deuxiemeEssai = false) {
  const animalId = animalIdForce ?? etat.selection;
  if (etat.eleveurId == null || animalId == null) {
    return;
  }

  if (!cleDe(etat.eleveurId) && !(await reclamerCle(etat.eleveurId))) {
    return;
  }

  const visuel = visuels.get(animalId);

  try {
    const resultat = await api.action(etat.eleveurId, animalId, action);
    const message = messageAvecMontant(resultat);
    hud.toast(message);
    hud.journal(message);
    visuel?.jouerAction(action);

    if (action === 'vente') {
      selectionner(null);
    }
    await rafraichir();
  } catch (erreur) {
    // Cle absente ou perimee : on la redemande une fois, puis on rejoue l'action.
    if (!deuxiemeEssai && (erreur.statut === 401 || erreur.statut === 403)) {
      oublierCle(etat.eleveurId);
      if (await reclamerCle(etat.eleveurId)) {
        await lancerAction(action, animalId, true);
        return;
      }
    }
    signaler(erreur);
    await rafraichir();
  }
}

async function creerAnimal() {
  const saisie = await hud.demanderAnimal();
  if (!saisie) {
    return;
  }

  if (!cleDe(etat.eleveurId) && !(await reclamerCle(etat.eleveurId))) {
    return;
  }

  try {
    const animal = await api.creerAnimal({
      espece: saisie.espece,
      nom: saisie.nom,
      race: saisie.race,
      couleur: saisie.couleur,
      enclos: saisie.enclos,
      eleveurId: saisie.acheter ? etat.eleveurId : null,
    });

    const message = saisie.acheter && etat.eleveurId
      ? `${animal.nom} rejoint ton troupeau.`
      : `${animal.nom} arrive au marché.`;
    hud.toast(message);
    hud.journal(message);

    await rafraichir();
    selectionner(animal.id);
  } catch (erreur) {
    signaler(erreur);
  }
}

async function creerEleveur() {
  const prenom = await hud.demanderEleveur();
  if (!prenom) {
    return;
  }

  try {
    // La reponse contient la cle d'acces : c'est la seule fois qu'elle est donnee.
    const { eleveur, cle } = await api.creerEleveur(prenom);
    definirCle(eleveur.id, cle);

    hud.toast(`${eleveur.prenom} rejoint la ferme avec ${eleveur.solde} €.`);
    hud.journal(`${eleveur.prenom} rejoint la ferme. Sa clé : ${cle}`);
    changerEleveur(eleveur.id);
    await rafraichir();
  } catch (erreur) {
    signaler(erreur);
  }
}

/**
 * Sans cle, aucune action n'est possible au nom de cet eleveur : on la demande.
 * Elle reste dans ce navigateur, le serveur n'en garde qu'une empreinte.
 */
async function reclamerCle(eleveurId) {
  const eleveur = etat.eleveurs.find((candidat) => candidat.id === eleveurId);
  if (!eleveur) {
    return false;
  }

  const cle = await hud.demanderCle(eleveur.prenom);
  if (!cle) {
    return false;
  }

  definirCle(eleveurId, cle);
  jouerAvec(eleveurId);
  return true;
}

async function demenager(enclos) {
  if (etat.selection == null) {
    return;
  }
  if (!cleDe(etat.eleveurId) && !(await reclamerCle(etat.eleveurId))) {
    return;
  }
  try {
    const animal = await api.deplacerAnimal(etat.selection, enclos);
    const message = `${animal.nom} rejoint l'enclos ${animal.enclos}.`;
    hud.toast(message);
    hud.journal(message);
    await rafraichir();
  } catch (erreur) {
    signaler(erreur);
  }
}

function changerEleveur(id) {
  etat.eleveurId = id;
  jouerAvec(id);
  if (id == null) {
    localStorage.removeItem(CLE_ELEVEUR);
  } else {
    localStorage.setItem(CLE_ELEVEUR, String(id));
  }
  majCompteurs();
  majFiche();
  majMarche();
  majCompte();
}

function selectionner(id) {
  etat.selection = id;
  for (const [identifiant, visuel] of visuels) {
    visuel.selectionner(identifiant === id);
  }
  const choisi = id == null ? null : visuels.get(id);
  focus = choisi ? { visuel: choisi, reste: 0.9 } : null;
  majFiche();
}

// ------------------------------------------------------------------- pointeur

let pointeurDepart = null;

monde.renderer.domElement.addEventListener('pointerdown', (evenement) => {
  pointeurDepart = { x: evenement.clientX, y: evenement.clientY };
});

monde.renderer.domElement.addEventListener('pointerup', (evenement) => {
  // On ignore les clics qui sont en fait des rotations de camera.
  if (!pointeurDepart) {
    return;
  }
  const deplacement = Math.hypot(evenement.clientX - pointeurDepart.x, evenement.clientY - pointeurDepart.y);
  pointeurDepart = null;
  if (deplacement > 6) {
    return;
  }

  pointeur.x = (evenement.clientX / window.innerWidth) * 2 - 1;
  pointeur.y = -(evenement.clientY / window.innerHeight) * 2 + 1;
  rayon.setFromCamera(pointeur, monde.camera);

  const touches = rayon.intersectObjects(monde.groupeAnimaux.children, true);
  const touche = touches.find((intersection) => intersection.object.userData.animalId != null);
  selectionner(touche ? touche.object.userData.animalId : null);
});

window.addEventListener('keydown', (evenement) => {
  if (evenement.key === 'Escape') {
    selectionner(null);
  }
});

// --------------------------------------------------------------------- boucle

function boucle() {
  requestAnimationFrame(boucle);

  const delta = Math.min(horloge.getDelta(), 0.1);
  const temps = horloge.elapsedTime;

  for (const [id, visuel] of visuels) {
    visuel.animer(delta, temps);
    if (visuel.partie) {
      visuel.detruire();
      visuels.delete(id);
    }
  }

  if (focus) {
    focus.reste -= delta;
    monde.controles.target.lerp(focus.visuel.groupe.position, Math.min(1, delta * 3));
    if (focus.reste <= 0) {
      focus = null;
    }
  }

  monde.controles.update();
  monde.renderer.render(monde.scene, monde.camera);
}

// -------------------------------------------------------------------- demarrage

hud.surChangementEleveur((id) => {
  changerEleveur(id);
  hud.majEleveurs(etat.eleveurs, etat.eleveurId);
});
hud.surAction(lancerAction);
hud.surNouvelAnimal(creerAnimal);
hud.surNouvelEleveur(creerEleveur);
hud.surDemenagement(demenager);
hud.surMarche((animalId, action) => {
  selectionner(animalId);
  if (action === 'achat') {
    lancerAction('achat', animalId);
  } else {
    hud.afficherOnglet('ferme');
  }
});

async function demarrer() {
  jouerAvec(etat.eleveurId);
  boucle();

  // L'API peut encore etre en train de demarrer derriere nginx : on insiste.
  for (let tentative = 1; tentative <= 20; tentative += 1) {
    try {
      hud.definirCatalogue(await api.listerEspeces());
    } catch {
      hud.chargement(`La ferme se réveille… (tentative ${tentative})`);
      await new Promise((attendre) => setTimeout(attendre, 1500));
      continue;
    }

    if (await rafraichir()) {
      hud.cacherChargement();
      hud.journal('Bienvenue à la ferme.');
      setInterval(() => rafraichir(), PERIODE_RAFRAICHISSEMENT);
      return;
    }
  }

  hud.chargement("L'API ne répond pas. Vérifie que la stack Docker est démarrée.");
}

// Petite porte d'entree pour le debogage et les tests de bout en bout :
// inspecter l'etat du jeu ou piloter une partie depuis la console du navigateur.
window.ferme = {
  etat,
  visuels,
  monde,
  hud,
  selectionner,
  rafraichir,
  lancerAction,
};

demarrer();

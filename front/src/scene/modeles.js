/**
 * Modeles 3D des animaux, construits a la main a partir de primitives.
 *
 * Deux fabriques suffisent pour toute la ferme : un quadrupede et un oiseau, que
 * chaque espece parametre (taille, oreilles, cornes, bec, laine…). Chaque modele
 * expose sa tete et ses pattes pour pouvoir les animer.
 */

import * as THREE from 'three';

const COULEURS = {
  blanche: '#f2efe6',
  blanc: '#f2efe6',
  marron: '#8b5a2b',
  noire: '#31313a',
  noir: '#31313a',
  orange: '#e0913c',
  grise: '#9aa0a6',
  gris: '#9aa0a6',
  rousse: '#c0632f',
  roux: '#c0632f',
  beige: '#e3cfa4',
  rose: '#e9a6a2',
};

export function couleurDepuisNom(nom) {
  const cle = (nom ?? '').trim().toLowerCase();
  if (COULEURS[cle]) {
    return new THREE.Color(COULEURS[cle]);
  }
  // Couleur stable deduite du texte, pour les couleurs fantaisistes.
  let empreinte = 0;
  for (const caractere of cle) {
    empreinte = (empreinte * 31 + caractere.charCodeAt(0)) % 360;
  }
  return new THREE.Color().setHSL(empreinte / 360, 0.42, 0.62);
}

function matiere(couleur, options = {}) {
  return new THREE.MeshStandardMaterial({ color: couleur, roughness: 0.85, ...options });
}

function ombrer(objet) {
  objet.castShadow = true;
  return objet;
}

// ---------------------------------------------------------------------------
// Fabrique de quadrupedes : vache, chevre, mouton, cochon, cheval, lapin
// ---------------------------------------------------------------------------

function creerQuadrupede(couleurNom, profil) {
  const {
    corps = { longueur: 1.5, rayon: 0.75, hauteur: 1.35 },
    patte = { rayon: 0.16, hauteur: 1.3, ecart: 0.7, largeur: 0.45 },
    tete = { taille: 0.95, avance: 1.35, hauteur: 1.75 },
    cou = null,
    oreilles = 'droites',
    cornes = null,
    museau = null,
    taches = false,
    laine = false,
    criniere = false,
    queue = 'fouet',
    echelle = 1,
  } = profil;

  const groupe = new THREE.Group();
  const robe = couleurDepuisNom(couleurNom);
  const matRobe = matiere(robe);
  const sombre = robe.clone().multiplyScalar(0.55);
  const matSombre = matiere(sombre);

  const buste = ombrer(new THREE.Mesh(
    new THREE.CapsuleGeometry(corps.rayon, corps.longueur, 6, 14), matRobe));
  buste.rotation.z = Math.PI / 2;
  buste.position.y = corps.hauteur;
  groupe.add(buste);

  if (laine) {
    // Le mouton : une grappe de boules pour la toison.
    for (const [x, y, z] of [
      [0.5, 0.35, 0.3], [-0.5, 0.3, -0.3], [0, 0.45, 0.45],
      [0, 0.4, -0.45], [0.55, 0.2, -0.35], [-0.55, 0.25, 0.35],
    ]) {
      const flocon = ombrer(new THREE.Mesh(new THREE.IcosahedronGeometry(corps.rayon * 0.55, 0), matRobe));
      flocon.position.set(x, corps.hauteur + y, z);
      groupe.add(flocon);
    }
  }

  if (taches) {
    for (const [x, y, z, r] of [
      [0.35, corps.hauteur + 0.4, 0.55, 0.36],
      [-0.45, corps.hauteur + 0.15, -0.6, 0.3],
      [0.6, corps.hauteur - 0.1, -0.5, 0.26],
    ]) {
      const tache = ombrer(new THREE.Mesh(new THREE.SphereGeometry(r, 10, 8), matSombre));
      tache.position.set(x, y, z);
      tache.scale.set(1, 0.6, 1);
      groupe.add(tache);
    }
  }

  if (cou) {
    // Encolure : relie le buste a la tete pour les especes au port haut.
    const encolure = ombrer(new THREE.Mesh(
      new THREE.CylinderGeometry(cou.rayon, cou.rayon * 1.35, cou.longueur, 8), matRobe));
    encolure.position.set(
      (corps.longueur / 2 + tete.avance) / 2,
      (corps.hauteur + tete.hauteur) / 2,
      0);
    encolure.rotation.z = -cou.inclinaison;
    groupe.add(encolure);
  }

  // --- tete
  const groupeTete = new THREE.Group();
  groupeTete.position.set(tete.avance, tete.hauteur, 0);

  const crane = ombrer(new THREE.Mesh(
    new THREE.BoxGeometry(tete.taille, tete.taille * 0.82, tete.taille * 0.82), matRobe));
  groupeTete.add(crane);

  if (museau) {
    const nez = ombrer(new THREE.Mesh(
      new THREE.BoxGeometry(museau.taille, museau.taille * 0.9, museau.taille * 1.4),
      matiere(museau.couleur ?? '#e0a3a8')));
    nez.position.set(tete.taille * 0.52, -tete.taille * 0.17, 0);
    groupeTete.add(nez);
  }

  for (const cote of [-1, 1]) {
    if (oreilles === 'droites') {
      const oreille = ombrer(new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.24, 0.34), matRobe));
      oreille.position.set(-0.2, 0.14, cote * tete.taille * 0.52);
      groupeTete.add(oreille);
    } else if (oreilles === 'tombantes') {
      const oreille = ombrer(new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.34, 0.2), matRobe));
      oreille.position.set(-0.1, 0.02, cote * tete.taille * 0.55);
      oreille.rotation.x = cote * 0.5;
      groupeTete.add(oreille);
    } else if (oreilles === 'longues') {
      const oreille = ombrer(new THREE.Mesh(new THREE.CapsuleGeometry(0.09, 0.5, 4, 8), matRobe));
      oreille.position.set(-0.15, 0.5, cote * 0.2);
      oreille.rotation.z = cote * 0.18;
      groupeTete.add(oreille);
    }

    if (cornes === 'vache') {
      const corne = ombrer(new THREE.Mesh(new THREE.ConeGeometry(0.11, 0.42, 6), matiere('#e8dfc8')));
      corne.position.set(-0.1, tete.taille * 0.48, cote * 0.28);
      corne.rotation.z = cote * -0.25;
      groupeTete.add(corne);
    } else if (cornes === 'chevre') {
      const corne = ombrer(new THREE.Mesh(new THREE.ConeGeometry(0.08, 0.5, 6), matiere('#cfc4a8')));
      corne.position.set(-0.28, tete.taille * 0.5, cote * 0.18);
      corne.rotation.z = 0.9;
      groupeTete.add(corne);
    }

    const oeil = new THREE.Mesh(new THREE.SphereGeometry(tete.taille * 0.085, 8, 8), matiere('#1c1c22'));
    oeil.position.set(tete.taille * 0.32, tete.taille * 0.12, cote * tete.taille * 0.32);
    groupeTete.add(oeil);
  }

  if (cornes === 'chevre') {
    const barbiche = ombrer(new THREE.Mesh(new THREE.ConeGeometry(0.08, 0.28, 5), matRobe));
    barbiche.position.set(tete.taille * 0.3, -tete.taille * 0.5, 0);
    barbiche.rotation.z = Math.PI;
    groupeTete.add(barbiche);
  }

  groupe.add(groupeTete);

  if (criniere) {
    const crins = ombrer(new THREE.Mesh(new THREE.BoxGeometry(0.14, 1.2, 0.34), matSombre));
    crins.position.set(tete.avance - 0.62, tete.hauteur - 0.35, 0);
    crins.rotation.z = -0.75;
    groupe.add(crins);
  }

  // --- pattes
  const pattes = [];
  for (const [x, z] of [
    [patte.ecart, patte.largeur], [patte.ecart, -patte.largeur],
    [-patte.ecart, patte.largeur], [-patte.ecart, -patte.largeur],
  ]) {
    const membre = ombrer(new THREE.Mesh(
      new THREE.CylinderGeometry(patte.rayon, patte.rayon * 0.88, patte.hauteur, 7), matSombre));
    membre.position.set(x, patte.hauteur / 2, z);
    groupe.add(membre);
    pattes.push(membre);
  }

  // --- queue
  if (queue === 'fouet') {
    const fouet = ombrer(new THREE.Mesh(new THREE.CylinderGeometry(0.06, 0.03, 1, 5), matSombre));
    fouet.position.set(-corps.longueur / 2 - 0.6, corps.hauteur + 0.15, 0);
    fouet.rotation.z = 0.5;
    groupe.add(fouet);
  } else if (queue === 'pompon') {
    const pompon = ombrer(new THREE.Mesh(new THREE.SphereGeometry(0.2, 8, 8), matiere('#f7f4ec')));
    pompon.position.set(-corps.longueur / 2 - 0.35, corps.hauteur + 0.2, 0);
    groupe.add(pompon);
  } else if (queue === 'tirebouchon') {
    const boucle = ombrer(new THREE.Mesh(new THREE.TorusGeometry(0.16, 0.05, 6, 12, Math.PI * 1.6), matRobe));
    boucle.position.set(-corps.longueur / 2 - 0.35, corps.hauteur + 0.25, 0);
    boucle.rotation.y = Math.PI / 2;
    groupe.add(boucle);
  } else if (queue === 'crin') {
    const crin = ombrer(new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.9, 0.22), matSombre));
    crin.position.set(-corps.longueur / 2 - 0.45, corps.hauteur - 0.1, 0);
    crin.rotation.z = 0.35;
    groupe.add(crin);
  }

  groupe.scale.setScalar(echelle);

  const hauteur = (tete.hauteur + tete.taille * 0.6) * echelle;
  const rayon = (corps.longueur / 2 + corps.rayon) * echelle;
  return { groupe, tete: groupeTete, pattes, hauteur, rayon };
}

// ---------------------------------------------------------------------------
// Fabrique d'oiseaux : poule, canard, oie
// ---------------------------------------------------------------------------

function creerOiseau(couleurNom, profil) {
  const {
    corps = 0.52,
    cou = 0,
    bec = { couleur: '#e8a33d', longueur: 0.28, plat: false },
    crete = true,
    pattes: couleurPattes = '#e8a33d',
    echelle = 1,
  } = profil;

  const groupe = new THREE.Group();
  const plumage = couleurDepuisNom(couleurNom);
  const matPlumage = matiere(plumage);

  const buste = ombrer(new THREE.Mesh(new THREE.SphereGeometry(corps, 14, 12), matPlumage));
  buste.scale.set(1.15, 1, 0.95);
  buste.position.y = corps * 1.4;
  groupe.add(buste);

  const matAile = matiere(plumage.clone().multiplyScalar(0.82));
  for (const cote of [1, -1]) {
    const aile = ombrer(new THREE.Mesh(new THREE.SphereGeometry(corps * 0.58, 10, 8), matAile));
    aile.scale.set(1.1, 0.7, 0.35);
    aile.position.set(-0.02, corps * 1.5, cote * corps * 0.88);
    groupe.add(aile);
  }

  if (cou > 0) {
    const gorge = ombrer(new THREE.Mesh(
      new THREE.CylinderGeometry(corps * 0.28, corps * 0.34, cou, 8), matPlumage));
    gorge.position.set(corps * 0.55, corps * 1.4 + cou / 2, 0);
    gorge.rotation.z = -0.25;
    groupe.add(gorge);
  }

  const tete = new THREE.Group();
  tete.position.set(corps * 0.8, corps * 2.35 + cou, 0);

  const crane = ombrer(new THREE.Mesh(new THREE.SphereGeometry(corps * 0.54, 12, 10), matPlumage));
  tete.add(crane);

  const matBec = matiere(bec.couleur);
  if (bec.plat) {
    const plat = ombrer(new THREE.Mesh(new THREE.BoxGeometry(bec.longueur, 0.07, 0.24), matBec));
    plat.position.set(corps * 0.6, -0.04, 0);
    tete.add(plat);
  } else {
    const pointe = ombrer(new THREE.Mesh(new THREE.ConeGeometry(0.1, bec.longueur, 6), matBec));
    pointe.position.set(corps * 0.58, -0.03, 0);
    pointe.rotation.z = -Math.PI / 2;
    tete.add(pointe);
  }

  if (crete) {
    const matCrete = matiere('#d8453c');
    for (const [x, h] of [[-0.06, 0.18], [0.06, 0.22], [0.18, 0.16]]) {
      const dent = ombrer(new THREE.Mesh(new THREE.BoxGeometry(0.09, h, 0.08), matCrete));
      dent.position.set(x, corps * 0.52, 0);
      tete.add(dent);
    }
    const barbillon = ombrer(new THREE.Mesh(new THREE.SphereGeometry(0.09, 8, 6), matCrete));
    barbillon.position.set(corps * 0.46, -corps * 0.38, 0);
    tete.add(barbillon);
  }

  for (const cote of [-1, 1]) {
    const oeil = new THREE.Mesh(new THREE.SphereGeometry(0.05, 8, 8), matiere('#1c1c22'));
    oeil.position.set(corps * 0.34, corps * 0.12, cote * corps * 0.3);
    tete.add(oeil);
  }
  groupe.add(tete);

  const matPatte = matiere(couleurPattes);
  const pattes = [];
  for (const z of [0.18, -0.18]) {
    const membre = ombrer(new THREE.Mesh(
      new THREE.CylinderGeometry(0.05, 0.05, corps * 0.9, 6), matPatte));
    membre.position.set(0, corps * 0.45, z);
    groupe.add(membre);
    pattes.push(membre);
  }

  const matQueue = matiere(plumage.clone().multiplyScalar(0.75));
  for (const angle of [0.5, 0.9, 0.2]) {
    const plume = ombrer(new THREE.Mesh(new THREE.ConeGeometry(0.12, corps * 0.8, 5), matQueue));
    plume.position.set(-corps * 1.15, corps * 1.85, 0);
    plume.rotation.z = Math.PI / 2 + angle;
    groupe.add(plume);
  }

  groupe.scale.setScalar(echelle);

  return {
    groupe,
    tete,
    pattes,
    hauteur: (corps * 3 + cou) * echelle,
    rayon: corps * 1.8 * echelle,
  };
}

// ---------------------------------------------------------------------------
// Catalogue : un profil par espece
// ---------------------------------------------------------------------------

const PROFILS = {
  VACHE: (couleur) => creerQuadrupede(couleur, {
    cornes: 'vache',
    museau: { taille: 0.3, couleur: '#e0a3a8' },
    taches: true,
    queue: 'fouet',
  }),

  CHEVRE: (couleur) => creerQuadrupede(couleur, {
    corps: { longueur: 1, rayon: 0.5, hauteur: 1 },
    patte: { rayon: 0.1, hauteur: 0.95, ecart: 0.45, largeur: 0.3 },
    tete: { taille: 0.6, avance: 0.95, hauteur: 1.35 },
    cornes: 'chevre',
    oreilles: 'tombantes',
    queue: 'pompon',
    echelle: 0.95,
  }),

  MOUTON: (couleur) => creerQuadrupede(couleur, {
    corps: { longueur: 1, rayon: 0.6, hauteur: 1 },
    patte: { rayon: 0.11, hauteur: 0.85, ecart: 0.45, largeur: 0.32 },
    tete: { taille: 0.55, avance: 0.95, hauteur: 1.25 },
    oreilles: 'tombantes',
    laine: true,
    queue: 'pompon',
  }),

  COCHON: (couleur) => creerQuadrupede(couleur, {
    corps: { longueur: 1.1, rayon: 0.62, hauteur: 0.85 },
    patte: { rayon: 0.13, hauteur: 0.6, ecart: 0.5, largeur: 0.34 },
    tete: { taille: 0.62, avance: 1, hauteur: 1.05 },
    museau: { taille: 0.34, couleur: '#f0b7b2' },
    oreilles: 'tombantes',
    queue: 'tirebouchon',
  }),

  CHEVAL: (couleur) => creerQuadrupede(couleur, {
    corps: { longueur: 1.7, rayon: 0.68, hauteur: 1.9 },
    patte: { rayon: 0.15, hauteur: 1.9, ecart: 0.8, largeur: 0.45 },
    cou: { rayon: 0.24, longueur: 1.25, inclinaison: 0.75 },
    tete: { taille: 0.72, avance: 1.75, hauteur: 2.7 },
    museau: { taille: 0.32, couleur: '#6b4a2f' },
    criniere: true,
    queue: 'crin',
  }),

  LAPIN: (couleur) => creerQuadrupede(couleur, {
    corps: { longueur: 0.4, rayon: 0.36, hauteur: 0.55 },
    patte: { rayon: 0.08, hauteur: 0.4, ecart: 0.25, largeur: 0.2 },
    tete: { taille: 0.42, avance: 0.5, hauteur: 0.8 },
    oreilles: 'longues',
    queue: 'pompon',
  }),

  POULE: (couleur) => creerOiseau(couleur, {}),

  CANARD: (couleur) => creerOiseau(couleur, {
    corps: 0.5,
    cou: 0.2,
    bec: { couleur: '#e8c23d', longueur: 0.34, plat: true },
    crete: false,
  }),

  OIE: (couleur) => creerOiseau(couleur, {
    corps: 0.62,
    cou: 0.85,
    bec: { couleur: '#e8873d', longueur: 0.3, plat: true },
    crete: false,
    echelle: 1.05,
  }),
};

/** Construit le modele correspondant a l'espece. */
export function creerModele(espece, couleur) {
  const fabrique = PROFILS[espece] ?? PROFILS.POULE;
  return fabrique(couleur);
}

/** Etiquette flottante avec le nom de l'animal et son proprietaire. */
export function creerEtiquette(nom, sousTitre) {
  const canvas = document.createElement('canvas');
  canvas.width = 512;
  canvas.height = 160;
  const ctx = canvas.getContext('2d');

  ctx.fillStyle = 'rgba(16, 26, 18, 0.82)';
  ctx.beginPath();
  ctx.roundRect(6, 6, canvas.width - 12, canvas.height - 12, 34);
  ctx.fill();
  ctx.strokeStyle = 'rgba(143, 212, 106, 0.85)';
  ctx.lineWidth = 4;
  ctx.stroke();

  ctx.textAlign = 'center';
  ctx.fillStyle = '#eef3ea';
  ctx.font = 'bold 60px Segoe UI, system-ui, sans-serif';
  ctx.fillText(nom, canvas.width / 2, sousTitre ? 76 : 100);

  if (sousTitre) {
    ctx.fillStyle = '#a8d48d';
    ctx.font = '40px Segoe UI, system-ui, sans-serif';
    ctx.fillText(sousTitre, canvas.width / 2, 128);
  }

  const texture = new THREE.CanvasTexture(canvas);
  texture.colorSpace = THREE.SRGBColorSpace;

  const sprite = new THREE.Sprite(new THREE.SpriteMaterial({ map: texture, transparent: true, depthTest: false }));
  sprite.scale.set(2.4, 0.75, 1);
  sprite.renderOrder = 10;
  return sprite;
}

/** Petite bulle emoji qui monte puis s'efface (lait, oeuf, argent…). */
export function creerBulle(emoji) {
  const canvas = document.createElement('canvas');
  canvas.width = 128;
  canvas.height = 128;
  const ctx = canvas.getContext('2d');
  ctx.font = '96px serif';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(emoji, 64, 70);

  const texture = new THREE.CanvasTexture(canvas);
  texture.colorSpace = THREE.SRGBColorSpace;

  const sprite = new THREE.Sprite(new THREE.SpriteMaterial({ map: texture, transparent: true, depthTest: false }));
  sprite.scale.setScalar(1.4);
  sprite.renderOrder = 11;
  return sprite;
}

/** Anneau pose au sol sous l'animal selectionne. */
export function creerAnneauSelection(rayon) {
  const anneau = new THREE.Mesh(
    new THREE.RingGeometry(rayon * 0.82, rayon, 40),
    new THREE.MeshBasicMaterial({ color: '#8fd46a', transparent: true, opacity: 0.9, side: THREE.DoubleSide }),
  );
  anneau.rotation.x = -Math.PI / 2;
  anneau.position.y = 0.06;
  anneau.visible = false;
  return anneau;
}

/** Petite icone posee au-dessus de l'animal quand il a faim ou qu'il est malade. */
export function creerAlerte() {
  const sprite = creerBulle('🍽️');
  sprite.visible = false;
  return sprite;
}

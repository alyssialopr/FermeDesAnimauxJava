/**
 * Modeles 3D des animaux, construits a la main a partir de primitives.
 * Chaque animal est un groupe dont on expose quelques parties (tete, pattes)
 * pour pouvoir les animer.
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
  objet.receiveShadow = false;
  return objet;
}

/** Vache : corps en capsule, tete, cornes, pis et taches. */
export function creerVache(couleurNom) {
  const groupe = new THREE.Group();
  const robe = couleurDepuisNom(couleurNom);
  const matRobe = matiere(robe);
  const sombre = robe.clone().multiplyScalar(0.55);

  const corps = ombrer(new THREE.Mesh(new THREE.CapsuleGeometry(0.75, 1.5, 6, 14), matRobe));
  corps.rotation.z = Math.PI / 2;
  corps.position.y = 1.35;
  groupe.add(corps);

  // Taches, plus visibles sur les robes claires.
  const matTache = matiere(sombre);
  const positionsTaches = [
    [0.35, 1.75, 0.55, 0.36],
    [-0.45, 1.5, -0.6, 0.3],
    [0.6, 1.25, -0.5, 0.26],
  ];
  for (const [x, y, z, r] of positionsTaches) {
    const tache = ombrer(new THREE.Mesh(new THREE.SphereGeometry(r, 10, 8), matTache));
    tache.position.set(x, y, z);
    tache.scale.set(1, 0.6, 1);
    groupe.add(tache);
  }

  const tete = new THREE.Group();
  tete.position.set(1.35, 1.75, 0);

  const crane = ombrer(new THREE.Mesh(new THREE.BoxGeometry(0.95, 0.78, 0.78), matRobe));
  tete.add(crane);

  const museau = ombrer(new THREE.Mesh(new THREE.BoxGeometry(0.3, 0.28, 0.42), matiere('#e0a3a8')));
  museau.position.set(0.5, -0.16, 0);
  tete.add(museau);

  const matCorne = matiere('#e8dfc8');
  for (const cote of [-1, 1]) {
    const corne = ombrer(new THREE.Mesh(new THREE.ConeGeometry(0.11, 0.42, 6), matCorne));
    corne.position.set(-0.1, 0.45, cote * 0.28);
    corne.rotation.z = cote * -0.25;
    tete.add(corne);

    const oreille = ombrer(new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.22, 0.34), matRobe));
    oreille.position.set(-0.2, 0.12, cote * 0.48);
    tete.add(oreille);

    const oeil = new THREE.Mesh(new THREE.SphereGeometry(0.08, 8, 8), matiere('#1c1c22'));
    oeil.position.set(0.3, 0.12, cote * 0.3);
    tete.add(oeil);
  }
  groupe.add(tete);

  const pattes = [];
  const matPatte = matiere(sombre);
  for (const [x, z] of [[0.7, 0.45], [0.7, -0.45], [-0.7, 0.45], [-0.7, -0.45]]) {
    const patte = ombrer(new THREE.Mesh(new THREE.CylinderGeometry(0.16, 0.14, 1.3, 7), matPatte));
    patte.position.set(x, 0.65, z);
    groupe.add(patte);
    pattes.push(patte);
  }

  const pis = ombrer(new THREE.Mesh(new THREE.SphereGeometry(0.3, 10, 8), matiere('#f0b9bb')));
  pis.position.set(-0.55, 0.85, 0);
  pis.scale.set(1, 0.8, 1);
  groupe.add(pis);

  const queue = ombrer(new THREE.Mesh(new THREE.CylinderGeometry(0.06, 0.03, 1, 5), matPatte));
  queue.position.set(-1.35, 1.5, 0);
  queue.rotation.z = 0.5;
  groupe.add(queue);

  return { groupe, tete, pattes, hauteur: 2.6, rayon: 1.6 };
}

/** Poule : corps ovale, bec, crete et pattes fines. */
export function creerPoule(couleurNom) {
  const groupe = new THREE.Group();
  const plumage = couleurDepuisNom(couleurNom);
  const matPlumage = matiere(plumage);

  const corps = ombrer(new THREE.Mesh(new THREE.SphereGeometry(0.52, 14, 12), matPlumage));
  corps.scale.set(1.15, 1, 0.95);
  corps.position.y = 0.72;
  groupe.add(corps);

  const aile = ombrer(new THREE.Mesh(new THREE.SphereGeometry(0.3, 10, 8), matiere(plumage.clone().multiplyScalar(0.82))));
  aile.scale.set(1.1, 0.7, 0.35);
  aile.position.set(-0.02, 0.78, 0.46);
  groupe.add(aile);

  const aileGauche = aile.clone();
  aileGauche.position.z = -0.46;
  groupe.add(aileGauche);

  const tete = new THREE.Group();
  tete.position.set(0.42, 1.22, 0);

  const crane = ombrer(new THREE.Mesh(new THREE.SphereGeometry(0.28, 12, 10), matPlumage));
  tete.add(crane);

  const bec = ombrer(new THREE.Mesh(new THREE.ConeGeometry(0.1, 0.28, 6), matiere('#e8a33d')));
  bec.position.set(0.3, -0.03, 0);
  bec.rotation.z = -Math.PI / 2;
  tete.add(bec);

  const matCrete = matiere('#d8453c');
  for (const [x, h] of [[-0.06, 0.18], [0.06, 0.22], [0.18, 0.16]]) {
    const dent = ombrer(new THREE.Mesh(new THREE.BoxGeometry(0.09, h, 0.08), matCrete));
    dent.position.set(x, 0.28, 0);
    tete.add(dent);
  }

  const barbillon = ombrer(new THREE.Mesh(new THREE.SphereGeometry(0.09, 8, 6), matCrete));
  barbillon.position.set(0.24, -0.2, 0);
  tete.add(barbillon);

  for (const cote of [-1, 1]) {
    const oeil = new THREE.Mesh(new THREE.SphereGeometry(0.05, 8, 8), matiere('#1c1c22'));
    oeil.position.set(0.18, 0.06, cote * 0.16);
    tete.add(oeil);
  }
  groupe.add(tete);

  const matPatte = matiere('#e8a33d');
  const pattes = [];
  for (const z of [0.18, -0.18]) {
    const patte = ombrer(new THREE.Mesh(new THREE.CylinderGeometry(0.05, 0.05, 0.45, 6), matPatte));
    patte.position.set(0, 0.22, z);
    groupe.add(patte);
    pattes.push(patte);
  }

  const matQueue = matiere(plumage.clone().multiplyScalar(0.75));
  for (const [angle, taille] of [[0.5, 0.42], [0.9, 0.36], [0.2, 0.34]]) {
    const plume = ombrer(new THREE.Mesh(new THREE.ConeGeometry(0.12, taille, 5), matQueue));
    plume.position.set(-0.6, 0.95, 0);
    plume.rotation.z = Math.PI / 2 + angle;
    groupe.add(plume);
  }

  return { groupe, tete, pattes, hauteur: 1.6, rayon: 0.9 };
}

/** Etiquette flottante avec le nom de l'animal et son proprietaire. */
export function creerEtiquette(nom, sousTitre) {
  const canvas = document.createElement('canvas');
  canvas.width = 512;
  canvas.height = 160;
  const ctx = canvas.getContext('2d');

  const rayon = 34;
  ctx.fillStyle = 'rgba(16, 26, 18, 0.82)';
  ctx.beginPath();
  ctx.roundRect(6, 6, canvas.width - 12, canvas.height - 12, rayon);
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
  sprite.scale.set(2.8, 0.88, 1);
  sprite.renderOrder = 10;
  return sprite;
}

/** Petite bulle emoji qui monte puis s'efface (lait, oeuf, coeur…). */
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

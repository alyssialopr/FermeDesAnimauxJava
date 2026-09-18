/**
 * Le decor : ciel, prairie, cloture des enclos, marche, lumieres et camera.
 * Ce module ne connait rien de l'API : il fabrique et positionne des objets 3D.
 */

import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';

export const TAILLE_ENCLOS = 16;
const ECART_ENCLOS = 4;

/** Position du centre d'un enclos selon son rang (grille 3 colonnes). */
export function positionEnclos(rang) {
  const colonnes = 3;
  const pas = TAILLE_ENCLOS + ECART_ENCLOS;
  const colonne = rang % colonnes;
  const ligne = Math.floor(rang / colonnes);
  return new THREE.Vector3(
    (colonne - (colonnes - 1) / 2) * pas,
    0,
    ligne * pas - 6,
  );
}

/** Zone du marche : les animaux sans proprietaire y attendent un acheteur. */
export const POSITION_MARCHE = new THREE.Vector3(0, 0, -26);

export function creerMonde(canvas) {
  const scene = new THREE.Scene();
  scene.background = new THREE.Color('#8fc4e8');
  scene.fog = new THREE.Fog('#8fc4e8', 95, 210);

  const camera = new THREE.PerspectiveCamera(50, window.innerWidth / window.innerHeight, 0.1, 400);
  camera.position.set(0, 26, 40);

  const renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  renderer.setSize(window.innerWidth, window.innerHeight);
  renderer.shadowMap.enabled = true;
  renderer.shadowMap.type = THREE.PCFSoftShadowMap;

  const controles = new OrbitControls(camera, renderer.domElement);
  controles.target.set(0, 0, 2);
  controles.enableDamping = true;
  controles.dampingFactor = 0.08;
  controles.minDistance = 12;
  controles.maxDistance = 90;
  controles.maxPolarAngle = Math.PI / 2.15;
  controles.update();

  ajouterLumieres(scene);
  ajouterPrairie(scene);

  const groupeEnclos = new THREE.Group();
  scene.add(groupeEnclos);

  const groupeAnimaux = new THREE.Group();
  scene.add(groupeAnimaux);

  window.addEventListener('resize', () => {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
  });

  return { scene, camera, renderer, controles, groupeEnclos, groupeAnimaux };
}

/**
 * Recadre la camera pour que toute la ferme (enclos + marche) tienne a l'ecran.
 * Appele au premier chargement et quand la ferme s'agrandit, jamais pendant que
 * le joueur manipule la camera.
 */
export function cadrerFerme(monde, positions) {
  const boite = new THREE.Box3();
  const marge = TAILLE_ENCLOS / 2;

  for (const position of [...positions, POSITION_MARCHE]) {
    boite.expandByPoint(new THREE.Vector3(position.x - marge, 0, position.z - marge));
    boite.expandByPoint(new THREE.Vector3(position.x + marge, 4, position.z + marge));
  }

  const centre = boite.getCenter(new THREE.Vector3());
  const taille = boite.getSize(new THREE.Vector3());
  const rayon = Math.max(taille.x, taille.z) * 0.5;
  const distance = Math.min(80, Math.max(26, rayon / Math.tan((monde.camera.fov * Math.PI) / 360)));

  monde.controles.target.copy(centre);
  monde.camera.position.set(centre.x, centre.y + distance * 0.55, centre.z + distance * 0.72);
  monde.camera.lookAt(centre);
  monde.controles.update();
}

function ajouterLumieres(scene) {
  scene.add(new THREE.HemisphereLight('#cfe8ff', '#4a7a3a', 1.1));

  const soleil = new THREE.DirectionalLight('#fff3d6', 2.1);
  soleil.position.set(24, 38, 18);
  soleil.castShadow = true;
  soleil.shadow.mapSize.set(2048, 2048);
  soleil.shadow.camera.near = 1;
  soleil.shadow.camera.far = 120;
  soleil.shadow.camera.left = -60;
  soleil.shadow.camera.right = 60;
  soleil.shadow.camera.top = 60;
  soleil.shadow.camera.bottom = -60;
  soleil.shadow.bias = -0.0008;
  scene.add(soleil);
}

function ajouterPrairie(scene) {
  const prairie = new THREE.Mesh(
    new THREE.CircleGeometry(110, 64),
    new THREE.MeshStandardMaterial({ color: '#5f8f42', roughness: 1 }),
  );
  prairie.rotation.x = -Math.PI / 2;
  prairie.receiveShadow = true;
  scene.add(prairie);

  // Quelques arbres et buissons pour donner de la profondeur au paysage.
  const decor = new THREE.Group();
  const aleatoire = generateurPseudoAleatoire(1789);
  for (let i = 0; i < 46; i += 1) {
    const angle = aleatoire() * Math.PI * 2;
    const rayon = 40 + aleatoire() * 58;
    const x = Math.cos(angle) * rayon;
    const z = Math.sin(angle) * rayon;
    decor.add(aleatoire() > 0.35 ? creerArbre(x, z, 0.8 + aleatoire() * 0.8) : creerBuisson(x, z));
  }
  scene.add(decor);
}

function creerArbre(x, z, echelle) {
  const arbre = new THREE.Group();

  const tronc = new THREE.Mesh(
    new THREE.CylinderGeometry(0.35, 0.5, 3, 6),
    new THREE.MeshStandardMaterial({ color: '#6b4a2f', roughness: 1 }),
  );
  tronc.position.y = 1.5;
  tronc.castShadow = true;
  arbre.add(tronc);

  const feuillage = new THREE.Mesh(
    new THREE.IcosahedronGeometry(2.2, 0),
    new THREE.MeshStandardMaterial({ color: '#3f7a35', flatShading: true, roughness: 1 }),
  );
  feuillage.position.y = 4.2;
  feuillage.castShadow = true;
  arbre.add(feuillage);

  arbre.position.set(x, 0, z);
  arbre.scale.setScalar(echelle);
  return arbre;
}

function creerBuisson(x, z) {
  const buisson = new THREE.Mesh(
    new THREE.IcosahedronGeometry(1.1, 0),
    new THREE.MeshStandardMaterial({ color: '#4c8438', flatShading: true, roughness: 1 }),
  );
  buisson.position.set(x, 0.7, z);
  buisson.castShadow = true;
  return buisson;
}

/**
 * Un enclos : une parcelle d'herbe plus claire, une cloture en bois et un panneau
 * portant son nom.
 */
export function creerEnclos(nom, position) {
  const enclos = new THREE.Group();
  enclos.position.copy(position);

  const demi = TAILLE_ENCLOS / 2;

  const parcelle = new THREE.Mesh(
    new THREE.PlaneGeometry(TAILLE_ENCLOS, TAILLE_ENCLOS),
    new THREE.MeshStandardMaterial({ color: '#75a850', roughness: 1 }),
  );
  parcelle.rotation.x = -Math.PI / 2;
  parcelle.position.y = 0.02;
  parcelle.receiveShadow = true;
  enclos.add(parcelle);

  const bois = new THREE.MeshStandardMaterial({ color: '#a9805a', roughness: 0.9 });
  const poteau = new THREE.BoxGeometry(0.28, 1.5, 0.28);
  const barre = new THREE.BoxGeometry(TAILLE_ENCLOS, 0.16, 0.14);

  for (let i = 0; i <= 8; i += 1) {
    const t = -demi + (i * TAILLE_ENCLOS) / 8;
    for (const [x, z] of [[t, -demi], [t, demi], [-demi, t], [demi, t]]) {
      const p = new THREE.Mesh(poteau, bois);
      p.position.set(x, 0.75, z);
      p.castShadow = true;
      enclos.add(p);
    }
  }

  for (const hauteur of [0.6, 1.15]) {
    for (const [x, z, rotation] of [
      [0, -demi, 0],
      [0, demi, 0],
      [-demi, 0, Math.PI / 2],
      [demi, 0, Math.PI / 2],
    ]) {
      const b = new THREE.Mesh(barre, bois);
      b.position.set(x, hauteur, z);
      b.rotation.y = rotation;
      b.castShadow = true;
      enclos.add(b);
    }
  }

  enclos.add(creerPanneau(`Enclos ${nom}`, new THREE.Vector3(-demi + 1.2, 0, -demi - 0.6)));
  return enclos;
}

/** La zone du marche, sans cloture : ici, les animaux attendent un acheteur. */
export function creerMarche(position) {
  const marche = new THREE.Group();
  marche.position.copy(position);

  const sol = new THREE.Mesh(
    new THREE.CircleGeometry(9, 40),
    new THREE.MeshStandardMaterial({ color: '#c2a878', roughness: 1 }),
  );
  sol.rotation.x = -Math.PI / 2;
  sol.position.y = 0.03;
  sol.receiveShadow = true;
  marche.add(sol);

  // Un petit auvent de marche.
  const bois = new THREE.MeshStandardMaterial({ color: '#8d6743', roughness: 0.9 });
  for (const [x, z] of [[-3.4, -3.4], [3.4, -3.4], [-3.4, 3.4], [3.4, 3.4]]) {
    const pied = new THREE.Mesh(new THREE.CylinderGeometry(0.16, 0.16, 3.4, 6), bois);
    pied.position.set(x, 1.7, z);
    pied.castShadow = true;
    marche.add(pied);
  }

  const toit = new THREE.Mesh(
    new THREE.ConeGeometry(6.4, 1.8, 4),
    new THREE.MeshStandardMaterial({ color: '#c0503f', flatShading: true, roughness: 0.9 }),
  );
  toit.position.y = 4.3;
  toit.rotation.y = Math.PI / 4;
  toit.castShadow = true;
  marche.add(toit);

  marche.add(creerPanneau('Marché', new THREE.Vector3(0, 0, 6.4)));
  return marche;
}

/** Panneau de bois avec un texte peint dessus. */
function creerPanneau(texte, position) {
  const panneau = new THREE.Group();

  const pied = new THREE.Mesh(
    new THREE.BoxGeometry(0.18, 2, 0.18),
    new THREE.MeshStandardMaterial({ color: '#8d6743', roughness: 1 }),
  );
  pied.position.y = 1;
  pied.castShadow = true;
  panneau.add(pied);

  const plaque = new THREE.Mesh(
    new THREE.BoxGeometry(3.6, 1, 0.12),
    new THREE.MeshStandardMaterial({ map: textureTexte(texte), roughness: 0.95 }),
  );
  plaque.position.y = 2.1;
  plaque.castShadow = true;
  panneau.add(plaque);

  panneau.position.copy(position);
  return panneau;
}

/** Texture "planche de bois avec du texte" generee dans un canvas 2D. */
function textureTexte(texte) {
  const canvas = document.createElement('canvas');
  canvas.width = 512;
  canvas.height = 144;
  const ctx = canvas.getContext('2d');

  ctx.fillStyle = '#c69c6d';
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  ctx.strokeStyle = 'rgba(120, 84, 50, 0.45)';
  ctx.lineWidth = 3;
  for (let y = 18; y < canvas.height; y += 30) {
    ctx.beginPath();
    ctx.moveTo(0, y);
    ctx.lineTo(canvas.width, y);
    ctx.stroke();
  }

  ctx.fillStyle = '#4a2f18';
  ctx.font = 'bold 62px Segoe UI, system-ui, sans-serif';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(texte, canvas.width / 2, canvas.height / 2 + 4);

  const texture = new THREE.CanvasTexture(canvas);
  texture.colorSpace = THREE.SRGBColorSpace;
  texture.anisotropy = 4;
  return texture;
}

/** Suite pseudo-aleatoire deterministe : le decor est identique a chaque partie. */
export function generateurPseudoAleatoire(graine) {
  let etat = graine >>> 0;
  return () => {
    etat = (etat * 1664525 + 1013904223) >>> 0;
    return etat / 4294967296;
  };
}

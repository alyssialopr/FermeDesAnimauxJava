/**
 * Comportement des animaux dans la scene : deambulation dans leur enclos,
 * animations des actions (repas, soin, balade, recolte, vente) et selection.
 */

import * as THREE from 'three';
import {
  creerAlerte,
  creerAnneauSelection,
  creerBulle,
  creerEtiquette,
  creerModele,
} from './modeles.js';

/** Les petites betes trottinent, les grosses avancent posement. */
const VITESSE = {
  VACHE: 1.5, CHEVRE: 2.2, MOUTON: 1.8, COCHON: 1.8, CHEVAL: 2.6,
  LAPIN: 3.2, POULE: 2.6, CANARD: 2.2, OIE: 2,
};

/** Ce qui sort de l'animal quand on le recolte. */
const EMOJI_PRODUCTION = {
  LAIT: '🥛', OEUFS: '🥚', LAINE: '🧶', DUVET: '🪶', LAPEREAUX: '🐇', AUCUNE: '✨',
};
const PAUSE_MIN = 1.5;
const PAUSE_MAX = 5;

export class AnimalVisuel {
  constructor(donnees) {
    this.donnees = donnees;
    this.id = donnees.id;

    const modele = creerModele(donnees.espece, donnees.couleur);
    this.corps = modele.groupe;
    this.tete = modele.tete;
    this.pattes = modele.pattes;
    this.rayon = modele.rayon;
    this.hauteur = modele.hauteur;
    this.vitesse = VITESSE[donnees.espece] ?? 1.5;

    this.groupe = new THREE.Group();
    this.groupe.add(this.corps);

    this.anneau = creerAnneauSelection(this.rayon * 1.25);
    this.groupe.add(this.anneau);

    this.etiquette = null;
    this.majEtiquette();

    // Icone d'alerte : l'animal reclame a manger ou le veterinaire.
    this.alerte = creerAlerte();
    this.alerte.scale.setScalar(0.95);
    this.alerte.position.y = this.hauteur + 1.7;
    this.groupe.add(this.alerte);
    this.majAlerte();

    // Chaque maillage porte l'identifiant : le raycast retrouve l'animal cliquable.
    this.groupe.traverse((objet) => {
      objet.userData.animalId = donnees.id;
    });

    this.zone = { centre: new THREE.Vector3(), rayon: 5 };
    this.cible = new THREE.Vector3();
    this.attente = 0;
    this.phase = Math.random() * Math.PI * 2;
    this.bulles = [];
    this.effet = null;
    this.partie = false;
    this.opacite = 1;
  }

  // ------------------------------------------------------------- placement

  definirZone(centre, rayon) {
    this.zone.centre.copy(centre);
    this.zone.rayon = rayon;
    this.choisirCible();
  }

  placer(position) {
    this.groupe.position.copy(position);
    this.cible.copy(position);
  }

  positionAleatoireDansZone() {
    const angle = Math.random() * Math.PI * 2;
    const distance = Math.sqrt(Math.random()) * this.zone.rayon;
    return new THREE.Vector3(
      this.zone.centre.x + Math.cos(angle) * distance,
      0,
      this.zone.centre.z + Math.sin(angle) * distance,
    );
  }

  choisirCible() {
    this.cible.copy(this.positionAleatoireDansZone());
    this.attente = PAUSE_MIN + Math.random() * (PAUSE_MAX - PAUSE_MIN);
  }

  // ---------------------------------------------------------------- donnees

  majDonnees(donnees) {
    const changement = donnees.nom !== this.donnees.nom
      || donnees.eleveurPrenom !== this.donnees.eleveurPrenom;
    this.donnees = donnees;
    if (changement) {
      this.majEtiquette();
    }
    this.majAlerte();
  }

  /** Affiche une gamelle si l'animal a faim, une tete malade s'il est mal en point. */
  majAlerte() {
    const { faim = 0, sante = 100, eleveurId } = this.donnees;
    // Les animaux du marche n'ont personne a alerter.
    const besoin = eleveurId == null ? null : sante < 30 ? '🤒' : faim > 70 ? '🍽️' : null;

    this.alerte.visible = besoin !== null;
    if (besoin && besoin !== this.alerteAffichee) {
      this.alerteAffichee = besoin;
      this.groupe.remove(this.alerte);
      this.alerte.material.map.dispose();
      this.alerte.material.dispose();
      this.alerte = creerBulle(besoin);
      this.alerte.scale.setScalar(0.95);
      this.alerte.position.y = this.hauteur + 1.7;
      this.alerte.userData.animalId = this.donnees.id;
      this.groupe.add(this.alerte);
    }
  }

  majEtiquette() {
    if (this.etiquette) {
      this.groupe.remove(this.etiquette);
      this.etiquette.material.map.dispose();
      this.etiquette.material.dispose();
    }
    const proprietaire = this.donnees.eleveurPrenom ? `à ${this.donnees.eleveurPrenom}` : 'au marché';
    this.etiquette = creerEtiquette(this.donnees.nom, proprietaire);
    this.etiquette.position.y = this.hauteur + 0.9;
    this.etiquette.userData.animalId = this.donnees.id;
    this.groupe.add(this.etiquette);
  }

  selectionner(actif) {
    this.anneau.visible = actif;
  }

  // -------------------------------------------------------------- animations

  /** Declenche l'animation correspondant a une action de l'API. */
  jouerAction(action) {
    switch (action) {
      case 'repas':
        this.effet = { type: 'brouter', reste: 2.2 };
        this.ajouterBulle('🌾');
        break;
      case 'soin':
        this.effet = { type: 'pulser', reste: 1.4 };
        this.ajouterBulle('💊');
        break;
      case 'balade':
        this.effet = { type: 'balade', reste: 7 };
        this.cible.copy(this.positionAleatoireDansZone());
        this.attente = 0;
        this.ajouterBulle('🚶');
        break;
      case 'recolte':
        this.effet = { type: 'pulser', reste: 1.2 };
        this.ajouterBulle(EMOJI_PRODUCTION[this.donnees.production] ?? '✨', -0.6);
        this.ajouterBulle('💶', 0.6);
        break;
      case 'achat':
        this.effet = { type: 'pulser', reste: 1.2 };
        this.ajouterBulle('💰');
        break;
      case 'vente':
        this.effet = { type: 'depart', reste: 3.4 };
        this.ajouterBulle('👋');
        break;
      default:
        break;
    }
  }

  ajouterBulle(emoji, decalage = 0) {
    const bulle = creerBulle(emoji);
    bulle.position.set(decalage, this.hauteur + 0.4, 0);
    this.groupe.add(bulle);
    this.bulles.push({ sprite: bulle, vie: 0 });
  }

  /** Depart definitif (animal vendu) : il s'en va puis disparait. */
  partir(direction) {
    this.effet = { type: 'depart', reste: 3.4 };
    this.cible.copy(direction);
  }

  // ------------------------------------------------------------------ boucle

  animer(delta, temps) {
    this.animerBulles(delta);
    this.animerEffet(delta);
    this.deplacer(delta, temps);
  }

  animerBulles(delta) {
    if (this.alerte?.visible) {
      this.alerte.position.y = this.hauteur + 1.7 + Math.sin(this.phase + performance.now() / 500) * 0.1;
    }
    for (let i = this.bulles.length - 1; i >= 0; i -= 1) {
      const bulle = this.bulles[i];
      bulle.vie += delta;
      bulle.sprite.position.y += delta * 1.5;
      bulle.sprite.material.opacity = Math.max(0, 1 - bulle.vie / 1.8);
      if (bulle.vie > 1.8) {
        this.groupe.remove(bulle.sprite);
        bulle.sprite.material.map.dispose();
        bulle.sprite.material.dispose();
        this.bulles.splice(i, 1);
      }
    }
  }

  animerEffet(delta) {
    if (!this.effet) {
      return;
    }
    this.effet.reste -= delta;

    if (this.effet.type === 'brouter' && this.tete) {
      const avancement = Math.max(0, Math.min(1, this.effet.reste / 2.2));
      this.tete.rotation.z = -0.55 * Math.sin(avancement * Math.PI);
      this.attente = Math.max(this.attente, 0.3);
    }

    if (this.effet.type === 'pulser') {
      const pulsation = 1 + 0.12 * Math.sin(this.effet.reste * 14);
      this.corps.scale.setScalar(pulsation);
    }

    if (this.effet.type === 'depart') {
      this.opacite = Math.max(0, this.effet.reste / 3.4);
      this.appliquerOpacite(this.opacite);
    }

    if (this.effet.reste <= 0) {
      if (this.effet.type === 'depart') {
        this.partie = true;
      }
      if (this.tete) {
        this.tete.rotation.z = 0;
      }
      this.corps.scale.setScalar(1);
      this.effet = null;
    }
  }

  deplacer(delta, temps) {
    const enBalade = this.effet?.type === 'balade';
    const enDepart = this.effet?.type === 'depart';
    const vitesse = this.vitesse * (enBalade ? 2 : 1) * (enDepart ? 2.4 : 1);

    const versCible = new THREE.Vector3().subVectors(this.cible, this.groupe.position);
    versCible.y = 0;
    const distance = versCible.length();

    if (distance < 0.25 && !enDepart) {
      this.attente -= delta;
      if (this.attente <= 0) {
        this.choisirCible();
      }
      this.reposer(temps);
      return;
    }

    versCible.normalize();
    this.groupe.position.addScaledVector(versCible, Math.min(vitesse * delta, distance));

    // Les modeles regardent vers +X : on oriente le groupe vers la direction suivie.
    const angle = Math.atan2(-versCible.z, versCible.x);
    this.groupe.rotation.y = this.rotationLissee(this.groupe.rotation.y, angle, delta);

    this.marcher(temps, enBalade ? 1.7 : 1);
  }

  rotationLissee(actuelle, voulue, delta) {
    let ecart = voulue - actuelle;
    while (ecart > Math.PI) ecart -= Math.PI * 2;
    while (ecart < -Math.PI) ecart += Math.PI * 2;
    return actuelle + ecart * Math.min(1, delta * 6);
  }

  marcher(temps, cadence) {
    const balancement = Math.sin((temps + this.phase) * 9 * cadence);
    this.pattes.forEach((patte, index) => {
      patte.rotation.z = balancement * 0.35 * (index % 2 === 0 ? 1 : -1);
    });
    this.corps.position.y = Math.abs(balancement) * 0.06;
  }

  reposer(temps) {
    const respiration = Math.sin((temps + this.phase) * 1.6) * 0.02;
    this.corps.position.y = respiration;
    this.pattes.forEach((patte) => {
      patte.rotation.z *= 0.85;
    });
  }

  appliquerOpacite(opacite) {
    this.corps.traverse((objet) => {
      if (objet.isMesh) {
        objet.material.transparent = opacite < 1;
        objet.material.opacity = opacite;
      }
    });
    if (this.etiquette) {
      this.etiquette.material.opacity = opacite;
    }
  }

  detruire() {
    this.groupe.traverse((objet) => {
      if (objet.isMesh || objet.isSprite) {
        objet.geometry?.dispose();
        objet.material?.map?.dispose();
        objet.material?.dispose();
      }
    });
    this.groupe.removeFromParent();
  }
}

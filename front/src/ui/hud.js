/**
 * Interface 2D posee au-dessus de la scene : barre du haut, fiche de l'animal
 * selectionne, journal, modales et petits messages.
 */

const EMOJI_ESPECE = { VACHE: '🐄', POULE: '🐔' };

export class Hud {
  constructor() {
    this.elements = {
      chargement: document.getElementById('chargement'),
      chargementTexte: document.getElementById('chargement-texte'),
      choixEleveur: document.getElementById('choix-eleveur'),
      nouvelEleveur: document.getElementById('nouvel-eleveur'),
      compteurTroupeau: document.getElementById('compteur-troupeau'),
      compteurLait: document.getElementById('compteur-lait'),
      compteurOeufs: document.getElementById('compteur-oeufs'),
      panneauVide: document.getElementById('panneau-vide'),
      fiche: document.getElementById('fiche'),
      ficheEmoji: document.getElementById('fiche-emoji'),
      ficheNom: document.getElementById('fiche-nom'),
      ficheDetail: document.getElementById('fiche-detail'),
      ficheEleveur: document.getElementById('fiche-eleveur'),
      ficheEnclos: document.getElementById('fiche-enclos'),
      ficheEtat: document.getElementById('fiche-etat'),
      ficheProduction: document.getElementById('fiche-production'),
      ficheProductionLabel: document.getElementById('fiche-production-label'),
      nouvelEnclos: document.getElementById('fiche-nouvel-enclos'),
      boutonDemenager: document.getElementById('bouton-demenager'),
      boutonNouvelAnimal: document.getElementById('bouton-nouvel-animal'),
      boutonAide: document.getElementById('bouton-aide'),
      journal: document.getElementById('journal-liste'),
      toasts: document.getElementById('toasts'),
      modaleAnimal: document.getElementById('modale-animal'),
      formulaireAnimal: document.getElementById('formulaire-animal'),
      modaleEleveur: document.getElementById('modale-eleveur'),
      formulaireEleveur: document.getElementById('formulaire-eleveur'),
      modaleAide: document.getElementById('modale-aide'),
      fermerAide: document.getElementById('fermer-aide'),
      especeAnimal: document.getElementById('animal-espece'),
      champLait: document.getElementById('champ-lait'),
      champOeufs: document.getElementById('champ-oeufs'),
    };

    this.elements.especeAnimal.addEventListener('change', () => this.majChampsEspece());
    this.elements.boutonAide.addEventListener('click', () => this.elements.modaleAide.showModal());
    this.elements.fermerAide.addEventListener('click', () => this.elements.modaleAide.close());
  }

  // --------------------------------------------------------------- branchements

  surChangementEleveur(rappel) {
    this.elements.choixEleveur.addEventListener('change', (evenement) => {
      rappel(evenement.target.value ? Number(evenement.target.value) : null);
    });
  }

  surAction(rappel) {
    document.querySelectorAll('.fiche__actions .action').forEach((bouton) => {
      bouton.addEventListener('click', () => rappel(bouton.dataset.action));
    });
  }

  surNouvelAnimal(rappel) {
    this.elements.boutonNouvelAnimal.addEventListener('click', rappel);
  }

  surNouvelEleveur(rappel) {
    this.elements.nouvelEleveur.addEventListener('click', rappel);
  }

  surDemenagement(rappel) {
    this.elements.boutonDemenager.addEventListener('click', () => {
      const valeur = this.elements.nouvelEnclos.value.trim();
      if (valeur) {
        rappel(valeur);
        this.elements.nouvelEnclos.value = '';
      }
    });
  }

  // -------------------------------------------------------------------- etat

  cacherChargement() {
    this.elements.chargement.classList.add('voile--parti');
    setTimeout(() => {
      this.elements.chargement.hidden = true;
    }, 600);
  }

  chargement(texte) {
    this.elements.chargementTexte.textContent = texte;
  }

  majEleveurs(eleveurs, selectionId) {
    const select = this.elements.choixEleveur;
    select.replaceChildren();

    const vide = document.createElement('option');
    vide.value = '';
    vide.textContent = eleveurs.length ? '— choisir —' : '— aucun éleveur —';
    select.append(vide);

    for (const eleveur of eleveurs) {
      const option = document.createElement('option');
      option.value = String(eleveur.id);
      option.textContent = `${eleveur.prenom} (${eleveur.nombreAnimaux})`;
      select.append(option);
    }
    select.value = selectionId ? String(selectionId) : '';
  }

  majCompteurs({ troupeau, lait, oeufs }) {
    this.elements.compteurTroupeau.textContent = troupeau;
    this.elements.compteurLait.textContent = lait;
    this.elements.compteurOeufs.textContent = oeufs;
  }

  // ------------------------------------------------------------------- fiche

  afficherFiche(animal, { eleveurId }) {
    const { elements } = this;
    elements.panneauVide.hidden = true;
    elements.fiche.hidden = false;

    elements.ficheEmoji.textContent = EMOJI_ESPECE[animal.espece] ?? '🐾';
    elements.ficheNom.textContent = animal.nom;
    elements.ficheDetail.textContent = `${animal.race} · ${animal.couleur}`;
    elements.ficheEleveur.textContent = animal.eleveurPrenom ?? 'personne (au marché)';
    elements.ficheEnclos.textContent = animal.enclos;
    elements.ficheEtat.textContent = animal.etat;

    if (animal.espece === 'VACHE') {
      elements.ficheProductionLabel.textContent = 'Lait / jour';
      elements.ficheProduction.textContent = `${animal.litresDeLaitParJour ?? '—'} L`;
    } else {
      elements.ficheProductionLabel.textContent = 'Œufs / semaine';
      elements.ficheProduction.textContent = `${animal.oeufsParSemaine ?? '—'}`;
    }

    const estMien = eleveurId != null && animal.eleveurId === eleveurId;
    const sansProprietaire = animal.eleveurId == null;
    const disponible = animal.etat === 'LIBRE';

    for (const bouton of document.querySelectorAll('.fiche__actions .action')) {
      const action = bouton.dataset.action;
      let actif;
      if (action === 'achat') {
        actif = eleveurId != null && sansProprietaire && disponible;
      } else {
        actif = estMien && disponible;
      }
      bouton.disabled = !actif;
      bouton.title = actif
        ? ''
        : this.raisonIndisponible(action, { eleveurId, estMien, sansProprietaire, disponible });
    }

    elements.boutonDemenager.disabled = !disponible;
  }

  raisonIndisponible(action, { eleveurId, estMien, sansProprietaire, disponible }) {
    if (eleveurId == null) return "Choisis d'abord un éleveur";
    if (!disponible) return "L'animal n'est plus à la ferme";
    if (action === 'achat') return sansProprietaire ? '' : 'Cet animal appartient déjà à quelqu’un';
    return estMien ? '' : 'Cet animal ne vous appartient pas';
  }

  masquerFiche() {
    this.elements.fiche.hidden = true;
    this.elements.panneauVide.hidden = false;
  }

  // ---------------------------------------------------------------- messages

  journal(message, type = 'info') {
    const ligne = document.createElement('li');
    ligne.textContent = message;
    if (type === 'erreur') {
      ligne.classList.add('erreur');
    }
    this.elements.journal.prepend(ligne);
    while (this.elements.journal.childElementCount > 40) {
      this.elements.journal.lastElementChild.remove();
    }
  }

  toast(message, type = 'info') {
    const bulle = document.createElement('div');
    bulle.className = `toast${type === 'erreur' ? ' toast--erreur' : ''}`;
    bulle.textContent = message;
    this.elements.toasts.append(bulle);

    setTimeout(() => {
      bulle.classList.add('toast--sortie');
      setTimeout(() => bulle.remove(), 300);
    }, 3200);
  }

  // ----------------------------------------------------------------- modales

  majChampsEspece() {
    const vache = this.elements.especeAnimal.value === 'VACHE';
    this.elements.champLait.hidden = !vache;
    this.elements.champOeufs.hidden = vache;
  }

  /** Ouvre le formulaire de creation et renvoie les donnees saisies, ou null. */
  demanderAnimal() {
    return new Promise((resoudre) => {
      const { modaleAnimal, formulaireAnimal } = this.elements;
      this.majChampsEspece();
      modaleAnimal.showModal();

      modaleAnimal.addEventListener('close', () => {
        if (modaleAnimal.returnValue !== 'creer') {
          resoudre(null);
          return;
        }
        const donnees = Object.fromEntries(new FormData(formulaireAnimal));
        const vache = donnees.espece === 'VACHE';
        resoudre({
          espece: donnees.espece,
          nom: String(donnees.nom).trim(),
          race: String(donnees.race).trim(),
          couleur: donnees.couleur,
          enclos: String(donnees.enclos).trim(),
          litresDeLaitParJour: vache ? Number(donnees.litresDeLaitParJour) : null,
          oeufsParSemaine: vache ? null : Number(donnees.oeufsParSemaine),
          acheter: donnees.acheter === 'on',
        });
        formulaireAnimal.reset();
      }, { once: true });
    });
  }

  demanderEleveur() {
    return new Promise((resoudre) => {
      const { modaleEleveur, formulaireEleveur } = this.elements;
      modaleEleveur.showModal();

      modaleEleveur.addEventListener('close', () => {
        if (modaleEleveur.returnValue !== 'creer') {
          resoudre(null);
          return;
        }
        const donnees = Object.fromEntries(new FormData(formulaireEleveur));
        resoudre(String(donnees.prenom).trim());
        formulaireEleveur.reset();
      }, { once: true });
    });
  }
}

/**
 * Interface 2D posee au-dessus de la scene : barre du haut, onglets Ferme /
 * Marche / Compte, fiche de l'animal selectionne, journal, modales et toasts.
 */

export const EMOJI_ESPECE = {
  VACHE: '🐄', POULE: '🐔', MOUTON: '🐑', CHEVRE: '🐐', COCHON: '🐖',
  CANARD: '🦆', LAPIN: '🐇', CHEVAL: '🐴', OIE: '🦢',
};

const EMOJI_MOUVEMENT = {
  ACHAT: '💰', VENTE: '🏷️', REPAS: '🌾', SOIN: '💊', RECOLTE: '🪣', PROMENADE: '🚶',
};

/** 12.5 -> "12,50 €" */
export function euros(montant) {
  if (montant === null || montant === undefined) {
    return '—';
  }
  return `${Number(montant).toFixed(2).replace('.', ',')} €`;
}

export function eurosSigne(montant) {
  const valeur = Number(montant);
  return `${valeur > 0 ? '+' : ''}${euros(valeur)}`;
}

/** 135 -> "2 min 15 s" */
export function duree(secondes) {
  const minutes = Math.floor(secondes / 60);
  const reste = Math.round(secondes % 60);
  return minutes > 0 ? `${minutes} min ${reste} s` : `${reste} s`;
}

export class Hud {
  constructor() {
    this.elements = {
      chargement: document.getElementById('chargement'),
      chargementTexte: document.getElementById('chargement-texte'),
      choixEleveur: document.getElementById('choix-eleveur'),
      nouvelEleveur: document.getElementById('nouvel-eleveur'),
      compteurSolde: document.getElementById('compteur-solde'),
      compteurFortune: document.getElementById('compteur-fortune'),
      compteurTroupeau: document.getElementById('compteur-troupeau'),
      panneauVide: document.getElementById('panneau-vide'),
      fiche: document.getElementById('fiche'),
      ficheEmoji: document.getElementById('fiche-emoji'),
      ficheNom: document.getElementById('fiche-nom'),
      ficheDetail: document.getElementById('fiche-detail'),
      fichePrix: document.getElementById('fiche-prix'),
      ficheEleveur: document.getElementById('fiche-eleveur'),
      ficheEnclos: document.getElementById('fiche-enclos'),
      ficheEtat: document.getElementById('fiche-etat'),
      ficheProduction: document.getElementById('fiche-production'),
      ficheRevente: document.getElementById('fiche-revente'),
      barreFaim: document.getElementById('barre-faim'),
      valeurFaim: document.getElementById('valeur-faim'),
      barreSante: document.getElementById('barre-sante'),
      valeurSante: document.getElementById('valeur-sante'),
      nouvelEnclos: document.getElementById('fiche-nouvel-enclos'),
      boutonDemenager: document.getElementById('bouton-demenager'),
      boutonNouvelAnimal: document.getElementById('bouton-nouvel-animal'),
      boutonAide: document.getElementById('bouton-aide'),
      journal: document.getElementById('journal-liste'),
      toasts: document.getElementById('toasts'),
      marcheListe: document.getElementById('marche-liste'),
      marcheVide: document.getElementById('marche-vide'),
      pastilleMarche: document.getElementById('pastille-marche'),
      classementListe: document.getElementById('classement-liste'),
      mouvementsListe: document.getElementById('mouvements-liste'),
      mouvementsVide: document.getElementById('mouvements-vide'),
      modaleAnimal: document.getElementById('modale-animal'),
      formulaireAnimal: document.getElementById('formulaire-animal'),
      modaleEleveur: document.getElementById('modale-eleveur'),
      formulaireEleveur: document.getElementById('formulaire-eleveur'),
      modaleCle: document.getElementById('modale-cle'),
      formulaireCle: document.getElementById('formulaire-cle'),
      explicationCle: document.getElementById('cle-explication'),
      modaleAide: document.getElementById('modale-aide'),
      fermerAide: document.getElementById('fermer-aide'),
      especeAnimal: document.getElementById('animal-espece'),
      resumeAnimal: document.getElementById('animal-resume'),
      prixAchat: document.getElementById('animal-prix-achat'),
    };

    this.catalogue = [];
    this.elements.especeAnimal.addEventListener('change', () => this.majResumeEspece());
    this.elements.boutonAide.addEventListener('click', () => this.elements.modaleAide.showModal());
    this.elements.fermerAide.addEventListener('click', () => this.elements.modaleAide.close());
    this.brancherOnglets();
  }

  brancherOnglets() {
    for (const onglet of document.querySelectorAll('.onglet')) {
      onglet.addEventListener('click', () => this.afficherOnglet(onglet.dataset.onglet));
    }
  }

  afficherOnglet(nom) {
    for (const onglet of document.querySelectorAll('.onglet')) {
      onglet.classList.toggle('onglet--actif', onglet.dataset.onglet === nom);
    }
    for (const vue of document.querySelectorAll('.vue')) {
      vue.classList.toggle('vue--active', vue.dataset.vue === nom);
    }
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

  /** Clic sur une ligne du marche : (animalId, action) -> 'achat' | 'voir'. */
  surMarche(rappel) {
    this.rappelMarche = rappel;
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

  definirCatalogue(catalogue) {
    this.catalogue = catalogue;
    const select = this.elements.especeAnimal;
    select.replaceChildren();

    for (const fiche of catalogue) {
      const option = document.createElement('option');
      option.value = fiche.espece;
      option.textContent = `${EMOJI_ESPECE[fiche.espece] ?? '🐾'} ${fiche.espece.toLowerCase()} — ${euros(fiche.prix)}`;
      select.append(option);
    }
    this.majResumeEspece();
  }

  ficheEspece(espece) {
    return this.catalogue.find((candidat) => candidat.espece === espece);
  }

  majResumeEspece() {
    const fiche = this.ficheEspece(this.elements.especeAnimal.value);
    if (!fiche) {
      return;
    }

    const production = fiche.production === 'AUCUNE'
      ? (Number(fiche.gainParBalade) > 0
        ? `Ne se récolte pas : ses promenades rapportent ${euros(fiche.gainParBalade)}.`
        : `Ne se récolte pas : chaque repas lui ajoute ${euros(fiche.gainParRepas)} de valeur.`)
      : `Donne ${fiche.quantiteParDefaut} ${fiche.unite} toutes les ${duree(fiche.delaiSecondes)}, `
        + `soit ${euros(fiche.quantiteParDefaut * fiche.prixUnitaire)} par récolte.`;

    this.elements.resumeAnimal.textContent =
      `${production} Repas ${euros(fiche.coutRepas)}, vétérinaire ${euros(fiche.coutSoin)}.`;
    this.elements.prixAchat.textContent = `(−${euros(fiche.prix)})`;
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
      option.textContent = `${eleveur.prenom} · ${euros(eleveur.solde)}`;
      select.append(option);
    }
    select.value = selectionId ? String(selectionId) : '';
  }

  majCompteurs({ solde, fortune, troupeau }) {
    this.elements.compteurSolde.textContent = solde == null ? '—' : euros(solde);
    this.elements.compteurFortune.textContent = fortune == null ? '—' : euros(fortune);
    this.elements.compteurTroupeau.textContent = troupeau;
  }

  // ------------------------------------------------------------------- fiche

  afficherFiche(animal, { eleveurId }) {
    const { elements } = this;
    elements.panneauVide.hidden = true;
    elements.fiche.hidden = false;

    elements.ficheEmoji.textContent = EMOJI_ESPECE[animal.espece] ?? '🐾';
    elements.ficheNom.textContent = animal.nom;
    elements.ficheDetail.textContent = `${animal.race} · ${animal.couleur}`;
    elements.fichePrix.textContent = euros(animal.prix);
    elements.ficheEleveur.textContent = animal.eleveurPrenom ?? 'personne (au marché)';
    elements.ficheEnclos.textContent = animal.enclos;
    elements.ficheEtat.textContent = animal.etat;
    elements.ficheRevente.textContent = euros(animal.valeurDeRevente);

    if (animal.production === 'AUCUNE') {
      const fiche = this.ficheEspece(animal.espece);
      elements.ficheProduction.textContent = Number(fiche?.gainParBalade) > 0
        ? `promenades ${euros(fiche.gainParBalade)}`
        : 'engraissement';
    } else if (animal.peutEtreRecolte) {
      elements.ficheProduction.textContent =
        `${animal.quantiteProduction} ${animal.unite} · ${euros(animal.valeurRecolte)}`;
    } else if (animal.secondesAvantRecolte > 0) {
      elements.ficheProduction.textContent = `prête dans ${duree(animal.secondesAvantRecolte)}`;
    } else {
      elements.ficheProduction.textContent = `${animal.quantiteProduction} ${animal.unite} (indisponible)`;
    }

    this.majJauge(elements.barreFaim, elements.valeurFaim, animal.faim, { inverse: true });
    this.majJauge(elements.barreSante, elements.valeurSante, animal.sante, { inverse: false });

    this.majActions(animal, eleveurId);
    elements.boutonDemenager.disabled = animal.etat !== 'LIBRE';
  }

  /** Barre de progression coloree : verte quand tout va bien, rouge quand ca urge. */
  majJauge(barre, valeurTexte, valeur, { inverse }) {
    barre.style.width = `${Math.max(0, Math.min(100, valeur))}%`;
    valeurTexte.textContent = `${valeur} %`;

    const critique = inverse ? valeur > 70 : valeur < 30;
    const attention = inverse ? valeur > 40 : valeur < 60;

    barre.classList.toggle('jauge__barre--alerte', critique);
    barre.classList.toggle('jauge__barre--attention', !critique && attention);
  }

  majActions(animal, eleveurId) {
    const fiche = this.ficheEspece(animal.espece);
    const estMien = eleveurId != null && animal.eleveurId === eleveurId;
    const sansProprietaire = animal.eleveurId == null;
    const disponible = animal.etat === 'LIBRE';

    const tarifs = {
      repas: fiche ? `−${euros(fiche.coutRepas)}` : '',
      soin: fiche ? `−${euros(fiche.coutSoin)}` : '',
      balade: fiche && Number(fiche.gainParBalade) > 0 ? `+${euros(fiche.gainParBalade)}` : 'gratuit',
      recolte: animal.production === 'AUCUNE' ? '—' : `+${euros(animal.valeurRecolte)}`,
      achat: `−${euros(animal.prix)}`,
      vente: `+${euros(animal.valeurDeRevente)}`,
    };

    for (const bouton of document.querySelectorAll('.fiche__actions .action')) {
      const action = bouton.dataset.action;
      let actif;

      if (action === 'achat') {
        actif = eleveurId != null && sansProprietaire && disponible;
      } else if (action === 'recolte') {
        actif = estMien && disponible && animal.peutEtreRecolte;
      } else {
        actif = estMien && disponible;
      }

      bouton.disabled = !actif;
      bouton.querySelector('small').textContent = tarifs[action] ?? '';
      bouton.title = actif ? '' : this.raisonIndisponible(action, animal, { eleveurId, estMien, sansProprietaire });
    }
  }

  raisonIndisponible(action, animal, { eleveurId, estMien, sansProprietaire }) {
    if (eleveurId == null) return "Choisis d'abord un éleveur";
    if (animal.etat !== 'LIBRE') return "L'animal n'est plus à la ferme";
    if (action === 'achat') return sansProprietaire ? '' : 'Cet animal appartient déjà à quelqu’un';
    if (!estMien) return 'Cet animal ne vous appartient pas';
    if (action === 'recolte') {
      if (animal.production === 'AUCUNE') return 'Cette espèce ne se récolte pas';
      if (animal.faim > 70) return 'Trop affamé pour produire : nourris-le';
      if (animal.sante < 30) return 'Épuisé : appelle le vétérinaire';
      if (animal.secondesAvantRecolte > 0) return `Prêt dans ${duree(animal.secondesAvantRecolte)}`;
    }
    return '';
  }

  masquerFiche() {
    this.elements.fiche.hidden = true;
    this.elements.panneauVide.hidden = false;
  }

  // ------------------------------------------------------------------ marche

  majMarche(animaux, { solde, eleveurId }) {
    const liste = this.elements.marcheListe;
    liste.replaceChildren();

    this.elements.pastilleMarche.textContent = String(animaux.length);
    this.elements.marcheVide.hidden = animaux.length > 0;

    for (const animal of animaux) {
      const ligne = document.createElement('li');
      ligne.className = 'article';

      const emoji = document.createElement('span');
      emoji.className = 'article__emoji';
      emoji.textContent = EMOJI_ESPECE[animal.espece] ?? '🐾';

      const texte = document.createElement('div');
      texte.className = 'article__texte';
      const nom = document.createElement('div');
      nom.className = 'article__nom';
      nom.textContent = animal.nom;
      const detail = document.createElement('div');
      detail.className = 'article__detail';
      detail.textContent = animal.production === 'AUCUNE'
        ? `${animal.race} · ne se récolte pas`
        : `${animal.race} · ${animal.quantiteProduction} ${animal.unite} par récolte`;
      texte.append(nom, detail);

      const prix = document.createElement('span');
      prix.className = 'article__prix';
      prix.textContent = euros(animal.prix);

      const acheter = document.createElement('button');
      acheter.className = 'bouton';
      acheter.textContent = 'Acheter';
      const tropCher = solde != null && Number(solde) < Number(animal.prix);
      acheter.disabled = eleveurId == null || tropCher;
      acheter.title = eleveurId == null
        ? "Choisis d'abord un éleveur"
        : tropCher ? 'Pas assez d’argent' : '';
      acheter.addEventListener('click', () => this.rappelMarche?.(animal.id, 'achat'));

      ligne.append(emoji, texte, prix, acheter);
      ligne.addEventListener('click', (evenement) => {
        if (evenement.target !== acheter) {
          this.rappelMarche?.(animal.id, 'voir');
        }
      });
      liste.append(ligne);
    }
  }

  // ------------------------------------------------------------------ compte

  majClassement(classement, eleveurId) {
    const liste = this.elements.classementListe;
    liste.replaceChildren();

    for (const ligne of classement) {
      const element = document.createElement('li');

      const rang = document.createElement('span');
      rang.className = `rang${ligne.eleveurId === eleveurId ? ' rang--moi' : ''}`;
      rang.textContent = ligne.rang === 1 ? '🥇' : ligne.rang === 2 ? '🥈' : ligne.rang === 3 ? '🥉' : `${ligne.rang}`;

      const texte = document.createElement('div');
      texte.className = 'article__texte';
      const nom = document.createElement('div');
      nom.className = 'article__nom';
      nom.textContent = ligne.prenom;
      const detail = document.createElement('div');
      detail.className = 'article__detail';
      detail.textContent = `${euros(ligne.solde)} en caisse · ${ligne.nombreAnimaux} animaux`;
      texte.append(nom, detail);

      const fortune = document.createElement('span');
      fortune.className = 'article__prix';
      fortune.textContent = euros(ligne.fortune);

      element.append(rang, texte, fortune);
      liste.append(element);
    }
  }

  majMouvements(mouvements) {
    const liste = this.elements.mouvementsListe;
    liste.replaceChildren();
    this.elements.mouvementsVide.hidden = mouvements.length > 0;

    for (const mouvement of mouvements) {
      const ligne = document.createElement('li');

      const emoji = document.createElement('span');
      emoji.className = 'article__emoji';
      emoji.textContent = EMOJI_MOUVEMENT[mouvement.type] ?? '💶';

      const texte = document.createElement('div');
      texte.className = 'article__texte';
      const titre = document.createElement('div');
      titre.className = 'article__detail';
      titre.textContent = mouvement.libelle;
      const date = document.createElement('div');
      date.className = 'article__detail';
      date.textContent = new Date(mouvement.horodatage).toLocaleTimeString('fr-FR');
      texte.append(titre, date);

      const montant = document.createElement('span');
      const positif = Number(mouvement.montant) > 0;
      montant.className = `mouvement__montant mouvement__montant--${positif ? 'positif' : 'negatif'}`;
      montant.textContent = eurosSigne(mouvement.montant);

      ligne.append(emoji, texte, montant);
      liste.append(ligne);
    }
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
    }, 3600);
  }

  // ----------------------------------------------------------------- modales

  /** Ouvre le formulaire de creation et renvoie les donnees saisies, ou null. */
  demanderAnimal() {
    return new Promise((resoudre) => {
      const { modaleAnimal, formulaireAnimal } = this.elements;
      this.majResumeEspece();
      modaleAnimal.showModal();

      modaleAnimal.addEventListener('close', () => {
        if (modaleAnimal.returnValue !== 'creer') {
          resoudre(null);
          return;
        }
        const donnees = Object.fromEntries(new FormData(formulaireAnimal));
        resoudre({
          espece: donnees.espece,
          nom: String(donnees.nom).trim(),
          race: String(donnees.race).trim(),
          couleur: donnees.couleur,
          enclos: String(donnees.enclos).trim(),
          acheter: donnees.acheter === 'on',
        });
        formulaireAnimal.reset();
        this.majResumeEspece();
      }, { once: true });
    });
  }

  /** Demande la cle d'un eleveur ; renvoie null si l'utilisateur renonce. */
  demanderCle(prenom) {
    return new Promise((resoudre) => {
      const { modaleCle, formulaireCle, explicationCle } = this.elements;
      explicationCle.textContent =
        `Les actions de ${prenom} sont protégées par une clé. Colle-la pour jouer avec cet éleveur.`;
      modaleCle.showModal();

      modaleCle.addEventListener('close', () => {
        if (modaleCle.returnValue !== 'valider') {
          resoudre(null);
          return;
        }
        const donnees = Object.fromEntries(new FormData(formulaireCle));
        resoudre(String(donnees.cle).trim());
        formulaireCle.reset();
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

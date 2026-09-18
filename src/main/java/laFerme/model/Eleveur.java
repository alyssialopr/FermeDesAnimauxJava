package laFerme.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import laFerme.exception.ActionImpossibleException;
import laFerme.exception.AnimalNonPossedeException;
import laFerme.exception.FondsInsuffisantsException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * L'eleveur reste le point d'entree des actions : c'est lui qui achete, vend, nourrit,
 * soigne, promene et recolte ses animaux. Il tient aussi la caisse : chaque action a
 * un cout ou rapporte de l'argent, et son solde est persiste.
 */
@Entity
@Table(name = "eleveur")
@Getter
@Setter
@NoArgsConstructor
public class Eleveur {

    /** De quoi acheter quelques poules et une chevre pour demarrer. */
    public static final BigDecimal SOLDE_DE_DEPART = new BigDecimal("300.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String prenom;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal solde = SOLDE_DE_DEPART;

    @OneToMany(mappedBy = "eleveur", cascade = CascadeType.PERSIST)
    private List<Animal> animaux = new ArrayList<>();

    public Eleveur(String prenom) {
        this.prenom = prenom;
    }

    // ---------------------------------------------------------------------
    // Caisse
    // ---------------------------------------------------------------------

    public void debiter(BigDecimal montant) {
        if (solde.compareTo(montant) < 0) {
            throw new FondsInsuffisantsException(prenom, montant.subtract(solde), solde);
        }
        solde = solde.subtract(montant);
    }

    public void crediter(BigDecimal montant) {
        solde = solde.add(montant);
    }

    /** Solde plus la valeur de revente du troupeau : de quoi faire un classement. */
    @Transient
    public BigDecimal fortune() {
        return animaux.stream()
                .filter(animal -> animal.getEtat().estDisponible())
                .map(Animal::valeurDeRevente)
                .reduce(solde, BigDecimal::add);
    }

    // ---------------------------------------------------------------------
    // Actions
    // ---------------------------------------------------------------------

    public ResultatAction acheter(Animal animal) {
        if (animal.getEtat() == EtatAnimal.MORT || animal.getEtat() == EtatAnimal.DISPARU) {
            throw new ActionImpossibleException(
                    "Impossible d'acheter %s : l'animal est %s.".formatted(animal.designation().toLowerCase(),
                            animal.getEtat()));
        }
        Eleveur proprietaire = animal.getEleveur();
        if (proprietaire != null && !estMoi(proprietaire)) {
            throw new ActionImpossibleException(
                    "%s appartient deja a %s.".formatted(animal.designation(), proprietaire.getPrenom()));
        }

        BigDecimal prix = animal.getPrix();
        debiter(prix);

        animal.acheter();
        animal.setEleveur(this);
        if (!animaux.contains(animal)) {
            animaux.add(animal);
        }

        return ResultatAction.depense(TypeMouvement.ACHAT,
                "%s a achete %s pour %s €.".formatted(prenom, animal.designation().toLowerCase(), prix.toPlainString()),
                prix);
    }

    public ResultatAction vendre(Animal animal) {
        exigerProprietaire(animal);

        BigDecimal recette = animal.valeurDeRevente();
        String message = animal.vendre();
        animaux.remove(animal);
        animal.setEleveur(null);
        crediter(recette);

        return ResultatAction.recette(TypeMouvement.VENTE,
                "%s Elle a rapporte %s €.".formatted(message, recette.toPlainString()), recette);
    }

    public ResultatAction nourrir(Animal animal) {
        exigerProprietaire(animal);

        BigDecimal cout = animal.getEspece().getCoutRepas();
        debiter(cout);
        String message = animal.nourrir();

        return ResultatAction.depense(TypeMouvement.REPAS,
                "%s (fourrage : %s €)".formatted(message, cout.toPlainString()), cout);
    }

    public ResultatAction soigner(Animal animal) {
        exigerProprietaire(animal);

        BigDecimal cout = animal.getEspece().getCoutSoin();
        debiter(cout);
        String message = animal.soigner();

        return ResultatAction.depense(TypeMouvement.SOIN,
                "%s (veterinaire : %s €)".formatted(message, cout.toPlainString()), cout);
    }

    public ResultatAction promener(Animal animal) {
        exigerProprietaire(animal);

        String message = animal.allerEnBalade();
        BigDecimal recette = animal.getEspece().getGainParBalade();
        if (recette.signum() == 0) {
            return new ResultatAction(TypeMouvement.PROMENADE, message, BigDecimal.ZERO);
        }

        crediter(recette);
        return ResultatAction.recette(TypeMouvement.PROMENADE,
                "%s Les promenades rapportent %s €.".formatted(message, recette.toPlainString()), recette);
    }

    public ResultatAction recolter(Animal animal) {
        exigerProprietaire(animal);

        String message = animal.recolter();
        BigDecimal recette = animal.valeurRecolte();
        crediter(recette);

        return ResultatAction.recette(TypeMouvement.RECOLTE,
                "%s Vendu %s €.".formatted(message, recette.toPlainString()), recette);
    }

    public String demenager(Animal animal, String nouvelEnclos) {
        exigerProprietaire(animal);
        return animal.demenager(nouvelEnclos);
    }

    private void exigerProprietaire(Animal animal) {
        Eleveur proprietaire = animal.getEleveur();
        if (proprietaire == null || !estMoi(proprietaire)) {
            throw new AnimalNonPossedeException();
        }
    }

    private boolean estMoi(Eleveur autre) {
        return autre == this || (id != null && id.equals(autre.getId()));
    }

    @Override
    public boolean equals(Object autre) {
        if (this == autre) {
            return true;
        }
        if (!(autre instanceof Eleveur eleveur)) {
            return false;
        }
        return id != null && id.equals(eleveur.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Eleveur{id=%s, prenom=%s, solde=%s}".formatted(id, prenom, solde);
    }
}

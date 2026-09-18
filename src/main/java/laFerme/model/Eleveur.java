package laFerme.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import laFerme.exception.ActionImpossibleException;
import laFerme.exception.AnimalNonPossedeException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * L'eleveur reste le point d'entree des actions : c'est lui qui achete, vend, nourrit,
 * soigne et promene ses animaux. Les verifications de propriete d'origine sont conservees.
 */
@Entity
@Table(name = "eleveur")
@Getter
@Setter
@NoArgsConstructor
public class Eleveur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String prenom;

    @OneToMany(mappedBy = "eleveur", cascade = CascadeType.PERSIST)
    private List<Animal> animaux = new ArrayList<>();

    public Eleveur(String prenom) {
        this.prenom = prenom;
    }

    // ---------------------------------------------------------------------
    // Actions
    // ---------------------------------------------------------------------

    public Animal acheter(Animal animal) {
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
        animal.acheter();
        animal.setEleveur(this);
        if (!animaux.contains(animal)) {
            animaux.add(animal);
        }
        return animal;
    }

    public String vendre(Animal animal) {
        exigerProprietaire(animal);
        String message = animal.vendre();
        animaux.remove(animal);
        animal.setEleveur(null);
        return message;
    }

    public String nourrir(Animal animal) {
        exigerProprietaire(animal);
        return animal.nourrir();
    }

    public String soigner(Animal animal) {
        exigerProprietaire(animal);
        return animal.soigner();
    }

    public String promener(Animal animal) {
        exigerProprietaire(animal);
        return animal.allerEnBalade();
    }

    public String recolter(Animal animal) {
        exigerProprietaire(animal);
        return animal.recolter();
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
        return "Eleveur{id=%s, prenom=%s}".formatted(id, prenom);
    }
}

package laFerme.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import laFerme.exception.ActionImpossibleException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

/**
 * Socle commun a tous les animaux de la ferme.
 *
 * <p>Historiquement {@code Animal} etait une interface implementee par {@link Vache} et
 * {@link Poule}. Pour la persistance, le contrat devient une entite abstraite : les regles
 * communes (etat, proprietaire, verifications) vivent ici, les specificites d'espece restent
 * dans les sous-classes.</p>
 *
 * <p>Strategie d'heritage : une seule table {@code animal} avec un discriminant
 * {@code espece}. Les colonnes propres a une espece sont donc nullables cote base.</p>
 */
@Entity
@Table(name = "animal")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "espece", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
public abstract class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String race;

    @Column(nullable = false)
    private String couleur;

    @Column(nullable = false)
    private String enclos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EtatAnimal etat = EtatAnimal.LIBRE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eleveur_id")
    private Eleveur eleveur;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe;

    protected Animal(String nom, String race, String couleur, String enclos) {
        this.nom = nom;
        this.race = race;
        this.couleur = couleur;
        this.enclos = enclos;
        this.etat = EtatAnimal.LIBRE;
    }

    @PrePersist
    void avantEnregistrement() {
        if (creeLe == null) {
            creeLe = Instant.now();
        }
    }

    // ---------------------------------------------------------------------
    // Contrat d'espece
    // ---------------------------------------------------------------------

    public abstract Espece getEspece();

    /** Ex. : "La vache emily", "La poule nugget". */
    public abstract String designation();

    /** Production propre a l'espece (lait, oeufs...). */
    public abstract String produire();

    // ---------------------------------------------------------------------
    // Actions de la ferme
    // ---------------------------------------------------------------------

    public void acheter() {
        this.etat = EtatAnimal.LIBRE;
    }

    public String vendre() {
        exigerDisponible("vendre");
        this.etat = EtatAnimal.VENDU;
        return designation() + " a ete vendue.";
    }

    public String nourrir() {
        exigerDisponible("nourrir");
        return designation() + " a ete nourrie.";
    }

    public String soigner() {
        exigerDisponible("soigner");
        return designation() + " a ete soignee.";
    }

    public String allerEnBalade() {
        exigerDisponible("promener");
        return designation() + " part en balade.";
    }

    public String recolter() {
        exigerDisponible("recolter");
        return produire();
    }

    public String demenager(String nouvelEnclos) {
        exigerDisponible("changer d'enclos");
        this.enclos = nouvelEnclos;
        return designation() + " a rejoint l'enclos " + nouvelEnclos + ".";
    }

    /**
     * Toutes les actions supposent un animal encore present a la ferme.
     */
    protected void exigerDisponible(String action) {
        if (!etat.estDisponible()) {
            throw new ActionImpossibleException(
                    "Impossible de %s %s : l'animal est %s.".formatted(action, designation().toLowerCase(), etat));
        }
    }

    @Override
    public boolean equals(Object autre) {
        if (this == autre) {
            return true;
        }
        if (!(autre instanceof Animal animal)) {
            return false;
        }
        return id != null && id.equals(animal.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "%s{id=%s, nom=%s, etat=%s}".formatted(getClass().getSimpleName(), id, nom, etat);
    }
}

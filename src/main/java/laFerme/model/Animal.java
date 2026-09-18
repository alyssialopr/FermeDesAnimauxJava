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
import jakarta.persistence.Transient;
import laFerme.exception.ActionImpossibleException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Socle commun a tous les animaux de la ferme.
 *
 * <p>Historiquement {@code Animal} etait une interface implementee par {@link Vache} et
 * {@link Poule}. Pour la persistance, le contrat devient une entite abstraite : les regles
 * communes (etat, proprietaire, faim, sante, production) vivent ici, et chaque sous-classe
 * ne porte plus que son espece.</p>
 *
 * <p>Strategie d'heritage : une seule table {@code animal} avec un discriminant
 * {@code espece}. Les tarifs et rythmes sont portes par l'enum {@link Espece}.</p>
 */
@Entity
@Table(name = "animal")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "espece", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
public abstract class Animal {

    /** Au-dela, l'animal a trop faim pour produire. */
    public static final int FAIM_MAXI_POUR_PRODUIRE = 70;

    /** En dessous, l'animal doit etre soigne avant d'etre recolte. */
    public static final int SANTE_MINI_POUR_PRODUIRE = 30;

    private static final int USURE_PAR_RECOLTE = 8;
    private static final int BONUS_BALADE = 5;
    private static final BigDecimal DECOTE_REVENTE = new BigDecimal("0.90");

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

    /** Valeur marchande : prix d'achat, et base du prix de revente. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prix;

    /** Ce que rend une recolte : litres, oeufs, kg… selon l'espece. */
    @Column(name = "quantite_production", nullable = false)
    private int quantiteProduction;

    @Column(nullable = false)
    private int sante = 100;

    @Column(name = "derniere_nourriture")
    private Instant derniereNourriture;

    @Column(name = "derniere_recolte")
    private Instant derniereRecolte;

    @Column(name = "derniere_balade")
    private Instant derniereBalade;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe;

    protected Animal(String nom, String race, String couleur, String enclos) {
        this.nom = nom;
        this.race = race;
        this.couleur = couleur;
        this.enclos = enclos;
        this.etat = EtatAnimal.LIBRE;
        this.prix = getEspece().getPrix();
        this.quantiteProduction = getEspece().getQuantiteParDefaut();
        this.derniereNourriture = Instant.now();
    }

    @PrePersist
    void avantEnregistrement() {
        if (creeLe == null) {
            creeLe = Instant.now();
        }
        if (prix == null) {
            prix = getEspece().getPrix();
        }
    }

    // ---------------------------------------------------------------------
    // Contrat d'espece
    // ---------------------------------------------------------------------

    public abstract Espece getEspece();

    /** Ex. : "La vache emily", "Le cochon babe". */
    public String designation() {
        return getEspece().getDesignation() + " " + nom;
    }

    // ---------------------------------------------------------------------
    // Etat vital, deduit du temps ecoule
    // ---------------------------------------------------------------------

    /** 0 = repu, 100 = affame. Monte tout seul avec le temps. */
    @Transient
    public int getFaim() {
        if (derniereNourriture == null) {
            return 100;
        }
        long minutes = Duration.between(derniereNourriture, Instant.now()).toMinutes();
        return (int) Math.max(0, Math.min(100, minutes * getEspece().getFaimParMinute()));
    }

    @Transient
    public boolean estAffame() {
        return getFaim() > FAIM_MAXI_POUR_PRODUIRE;
    }

    /** Secondes restantes avant la prochaine recolte (ou promenade payante). */
    @Transient
    public long getSecondesAvantRecolte() {
        return secondesAvant(derniereRecolte);
    }

    @Transient
    public long getSecondesAvantBalade() {
        return secondesAvant(derniereBalade);
    }

    private long secondesAvant(Instant derniereFois) {
        if (derniereFois == null) {
            return 0;
        }
        long restant = getEspece().getDelai().minus(Duration.between(derniereFois, Instant.now())).toSeconds();
        return Math.max(0, restant);
    }

    /** Vrai si une recolte est possible tout de suite. */
    @Transient
    public boolean peutEtreRecolte() {
        return getEspece().estRecoltable()
                && etat.estDisponible()
                && !estAffame()
                && sante >= SANTE_MINI_POUR_PRODUIRE
                && getSecondesAvantRecolte() == 0;
    }

    /** Ce que rapporte une recolte au prix du marche. */
    @Transient
    public BigDecimal valeurRecolte() {
        return getEspece().getPrixUnitaire()
                .multiply(BigDecimal.valueOf(quantiteProduction))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Prix auquel la ferme rachete l'animal : sa valeur, moins la decote de revente. */
    @Transient
    public BigDecimal valeurDeRevente() {
        return prix.multiply(DECOTE_REVENTE).setScale(2, RoundingMode.HALF_UP);
    }

    // ---------------------------------------------------------------------
    // Actions de la ferme
    // ---------------------------------------------------------------------

    public void acheter() {
        this.etat = EtatAnimal.LIBRE;
        if (derniereNourriture == null) {
            derniereNourriture = Instant.now();
        }
    }

    public String vendre() {
        exigerDisponible("vendre");
        this.etat = EtatAnimal.VENDU;
        return designation() + " a ete vendue.";
    }

    public String nourrir() {
        exigerDisponible("nourrir");
        if (getFaim() == 0) {
            throw new ActionImpossibleException("%s n'a pas faim pour le moment.".formatted(designation()));
        }
        derniereNourriture = Instant.now();

        BigDecimal engraissement = getEspece().getGainParRepas();
        if (engraissement.signum() > 0) {
            prix = prix.add(engraissement);
            return "%s a ete nourri et prend de la valeur (%s €).".formatted(designation(), prix.toPlainString());
        }
        return designation() + " a ete nourrie.";
    }

    public String soigner() {
        exigerDisponible("soigner");
        if (sante >= 100) {
            throw new ActionImpossibleException("%s est deja en pleine forme.".formatted(designation()));
        }
        sante = 100;
        return designation() + " a ete soignee.";
    }

    public String allerEnBalade() {
        exigerDisponible("promener");
        long attente = getSecondesAvantBalade();
        if (attente > 0) {
            throw new ActionImpossibleException(
                    "%s se repose encore : revenez dans %s.".formatted(designation(), enMinutes(attente)));
        }
        derniereBalade = Instant.now();
        sante = Math.min(100, sante + BONUS_BALADE);
        return designation() + " part en balade.";
    }

    /**
     * Recolte la production. Suppose que l'animal est en etat de produire, ce que
     * verifient les regles ci-dessous.
     */
    public String recolter() {
        exigerDisponible("recolter");

        if (!getEspece().estRecoltable()) {
            throw new ActionImpossibleException(
                    "%s ne se recolte pas.".formatted(designation()));
        }
        if (estAffame()) {
            throw new ActionImpossibleException(
                    "%s a bien trop faim pour produire : nourrissez-la d'abord.".formatted(designation()));
        }
        if (sante < SANTE_MINI_POUR_PRODUIRE) {
            throw new ActionImpossibleException(
                    "%s est trop fatiguee (sante %d %%) : elle a besoin de soins.".formatted(designation(), sante));
        }
        long attente = getSecondesAvantRecolte();
        if (attente > 0) {
            throw new ActionImpossibleException(
                    "%s a deja ete recoltee : revenez dans %s.".formatted(designation(), enMinutes(attente)));
        }

        derniereRecolte = Instant.now();
        sante = Math.max(0, sante - USURE_PAR_RECOLTE);

        return "%s a donne %d %s.".formatted(designation(), quantiteProduction, getEspece().getProduction().getUnite());
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

    private static String enMinutes(long secondes) {
        long minutes = secondes / 60;
        return minutes > 0 ? minutes + " min " + (secondes % 60) + " s" : secondes + " s";
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

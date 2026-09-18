package laFerme.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Les especes elevees a la ferme, avec tout ce qui les differencie : ce qu'elles
 * produisent, ce qu'elles coutent et a quel rythme elles ont faim. Sert aussi de
 * discriminant JPA sur la table animal.
 *
 * <p>La ferme tourne sur une horloge acceleree : les delais sont en minutes, pas
 * en heures, pour qu'une partie soit jouable en quelques minutes.</p>
 *
 * <pre>
 * espece   production        qte  prix/u  prix    repas  soin   faim/min  delai
 * VACHE    lait               18   1.50    230     4.00  20.00     6       3 min
 * CHEVRE   lait                4   1.80     60     1.50  12.00     8       3 min
 * MOUTON   laine               3   6.00    120     2.00  14.00     5       6 min
 * POULE    oeufs               5   0.60     26     0.40   5.00    10       2 min
 * CANARD   oeufs               4   0.75     28     0.50   6.00    10       2 min
 * OIE      duvet             120   0.15    120     1.00   8.00     7       6 min
 * LAPIN    lapereaux           2  12.00     70     0.50   5.00    12       6 min
 * COCHON   aucune (engraisse)  -      -    110     3.00  18.00     8       3 min
 * CHEVAL   aucune (balades)    -      -    500     6.00  40.00     5       3 min
 * </pre>
 */
@Schema(name = "Espece", description = """
        Espece de l'animal. Elle determine sa production, ses tarifs et la vitesse \
        a laquelle il a faim. Le catalogue complet est expose par `GET /api/especes`.""")
public enum Espece {

    VACHE("La vache", Production.LAIT, 18, "1.50", "230.00", "4.00", "20.00", 6, 3, "0", "0"),
    CHEVRE("La chevre", Production.LAIT, 4, "1.80", "60.00", "1.50", "12.00", 8, 3, "0", "0"),
    MOUTON("Le mouton", Production.LAINE, 3, "6.00", "120.00", "2.00", "14.00", 5, 6, "0", "0"),
    POULE("La poule", Production.OEUFS, 5, "0.60", "26.00", "0.40", "5.00", 10, 2, "0", "0"),
    CANARD("Le canard", Production.OEUFS, 4, "0.75", "28.00", "0.50", "6.00", 10, 2, "0", "0"),
    OIE("L'oie", Production.DUVET, 120, "0.15", "120.00", "1.00", "8.00", 7, 6, "0", "0"),
    LAPIN("Le lapin", Production.LAPEREAUX, 2, "12.00", "70.00", "0.50", "5.00", 12, 6, "0", "0"),
    /** Ne se recolte pas : chaque repas l'engraisse et augmente sa valeur a la revente. */
    COCHON("Le cochon", Production.AUCUNE, 0, "0", "110.00", "3.00", "18.00", 8, 3, "9.00", "0"),
    /** Ne se recolte pas : les promenades sont payantes. */
    CHEVAL("Le cheval", Production.AUCUNE, 0, "0", "500.00", "6.00", "40.00", 5, 3, "0", "45.00");

    private final String designation;
    private final Production production;
    private final int quantiteParDefaut;
    private final BigDecimal prixUnitaire;
    private final BigDecimal prix;
    private final BigDecimal coutRepas;
    private final BigDecimal coutSoin;
    private final int faimParMinute;
    private final Duration delai;
    private final BigDecimal gainParRepas;
    private final BigDecimal gainParBalade;

    Espece(String designation, Production production, int quantiteParDefaut, String prixUnitaire, String prix,
           String coutRepas, String coutSoin, int faimParMinute, int delaiMinutes,
           String gainParRepas, String gainParBalade) {
        this.designation = designation;
        this.production = production;
        this.quantiteParDefaut = quantiteParDefaut;
        this.prixUnitaire = new BigDecimal(prixUnitaire);
        this.prix = new BigDecimal(prix);
        this.coutRepas = new BigDecimal(coutRepas);
        this.coutSoin = new BigDecimal(coutSoin);
        this.faimParMinute = faimParMinute;
        this.delai = Duration.ofMinutes(delaiMinutes);
        this.gainParRepas = new BigDecimal(gainParRepas);
        this.gainParBalade = new BigDecimal(gainParBalade);
    }

    /** Ex. : "La vache", "Le cochon". */
    public String getDesignation() {
        return designation;
    }

    public Production getProduction() {
        return production;
    }

    public int getQuantiteParDefaut() {
        return quantiteParDefaut;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public BigDecimal getPrix() {
        return prix;
    }

    public BigDecimal getCoutRepas() {
        return coutRepas;
    }

    public BigDecimal getCoutSoin() {
        return coutSoin;
    }

    /** Points de faim gagnes par minute : 100 = affame. */
    public int getFaimParMinute() {
        return faimParMinute;
    }

    /** Delai entre deux recoltes, ou entre deux promenades payantes. */
    public Duration getDelai() {
        return delai;
    }

    /** Valeur ajoutee a l'animal a chaque repas (engraissement du cochon). */
    public BigDecimal getGainParRepas() {
        return gainParRepas;
    }

    /** Recette d'une promenade (cheval). */
    public BigDecimal getGainParBalade() {
        return gainParBalade;
    }

    public boolean estRecoltable() {
        return production.estRecoltable();
    }
}

package laFerme.model;

import laFerme.exception.ActionImpossibleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regles portees par l'animal lui-meme, sans Spring ni base de donnees.
 */
class AnimalTest {

    private Vache vache() {
        return new Vache("emily", "Highland", "marron", "1");
    }

    private Poule poule() {
        return new Poule("nugget", "suisse", "orange", "3");
    }

    /** Fait comme si le dernier repas remontait a tant de minutes. */
    private <T extends Animal> T affameDepuis(T animal, int minutes) {
        animal.setDerniereNourriture(Instant.now().minus(Duration.ofMinutes(minutes)));
        return animal;
    }

    @Test
    @DisplayName("un animal arrive libre, repu et au prix de son espece")
    void etatInitial() {
        Vache emily = vache();

        assertThat(emily.getEtat()).isEqualTo(EtatAnimal.LIBRE);
        assertThat(emily.getFaim()).isZero();
        assertThat(emily.getSante()).isEqualTo(100);
        assertThat(emily.getPrix()).isEqualByComparingTo(Espece.VACHE.getPrix());
        assertThat(emily.getQuantiteProduction()).isEqualTo(Espece.VACHE.getQuantiteParDefaut());
    }

    @Test
    @DisplayName("chaque espece a sa designation et sa production")
    void especes() {
        assertThat(vache().designation()).isEqualTo("La vache emily");
        assertThat(poule().designation()).isEqualTo("La poule nugget");
        assertThat(new Cochon("babe", "Large White", "rousse", "6").designation()).isEqualTo("Le cochon babe");

        assertThat(vache().getEspece().getProduction()).isEqualTo(Production.LAIT);
        assertThat(poule().getEspece().getProduction()).isEqualTo(Production.OEUFS);
        assertThat(new Mouton("nuage", "Merinos", "blanche", "5").getEspece().getProduction())
                .isEqualTo(Production.LAINE);
        assertThat(new Cheval("tonnerre", "Comtois", "marron", "6").getEspece().getProduction())
                .isEqualTo(Production.AUCUNE);
    }

    @Nested
    @DisplayName("faim")
    class Faim {

        @Test
        @DisplayName("la faim monte toute seule avec le temps")
        void laFaimMonte() {
            assertThat(affameDepuis(vache(), 5).getFaim()).isEqualTo(30);
            assertThat(affameDepuis(vache(), 20).getFaim()).isEqualTo(100);
            // La poule a plus souvent faim que la vache.
            assertThat(affameDepuis(poule(), 5).getFaim()).isEqualTo(50);
        }

        @Test
        @DisplayName("nourrir remet la faim a zero")
        void nourrirRassasie() {
            Vache emily = affameDepuis(vache(), 15);

            assertThat(emily.nourrir()).isEqualTo("La vache emily a ete nourrie.");
            assertThat(emily.getFaim()).isZero();
        }

        @Test
        @DisplayName("on ne nourrit pas un animal qui n'a pas faim")
        void pasDeGaspillage() {
            assertThatThrownBy(() -> vache().nourrir())
                    .isInstanceOf(ActionImpossibleException.class)
                    .hasMessageContaining("n'a pas faim");
        }
    }

    @Nested
    @DisplayName("recolte")
    class Recolte {

        @Test
        @DisplayName("la recolte rend la production de l'espece et fatigue un peu l'animal")
        void recolte() {
            Vache emily = affameDepuis(vache(), 3);

            assertThat(emily.peutEtreRecolte()).isTrue();
            assertThat(emily.recolter()).isEqualTo("La vache emily a donne 18 litres de lait.");
            assertThat(emily.getSante()).isEqualTo(92);
            assertThat(emily.valeurRecolte()).isEqualByComparingTo("27.00");
        }

        @Test
        @DisplayName("une seule recolte par periode")
        void delaiEntreDeuxRecoltes() {
            Vache emily = affameDepuis(vache(), 3);
            emily.recolter();

            assertThat(emily.peutEtreRecolte()).isFalse();
            assertThat(emily.getSecondesAvantRecolte()).isPositive();
            assertThatThrownBy(emily::recolter)
                    .isInstanceOf(ActionImpossibleException.class)
                    .hasMessageContaining("revenez dans");
        }

        @Test
        @DisplayName("un animal affame ne produit pas")
        void affameNeProduitPas() {
            Vache emily = affameDepuis(vache(), 20);

            assertThat(emily.estAffame()).isTrue();
            assertThat(emily.peutEtreRecolte()).isFalse();
            assertThatThrownBy(emily::recolter)
                    .isInstanceOf(ActionImpossibleException.class)
                    .hasMessageContaining("trop faim");
        }

        @Test
        @DisplayName("un animal epuise doit etre soigne avant d'etre recolte")
        void santeTropBasse() {
            Vache emily = vache();
            emily.setSante(20);

            assertThatThrownBy(emily::recolter)
                    .isInstanceOf(ActionImpossibleException.class)
                    .hasMessageContaining("besoin de soins");
        }

        @Test
        @DisplayName("le cochon et le cheval ne se recoltent pas")
        void especesSansRecolte() {
            Cochon babe = new Cochon("babe", "Large White", "rousse", "6");

            assertThat(babe.peutEtreRecolte()).isFalse();
            assertThatThrownBy(babe::recolter)
                    .isInstanceOf(ActionImpossibleException.class)
                    .hasMessageContaining("ne se recolte pas");
        }
    }

    @Nested
    @DisplayName("soins et balades")
    class SoinsEtBalades {

        @Test
        @DisplayName("soigner remet la sante a 100")
        void soigner() {
            Vache emily = vache();
            emily.setSante(40);

            assertThat(emily.soigner()).isEqualTo("La vache emily a ete soignee.");
            assertThat(emily.getSante()).isEqualTo(100);
        }

        @Test
        @DisplayName("on n'appelle pas le veterinaire pour rien")
        void soignerEnPleineForme() {
            assertThatThrownBy(() -> vache().soigner())
                    .isInstanceOf(ActionImpossibleException.class)
                    .hasMessageContaining("en pleine forme");
        }

        @Test
        @DisplayName("la balade fait du bien, mais pas en boucle")
        void balade() {
            Vache emily = vache();
            emily.setSante(80);

            assertThat(emily.allerEnBalade()).isEqualTo("La vache emily part en balade.");
            assertThat(emily.getSante()).isEqualTo(85);
            assertThatThrownBy(emily::allerEnBalade)
                    .isInstanceOf(ActionImpossibleException.class)
                    .hasMessageContaining("se repose encore");
        }
    }

    @Nested
    @DisplayName("valeur et sortie du cheptel")
    class ValeurEtSortie {

        @Test
        @DisplayName("chaque repas engraisse le cochon et augmente sa valeur")
        void engraissement() {
            Cochon babe = affameDepuis(new Cochon("babe", "Large White", "rousse", "6"), 10);
            BigDecimal avant = babe.getPrix();

            assertThat(babe.nourrir()).contains("prend de la valeur");
            assertThat(babe.getPrix()).isEqualByComparingTo(avant.add(Espece.COCHON.getGainParRepas()));
        }

        @Test
        @DisplayName("la ferme rachete l'animal avec 10 % de decote")
        void decoteDeRevente() {
            assertThat(vache().valeurDeRevente()).isEqualByComparingTo("207.00");
        }

        @Test
        @DisplayName("la vente bascule l'animal en VENDU")
        void vente() {
            Vache emily = vache();

            assertThat(emily.vendre()).isEqualTo("La vache emily a ete vendue.");
            assertThat(emily.getEtat()).isEqualTo(EtatAnimal.VENDU);
        }

        @Test
        @DisplayName("un animal vendu, mort ou disparu n'accepte plus aucune action")
        void plusAucuneActionSiIndisponible() {
            Vache vendue = vache();
            vendue.vendre();

            assertThatThrownBy(vendue::nourrir)
                    .isInstanceOf(ActionImpossibleException.class)
                    .hasMessageContaining("VENDU");
            assertThatThrownBy(vendue::soigner).isInstanceOf(ActionImpossibleException.class);
            assertThatThrownBy(vendue::allerEnBalade).isInstanceOf(ActionImpossibleException.class);
            assertThatThrownBy(vendue::recolter).isInstanceOf(ActionImpossibleException.class);
            assertThatThrownBy(vendue::vendre).isInstanceOf(ActionImpossibleException.class);

            Poule morte = poule();
            morte.setEtat(EtatAnimal.MORT);
            assertThatThrownBy(morte::soigner)
                    .isInstanceOf(ActionImpossibleException.class)
                    .hasMessageContaining("MORT");
        }

        @Test
        @DisplayName("le demenagement change l'enclos")
        void demenagement() {
            Poule nugget = poule();

            assertThat(nugget.demenager("7")).isEqualTo("La poule nugget a rejoint l'enclos 7.");
            assertThat(nugget.getEnclos()).isEqualTo("7");
        }
    }
}

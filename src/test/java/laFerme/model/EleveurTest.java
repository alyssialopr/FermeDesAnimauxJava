package laFerme.model;

import laFerme.exception.ActionImpossibleException;
import laFerme.exception.AnimalNonPossedeException;
import laFerme.exception.FondsInsuffisantsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regles de propriete et de caisse de l'eleveur.
 */
class EleveurTest {

    private Eleveur alyssia;
    private Eleveur killian;
    private Poule nugget;

    @BeforeEach
    void preparerLaFerme() {
        alyssia = new Eleveur("alyssia");
        killian = new Eleveur("killian");
        nugget = new Poule("nugget", "suisse", "orange", "3");
    }

    private <T extends Animal> T affameDepuis(T animal, int minutes) {
        animal.setDerniereNourriture(Instant.now().minus(Duration.ofMinutes(minutes)));
        return animal;
    }

    @Nested
    @DisplayName("achat et vente")
    class AchatEtVente {

        @Test
        @DisplayName("acheter rattache l'animal et debite le prix")
        void achat() {
            var resultat = alyssia.acheter(nugget);

            assertThat(alyssia.getAnimaux()).containsExactly(nugget);
            assertThat(nugget.getEleveur()).isSameAs(alyssia);
            assertThat(alyssia.getSolde())
                    .isEqualByComparingTo(Eleveur.SOLDE_DE_DEPART.subtract(Espece.POULE.getPrix()));
            assertThat(resultat.type()).isEqualTo(TypeMouvement.ACHAT);
            assertThat(resultat.montant()).isEqualByComparingTo(Espece.POULE.getPrix().negate());
            assertThat(resultat.message()).contains("a achete la poule nugget pour 26.00 €");
        }

        @Test
        @DisplayName("on n'achete pas au-dessus de ses moyens")
        void fondsInsuffisants() {
            var cheval = new Cheval("tonnerre", "Comtois", "marron", "6");

            assertThatThrownBy(() -> alyssia.acheter(cheval))
                    .isInstanceOf(FondsInsuffisantsException.class)
                    .hasMessageContaining("il manque 200.00 €");
            assertThat(alyssia.getAnimaux()).isEmpty();
            assertThat(alyssia.getSolde()).isEqualByComparingTo(Eleveur.SOLDE_DE_DEPART);
        }

        @Test
        @DisplayName("acheter deux fois le meme animal ne le duplique pas et ne repaie pas")
        void achatIdempotent() {
            alyssia.acheter(nugget);
            BigDecimal apresPremierAchat = alyssia.getSolde();

            alyssia.acheter(nugget);

            assertThat(alyssia.getAnimaux()).hasSize(1);
            assertThat(alyssia.getSolde())
                    .isEqualByComparingTo(apresPremierAchat.subtract(Espece.POULE.getPrix()));
        }

        @Test
        @DisplayName("on ne rachete pas l'animal d'un autre eleveur")
        void achatImpossibleSiDejaPossede() {
            alyssia.acheter(nugget);

            assertThatThrownBy(() -> killian.acheter(nugget))
                    .isInstanceOf(ActionImpossibleException.class)
                    .hasMessageContaining("appartient deja a alyssia");
        }

        @Test
        @DisplayName("on n'achete pas un animal mort ou disparu")
        void achatImpossibleSiDisparu() {
            nugget.setEtat(EtatAnimal.DISPARU);

            assertThatThrownBy(() -> alyssia.acheter(nugget))
                    .isInstanceOf(ActionImpossibleException.class)
                    .hasMessageContaining("DISPARU");
        }

        @Test
        @DisplayName("la vente sort l'animal du troupeau et credite la decote")
        void vente() {
            alyssia.acheter(nugget);
            BigDecimal avant = alyssia.getSolde();

            var resultat = alyssia.vendre(nugget);

            assertThat(alyssia.getAnimaux()).isEmpty();
            assertThat(nugget.getEleveur()).isNull();
            assertThat(nugget.getEtat()).isEqualTo(EtatAnimal.VENDU);
            assertThat(alyssia.getSolde()).isEqualByComparingTo(avant.add(new BigDecimal("23.40")));
            assertThat(resultat.type()).isEqualTo(TypeMouvement.VENTE);
            assertThat(resultat.montant()).isEqualByComparingTo("23.40");
        }
    }

    @Nested
    @DisplayName("regle de propriete")
    class Propriete {

        @Test
        @DisplayName("on ne touche pas aux animaux des autres")
        void animauxDesAutres() {
            alyssia.acheter(nugget);

            assertThatThrownBy(() -> killian.nourrir(nugget))
                    .isInstanceOf(AnimalNonPossedeException.class)
                    .hasMessage("Cet animal ne vous appartient pas");
            assertThatThrownBy(() -> killian.soigner(nugget)).isInstanceOf(AnimalNonPossedeException.class);
            assertThatThrownBy(() -> killian.promener(nugget)).isInstanceOf(AnimalNonPossedeException.class);
            assertThatThrownBy(() -> killian.vendre(nugget)).isInstanceOf(AnimalNonPossedeException.class);
            assertThatThrownBy(() -> killian.recolter(nugget)).isInstanceOf(AnimalNonPossedeException.class);
            assertThat(killian.getSolde()).isEqualByComparingTo(Eleveur.SOLDE_DE_DEPART);
        }

        @Test
        @DisplayName("un animal sans proprietaire ne peut pas etre nourri")
        void animalSansProprietaire() {
            assertThatThrownBy(() -> alyssia.nourrir(nugget)).isInstanceOf(AnimalNonPossedeException.class);
        }
    }

    @Nested
    @DisplayName("exploitation quotidienne")
    class Exploitation {

        @Test
        @DisplayName("nourrir coute le prix du fourrage")
        void repas() {
            alyssia.acheter(nugget);
            affameDepuis(nugget, 5);
            BigDecimal avant = alyssia.getSolde();

            var resultat = alyssia.nourrir(nugget);

            assertThat(alyssia.getSolde()).isEqualByComparingTo(avant.subtract(Espece.POULE.getCoutRepas()));
            assertThat(resultat.type()).isEqualTo(TypeMouvement.REPAS);
            assertThat(resultat.message()).contains("fourrage : 0.40 €");
        }

        @Test
        @DisplayName("soigner coute le prix du veterinaire")
        void soin() {
            alyssia.acheter(nugget);
            nugget.setSante(50);
            BigDecimal avant = alyssia.getSolde();

            var resultat = alyssia.soigner(nugget);

            assertThat(alyssia.getSolde()).isEqualByComparingTo(avant.subtract(Espece.POULE.getCoutSoin()));
            assertThat(resultat.type()).isEqualTo(TypeMouvement.SOIN);
        }

        @Test
        @DisplayName("la recolte rapporte la production au prix du marche")
        void recolte() {
            alyssia.acheter(nugget);
            BigDecimal avant = alyssia.getSolde();

            var resultat = alyssia.recolter(nugget);

            // 5 oeufs a 0.60 €
            assertThat(alyssia.getSolde()).isEqualByComparingTo(avant.add(new BigDecimal("3.00")));
            assertThat(resultat.type()).isEqualTo(TypeMouvement.RECOLTE);
            assertThat(resultat.message()).contains("a donne 5 oeufs").contains("Vendu 3.00 €");
        }

        @Test
        @DisplayName("promener une poule ne rapporte rien, promener un cheval si")
        void promenades() {
            alyssia.acheter(nugget);
            var sansRecette = alyssia.promener(nugget);
            assertThat(sansRecette.montant()).isEqualByComparingTo("0");

            var cheval = new Cheval("tonnerre", "Comtois", "marron", "6");
            killian.crediter(new BigDecimal("500.00"));
            killian.acheter(cheval);
            BigDecimal avant = killian.getSolde();

            var recette = killian.promener(cheval);

            assertThat(killian.getSolde()).isEqualByComparingTo(avant.add(Espece.CHEVAL.getGainParBalade()));
            assertThat(recette.type()).isEqualTo(TypeMouvement.PROMENADE);
            assertThat(recette.message()).contains("Les promenades rapportent 45.00 €");
        }

        @Test
        @DisplayName("la fortune additionne la caisse et la valeur du troupeau")
        void fortune() {
            alyssia.acheter(nugget);

            // 300 - 26 d'achat, plus la valeur de revente de la poule (23.40)
            assertThat(alyssia.fortune()).isEqualByComparingTo("297.40");
        }
    }
}

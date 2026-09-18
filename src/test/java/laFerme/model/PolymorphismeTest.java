package laFerme.model;

import laFerme.exception.ActionImpossibleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La ferme manipule des {@link Animal}, jamais des especes concretes : c'est la
 * sous-classe qui decide de son cri, de la facon d'annoncer sa recolte et de ce
 * que lui font un repas ou une balade.
 */
class PolymorphismeTest {

    /** Un representant de chaque espece, vu comme un simple Animal. */
    private static List<Animal> laFerme() {
        return List.of(
                new Vache("emily", "Highland", "marron", "1"),
                new Chevre("biquette", "Alpine", "marron", "1"),
                new Mouton("nuage", "Merinos", "blanche", "1"),
                new Poule("nugget", "Sussex", "orange", "2"),
                new Canard("colvert", "Rouen", "grise", "2"),
                new Oie("blanchette", "Toulouse", "blanche", "2"),
                new Lapin("pompon", "Angora", "grise", "3"),
                new Cochon("truffe", "Large White", "rousse", "3"),
                new Cheval("tonnerre", "Comtois", "marron", "4"));
    }

    @Test
    @DisplayName("chaque espece a son cri, sans que l'appelant connaisse sa classe")
    void chaqueEspeceALeSien() {
        Map<Espece, String> cris = laFerme().stream()
                .collect(java.util.stream.Collectors.toMap(Animal::getEspece, Animal::cri));

        assertThat(cris).containsExactlyInAnyOrderEntriesOf(Map.of(
                Espece.VACHE, "Meuh !",
                Espece.CHEVRE, "Mêê !",
                Espece.MOUTON, "Bêê !",
                Espece.POULE, "Cot cot codec !",
                Espece.CANARD, "Coin coin !",
                Espece.OIE, "Couac !",
                Espece.LAPIN, "Couic !",
                Espece.COCHON, "Groin groin !",
                Espece.CHEVAL, "Hiiiii !"));
    }

    @Test
    @DisplayName("la recolte se dit differemment selon l'espece")
    void laRecolteSeDitAutrement() {
        assertThat(recolter(new Vache("emily", "Highland", "marron", "1")))
                .isEqualTo("La vache emily a donne 18 litres de lait.");
        assertThat(recolter(new Poule("nugget", "Sussex", "orange", "2")))
                .isEqualTo("La poule nugget a pondu 5 oeufs.");
        assertThat(recolter(new Mouton("nuage", "Merinos", "blanche", "1")))
                .isEqualTo("Le mouton nuage a ete tondu : 3 kg de laine.");
        assertThat(recolter(new Oie("blanchette", "Toulouse", "blanche", "2")))
                .isEqualTo("L'oie blanchette a fourni 120 g de duvet.");
        assertThat(recolter(new Lapin("pompon", "Angora", "grise", "3")))
                .isEqualTo("Le lapin pompon a eu une portee de 2 lapereaux.");
    }

    @Test
    @DisplayName("les especes qui ne se recoltent pas gardent le refus par defaut")
    void refusParDefaut() {
        for (Animal animal : List.of(
                new Cochon("truffe", "Large White", "rousse", "3"),
                new Cheval("tonnerre", "Comtois", "marron", "4"))) {

            assertThat(animal.peutEtreRecolte()).isFalse();
            assertThatThrownBy(animal::recolter)
                    .isInstanceOf(ActionImpossibleException.class)
                    .hasMessageContaining("ne se recolte pas");
        }
    }

    @Test
    @DisplayName("le meme appel nourrir() n'a pas le meme effet selon l'espece")
    void memeAppelEffetsDifferents() {
        Animal vache = affamee(new Vache("emily", "Highland", "marron", "1"));
        Animal cochon = affamee(new Cochon("truffe", "Large White", "rousse", "3"));

        BigDecimal prixCochonAvant = cochon.getPrix();
        BigDecimal prixVacheAvant = vache.getPrix();

        assertThat(vache.nourrir()).isEqualTo("La vache emily a ete nourrie.");
        assertThat(cochon.nourrir()).contains("il vaut maintenant");

        assertThat(vache.getPrix()).isEqualByComparingTo(prixVacheAvant);
        assertThat(cochon.getPrix()).isEqualByComparingTo(prixCochonAvant.add(new BigDecimal("9.00")));
    }

    @Test
    @DisplayName("seule la balade du cheval rapporte de l'argent")
    void seulLeChevalFaitPayerSesBalades() {
        for (Animal animal : laFerme()) {
            BigDecimal attendu = animal.getEspece() == Espece.CHEVAL
                    ? new BigDecimal("45.00")
                    : BigDecimal.ZERO;

            assertThat(animal.recetteDeLaBalade())
                    .as("recette de balade de %s", animal.getEspece())
                    .isEqualByComparingTo(attendu);
        }

        assertThat(new Cheval("tonnerre", "Comtois", "marron", "4").allerEnBalade())
                .isEqualTo("Le cheval tonnerre emmene des promeneurs en foret.");
    }

    @Test
    @DisplayName("toutes les especes repondent au meme contrat")
    void memeContratPourTous() {
        for (Animal animal : laFerme()) {
            assertThat(animal.cri()).isNotBlank();
            assertThat(animal.designation()).contains(animal.getNom());
            assertThat(animal.getPrix()).isEqualByComparingTo(animal.getEspece().getPrix());
            assertThat(animal.valeurDeRevente()).isLessThan(animal.getPrix());
            assertThat(animal.getEtat()).isEqualTo(EtatAnimal.LIBRE);
        }
    }

    private String recolter(Animal animal) {
        return animal.recolter();
    }

    private <T extends Animal> T affamee(T animal) {
        animal.setDerniereNourriture(Instant.now().minus(Duration.ofMinutes(10)));
        return animal;
    }
}

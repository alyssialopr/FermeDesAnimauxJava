package laFerme.model;

import laFerme.exception.ActionImpossibleException;
import laFerme.exception.AnimalNonPossedeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Les regles de propriete de l'eleveur, reprises de la version console.
 */
class EleveurTest {

    private Eleveur alyssia;
    private Eleveur killian;
    private Vache emily;

    @BeforeEach
    void preparerLaFerme() {
        alyssia = new Eleveur("alyssia");
        killian = new Eleveur("killian");
        emily = new Vache("emily", "Highland", "marron", "1");
    }

    @Test
    @DisplayName("acheter rattache l'animal a l'eleveur")
    void achat() {
        alyssia.acheter(emily);

        assertThat(alyssia.getAnimaux()).containsExactly(emily);
        assertThat(emily.getEleveur()).isSameAs(alyssia);
        assertThat(emily.getEtat()).isEqualTo(EtatAnimal.LIBRE);
    }

    @Test
    @DisplayName("acheter deux fois le meme animal ne le duplique pas")
    void achatIdempotent() {
        alyssia.acheter(emily);
        alyssia.acheter(emily);

        assertThat(alyssia.getAnimaux()).hasSize(1);
    }

    @Test
    @DisplayName("on ne touche pas aux animaux des autres")
    void animauxDesAutres() {
        alyssia.acheter(emily);

        assertThatThrownBy(() -> killian.nourrir(emily))
                .isInstanceOf(AnimalNonPossedeException.class)
                .hasMessage("Cet animal ne vous appartient pas");
        assertThatThrownBy(() -> killian.soigner(emily)).isInstanceOf(AnimalNonPossedeException.class);
        assertThatThrownBy(() -> killian.promener(emily)).isInstanceOf(AnimalNonPossedeException.class);
        assertThatThrownBy(() -> killian.vendre(emily)).isInstanceOf(AnimalNonPossedeException.class);
        assertThatThrownBy(() -> killian.recolter(emily)).isInstanceOf(AnimalNonPossedeException.class);
    }

    @Test
    @DisplayName("un animal sans proprietaire ne peut pas etre nourri")
    void animalSansProprietaire() {
        assertThatThrownBy(() -> alyssia.nourrir(emily)).isInstanceOf(AnimalNonPossedeException.class);
    }

    @Test
    @DisplayName("on ne rachete pas l'animal d'un autre eleveur")
    void achatImpossibleSiDejaPossede() {
        alyssia.acheter(emily);

        assertThatThrownBy(() -> killian.acheter(emily))
                .isInstanceOf(ActionImpossibleException.class)
                .hasMessageContaining("appartient deja a alyssia");
    }

    @Test
    @DisplayName("on n'achete pas un animal mort ou disparu")
    void achatImpossibleSiDisparu() {
        emily.setEtat(EtatAnimal.DISPARU);

        assertThatThrownBy(() -> alyssia.acheter(emily))
                .isInstanceOf(ActionImpossibleException.class)
                .hasMessageContaining("DISPARU");
    }

    @Test
    @DisplayName("les actions de l'eleveur renvoient le message de l'animal")
    void actions() {
        alyssia.acheter(emily);

        assertThat(alyssia.nourrir(emily)).isEqualTo("La vache emily a ete nourrie.");
        assertThat(alyssia.soigner(emily)).isEqualTo("La vache emily a ete soignee.");
        assertThat(alyssia.promener(emily)).isEqualTo("La vache emily part en balade.");
        assertThat(alyssia.recolter(emily)).contains("litres de lait");
        assertThat(alyssia.demenager(emily, "5")).contains("enclos 5");
    }

    @Test
    @DisplayName("la vente sort l'animal du troupeau")
    void vente() {
        alyssia.acheter(emily);

        assertThat(alyssia.vendre(emily)).isEqualTo("La vache emily a ete vendue.");
        assertThat(alyssia.getAnimaux()).isEmpty();
        assertThat(emily.getEleveur()).isNull();
        assertThat(emily.getEtat()).isEqualTo(EtatAnimal.VENDU);
    }
}

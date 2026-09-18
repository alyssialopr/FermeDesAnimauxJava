package laFerme.model;

import laFerme.exception.ActionImpossibleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

    @Test
    @DisplayName("un animal arrive libre a la ferme")
    void etatInitial() {
        assertThat(vache().getEtat()).isEqualTo(EtatAnimal.LIBRE);
        assertThat(poule().getEtat()).isEqualTo(EtatAnimal.LIBRE);
    }

    @Test
    @DisplayName("chaque espece conserve son propre vocabulaire")
    void messagesParEspece() {
        assertThat(vache().nourrir()).isEqualTo("La vache emily a ete nourrie.");
        assertThat(poule().nourrir()).isEqualTo("La poule nugget a ete nourrie.");
        assertThat(vache().soigner()).isEqualTo("La vache emily a ete soignee.");
        assertThat(poule().allerEnBalade()).isEqualTo("La poule nugget part en balade.");
    }

    @Test
    @DisplayName("la production depend de l'espece")
    void production() {
        assertThat(vache().produire()).isEqualTo("La vache emily a donne 18 litres de lait.");
        assertThat(poule().produire()).isEqualTo("La poule nugget a pondu 5 oeufs cette semaine.");

        Vache laitiere = vache();
        laitiere.setLitresDeLaitParJour(25);
        assertThat(laitiere.produire()).contains("25 litres");
    }

    @Test
    @DisplayName("l'espece expose le bon discriminant")
    void espece() {
        assertThat(vache().getEspece()).isEqualTo(Espece.VACHE);
        assertThat(poule().getEspece()).isEqualTo(Espece.POULE);
    }

    @Test
    @DisplayName("la vente bascule l'animal en VENDU")
    void vente() {
        Vache vache = vache();
        assertThat(vache.vendre()).isEqualTo("La vache emily a ete vendue.");
        assertThat(vache.getEtat()).isEqualTo(EtatAnimal.VENDU);
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
        Poule poule = poule();
        assertThat(poule.demenager("7")).isEqualTo("La poule nugget a rejoint l'enclos 7.");
        assertThat(poule.getEnclos()).isEqualTo("7");
    }

    @Test
    @DisplayName("l'achat remet l'animal a l'etat libre")
    void achat() {
        Vache vache = vache();
        vache.vendre();
        vache.acheter();
        assertThat(vache.getEtat()).isEqualTo(EtatAnimal.LIBRE);
    }
}

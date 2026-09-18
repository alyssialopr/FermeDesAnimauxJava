package laFerme.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

/**
 * Il donne de la laine a chaque tonte.
 */
@Entity
@DiscriminatorValue("MOUTON")
@NoArgsConstructor
public class Mouton extends Animal {

    public Mouton(String nom, String race, String couleur, String enclos) {
        super(nom, race, couleur, enclos);
    }

    @Override
    public Espece getEspece() {
        return Espece.MOUTON;
    }

    @Override
    public String cri() {
        return "Bêê !";
    }

    @Override
    protected String messageRecolte() {
        return "%s a ete tondu : %s.".formatted(designation(), production());
    }
}

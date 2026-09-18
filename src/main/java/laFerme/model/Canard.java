package laFerme.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

/**
 * Il pond des oeufs un peu plus gros que la poule.
 */
@Entity
@DiscriminatorValue("CANARD")
@NoArgsConstructor
public class Canard extends Animal {

    public Canard(String nom, String race, String couleur, String enclos) {
        super(nom, race, couleur, enclos);
    }

    @Override
    public Espece getEspece() {
        return Espece.CANARD;
    }
}

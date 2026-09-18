package laFerme.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

/**
 * Son lait se vend plus cher que celui de la vache.
 */
@Entity
@DiscriminatorValue("CHEVRE")
@NoArgsConstructor
public class Chevre extends Animal {

    public Chevre(String nom, String race, String couleur, String enclos) {
        super(nom, race, couleur, enclos);
    }

    @Override
    public Espece getEspece() {
        return Espece.CHEVRE;
    }
}

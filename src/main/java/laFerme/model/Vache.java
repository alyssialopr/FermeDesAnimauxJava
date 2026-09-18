package laFerme.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

/**
 * Elle donne du lait a chaque traite.
 */
@Entity
@DiscriminatorValue("VACHE")
@NoArgsConstructor
public class Vache extends Animal {

    public Vache(String nom, String race, String couleur, String enclos) {
        super(nom, race, couleur, enclos);
    }

    @Override
    public Espece getEspece() {
        return Espece.VACHE;
    }
}

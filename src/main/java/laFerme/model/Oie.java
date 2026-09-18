package laFerme.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

/**
 * Elle fournit du duvet.
 */
@Entity
@DiscriminatorValue("OIE")
@NoArgsConstructor
public class Oie extends Animal {

    public Oie(String nom, String race, String couleur, String enclos) {
        super(nom, race, couleur, enclos);
    }

    @Override
    public Espece getEspece() {
        return Espece.OIE;
    }
}

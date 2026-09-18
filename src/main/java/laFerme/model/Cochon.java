package laFerme.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

/**
 * Il ne se recolte pas : chaque repas l'engraisse et augmente sa valeur a la revente.
 */
@Entity
@DiscriminatorValue("COCHON")
@NoArgsConstructor
public class Cochon extends Animal {

    public Cochon(String nom, String race, String couleur, String enclos) {
        super(nom, race, couleur, enclos);
    }

    @Override
    public Espece getEspece() {
        return Espece.COCHON;
    }
}

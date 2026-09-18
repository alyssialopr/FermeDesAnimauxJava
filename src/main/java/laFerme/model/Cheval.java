package laFerme.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

/**
 * Il ne se recolte pas : ce sont ses promenades qui rapportent.
 */
@Entity
@DiscriminatorValue("CHEVAL")
@NoArgsConstructor
public class Cheval extends Animal {

    public Cheval(String nom, String race, String couleur, String enclos) {
        super(nom, race, couleur, enclos);
    }

    @Override
    public Espece getEspece() {
        return Espece.CHEVAL;
    }
}

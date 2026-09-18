package laFerme.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

/**
 * Elle pond regulierement.
 */
@Entity
@DiscriminatorValue("POULE")
@NoArgsConstructor
public class Poule extends Animal {

    public Poule(String nom, String race, String couleur, String enclos) {
        super(nom, race, couleur, enclos);
    }

    @Override
    public Espece getEspece() {
        return Espece.POULE;
    }

    @Override
    public String cri() {
        return "Cot cot codec !";
    }

    @Override
    protected String messageRecolte() {
        return "%s a pondu %s.".formatted(designation(), production());
    }
}

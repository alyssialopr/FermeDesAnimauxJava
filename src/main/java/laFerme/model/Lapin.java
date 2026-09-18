package laFerme.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

/**
 * Sa portee de lapereaux se vend bien.
 */
@Entity
@DiscriminatorValue("LAPIN")
@NoArgsConstructor
public class Lapin extends Animal {

    public Lapin(String nom, String race, String couleur, String enclos) {
        super(nom, race, couleur, enclos);
    }

    @Override
    public Espece getEspece() {
        return Espece.LAPIN;
    }

    @Override
    public String cri() {
        return "Couic !";
    }

    @Override
    protected String messageRecolte() {
        return "%s a eu une portee de %s.".formatted(designation(), production());
    }
}

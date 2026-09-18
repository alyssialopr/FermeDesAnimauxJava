package laFerme.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Le cheval ne se recolte pas : ce sont ses promenades qui rapportent. Il
 * redefinit donc l'effet de la balade.
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

    @Override
    public String cri() {
        return "Hiiiii !";
    }

    @Override
    public BigDecimal recetteDeLaBalade() {
        return getEspece().getGainParBalade();
    }

    @Override
    protected String effetDeLaBalade() {
        return "%s emmene des promeneurs en foret.".formatted(designation());
    }
}

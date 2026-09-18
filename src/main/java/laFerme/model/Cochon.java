package laFerme.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Le cochon ne se recolte pas : chaque repas l'engraisse et augmente sa valeur
 * a la revente. Il redefinit donc l'effet du repas et laisse le refus de recolte
 * par defaut.
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

    @Override
    public String cri() {
        return "Groin groin !";
    }

    @Override
    public BigDecimal valeurAjouteeParRepas() {
        return getEspece().getGainParRepas();
    }

    @Override
    protected String effetDuRepas() {
        setPrix(getPrix().add(valeurAjouteeParRepas()));
        return "%s a bien mange : il vaut maintenant %s €.".formatted(designation(), getPrix().toPlainString());
    }
}

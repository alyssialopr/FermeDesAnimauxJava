package laFerme.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("VACHE")
@Getter
@Setter
@NoArgsConstructor
public class Vache extends Animal {

    private static final int LAIT_PAR_DEFAUT = 18;

    /** Production laitiere quotidienne, en litres. */
    @Column(name = "litres_de_lait_par_jour")
    private Integer litresDeLaitParJour = LAIT_PAR_DEFAUT;

    public Vache(String nom, String race, String couleur, String enclos) {
        super(nom, race, couleur, enclos);
    }

    @Override
    public Espece getEspece() {
        return Espece.VACHE;
    }

    @Override
    public String designation() {
        return "La vache " + getNom();
    }

    @Override
    public String produire() {
        int litres = litresDeLaitParJour == null ? LAIT_PAR_DEFAUT : litresDeLaitParJour;
        return "%s a donne %d litres de lait.".formatted(designation(), litres);
    }
}

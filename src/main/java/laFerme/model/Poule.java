package laFerme.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("POULE")
@Getter
@Setter
@NoArgsConstructor
public class Poule extends Animal {

    private static final int OEUFS_PAR_DEFAUT = 5;

    /** Nombre d'oeufs pondus par semaine. */
    @Column(name = "oeufs_par_semaine")
    private Integer oeufsParSemaine = OEUFS_PAR_DEFAUT;

    public Poule(String nom, String race, String couleur, String enclos) {
        super(nom, race, couleur, enclos);
    }

    @Override
    public Espece getEspece() {
        return Espece.POULE;
    }

    @Override
    public String designation() {
        return "La poule " + getNom();
    }

    @Override
    public String produire() {
        int oeufs = oeufsParSemaine == null ? OEUFS_PAR_DEFAUT : oeufsParSemaine;
        return "%s a pondu %d oeufs cette semaine.".formatted(designation(), oeufs);
    }
}

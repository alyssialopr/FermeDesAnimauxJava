package laFerme.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EtatAnimal", description = """
        Etat de l'animal. `LIBRE` : present a la ferme, toutes les actions sont possibles. \
        `VENDU` : sorti du troupeau apres une vente. `MORT` et `DISPARU` : sorties definitives \
        declarees par la ferme. Hors `LIBRE`, aucune action n'est acceptee.""")
public enum EtatAnimal {
    LIBRE, VENDU, MORT, DISPARU;

    /**
     * Un animal n'est "utilisable" (nourri, soigne, promene, vendu) que s'il est encore
     * a la ferme : vendu, mort ou disparu, plus aucune action n'a de sens.
     */
    public boolean estDisponible() {
        return this == LIBRE;
    }
}

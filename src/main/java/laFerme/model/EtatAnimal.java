package laFerme.model;

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

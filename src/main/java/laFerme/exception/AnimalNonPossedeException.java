package laFerme.exception;

/**
 * Reprend la regle historique de l'Eleveur : on ne touche pas aux animaux des autres.
 */
public class AnimalNonPossedeException extends RuntimeException {

    public AnimalNonPossedeException() {
        super("Cet animal ne vous appartient pas");
    }
}

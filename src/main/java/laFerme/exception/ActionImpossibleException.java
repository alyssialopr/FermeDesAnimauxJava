package laFerme.exception;

/**
 * Action refusee par une regle metier (animal vendu, mort, disparu, enclos invalide...).
 */
public class ActionImpossibleException extends RuntimeException {

    public ActionImpossibleException(String message) {
        super(message);
    }
}

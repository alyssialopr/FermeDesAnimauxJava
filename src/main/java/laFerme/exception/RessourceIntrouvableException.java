package laFerme.exception;

public class RessourceIntrouvableException extends RuntimeException {

    public RessourceIntrouvableException(String ressource, Object id) {
        super("%s introuvable : %s".formatted(ressource, id));
    }
}

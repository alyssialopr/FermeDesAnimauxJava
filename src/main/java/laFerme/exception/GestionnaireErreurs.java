package laFerme.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduction des erreurs metier en reponses HTTP normalisees (RFC 7807).
 */
@RestControllerAdvice
@Slf4j
public class GestionnaireErreurs {

    @ExceptionHandler(RessourceIntrouvableException.class)
    ProblemDetail introuvable(RessourceIntrouvableException exception, HttpServletRequest requete) {
        return probleme(HttpStatus.NOT_FOUND, "Ressource introuvable", exception.getMessage(), requete);
    }

    @ExceptionHandler(AnimalNonPossedeException.class)
    ProblemDetail nonPossede(AnimalNonPossedeException exception, HttpServletRequest requete) {
        return probleme(HttpStatus.CONFLICT, "Animal non possede", exception.getMessage(), requete);
    }

    @ExceptionHandler(ActionImpossibleException.class)
    ProblemDetail actionImpossible(ActionImpossibleException exception, HttpServletRequest requete) {
        return probleme(HttpStatus.CONFLICT, "Action impossible", exception.getMessage(), requete);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception, HttpServletRequest requete) {
        Map<String, String> champs = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(erreur -> champs.putIfAbsent(erreur.getField(), erreur.getDefaultMessage()));

        ProblemDetail detail = probleme(HttpStatus.BAD_REQUEST, "Requete invalide",
                "Certains champs sont invalides.", requete);
        detail.setProperty("champs", champs);
        return detail;
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    ProblemDetail requeteIllisible(Exception exception, HttpServletRequest requete) {
        return probleme(HttpStatus.BAD_REQUEST, "Requete invalide",
                "Le corps ou un parametre de la requete n'a pas pu etre interprete.", requete);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inattendue(Exception exception, HttpServletRequest requete) {
        log.error("Erreur inattendue sur {} {}", requete.getMethod(), requete.getRequestURI(), exception);
        return probleme(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne",
                "Une erreur inattendue est survenue.", requete);
    }

    private ProblemDetail probleme(HttpStatus statut, String titre, String detailMessage, HttpServletRequest requete) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(statut, detailMessage);
        detail.setTitle(titre);
        detail.setProperty("horodatage", Instant.now());
        detail.setProperty("chemin", requete.getRequestURI());
        return detail;
    }
}

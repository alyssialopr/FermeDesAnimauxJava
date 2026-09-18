package laFerme.exception;

import java.math.BigDecimal;

/**
 * L'eleveur n'a pas assez d'argent pour l'operation demandee.
 */
public class FondsInsuffisantsException extends RuntimeException {

    public FondsInsuffisantsException(String prenom, BigDecimal manquant, BigDecimal solde) {
        super("%s n'a pas assez d'argent : il manque %s € (solde : %s €).".formatted(
                prenom, manquant.toPlainString(), solde.toPlainString()));
    }
}

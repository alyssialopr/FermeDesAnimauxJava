package laFerme.model;

import java.math.BigDecimal;

/**
 * Ce que produit une action de l'eleveur : le message a afficher et le mouvement
 * d'argent correspondant (negatif pour une depense, positif pour une recette).
 */
public record ResultatAction(TypeMouvement type, String message, BigDecimal montant) {

    public static ResultatAction depense(TypeMouvement type, String message, BigDecimal montant) {
        return new ResultatAction(type, message, montant.negate());
    }

    public static ResultatAction recette(TypeMouvement type, String message, BigDecimal montant) {
        return new ResultatAction(type, message, montant);
    }
}

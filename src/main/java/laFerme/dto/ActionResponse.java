package laFerme.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Resultat d'une action de la ferme : le message metier, le mouvement d'argent
 * qu'elle a provoque et l'etat de l'animal apres l'action.
 */
@Schema(name = "ActionResponse", description = "Resultat d'une action de l'eleveur sur un animal")
public record ActionResponse(

        @Schema(description = "Message metier de l'action, formule selon l'espece",
                example = "La vache emily a ete nourrie. (fourrage : 4.00 €)")
        String message,

        @Schema(description = "Mouvement d'argent : negatif pour une depense, positif pour une recette",
                example = "-4.00")
        BigDecimal montant,

        @Schema(description = "Solde de l'eleveur apres l'action", example = "244.60")
        BigDecimal solde,

        @Schema(description = "Etat de l'animal apres l'action")
        AnimalResponse animal
) {
}

package laFerme.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "ClassementResponse", description = "Place d'un eleveur au classement des fortunes")
public record ClassementResponse(

        @Schema(description = "Rang, 1 = le plus riche", example = "1")
        int rang,

        @Schema(description = "Identifiant de l'eleveur", example = "1")
        Long eleveurId,

        @Schema(description = "Prenom de l'eleveur", example = "alyssia")
        String prenom,

        @Schema(description = "Argent disponible", example = "248.60")
        BigDecimal solde,

        @Schema(description = "Valeur de revente du troupeau", example = "414.00")
        BigDecimal valeurTroupeau,

        @Schema(description = "Solde plus troupeau", example = "662.60")
        BigDecimal fortune,

        @Schema(description = "Taille du troupeau", example = "2")
        int nombreAnimaux
) {
}

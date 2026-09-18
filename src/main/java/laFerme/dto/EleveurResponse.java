package laFerme.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(name = "EleveurResponse", description = "Eleveur, son porte-monnaie et, selon la route, son troupeau")
public record EleveurResponse(

        @Schema(description = "Identifiant de l'eleveur", example = "1")
        Long id,

        @Schema(description = "Prenom de l'eleveur", example = "alyssia")
        String prenom,

        @Schema(description = "Argent disponible, en euros", example = "248.60")
        BigDecimal solde,

        @Schema(description = "Solde plus la valeur de revente du troupeau", example = "662.60")
        BigDecimal fortune,

        @Schema(description = "Taille du troupeau", example = "2")
        int nombreAnimaux,

        @Schema(description = "Troupeau detaille. Renseigne sur la fiche d'un eleveur, "
                + "null dans la liste des eleveurs")
        List<AnimalResponse> animaux
) {
}

package laFerme.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import laFerme.model.TypeMouvement;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(name = "MouvementResponse", description = "Ligne du releve de compte d'un eleveur")
public record MouvementResponse(

        @Schema(description = "Identifiant du mouvement", example = "12")
        Long id,

        @Schema(description = "Nature du mouvement", example = "RECOLTE")
        TypeMouvement type,

        @Schema(description = "Montant : negatif pour une depense, positif pour une recette", example = "27.00")
        BigDecimal montant,

        @Schema(description = "Solde apres l'operation", example = "271.60")
        BigDecimal soldeApres,

        @Schema(description = "Libelle lisible", example = "La vache emily a donne 18 litres de lait. Vendu 27.00 €.")
        String libelle,

        @Schema(description = "Animal concerne, s'il existe encore", example = "1")
        Long animalId,

        @Schema(description = "Date de l'operation", example = "2026-09-18T09:12:44.512Z")
        Instant horodatage
) {
}

package laFerme.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import laFerme.model.Espece;
import laFerme.model.Production;

import java.math.BigDecimal;

/**
 * Fiche du catalogue : tout ce qu'il faut savoir avant d'acheter une espece.
 */
@Schema(name = "EspeceResponse", description = "Caracteristiques et tarifs d'une espece")
public record EspeceResponse(

        @Schema(description = "Identifiant de l'espece", example = "VACHE")
        Espece espece,

        @Schema(description = "Designation utilisee dans les messages", example = "La vache")
        String designation,

        @Schema(description = "Ce que rend une recolte", example = "LAIT")
        Production production,

        @Schema(description = "Unite de la production", example = "litres de lait")
        String unite,

        @Schema(description = "Quantite rendue a chaque recolte", example = "18")
        int quantiteParDefaut,

        @Schema(description = "Prix de vente d'une unite de production", example = "1.50")
        BigDecimal prixUnitaire,

        @Schema(description = "Prix d'achat d'un animal de cette espece", example = "230.00")
        BigDecimal prix,

        @Schema(description = "Cout d'un repas", example = "4.00")
        BigDecimal coutRepas,

        @Schema(description = "Cout d'une visite du veterinaire", example = "20.00")
        BigDecimal coutSoin,

        @Schema(description = "Points de faim gagnes par minute", example = "6")
        int faimParMinute,

        @Schema(description = "Delai entre deux recoltes, en secondes", example = "180")
        long delaiSecondes,

        @Schema(description = "Valeur ajoutee a l'animal a chaque repas (engraissement)", example = "0.00")
        BigDecimal gainParRepas,

        @Schema(description = "Recette d'une promenade", example = "0.00")
        BigDecimal gainParBalade
) {
}

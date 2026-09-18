package laFerme.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import laFerme.model.Espece;

/**
 * Creation d'un animal. L'eleveur est optionnel : un animal peut arriver a la ferme
 * avant d'etre achete par quelqu'un.
 */
@Schema(name = "CreerAnimalRequest", description = "Animal a faire entrer a la ferme")
public record CreerAnimalRequest(

        @Schema(description = "Espece de l'animal, elle determine son comportement et sa production",
                example = "VACHE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "l'espece est obligatoire (VACHE ou POULE)")
        Espece espece,

        @Schema(description = "Nom de l'animal", example = "emily",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "le nom est obligatoire")
        @Size(max = 255)
        String nom,

        @Schema(description = "Race de l'animal", example = "Highland",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "la race est obligatoire")
        @Size(max = 255)
        String race,

        @Schema(description = "Couleur de la robe ou du plumage", example = "marron",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "la couleur est obligatoire")
        @Size(max = 255)
        String couleur,

        @Schema(description = "Enclos dans lequel l'animal est installe", example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "l'enclos est obligatoire")
        @Size(max = 255)
        String enclos,

        @Schema(description = "Production laitiere quotidienne. Vaches uniquement, 18 par defaut",
                example = "22")
        @Min(value = 0, message = "la production laitiere ne peut pas etre negative")
        Integer litresDeLaitParJour,

        @Schema(description = "Ponte hebdomadaire. Poules uniquement, 5 par defaut", example = "6")
        @Min(value = 0, message = "le nombre d'oeufs ne peut pas etre negatif")
        Integer oeufsParSemaine,

        @Schema(description = "Eleveur qui achete l'animal des son arrivee. Optionnel : sans lui, "
                + "l'animal reste sans proprietaire jusqu'a un achat", example = "1")
        Long eleveurId
) {
}

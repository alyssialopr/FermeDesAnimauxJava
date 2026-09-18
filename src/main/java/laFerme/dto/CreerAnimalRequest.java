package laFerme.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import laFerme.model.Espece;

/**
 * Creation d'un animal. L'eleveur est optionnel : sans lui, l'animal arrive au
 * marche et attend un acheteur. Avec lui, l'animal est achete dans la foulee et
 * son prix est debite.
 */
@Schema(name = "CreerAnimalRequest", description = "Animal a faire entrer a la ferme")
public record CreerAnimalRequest(

        @Schema(description = "Espece de l'animal : elle fixe sa production, ses tarifs et son appetit",
                example = "VACHE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "l'espece est obligatoire")
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

        @Schema(description = "Quantite rendue a chaque recolte. Par defaut, celle de l'espece",
                example = "18")
        @Min(value = 0, message = "la production ne peut pas etre negative")
        Integer quantiteProduction,

        @Schema(description = "Eleveur qui achete l'animal des son arrivee. Son prix lui est debite. "
                + "Sans lui, l'animal attend au marche", example = "1")
        Long eleveurId
) {
}

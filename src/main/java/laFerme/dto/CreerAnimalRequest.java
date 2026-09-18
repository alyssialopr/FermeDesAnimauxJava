package laFerme.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import laFerme.model.Espece;

/**
 * Creation d'un animal. L'eleveur est optionnel : un animal peut arriver a la ferme
 * avant d'etre achete par quelqu'un.
 */
public record CreerAnimalRequest(

        @NotNull(message = "l'espece est obligatoire (VACHE ou POULE)")
        Espece espece,

        @NotBlank(message = "le nom est obligatoire")
        @Size(max = 255)
        String nom,

        @NotBlank(message = "la race est obligatoire")
        @Size(max = 255)
        String race,

        @NotBlank(message = "la couleur est obligatoire")
        @Size(max = 255)
        String couleur,

        @NotBlank(message = "l'enclos est obligatoire")
        @Size(max = 255)
        String enclos,

        /** Specifique aux vaches. */
        @Min(value = 0, message = "la production laitiere ne peut pas etre negative")
        Integer litresDeLaitParJour,

        /** Specifique aux poules. */
        @Min(value = 0, message = "le nombre d'oeufs ne peut pas etre negatif")
        Integer oeufsParSemaine,

        Long eleveurId
) {
}

package laFerme.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import laFerme.model.EtatAnimal;

/**
 * Declaration d'un animal mort ou disparu. La vente passe par l'eleveur, pas par ici.
 */
@Schema(name = "MajEtatRequest", description = "Declaration de sortie definitive d'un animal")
public record MajEtatRequest(

        @Schema(description = "Nouvel etat, MORT ou DISPARU uniquement. La vente se fait via "
                + "l'eleveur et un animal ne revient jamais a l'etat LIBRE par cette route",
                example = "DISPARU", allowableValues = {"MORT", "DISPARU"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "l'etat est obligatoire (MORT ou DISPARU)")
        EtatAnimal etat
) {
}

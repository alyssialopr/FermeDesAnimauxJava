package laFerme.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "CreerEleveurRequest", description = "Nouvel eleveur de la ferme")
public record CreerEleveurRequest(

        @Schema(description = "Prenom de l'eleveur, unique a la ferme (casse ignoree)",
                example = "alyssia", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "le prenom est obligatoire")
        @Size(max = 255)
        String prenom
) {
}

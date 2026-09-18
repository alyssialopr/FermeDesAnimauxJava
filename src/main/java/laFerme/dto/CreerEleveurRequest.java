package laFerme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreerEleveurRequest(

        @NotBlank(message = "le prenom est obligatoire")
        @Size(max = 255)
        String prenom
) {
}

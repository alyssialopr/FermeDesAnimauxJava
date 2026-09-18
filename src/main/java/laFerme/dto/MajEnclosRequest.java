package laFerme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MajEnclosRequest(

        @NotBlank(message = "l'enclos est obligatoire")
        @Size(max = 255)
        String enclos
) {
}

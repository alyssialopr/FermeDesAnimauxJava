package laFerme.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "MajEnclosRequest", description = "Nouvel enclos de l'animal")
public record MajEnclosRequest(

        @Schema(description = "Enclos de destination", example = "7",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "l'enclos est obligatoire")
        @Size(max = 40, message = "40 caracteres maximum")
        @Pattern(regexp = "[\\p{L}\\p{N} '\\-]+", message = "lettres, chiffres, espaces, apostrophes et tirets uniquement")
        String enclos
) {
}

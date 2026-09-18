package laFerme.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "EleveurResponse", description = "Eleveur et, selon la route, le detail de son troupeau")
public record EleveurResponse(

        @Schema(description = "Identifiant de l'eleveur", example = "1")
        Long id,

        @Schema(description = "Prenom de l'eleveur", example = "alyssia")
        String prenom,

        @Schema(description = "Taille du troupeau", example = "2")
        int nombreAnimaux,

        @Schema(description = "Troupeau detaille. Renseigne sur la fiche d'un eleveur, "
                + "null dans la liste des eleveurs")
        List<AnimalResponse> animaux
) {
}

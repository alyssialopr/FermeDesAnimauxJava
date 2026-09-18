package laFerme.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultat d'une action de la ferme : le message metier (celui qui etait affiche en
 * console dans la version console) et l'etat de l'animal apres l'action.
 */
@Schema(name = "ActionResponse", description = "Resultat d'une action de l'eleveur sur un animal")
public record ActionResponse(

        @Schema(description = "Message metier de l'action, formule selon l'espece",
                example = "La vache emily a ete nourrie.")
        String message,

        @Schema(description = "Etat de l'animal apres l'action")
        AnimalResponse animal
) {
}

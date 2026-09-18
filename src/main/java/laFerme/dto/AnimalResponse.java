package laFerme.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import laFerme.model.Espece;
import laFerme.model.EtatAnimal;

import java.time.Instant;

@Schema(name = "AnimalResponse", description = "Animal tel qu'il est enregistre a la ferme")
public record AnimalResponse(

        @Schema(description = "Identifiant de l'animal", example = "1")
        Long id,

        @Schema(description = "Espece de l'animal", example = "VACHE")
        Espece espece,

        @Schema(description = "Nom de l'animal", example = "emily")
        String nom,

        @Schema(description = "Race de l'animal", example = "Highland")
        String race,

        @Schema(description = "Couleur de la robe ou du plumage", example = "marron")
        String couleur,

        @Schema(description = "Enclos courant", example = "1")
        String enclos,

        @Schema(description = "Etat de l'animal : seul LIBRE autorise les actions", example = "LIBRE")
        EtatAnimal etat,

        @Schema(description = "Identifiant du proprietaire, null si l'animal n'appartient a personne",
                example = "1")
        Long eleveurId,

        @Schema(description = "Prenom du proprietaire", example = "alyssia")
        String eleveurPrenom,

        @Schema(description = "Production laitiere quotidienne, null pour une poule", example = "18")
        Integer litresDeLaitParJour,

        @Schema(description = "Ponte hebdomadaire, null pour une vache", example = "5")
        Integer oeufsParSemaine,

        @Schema(description = "Date d'arrivee a la ferme", example = "2026-09-18T07:26:51.173Z")
        Instant creeLe
) {
}

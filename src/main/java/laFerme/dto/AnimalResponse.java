package laFerme.dto;

import laFerme.model.Espece;
import laFerme.model.EtatAnimal;

import java.time.Instant;

public record AnimalResponse(
        Long id,
        Espece espece,
        String nom,
        String race,
        String couleur,
        String enclos,
        EtatAnimal etat,
        Long eleveurId,
        String eleveurPrenom,
        Integer litresDeLaitParJour,
        Integer oeufsParSemaine,
        Instant creeLe
) {
}

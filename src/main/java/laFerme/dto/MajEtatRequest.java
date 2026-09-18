package laFerme.dto;

import jakarta.validation.constraints.NotNull;
import laFerme.model.EtatAnimal;

/**
 * Declaration d'un animal mort ou disparu. La vente passe par l'eleveur, pas par ici.
 */
public record MajEtatRequest(

        @NotNull(message = "l'etat est obligatoire (MORT ou DISPARU)")
        EtatAnimal etat
) {
}

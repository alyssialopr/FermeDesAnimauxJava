package laFerme.dto;

import java.util.List;

public record EleveurResponse(
        Long id,
        String prenom,
        int nombreAnimaux,
        List<AnimalResponse> animaux
) {
}

package laFerme.utils;

import laFerme.dto.AnimalResponse;
import laFerme.dto.EleveurResponse;
import laFerme.model.Eleveur;

import java.util.Comparator;
import java.util.List;

public final class EleveurMapper {

    private EleveurMapper() {
    }

    public static EleveurResponse versReponse(Eleveur eleveur) {
        List<AnimalResponse> animaux = eleveur.getAnimaux().stream()
                .sorted(Comparator.comparing(animal -> animal.getNom().toLowerCase()))
                .map(AnimalMapper::versReponse)
                .toList();
        return new EleveurResponse(eleveur.getId(), eleveur.getPrenom(), animaux.size(), animaux);
    }

    /** Variante sans le troupeau, pour les listes. */
    public static EleveurResponse versResume(Eleveur eleveur) {
        return new EleveurResponse(eleveur.getId(), eleveur.getPrenom(), eleveur.getAnimaux().size(), null);
    }
}

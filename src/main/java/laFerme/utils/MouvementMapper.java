package laFerme.utils;

import laFerme.dto.MouvementResponse;
import laFerme.model.Mouvement;

public final class MouvementMapper {

    private MouvementMapper() {
    }

    public static MouvementResponse versReponse(Mouvement mouvement) {
        return new MouvementResponse(
                mouvement.getId(),
                mouvement.getType(),
                mouvement.getMontant(),
                mouvement.getSoldeApres(),
                mouvement.getLibelle(),
                mouvement.getAnimalId(),
                mouvement.getHorodatage());
    }
}

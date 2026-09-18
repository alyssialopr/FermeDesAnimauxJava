package laFerme.utils;

import laFerme.dto.EspeceResponse;
import laFerme.model.Espece;

public final class EspeceMapper {

    private EspeceMapper() {
    }

    public static EspeceResponse versReponse(Espece espece) {
        return new EspeceResponse(
                espece,
                espece.getDesignation(),
                espece.getProduction(),
                espece.getProduction().getUnite(),
                espece.getQuantiteParDefaut(),
                espece.getPrixUnitaire(),
                espece.getPrix(),
                espece.getCoutRepas(),
                espece.getCoutSoin(),
                espece.getFaimParMinute(),
                espece.getDelai().toSeconds(),
                espece.getGainParRepas(),
                espece.getGainParBalade());
    }
}

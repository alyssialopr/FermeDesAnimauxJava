package laFerme.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TypeMouvement", description = "Nature d'un mouvement d'argent sur le compte de l'eleveur")
public enum TypeMouvement {
    ACHAT,
    VENTE,
    REPAS,
    SOIN,
    RECOLTE,
    PROMENADE
}

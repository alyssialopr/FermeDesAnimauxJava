package laFerme.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Especes elevees a la ferme. Sert aussi de discriminateur JPA sur la table animal.
 */
@Schema(name = "Espece", description = """
        Espece de l'animal. Elle determine la formulation des messages et la nature \
        de la production : du lait pour une vache, des oeufs pour une poule.""")
public enum Espece {
    VACHE,
    POULE
}

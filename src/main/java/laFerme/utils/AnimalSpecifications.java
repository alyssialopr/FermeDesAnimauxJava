package laFerme.utils;

import laFerme.model.Animal;
import laFerme.model.Espece;
import laFerme.model.EtatAnimal;
import laFerme.model.Poule;
import laFerme.model.Vache;
import org.springframework.data.jpa.domain.Specification;

/**
 * Criteres de recherche combinables pour la liste des animaux.
 */
public final class AnimalSpecifications {

    private AnimalSpecifications() {
    }

    public static Specification<Animal> espece(Espece espece) {
        if (espece == null) {
            return null;
        }
        Class<? extends Animal> type = switch (espece) {
            case VACHE -> Vache.class;
            case POULE -> Poule.class;
        };
        return (racine, requete, cb) -> cb.equal(racine.type(), cb.literal(type));
    }

    public static Specification<Animal> etat(EtatAnimal etat) {
        if (etat == null) {
            return null;
        }
        return (racine, requete, cb) -> cb.equal(racine.get("etat"), etat);
    }

    public static Specification<Animal> eleveur(Long eleveurId) {
        if (eleveurId == null) {
            return null;
        }
        return (racine, requete, cb) -> cb.equal(racine.get("eleveur").get("id"), eleveurId);
    }

    public static Specification<Animal> enclos(String enclos) {
        if (enclos == null || enclos.isBlank()) {
            return null;
        }
        return (racine, requete, cb) -> cb.equal(racine.get("enclos"), enclos);
    }
}

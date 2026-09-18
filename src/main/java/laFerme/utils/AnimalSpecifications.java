package laFerme.utils;

import laFerme.model.Animal;
import laFerme.model.Canard;
import laFerme.model.Cheval;
import laFerme.model.Chevre;
import laFerme.model.Cochon;
import laFerme.model.Espece;
import laFerme.model.EtatAnimal;
import laFerme.model.Lapin;
import laFerme.model.Mouton;
import laFerme.model.Oie;
import laFerme.model.Poule;
import laFerme.model.Vache;
import org.springframework.data.jpa.domain.Specification;

/**
 * Criteres de recherche combinables pour la liste des animaux.
 * Un critere absent renvoie une specification sans restriction, ce qui permet de tous
 * les combiner avec {@link Specification#allOf} sans tests conditionnels ailleurs.
 */
public final class AnimalSpecifications {

    private AnimalSpecifications() {
    }

    public static Specification<Animal> espece(Espece espece) {
        if (espece == null) {
            return Specification.unrestricted();
        }
        Class<? extends Animal> type = switch (espece) {
            case VACHE -> Vache.class;
            case POULE -> Poule.class;
            case MOUTON -> Mouton.class;
            case CHEVRE -> Chevre.class;
            case COCHON -> Cochon.class;
            case CANARD -> Canard.class;
            case LAPIN -> Lapin.class;
            case CHEVAL -> Cheval.class;
            case OIE -> Oie.class;
        };
        return (racine, requete, cb) -> cb.equal(racine.type(), cb.literal(type));
    }

    public static Specification<Animal> etat(EtatAnimal etat) {
        if (etat == null) {
            return Specification.unrestricted();
        }
        return (racine, requete, cb) -> cb.equal(racine.get("etat"), etat);
    }

    public static Specification<Animal> eleveur(Long eleveurId) {
        if (eleveurId == null) {
            return Specification.unrestricted();
        }
        return (racine, requete, cb) -> cb.equal(racine.get("eleveur").get("id"), eleveurId);
    }

    public static Specification<Animal> enclos(String enclos) {
        if (enclos == null || enclos.isBlank()) {
            return Specification.unrestricted();
        }
        return (racine, requete, cb) -> cb.equal(racine.get("enclos"), enclos);
    }
}

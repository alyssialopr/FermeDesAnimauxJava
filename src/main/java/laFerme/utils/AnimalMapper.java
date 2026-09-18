package laFerme.utils;

import laFerme.dto.AnimalResponse;
import laFerme.dto.CreerAnimalRequest;
import laFerme.model.Animal;
import laFerme.model.Eleveur;
import laFerme.model.Poule;
import laFerme.model.Vache;

/**
 * Conversion entre les entites du domaine et les objets exposes par l'API.
 * Les entites ne sont jamais serialisees directement (pas de fuite de mapping JPA).
 */
public final class AnimalMapper {

    private AnimalMapper() {
    }

    /**
     * Fabrique l'animal correspondant a l'espece demandee. C'est le seul endroit du code
     * qui connait la correspondance espece -> classe concrete.
     */
    public static Animal versEntite(CreerAnimalRequest requete) {
        return switch (requete.espece()) {
            case VACHE -> {
                Vache vache = new Vache(requete.nom(), requete.race(), requete.couleur(), requete.enclos());
                if (requete.litresDeLaitParJour() != null) {
                    vache.setLitresDeLaitParJour(requete.litresDeLaitParJour());
                }
                yield vache;
            }
            case POULE -> {
                Poule poule = new Poule(requete.nom(), requete.race(), requete.couleur(), requete.enclos());
                if (requete.oeufsParSemaine() != null) {
                    poule.setOeufsParSemaine(requete.oeufsParSemaine());
                }
                yield poule;
            }
        };
    }

    public static AnimalResponse versReponse(Animal animal) {
        Eleveur eleveur = animal.getEleveur();
        return new AnimalResponse(
                animal.getId(),
                animal.getEspece(),
                animal.getNom(),
                animal.getRace(),
                animal.getCouleur(),
                animal.getEnclos(),
                animal.getEtat(),
                eleveur == null ? null : eleveur.getId(),
                eleveur == null ? null : eleveur.getPrenom(),
                animal instanceof Vache vache ? vache.getLitresDeLaitParJour() : null,
                animal instanceof Poule poule ? poule.getOeufsParSemaine() : null,
                animal.getCreeLe());
    }
}

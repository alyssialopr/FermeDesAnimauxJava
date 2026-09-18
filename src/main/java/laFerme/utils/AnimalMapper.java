package laFerme.utils;

import laFerme.dto.AnimalResponse;
import laFerme.dto.CreerAnimalRequest;
import laFerme.model.Animal;
import laFerme.model.Canard;
import laFerme.model.Cheval;
import laFerme.model.Chevre;
import laFerme.model.Cochon;
import laFerme.model.Eleveur;
import laFerme.model.Espece;
import laFerme.model.Lapin;
import laFerme.model.Mouton;
import laFerme.model.Oie;
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
        Animal animal = nouvelAnimal(requete.espece(), requete.nom(), requete.race(),
                requete.couleur(), requete.enclos());

        if (requete.quantiteProduction() != null) {
            animal.setQuantiteProduction(requete.quantiteProduction());
        }
        return animal;
    }

    public static Animal nouvelAnimal(Espece espece, String nom, String race, String couleur, String enclos) {
        return switch (espece) {
            case VACHE -> new Vache(nom, race, couleur, enclos);
            case POULE -> new Poule(nom, race, couleur, enclos);
            case MOUTON -> new Mouton(nom, race, couleur, enclos);
            case CHEVRE -> new Chevre(nom, race, couleur, enclos);
            case COCHON -> new Cochon(nom, race, couleur, enclos);
            case CANARD -> new Canard(nom, race, couleur, enclos);
            case LAPIN -> new Lapin(nom, race, couleur, enclos);
            case CHEVAL -> new Cheval(nom, race, couleur, enclos);
            case OIE -> new Oie(nom, race, couleur, enclos);
        };
    }

    public static AnimalResponse versReponse(Animal animal) {
        Eleveur eleveur = animal.getEleveur();
        Espece espece = animal.getEspece();

        return new AnimalResponse(
                animal.getId(),
                espece,
                animal.getNom(),
                animal.getRace(),
                animal.getCouleur(),
                animal.getEnclos(),
                animal.getEtat(),
                eleveur == null ? null : eleveur.getId(),
                eleveur == null ? null : eleveur.getPrenom(),
                animal.getPrix(),
                animal.valeurDeRevente(),
                espece.getProduction(),
                espece.getProduction().getUnite(),
                animal.getQuantiteProduction(),
                animal.valeurRecolte(),
                animal.getFaim(),
                animal.getSante(),
                animal.peutEtreRecolte(),
                animal.getSecondesAvantRecolte(),
                animal.cri(),
                animal.getCreeLe());
    }
}

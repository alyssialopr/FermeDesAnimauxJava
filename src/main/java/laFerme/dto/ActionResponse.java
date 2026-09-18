package laFerme.dto;

/**
 * Resultat d'une action de la ferme : le message metier (celui qui etait affiche en
 * console dans la version console) et l'etat de l'animal apres l'action.
 */
public record ActionResponse(
        String message,
        AnimalResponse animal
) {
}

package laFerme.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import laFerme.model.Espece;
import laFerme.model.EtatAnimal;
import laFerme.model.Production;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(name = "AnimalResponse", description = "Animal tel qu'il est enregistre a la ferme")
public record AnimalResponse(

        @Schema(description = "Identifiant de l'animal", example = "1")
        Long id,

        @Schema(description = "Espece de l'animal", example = "VACHE")
        Espece espece,

        @Schema(description = "Nom de l'animal", example = "emily")
        String nom,

        @Schema(description = "Race de l'animal", example = "Highland")
        String race,

        @Schema(description = "Couleur de la robe ou du plumage", example = "marron")
        String couleur,

        @Schema(description = "Enclos courant", example = "1")
        String enclos,

        @Schema(description = "Etat de l'animal : seul LIBRE autorise les actions", example = "LIBRE")
        EtatAnimal etat,

        @Schema(description = "Identifiant du proprietaire, null si l'animal est au marche", example = "1")
        Long eleveurId,

        @Schema(description = "Prenom du proprietaire", example = "alyssia")
        String eleveurPrenom,

        @Schema(description = "Valeur marchande, c'est le prix d'achat au marche", example = "230.00")
        BigDecimal prix,

        @Schema(description = "Ce que la ferme rachete l'animal (valeur moins 10 % de decote)", example = "207.00")
        BigDecimal valeurDeRevente,

        @Schema(description = "Ce que rend une recolte", example = "LAIT")
        Production production,

        @Schema(description = "Unite de la production", example = "litres de lait")
        String unite,

        @Schema(description = "Quantite rendue a chaque recolte", example = "18")
        int quantiteProduction,

        @Schema(description = "Ce que rapporte une recolte au prix du marche", example = "27.00")
        BigDecimal valeurRecolte,

        @Schema(description = "0 = repu, 100 = affame. Monte tout seul avec le temps", example = "35")
        int faim,

        @Schema(description = "Sante en pourcentage : baisse a chaque recolte, remontee par les soins",
                example = "92")
        int sante,

        @Schema(description = "Vrai si une recolte est possible tout de suite", example = "true")
        boolean peutEtreRecolte,

        @Schema(description = "Secondes restantes avant la prochaine recolte", example = "0")
        long secondesAvantRecolte,

        @Schema(description = "Le cri de l'espece", example = "Meuh !")
        String cri,

        @Schema(description = "Date d'arrivee a la ferme", example = "2026-09-18T07:26:51.173Z")
        Instant creeLe
) {
}

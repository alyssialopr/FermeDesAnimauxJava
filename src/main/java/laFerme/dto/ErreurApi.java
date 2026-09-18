package laFerme.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * Forme des reponses d'erreur (RFC 7807). Cette classe ne sert qu'a documenter le
 * contrat : a l'execution, c'est {@link org.springframework.http.ProblemDetail} qui
 * est serialise par {@code GestionnaireErreurs}.
 */
@Schema(name = "Erreur", description = "Erreur normalisee au format RFC 7807 (application/problem+json)")
public record ErreurApi(

        @Schema(description = "Intitule court de l'erreur", example = "Animal non possede")
        String title,

        @Schema(description = "Code HTTP", example = "409")
        int status,

        @Schema(description = "Message explicatif", example = "Cet animal ne vous appartient pas")
        String detail,

        @Schema(description = "Chemin de la requete fautive", example = "/api/eleveurs/2/animaux/1/repas")
        String instance,

        @Schema(description = "Date de l'erreur", example = "2026-09-18T07:27:13.445Z")
        Instant horodatage,

        @Schema(description = "Detail par champ, uniquement pour les erreurs de validation (400)",
                example = "{\"nom\": \"le nom est obligatoire\"}")
        Map<String, String> champs
) {
}

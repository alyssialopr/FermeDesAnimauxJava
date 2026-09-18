package laFerme.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import laFerme.dto.AnimalResponse;
import laFerme.dto.CreerAnimalRequest;
import laFerme.dto.ErreurApi;
import laFerme.dto.MajEnclosRequest;
import laFerme.dto.MajEtatRequest;
import laFerme.model.Espece;
import laFerme.model.EtatAnimal;
import laFerme.services.AnimalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/animaux")
@RequiredArgsConstructor
@Tag(name = "Animaux", description = """
        Le cheptel : entree d'un animal a la ferme, recherche filtree, deplacement \
        d'enclos et declaration de sortie definitive (mort ou disparu).""")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Requete invalide : champ manquant, "
                + "valeur d'enumeration inconnue ou corps illisible",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ErreurApi.class),
                        examples = @ExampleObject(name = "Validation", value = """
                                {
                                  "title": "Requete invalide",
                                  "status": 400,
                                  "detail": "Certains champs sont invalides.",
                                  "instance": "/api/animaux",
                                  "horodatage": "2026-09-18T07:27:13.637Z",
                                  "champs": {"nom": "le nom est obligatoire"}
                                }"""))),
        @ApiResponse(responseCode = "404", description = "Animal ou eleveur inconnu",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ErreurApi.class),
                        examples = @ExampleObject(name = "Introuvable", value = """
                                {
                                  "title": "Ressource introuvable",
                                  "status": 404,
                                  "detail": "Animal introuvable : 42",
                                  "instance": "/api/animaux/42",
                                  "horodatage": "2026-09-18T07:27:13.544Z"
                                }"""))),
        @ApiResponse(responseCode = "409", description = "Regle metier : animal vendu, mort ou disparu",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ErreurApi.class),
                        examples = @ExampleObject(name = "Action impossible", value = """
                                {
                                  "title": "Action impossible",
                                  "status": 409,
                                  "detail": "Impossible de nourrir la vache emily : l'animal est VENDU.",
                                  "instance": "/api/animaux/2/etat",
                                  "horodatage": "2026-09-18T07:27:13.445Z"
                                }""")))
})
public class AnimalController {

    private final AnimalService animalService;

    @PostMapping
    @Operation(summary = "Faire entrer un animal a la ferme",
            description = """
                    Cree une vache ou une poule. Les champs specifiques a l'espece \
                    (`litresDeLaitParJour`, `oeufsParSemaine`) sont facultatifs et prennent \
                    une valeur par defaut. Si `eleveurId` est fourni, l'animal est achete \
                    dans la foulee par cet eleveur.""")
    @ApiResponse(responseCode = "201", description = "Animal enregistre, son adresse est dans l'en-tete Location")
    public ResponseEntity<AnimalResponse> creerAnimal(@Valid @RequestBody CreerAnimalRequest requete) {
        AnimalResponse animal = animalService.creer(requete);
        return ResponseEntity.created(URI.create("/api/animaux/" + animal.id())).body(animal);
    }

    @GetMapping
    @Operation(summary = "Lister les animaux, avec filtres optionnels",
            description = "Les filtres se combinent. Sans filtre, tout le cheptel est renvoye, trie par nom.")
    @ApiResponse(responseCode = "200", description = "Cheptel correspondant aux filtres")
    public List<AnimalResponse> recupereAnimaux(
            @Parameter(description = "Ne garder qu'une espece", example = "POULE")
            @RequestParam(required = false) Espece espece,

            @Parameter(description = "Ne garder que les animaux dans cet etat", example = "LIBRE")
            @RequestParam(required = false) EtatAnimal etat,

            @Parameter(description = "Ne garder que le troupeau de cet eleveur", example = "1")
            @RequestParam(required = false) Long eleveurId,

            @Parameter(description = "Ne garder que les animaux de cet enclos", example = "3")
            @RequestParam(required = false) String enclos) {
        return animalService.lister(espece, etat, eleveurId, enclos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer un animal par son identifiant")
    @ApiResponse(responseCode = "200", description = "Fiche de l'animal")
    public AnimalResponse recupereAnimalParId(
            @Parameter(description = "Identifiant de l'animal", example = "1") @PathVariable Long id) {
        return animalService.recupereParId(id);
    }

    @PatchMapping("/{id}/enclos")
    @Operation(summary = "Deplacer un animal dans un autre enclos",
            description = "Refuse si l'animal n'est plus a la ferme (vendu, mort ou disparu).")
    @ApiResponse(responseCode = "200", description = "Animal deplace")
    public AnimalResponse changerEnclos(
            @Parameter(description = "Identifiant de l'animal", example = "1") @PathVariable Long id,
            @Valid @RequestBody MajEnclosRequest requete) {
        return animalService.changerEnclos(id, requete.enclos());
    }

    @PatchMapping("/{id}/etat")
    @Operation(summary = "Declarer un animal mort ou disparu",
            description = """
                    Seuls `MORT` et `DISPARU` sont acceptes ici : la vente passe par l'eleveur \
                    et un animal deja sorti du cheptel ne peut plus changer d'etat.""")
    @ApiResponse(responseCode = "200", description = "Etat mis a jour")
    public AnimalResponse changerEtat(
            @Parameter(description = "Identifiant de l'animal", example = "1") @PathVariable Long id,
            @Valid @RequestBody MajEtatRequest requete) {
        return animalService.changerEtat(id, requete.etat());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Retirer definitivement un animal du registre",
            description = "Efface l'animal de la base. Pour tracer une sortie, preferer PATCH /etat.")
    @ApiResponse(responseCode = "204", description = "Animal supprime", content = @Content)
    public ResponseEntity<Void> supprimerAnimal(
            @Parameter(description = "Identifiant de l'animal", example = "1") @PathVariable Long id) {
        animalService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}

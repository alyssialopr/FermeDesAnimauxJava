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
import laFerme.dto.ActionResponse;
import laFerme.dto.ClassementResponse;
import laFerme.dto.CreerEleveurRequest;
import laFerme.dto.EleveurCreeResponse;
import laFerme.dto.EleveurResponse;
import laFerme.dto.ErreurApi;
import laFerme.dto.MouvementResponse;
import laFerme.services.EleveurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/eleveurs")
@RequiredArgsConstructor
@Tag(name = "Eleveurs", description = """
        Les eleveurs et leurs actions sur le cheptel : achat, vente, repas, soin, \
        balade et recolte. Un eleveur n'agit que sur ses propres animaux.""")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Requete invalide : champ manquant ou corps illisible",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ErreurApi.class),
                        examples = @ExampleObject(name = "Validation", value = """
                                {
                                  "title": "Requete invalide",
                                  "status": 400,
                                  "detail": "Certains champs sont invalides.",
                                  "instance": "/api/eleveurs",
                                  "horodatage": "2026-09-18T07:27:13.637Z",
                                  "champs": {"prenom": "le prenom est obligatoire"}
                                }"""))),
        @ApiResponse(responseCode = "404", description = "Eleveur ou animal inconnu",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ErreurApi.class),
                        examples = @ExampleObject(name = "Introuvable", value = """
                                {
                                  "title": "Ressource introuvable",
                                  "status": 404,
                                  "detail": "Eleveur introuvable : 42",
                                  "instance": "/api/eleveurs/42",
                                  "horodatage": "2026-09-18T07:27:13.544Z"
                                }"""))),
        @ApiResponse(responseCode = "409",
                description = """
                        Regle metier : animal appartenant a un autre eleveur, animal vendu/mort/disparu, \
                        prenom deja pris, ou suppression d'un eleveur dont le troupeau n'est pas vide""",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ErreurApi.class),
                        examples = {
                                @ExampleObject(name = "Animal d'un autre eleveur", value = """
                                        {
                                          "title": "Animal non possede",
                                          "status": 409,
                                          "detail": "Cet animal ne vous appartient pas",
                                          "instance": "/api/eleveurs/2/animaux/1/repas",
                                          "horodatage": "2026-09-18T07:27:13.445Z"
                                        }"""),
                                @ExampleObject(name = "Animal indisponible", value = """
                                        {
                                          "title": "Action impossible",
                                          "status": 409,
                                          "detail": "Impossible de nourrir la vache emily : l'animal est VENDU.",
                                          "instance": "/api/eleveurs/1/animaux/2/repas",
                                          "horodatage": "2026-09-18T07:27:13.527Z"
                                        }""")}))
})
public class EleveurController {

    private final EleveurService eleveurService;

    @PostMapping
    @Operation(summary = "Creer un eleveur",
            description = """
                    Le prenom doit etre unique a la ferme. La reponse contient la **cle \
                    d'acces** de l'eleveur : c'est la seule fois qu'elle est renvoyee, le \
                    serveur n'en garde qu'une empreinte BCrypt. Elle sert ensuite a \
                    s'authentifier (`Authorization: Bearer <id>.<cle>`).""")
    @ApiResponse(responseCode = "201", description = "Eleveur cree, avec sa cle d'acces")
    public ResponseEntity<EleveurCreeResponse> creerEleveur(@Valid @RequestBody CreerEleveurRequest requete) {
        EleveurCreeResponse cree = eleveurService.creer(requete);
        return ResponseEntity.created(URI.create("/api/eleveurs/" + cree.eleveur().id())).body(cree);
    }

    @GetMapping
    @Operation(summary = "Lister les eleveurs",
            description = "Version resumee : le champ `animaux` est null, seul `nombreAnimaux` est renseigne.")
    @ApiResponse(responseCode = "200", description = "Eleveurs de la ferme, tries par prenom")
    public List<EleveurResponse> recupereEleveurs() {
        return eleveurService.lister();
    }

    @GetMapping("/classement")
    @Operation(summary = "Classer les eleveurs par fortune",
            description = "La fortune, c'est l'argent en caisse plus la valeur de revente du troupeau.")
    @ApiResponse(responseCode = "200", description = "Classement, du plus riche au plus pauvre")
    public List<ClassementResponse> recupereClassement() {
        return eleveurService.classement();
    }

    @GetMapping("/{id}/mouvements")
    @PreAuthorize("@securite.estEleveur(#id)")
    @Operation(summary = "Consulter le releve de compte d'un eleveur",
            description = """
                    Les 50 dernieres operations (achats, ventes, repas, soins, recoltes, \
                    promenades), de la plus recente a la plus ancienne.""")
    @ApiResponse(responseCode = "200", description = "Releve de compte")
    public List<MouvementResponse> recupereMouvements(
            @Parameter(description = "Identifiant de l'eleveur", example = "1") @PathVariable Long id) {
        return eleveurService.mouvements(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer un eleveur et son troupeau")
    @ApiResponse(responseCode = "200", description = "Fiche de l'eleveur, troupeau detaille inclus")
    public EleveurResponse recupereEleveurParId(
            @Parameter(description = "Identifiant de l'eleveur", example = "1") @PathVariable Long id) {
        return eleveurService.recupereParId(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@securite.estEleveur(#id)")
    @Operation(summary = "Supprimer un eleveur",
            description = "Refuse tant que l'eleveur possede des animaux : il faut d'abord les vendre.")
    @ApiResponse(responseCode = "204", description = "Eleveur supprime", content = @Content)
    public ResponseEntity<Void> supprimerEleveur(
            @Parameter(description = "Identifiant de l'eleveur", example = "1") @PathVariable Long id) {
        eleveurService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------
    // Actions : ce sont les methodes de l'Eleveur d'origine, exposees en HTTP
    // -----------------------------------------------------------------

    @PostMapping("/{eleveurId}/animaux/{animalId}/achat")
    @PreAuthorize("@securite.estEleveur(#eleveurId)")
    @Operation(summary = "Acheter un animal",
            description = """
                    Rattache l'animal a l'eleveur et le remet a l'etat `LIBRE`. Refuse si \
                    l'animal appartient deja a quelqu'un d'autre, ou s'il est mort ou disparu.""")
    @ApiResponse(responseCode = "200", description = "Animal achete",
            content = @Content(schema = @Schema(implementation = ActionResponse.class),
                    examples = @ExampleObject(value = """
                            {"message": "killian a achete la poule caramel.", "animal": {"id": 5, "etat": "LIBRE"}}""")))
    public ActionResponse acheter(
            @Parameter(description = "Identifiant de l'acheteur", example = "2") @PathVariable Long eleveurId,
            @Parameter(description = "Identifiant de l'animal", example = "5") @PathVariable Long animalId) {
        return eleveurService.acheter(eleveurId, animalId);
    }

    @PostMapping("/{eleveurId}/animaux/{animalId}/vente")
    @PreAuthorize("@securite.estEleveur(#eleveurId)")
    @Operation(summary = "Vendre un de ses animaux",
            description = "L'animal passe a l'etat `VENDU` et quitte le troupeau.")
    @ApiResponse(responseCode = "200", description = "Animal vendu",
            content = @Content(schema = @Schema(implementation = ActionResponse.class),
                    examples = @ExampleObject(value = """
                            {"message": "La vache emily a ete vendue.", "animal": {"id": 1, "etat": "VENDU"}}""")))
    public ActionResponse vendre(
            @Parameter(description = "Identifiant du proprietaire", example = "1") @PathVariable Long eleveurId,
            @Parameter(description = "Identifiant de l'animal", example = "1") @PathVariable Long animalId) {
        return eleveurService.vendre(eleveurId, animalId);
    }

    @PostMapping("/{eleveurId}/animaux/{animalId}/repas")
    @PreAuthorize("@securite.estEleveur(#eleveurId)")
    @Operation(summary = "Nourrir un de ses animaux")
    @ApiResponse(responseCode = "200", description = "Animal nourri",
            content = @Content(schema = @Schema(implementation = ActionResponse.class),
                    examples = @ExampleObject(value = """
                            {"message": "La vache emily a ete nourrie.", "animal": {"id": 1, "etat": "LIBRE"}}""")))
    public ActionResponse nourrir(
            @Parameter(description = "Identifiant du proprietaire", example = "1") @PathVariable Long eleveurId,
            @Parameter(description = "Identifiant de l'animal", example = "1") @PathVariable Long animalId) {
        return eleveurService.nourrir(eleveurId, animalId);
    }

    @PostMapping("/{eleveurId}/animaux/{animalId}/soin")
    @PreAuthorize("@securite.estEleveur(#eleveurId)")
    @Operation(summary = "Soigner un de ses animaux")
    @ApiResponse(responseCode = "200", description = "Animal soigne",
            content = @Content(schema = @Schema(implementation = ActionResponse.class),
                    examples = @ExampleObject(value = """
                            {"message": "La poule nugget a ete soignee.", "animal": {"id": 3, "etat": "LIBRE"}}""")))
    public ActionResponse soigner(
            @Parameter(description = "Identifiant du proprietaire", example = "2") @PathVariable Long eleveurId,
            @Parameter(description = "Identifiant de l'animal", example = "3") @PathVariable Long animalId) {
        return eleveurService.soigner(eleveurId, animalId);
    }

    @PostMapping("/{eleveurId}/animaux/{animalId}/balade")
    @PreAuthorize("@securite.estEleveur(#eleveurId)")
    @Operation(summary = "Emmener un de ses animaux en balade")
    @ApiResponse(responseCode = "200", description = "Animal parti en balade",
            content = @Content(schema = @Schema(implementation = ActionResponse.class),
                    examples = @ExampleObject(value = """
                            {"message": "La vache emily part en balade.", "animal": {"id": 1, "etat": "LIBRE"}}""")))
    public ActionResponse promener(
            @Parameter(description = "Identifiant du proprietaire", example = "1") @PathVariable Long eleveurId,
            @Parameter(description = "Identifiant de l'animal", example = "1") @PathVariable Long animalId) {
        return eleveurService.promener(eleveurId, animalId);
    }

    @PostMapping("/{eleveurId}/animaux/{animalId}/recolte")
    @PreAuthorize("@securite.estEleveur(#eleveurId)")
    @Operation(summary = "Recolter la production d'un de ses animaux",
            description = "Du lait pour une vache, des oeufs pour une poule.")
    @ApiResponse(responseCode = "200", description = "Production recoltee",
            content = @Content(schema = @Schema(implementation = ActionResponse.class),
                    examples = @ExampleObject(value = """
                            {"message": "La poule nugget a pondu 5 oeufs cette semaine.", \
                            "animal": {"id": 3, "etat": "LIBRE"}}""")))
    public ActionResponse recolter(
            @Parameter(description = "Identifiant du proprietaire", example = "2") @PathVariable Long eleveurId,
            @Parameter(description = "Identifiant de l'animal", example = "3") @PathVariable Long animalId) {
        return eleveurService.recolter(eleveurId, animalId);
    }
}

package laFerme.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import laFerme.dto.ActionResponse;
import laFerme.dto.CreerEleveurRequest;
import laFerme.dto.EleveurResponse;
import laFerme.services.EleveurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
@Tag(name = "Eleveurs", description = "Les eleveurs et leurs actions sur le cheptel")
public class EleveurController {

    private final EleveurService eleveurService;

    @PostMapping
    @Operation(summary = "Creer un eleveur")
    public ResponseEntity<EleveurResponse> creerEleveur(@Valid @RequestBody CreerEleveurRequest requete) {
        EleveurResponse eleveur = eleveurService.creer(requete);
        return ResponseEntity.created(URI.create("/api/eleveurs/" + eleveur.id())).body(eleveur);
    }

    @GetMapping
    @Operation(summary = "Lister les eleveurs")
    public List<EleveurResponse> recupereEleveurs() {
        return eleveurService.lister();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer un eleveur et son troupeau")
    public EleveurResponse recupereEleveurParId(@PathVariable Long id) {
        return eleveurService.recupereParId(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un eleveur (son troupeau doit etre vide)")
    public ResponseEntity<Void> supprimerEleveur(@PathVariable Long id) {
        eleveurService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------
    // Actions : ce sont les methodes de l'Eleveur d'origine, exposees en HTTP
    // -----------------------------------------------------------------

    @PostMapping("/{eleveurId}/animaux/{animalId}/achat")
    @Operation(summary = "Acheter un animal")
    public ActionResponse acheter(@PathVariable Long eleveurId, @PathVariable Long animalId) {
        return eleveurService.acheter(eleveurId, animalId);
    }

    @PostMapping("/{eleveurId}/animaux/{animalId}/vente")
    @Operation(summary = "Vendre un de ses animaux")
    public ActionResponse vendre(@PathVariable Long eleveurId, @PathVariable Long animalId) {
        return eleveurService.vendre(eleveurId, animalId);
    }

    @PostMapping("/{eleveurId}/animaux/{animalId}/repas")
    @Operation(summary = "Nourrir un de ses animaux")
    public ActionResponse nourrir(@PathVariable Long eleveurId, @PathVariable Long animalId) {
        return eleveurService.nourrir(eleveurId, animalId);
    }

    @PostMapping("/{eleveurId}/animaux/{animalId}/soin")
    @Operation(summary = "Soigner un de ses animaux")
    public ActionResponse soigner(@PathVariable Long eleveurId, @PathVariable Long animalId) {
        return eleveurService.soigner(eleveurId, animalId);
    }

    @PostMapping("/{eleveurId}/animaux/{animalId}/balade")
    @Operation(summary = "Emmener un de ses animaux en balade")
    public ActionResponse promener(@PathVariable Long eleveurId, @PathVariable Long animalId) {
        return eleveurService.promener(eleveurId, animalId);
    }

    @PostMapping("/{eleveurId}/animaux/{animalId}/recolte")
    @Operation(summary = "Recolter la production d'un de ses animaux (lait, oeufs)")
    public ActionResponse recolter(@PathVariable Long eleveurId, @PathVariable Long animalId) {
        return eleveurService.recolter(eleveurId, animalId);
    }
}

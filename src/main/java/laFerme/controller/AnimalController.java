package laFerme.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import laFerme.dto.AnimalResponse;
import laFerme.dto.CreerAnimalRequest;
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
@Tag(name = "Animaux", description = "Le cheptel de la ferme")
public class AnimalController {

    private final AnimalService animalService;

    @PostMapping
    @Operation(summary = "Faire entrer un animal a la ferme")
    public ResponseEntity<AnimalResponse> creerAnimal(@Valid @RequestBody CreerAnimalRequest requete) {
        AnimalResponse animal = animalService.creer(requete);
        return ResponseEntity.created(URI.create("/api/animaux/" + animal.id())).body(animal);
    }

    @GetMapping
    @Operation(summary = "Lister les animaux, avec filtres optionnels")
    public List<AnimalResponse> recupereAnimaux(
            @RequestParam(required = false) Espece espece,
            @RequestParam(required = false) EtatAnimal etat,
            @RequestParam(required = false) Long eleveurId,
            @RequestParam(required = false) String enclos) {
        return animalService.lister(espece, etat, eleveurId, enclos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer un animal par son identifiant")
    public AnimalResponse recupereAnimalParId(@PathVariable Long id) {
        return animalService.recupereParId(id);
    }

    @PatchMapping("/{id}/enclos")
    @Operation(summary = "Deplacer un animal dans un autre enclos")
    public AnimalResponse changerEnclos(@PathVariable Long id, @Valid @RequestBody MajEnclosRequest requete) {
        return animalService.changerEnclos(id, requete.enclos());
    }

    @PatchMapping("/{id}/etat")
    @Operation(summary = "Declarer un animal mort ou disparu")
    public AnimalResponse changerEtat(@PathVariable Long id, @Valid @RequestBody MajEtatRequest requete) {
        return animalService.changerEtat(id, requete.etat());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Retirer definitivement un animal du registre")
    public ResponseEntity<Void> supprimerAnimal(@PathVariable Long id) {
        animalService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}

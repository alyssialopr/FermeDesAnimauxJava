package laFerme.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import laFerme.dto.EspeceResponse;
import laFerme.services.CatalogueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/especes")
@RequiredArgsConstructor
@Tag(name = "Catalogue", description = """
        Le catalogue du marche : prix d'achat, production, tarifs des repas et des \
        soins pour chaque espece elevee a la ferme.""")
public class CatalogueController {

    private final CatalogueService catalogueService;

    @GetMapping
    @Operation(summary = "Lister les especes et leurs tarifs",
            description = "Triees par prix croissant. C'est ce qui alimente le marche du jeu.")
    @ApiResponse(responseCode = "200", description = "Catalogue des especes")
    public List<EspeceResponse> recupereEspeces() {
        return catalogueService.especes();
    }
}

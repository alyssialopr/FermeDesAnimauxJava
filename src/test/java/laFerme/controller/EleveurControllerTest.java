package laFerme.controller;

import laFerme.dto.ActionResponse;
import laFerme.dto.AnimalResponse;
import laFerme.dto.EleveurCreeResponse;
import laFerme.dto.EleveurResponse;
import laFerme.exception.AnimalNonPossedeException;
import laFerme.model.Espece;
import laFerme.model.EtatAnimal;
import laFerme.model.Production;
import laFerme.services.EleveurService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Ces tests verifient le contrat HTTP du controleur ; l'authentification, elle,
// est couverte de bout en bout par FermeApplicationTests sur la vraie chaine.
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(EleveurController.class)
class EleveurControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EleveurService eleveurService;

    private AnimalResponse emily() {
        return new AnimalResponse(1L, Espece.VACHE, "emily", "Highland", "marron", "1",
                EtatAnimal.LIBRE, 1L, "alyssia",
                new BigDecimal("230.00"), new BigDecimal("207.00"),
                Production.LAIT, "litres de lait", 18, new BigDecimal("27.00"),
                12, 100, true, 0L, "Meuh !", Instant.parse("2026-01-01T10:00:00Z"));
    }

    @Test
    @DisplayName("GET /api/eleveurs renvoie les eleveurs")
    void listerLesEleveurs() throws Exception {
        given(eleveurService.lister()).willReturn(List.of(new EleveurResponse(
                1L, "alyssia", new BigDecimal("248.60"), new BigDecimal("662.60"), 2, null)));

        mockMvc.perform(get("/api/eleveurs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].prenom").value("alyssia"))
                .andExpect(jsonPath("$[0].nombreAnimaux").value(2))
                .andExpect(jsonPath("$[0].solde").value(248.60));
    }

    @Test
    @DisplayName("nourrir un animal renvoie le message de l'action")
    void nourrir() throws Exception {
        given(eleveurService.nourrir(1L, 1L)).willReturn(new ActionResponse(
                "La vache emily a ete nourrie. (fourrage : 4.00 €)",
                new BigDecimal("-4.00"), new BigDecimal("244.60"), emily()));

        mockMvc.perform(post("/api/eleveurs/1/animaux/1/repas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("La vache emily a ete nourrie. (fourrage : 4.00 €)"))
                .andExpect(jsonPath("$.montant").value(-4.00))
                .andExpect(jsonPath("$.solde").value(244.60))
                .andExpect(jsonPath("$.animal.nom").value("emily"));
    }

    @Test
    @DisplayName("agir sur l'animal d'un autre renvoie 409")
    void animalDUnAutre() throws Exception {
        given(eleveurService.nourrir(2L, 1L)).willThrow(new AnimalNonPossedeException());

        mockMvc.perform(post("/api/eleveurs/2/animaux/1/repas"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Animal non possede"))
                .andExpect(jsonPath("$.detail").value("Cet animal ne vous appartient pas"));
    }

    @Test
    @DisplayName("creer un eleveur renvoie sa cle d'acces, une seule fois")
    void creationRenvoieLaCle() throws Exception {
        given(eleveurService.creer(org.mockito.ArgumentMatchers.any())).willReturn(new EleveurCreeResponse(
                new EleveurResponse(3L, "camille", new BigDecimal("300.00"), new BigDecimal("300.00"), 0, List.of()),
                "f47ac10b-58cc-4372-a567-0e02b2c3d479"));

        mockMvc.perform(post("/api/eleveurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prenom":"camille"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/eleveurs/3"))
                .andExpect(jsonPath("$.eleveur.prenom").value("camille"))
                .andExpect(jsonPath("$.cle").value("f47ac10b-58cc-4372-a567-0e02b2c3d479"));
    }

    @Test
    @DisplayName("creer un eleveur sans prenom renvoie 400")
    void prenomObligatoire() throws Exception {
        mockMvc.perform(post("/api/eleveurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prenom":"  "}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.prenom").exists());
    }
}

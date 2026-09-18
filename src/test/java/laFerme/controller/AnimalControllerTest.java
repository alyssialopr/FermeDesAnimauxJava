package laFerme.controller;

import laFerme.dto.AnimalResponse;
import laFerme.exception.ActionImpossibleException;
import laFerme.exception.RessourceIntrouvableException;
import laFerme.model.Espece;
import laFerme.model.EtatAnimal;
import laFerme.model.Production;
import laFerme.services.AnimalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnimalController.class)
class AnimalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnimalService animalService;

    private AnimalResponse emily() {
        return new AnimalResponse(1L, Espece.VACHE, "emily", "Highland", "marron", "1",
                EtatAnimal.LIBRE, 1L, "alyssia",
                new BigDecimal("230.00"), new BigDecimal("207.00"),
                Production.LAIT, "litres de lait", 18, new BigDecimal("27.00"),
                12, 100, true, 0L, Instant.parse("2026-01-01T10:00:00Z"));
    }

    @Test
    @DisplayName("GET /api/animaux renvoie le cheptel")
    void listerLesAnimaux() throws Exception {
        given(animalService.lister(null, null, null, null)).willReturn(List.of(emily()));

        mockMvc.perform(get("/api/animaux"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("emily"))
                .andExpect(jsonPath("$[0].espece").value("VACHE"))
                .andExpect(jsonPath("$[0].quantiteProduction").value(18))
                .andExpect(jsonPath("$[0].prix").value(230.00))
                .andExpect(jsonPath("$[0].peutEtreRecolte").value(true));
    }

    @Test
    @DisplayName("GET /api/animaux accepte les filtres")
    void filtrerLesAnimaux() throws Exception {
        given(animalService.lister(Espece.POULE, EtatAnimal.LIBRE, 2L, "3")).willReturn(List.of());

        mockMvc.perform(get("/api/animaux")
                        .param("espece", "POULE")
                        .param("etat", "LIBRE")
                        .param("eleveurId", "2")
                        .param("enclos", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("POST /api/animaux renvoie 201 et l'adresse de la ressource")
    void creerUnAnimal() throws Exception {
        given(animalService.creer(any())).willReturn(emily());

        mockMvc.perform(post("/api/animaux")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"espece":"VACHE","nom":"emily","race":"Highland","couleur":"marron","enclos":"1"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/animaux/1"))
                .andExpect(jsonPath("$.nom").value("emily"));
    }

    @Test
    @DisplayName("un animal inconnu renvoie un 404 normalise")
    void animalInconnu() throws Exception {
        given(animalService.recupereParId(42L)).willThrow(new RessourceIntrouvableException("Animal", 42L));

        mockMvc.perform(get("/api/animaux/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Ressource introuvable"))
                .andExpect(jsonPath("$.detail").value("Animal introuvable : 42"))
                .andExpect(jsonPath("$.instance").value("/api/animaux/42"));
    }

    @Test
    @DisplayName("les champs manquants sont detailles dans la reponse 400")
    void requeteInvalide() throws Exception {
        mockMvc.perform(post("/api/animaux")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"espece":null,"nom":"","race":"Highland","couleur":"marron","enclos":"1"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requete invalide"))
                .andExpect(jsonPath("$.champs.nom").exists())
                .andExpect(jsonPath("$.champs.espece").exists());
    }

    @Test
    @DisplayName("un etat inconnu dans le corps donne un 400")
    void etatInconnu() throws Exception {
        mockMvc.perform(patch("/api/animaux/1/etat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"etat":"EN_VACANCES"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requete invalide"));
    }

    @Test
    @DisplayName("DELETE /api/animaux/{id} renvoie 204")
    void supprimerUnAnimal() throws Exception {
        mockMvc.perform(delete("/api/animaux/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("une action impossible cote service remonte en 409")
    void suppressionImpossible() throws Exception {
        willThrow(new ActionImpossibleException("Impossible pour le moment."))
                .given(animalService).changerEtat(eq(1L), any());

        mockMvc.perform(patch("/api/animaux/1/etat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"etat":"MORT"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Action impossible"));
    }
}

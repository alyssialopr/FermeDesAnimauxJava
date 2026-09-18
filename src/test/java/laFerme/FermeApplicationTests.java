package laFerme;

import laFerme.model.EtatAnimal;
import laFerme.repository.AnimalRepository;
import laFerme.repository.EleveurRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de bout en bout sur un vrai PostgreSQL jetable : valide les migrations
 * Flyway, le mapping JPA (heritage compris) et l'enchainement des actions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class FermeApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EleveurRepository eleveurRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Test
    @DisplayName("le schema Flyway est applique et valide par Hibernate")
    void contexteEtSchema() {
        // Si le contexte demarre, c'est que Flyway a cree le schema et qu'Hibernate
        // l'a valide (ddl-auto=validate).
        assertThat(animalRepository.count()).isNotNegative();
        assertThat(eleveurRepository.count()).isNotNegative();
    }

    @Test
    @DisplayName("parcours complet : creation, achat, repas, recolte, vente")
    void parcoursComplet() throws Exception {
        String eleveur = mockMvc.perform(post("/api/eleveurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prenom":"mathilde"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prenom").value("mathilde"))
                .andReturn().getResponse().getContentAsString();
        long eleveurId = extraireId(eleveur);

        String animal = mockMvc.perform(post("/api/animaux")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"espece":"VACHE","nom":"blanchette","race":"Normande","couleur":"blanche",
                                 "enclos":"4","litresDeLaitParJour":22,"eleveurId":%d}""".formatted(eleveurId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eleveurPrenom").value("mathilde"))
                .andExpect(jsonPath("$.etat").value("LIBRE"))
                .andReturn().getResponse().getContentAsString();
        long animalId = extraireId(animal);

        mockMvc.perform(post("/api/eleveurs/%d/animaux/%d/repas".formatted(eleveurId, animalId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("La vache blanchette a ete nourrie."));

        mockMvc.perform(post("/api/eleveurs/%d/animaux/%d/recolte".formatted(eleveurId, animalId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("La vache blanchette a donne 22 litres de lait."));

        mockMvc.perform(post("/api/eleveurs/%d/animaux/%d/vente".formatted(eleveurId, animalId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animal.etat").value("VENDU"));

        // La vente est bien persistee
        assertThat(animalRepository.findById(animalId))
                .get()
                .satisfies(vendue -> {
                    assertThat(vendue.getEtat()).isEqualTo(EtatAnimal.VENDU);
                    assertThat(vendue.getEleveur()).isNull();
                });

        mockMvc.perform(get("/api/eleveurs/%d".formatted(eleveurId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreAnimaux").value(0));
    }

    @Test
    @DisplayName("un eleveur ne peut pas toucher a l'animal d'un autre, meme en base")
    void proprieteRespecteeEnBase() throws Exception {
        long premier = extraireId(mockMvc.perform(post("/api/eleveurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prenom":"gaspard"}"""))
                .andReturn().getResponse().getContentAsString());

        long second = extraireId(mockMvc.perform(post("/api/eleveurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prenom":"olivia"}"""))
                .andReturn().getResponse().getContentAsString());

        long poule = extraireId(mockMvc.perform(post("/api/animaux")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"espece":"POULE","nom":"cannelle","race":"Marans","couleur":"noire",
                                 "enclos":"5","oeufsParSemaine":4,"eleveurId":%d}""".formatted(premier)))
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(post("/api/eleveurs/%d/animaux/%d/soin".formatted(second, poule)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Cet animal ne vous appartient pas"));

        mockMvc.perform(get("/api/animaux/%d".formatted(poule)))
                .andExpect(jsonPath("$.eleveurPrenom").value("gaspard"))
                .andExpect(jsonPath("$.oeufsParSemaine").value(4));
    }

    @Test
    @DisplayName("un prenom d'eleveur deja pris est refuse")
    void prenomUnique() throws Exception {
        mockMvc.perform(post("/api/eleveurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prenom":"solene"}"""))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/eleveurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prenom":"Solene"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Action impossible"));
    }

    private long extraireId(String json) {
        int debut = json.indexOf("\"id\":") + 5;
        int fin = debut;
        while (fin < json.length() && Character.isDigit(json.charAt(fin))) {
            fin++;
        }
        return Long.parseLong(json.substring(debut, fin));
    }
}

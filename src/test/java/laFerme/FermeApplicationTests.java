package laFerme;

import laFerme.model.EtatAnimal;
import laFerme.repository.AnimalRepository;
import laFerme.repository.EleveurRepository;
import laFerme.repository.MouvementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de bout en bout sur un vrai PostgreSQL jetable : valide les migrations
 * Flyway, le mapping JPA (heritage compris), l'economie de la ferme et la
 * persistance du releve de compte.
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

    @Autowired
    private MouvementRepository mouvementRepository;

    @Test
    @DisplayName("le schema Flyway est applique et valide par Hibernate")
    void contexteEtSchema() {
        // Si le contexte demarre, c'est que Flyway a cree le schema et qu'Hibernate
        // l'a valide (ddl-auto=validate).
        assertThat(animalRepository.count()).isNotNegative();
        assertThat(eleveurRepository.count()).isNotNegative();
        assertThat(mouvementRepository.count()).isNotNegative();
    }

    @Test
    @DisplayName("le catalogue expose les neuf especes de la ferme")
    void catalogue() throws Exception {
        mockMvc.perform(get("/api/especes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$[?(@.espece == 'VACHE')].prix").value(org.hamcrest.Matchers.contains(230.00)))
                .andExpect(jsonPath("$[?(@.espece == 'CHEVAL')].gainParBalade")
                        .value(org.hamcrest.Matchers.contains(45.00)));
    }

    @Test
    @DisplayName("parcours complet : creation, achat, repas, recolte, vente, releve de compte")
    void parcoursComplet() throws Exception {
        String reponse = mockMvc.perform(post("/api/eleveurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prenom":"mathilde"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eleveur.prenom").value("mathilde"))
                .andExpect(jsonPath("$.eleveur.solde").value(300.00))
                .andExpect(jsonPath("$.cle").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        long eleveurId = extraireId(reponse);
        String jeton = jeton(eleveurId, extraireCle(reponse));

        // Achetee a l'arrivee : le prix de la chevre est debite.
        String animal = mockMvc.perform(post("/api/animaux")
                        .header(HttpHeaders.AUTHORIZATION, jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"espece":"CHEVRE","nom":"biquette","race":"Alpine","couleur":"marron",
                                 "enclos":"4","eleveurId":%d}""".formatted(eleveurId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eleveurPrenom").value("mathilde"))
                .andExpect(jsonPath("$.prix").value(60.00))
                .andExpect(jsonPath("$.production").value("LAIT"))
                .andExpect(jsonPath("$.quantiteProduction").value(4))
                .andReturn().getResponse().getContentAsString();
        long animalId = extraireId(animal);

        mockMvc.perform(get("/api/eleveurs/%d".formatted(eleveurId)))
                .andExpect(jsonPath("$.solde").value(240.00));

        // 4 litres a 1.80 € : la recolte rapporte 7.20 €.
        mockMvc.perform(post("/api/eleveurs/%d/animaux/%d/recolte".formatted(eleveurId, animalId))
                        .header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "La chevre biquette a donne 4 litres de lait. Vendu 7.20 €."))
                .andExpect(jsonPath("$.montant").value(7.20))
                .andExpect(jsonPath("$.solde").value(247.20))
                .andExpect(jsonPath("$.animal.sante").value(92));

        // Deuxieme recolte refusee : il faut attendre.
        mockMvc.perform(post("/api/eleveurs/%d/animaux/%d/recolte".formatted(eleveurId, animalId))
                        .header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("revenez dans")));

        mockMvc.perform(post("/api/eleveurs/%d/animaux/%d/vente".formatted(eleveurId, animalId))
                        .header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animal.etat").value("VENDU"))
                .andExpect(jsonPath("$.montant").value(54.00))
                .andExpect(jsonPath("$.solde").value(301.20));

        // La vente est bien persistee
        assertThat(animalRepository.findById(animalId))
                .get()
                .satisfies(vendue -> {
                    assertThat(vendue.getEtat()).isEqualTo(EtatAnimal.VENDU);
                    assertThat(vendue.getEleveur()).isNull();
                });

        assertThat(eleveurRepository.findById(eleveurId))
                .get()
                .satisfies(proprietaire -> assertThat(proprietaire.getSolde())
                        .isEqualByComparingTo(new BigDecimal("301.20")));

        // Le releve de compte garde la trace des trois operations.
        mockMvc.perform(get("/api/eleveurs/%d/mouvements".formatted(eleveurId))
                        .header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("VENTE"))
                .andExpect(jsonPath("$[1].type").value("RECOLTE"))
                .andExpect(jsonPath("$[2].type").value("ACHAT"))
                .andExpect(jsonPath("$[2].montant").value(-60.00));
    }

    @Test
    @DisplayName("on n'achete pas au-dessus de ses moyens, et rien n'est cree au passage")
    void fondsInsuffisants() throws Exception {
        String reponse = mockMvc.perform(post("/api/eleveurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prenom":"hugo"}"""))
                .andReturn().getResponse().getContentAsString();
        long eleveurId = extraireId(reponse);

        long avant = animalRepository.count();

        mockMvc.perform(post("/api/animaux")
                        .header(HttpHeaders.AUTHORIZATION, jeton(eleveurId, extraireCle(reponse)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"espece":"CHEVAL","nom":"tonnerre","race":"Comtois","couleur":"marron",
                                 "enclos":"6","eleveurId":%d}""".formatted(eleveurId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Fonds insuffisants"));

        // La transaction a ete annulee : l'animal n'existe pas.
        assertThat(animalRepository.count()).isEqualTo(avant);
    }

    @Test
    @DisplayName("un eleveur ne peut pas toucher a l'animal d'un autre, meme en base")
    void proprieteRespecteeEnBase() throws Exception {
        String premierCompte = mockMvc.perform(post("/api/eleveurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prenom":"gaspard"}"""))
                .andReturn().getResponse().getContentAsString();
        long premier = extraireId(premierCompte);
        String jetonPremier = jeton(premier, extraireCle(premierCompte));

        String secondCompte = mockMvc.perform(post("/api/eleveurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prenom":"olivia"}"""))
                .andReturn().getResponse().getContentAsString();
        long second = extraireId(secondCompte);
        String jetonSecond = jeton(second, extraireCle(secondCompte));

        long poule = extraireId(mockMvc.perform(post("/api/animaux")
                        .header(HttpHeaders.AUTHORIZATION, jetonPremier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"espece":"POULE","nom":"cannelle","race":"Marans","couleur":"noire",
                                 "enclos":"5","quantiteProduction":4,"eleveurId":%d}""".formatted(premier)))
                .andReturn().getResponse().getContentAsString());

        // Olivia s'authentifie bien, mais agit au nom de gaspard : refuse.
        mockMvc.perform(post("/api/eleveurs/%d/animaux/%d/soin".formatted(premier, poule))
                        .header(HttpHeaders.AUTHORIZATION, jetonSecond))
                .andExpect(status().isForbidden());

        // Avec sa propre cle, olivia ne possede simplement pas cet animal.
        mockMvc.perform(post("/api/eleveurs/%d/animaux/%d/soin".formatted(second, poule))
                        .header(HttpHeaders.AUTHORIZATION, jetonSecond))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Cet animal ne vous appartient pas"));

        mockMvc.perform(get("/api/animaux/%d".formatted(poule)))
                .andExpect(jsonPath("$.eleveurPrenom").value("gaspard"))
                .andExpect(jsonPath("$.quantiteProduction").value(4));
    }

    @Test
    @DisplayName("le classement range les eleveurs par fortune")
    void classement() throws Exception {
        mockMvc.perform(post("/api/eleveurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prenom":"zoe"}"""))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/eleveurs/classement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rang").value(1))
                .andExpect(jsonPath("$[0].fortune").isNumber());
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

    @Test
    @DisplayName("sans cle, aucune action n'est possible")
    void sansCleAucuneAction() throws Exception {
        String compte = mockMvc.perform(post("/api/eleveurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prenom":"leonie"}"""))
                .andReturn().getResponse().getContentAsString();
        long eleveurId = extraireId(compte);

        // Lecture : ouverte.
        mockMvc.perform(get("/api/eleveurs/%d".formatted(eleveurId)))
                .andExpect(status().isOk());

        // Ecriture : refusee, et l'erreur reste au format de l'API.
        mockMvc.perform(post("/api/animaux")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"espece":"POULE","nom":"sansfoi","race":"Sussex","couleur":"noire",
                                 "enclos":"9","eleveurId":%d}""".formatted(eleveurId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentification requise"));

        // Le releve de compte est prive.
        mockMvc.perform(get("/api/eleveurs/%d/mouvements".formatted(eleveurId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("une mauvaise cle ne passe pas")
    void mauvaiseCle() throws Exception {
        String compte = mockMvc.perform(post("/api/eleveurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prenom":"victor"}"""))
                .andReturn().getResponse().getContentAsString();
        long eleveurId = extraireId(compte);

        mockMvc.perform(get("/api/eleveurs/%d/mouvements".formatted(eleveurId))
                        .header(HttpHeaders.AUTHORIZATION, jeton(eleveurId, "pas-la-bonne-cle")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("la cle n'est jamais renvoyee apres la creation")
    void laCleNeFuitPas() throws Exception {
        String compte = mockMvc.perform(post("/api/eleveurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prenom":"norbert"}"""))
                .andReturn().getResponse().getContentAsString();
        long eleveurId = extraireId(compte);

        String fiche = mockMvc.perform(get("/api/eleveurs/%d".formatted(eleveurId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(fiche).doesNotContain("cle").doesNotContain("$2a$");
    }

    @Test
    @DisplayName("les entrees farfelues sont refusees avant d'atteindre la base")
    void entreesRefusees() throws Exception {
        String compte = mockMvc.perform(post("/api/eleveurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prenom":"amelie"}"""))
                .andReturn().getResponse().getContentAsString();
        long eleveurId = extraireId(compte);

        mockMvc.perform(post("/api/animaux")
                        .header(HttpHeaders.AUTHORIZATION, jeton(eleveurId, extraireCle(compte)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"espece":"POULE","nom":"<script>alert(1)</script>","race":"Sussex",
                                 "couleur":"noire","enclos":"9"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.nom").exists());
    }

    private String jeton(long eleveurId, String cle) {
        return "Bearer %d.%s".formatted(eleveurId, cle);
    }

    private String extraireCle(String json) {
        int debut = json.indexOf("\"cle\":\"") + 7;
        return json.substring(debut, json.indexOf('"', debut));
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

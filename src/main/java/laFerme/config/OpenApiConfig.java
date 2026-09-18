package laFerme.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int port;

    @Bean
    OpenAPI apiDeLaFerme() {
        return new OpenAPI()
                .info(info())
                .servers(List.of(new Server()
                        .url("http://localhost:" + port)
                        .description("Environnement local (docker compose)")))
                // Les tags sont decrits sur les controleurs eux-memes (@Tag), pour
                // eviter de dupliquer la description a deux endroits.
                .externalDocs(new ExternalDocumentation()
                        .description("Code source et documentation du projet")
                        .url("https://github.com/alyssialopr/FermeDesAnimauxJava"));
    }

    private Info info() {
        return new Info()
                .title("API Ferme des animaux")
                .version("1.0.0")
                .description("""
                        Gestion d'une ferme : des **eleveurs** achetent, vendent, nourrissent, soignent \
                        et promenent des **vaches** et des **poules**.

                        ### Principes

                        - un animal peut arriver a la ferme sans proprietaire, puis etre achete ;
                        - un eleveur ne peut agir que sur ses propres animaux, sinon la reponse est \
                        `409` avec le message *Cet animal ne vous appartient pas* ;
                        - un animal `VENDU`, `MORT` ou `DISPARU` n'accepte plus aucune action ;
                        - chaque action renvoie le message metier correspondant ainsi que l'etat de \
                        l'animal apres l'action.

                        ### Erreurs

                        Toutes les erreurs suivent la RFC 7807 (`application/problem+json`) : `title`, \
                        `status`, `detail`, `instance`, `horodatage`, et `champs` pour les erreurs de \
                        validation.""")
                .contact(new Contact()
                        .name("Ferme des animaux - IIM")
                        .url("https://github.com/alyssialopr/FermeDesAnimauxJava"))
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT"));
    }
}

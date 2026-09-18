package laFerme.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI apiDeLaFerme() {
        return new OpenAPI().info(new Info()
                .title("API Ferme des animaux")
                .version("1.0.0")
                .description("""
                        Gestion d'une ferme : des eleveurs achetent, vendent, nourrissent, soignent \
                        et promenent des vaches et des poules. Documentation interactive : /swagger-ui.html"""));
    }
}

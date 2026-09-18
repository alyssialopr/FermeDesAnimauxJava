package laFerme.config;

import laFerme.model.Eleveur;
import laFerme.model.Poule;
import laFerme.model.Vache;
import laFerme.repository.AnimalRepository;
import laFerme.repository.EleveurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Jeu de donnees de demarrage : c'est l'ancien {@code Main.java} de la version console,
 * rejoue une seule fois si la base est vide. Desactivable via {@code ferme.donnees-demo=false}.
 */
@Component
@ConditionalOnProperty(name = "ferme.donnees-demo", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DonneesDemo implements ApplicationRunner {

    private final EleveurRepository eleveurRepository;
    private final AnimalRepository animalRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (eleveurRepository.count() > 0 || animalRepository.count() > 0) {
            log.info("La ferme contient deja des donnees, le jeu de demonstration est ignore.");
            return;
        }

        var emily = new Vache("emily", "Highland", "marron", "1");
        var marguerite = new Vache("marguerite", "Charolaise", "blanche", "2");
        var nugget = new Poule("nugget", "suisse", "orange", "3");
        var plume = new Poule("plume", "francais", "blanche", "3");

        var alyssia = new Eleveur("alyssia");
        var killian = new Eleveur("killian");

        killian.acheter(nugget);
        killian.acheter(plume);

        alyssia.acheter(emily);
        alyssia.acheter(marguerite);

        eleveurRepository.saveAll(List.of(alyssia, killian));
        log.info("Jeu de demonstration installe : {} eleveurs, {} animaux",
                eleveurRepository.count(), animalRepository.count());
    }
}

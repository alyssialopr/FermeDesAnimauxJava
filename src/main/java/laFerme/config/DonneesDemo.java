package laFerme.config;

import laFerme.model.Animal;
import laFerme.model.Eleveur;
import laFerme.model.Espece;
import laFerme.repository.AnimalRepository;
import laFerme.repository.EleveurRepository;
import laFerme.utils.AnimalMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Jeu de donnees de demarrage, rejoue une seule fois si la base est vide :
 * les eleveurs et animaux de l'ancien {@code Main.java}, plus un marche garni
 * pour avoir de quoi acheter des le premier lancement.
 * Desactivable via {@code ferme.donnees-demo=false}.
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

        var alyssia = new Eleveur("alyssia");
        var killian = new Eleveur("killian");

        // Troupeaux d'origine : ces animaux sont deja a eux, rien n'est debite.
        installer(alyssia, Espece.VACHE, "emily", "Highland", "marron", "1");
        installer(alyssia, Espece.VACHE, "marguerite", "Charolaise", "blanche", "2");
        installer(killian, Espece.POULE, "nugget", "suisse", "orange", "3");
        installer(killian, Espece.POULE, "plume", "francais", "blanche", "3");

        eleveurRepository.saveAll(List.of(alyssia, killian));

        // Le marche : des animaux sans proprietaire, a vendre.
        animalRepository.saveAll(List.of(
                creer(Espece.POULE, "caramel", "Marans", "noire", "4"),
                creer(Espece.POULE, "biscotte", "Sussex", "blanche", "4"),
                creer(Espece.LAPIN, "pompon", "Angora", "grise", "4"),
                creer(Espece.CANARD, "colvert", "Rouen", "grise", "4"),
                creer(Espece.CHEVRE, "biquette", "Alpine", "marron", "5"),
                creer(Espece.MOUTON, "nuage", "Merinos", "blanche", "5"),
                creer(Espece.OIE, "blanchette", "Toulouse", "blanche", "5"),
                creer(Espece.COCHON, "truffe", "Large White", "rousse", "6"),
                creer(Espece.CHEVAL, "tonnerre", "Comtois", "marron", "6")));

        log.info("Jeu de demonstration installe : {} eleveurs, {} animaux dont {} au marche",
                eleveurRepository.count(), animalRepository.count(), 9);
    }

    private void installer(Eleveur eleveur, Espece espece, String nom, String race, String couleur, String enclos) {
        Animal animal = creer(espece, nom, race, couleur, enclos);
        animal.setEleveur(eleveur);
        eleveur.getAnimaux().add(animal);
    }

    private Animal creer(Espece espece, String nom, String race, String couleur, String enclos) {
        return AnimalMapper.nouvelAnimal(espece, nom, race, couleur, enclos);
    }
}

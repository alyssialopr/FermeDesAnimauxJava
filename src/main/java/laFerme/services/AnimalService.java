package laFerme.services;

import laFerme.dto.AnimalResponse;
import laFerme.dto.CreerAnimalRequest;
import laFerme.exception.ActionImpossibleException;
import laFerme.exception.RessourceIntrouvableException;
import laFerme.model.Animal;
import laFerme.model.Eleveur;
import laFerme.model.Espece;
import laFerme.model.EtatAnimal;
import laFerme.repository.AnimalRepository;
import laFerme.repository.EleveurRepository;
import laFerme.utils.AnimalMapper;
import laFerme.utils.AnimalSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final EleveurRepository eleveurRepository;

    public List<AnimalResponse> lister(Espece espece, EtatAnimal etat, Long eleveurId, String enclos) {
        Specification<Animal> criteres = Specification.allOf(
                AnimalSpecifications.espece(espece),
                AnimalSpecifications.etat(etat),
                AnimalSpecifications.eleveur(eleveurId),
                AnimalSpecifications.enclos(enclos));

        return animalRepository.findAll(criteres, Sort.by("nom")).stream()
                .map(AnimalMapper::versReponse)
                .toList();
    }

    public AnimalResponse recupereParId(Long id) {
        return AnimalMapper.versReponse(exigerAnimal(id));
    }

    @Transactional
    public AnimalResponse creer(CreerAnimalRequest requete) {
        Animal animal = AnimalMapper.versEntite(requete);

        if (requete.eleveurId() != null) {
            Eleveur eleveur = eleveurRepository.findById(requete.eleveurId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Eleveur", requete.eleveurId()));
            eleveur.acheter(animal);
        }

        Animal enregistre = animalRepository.save(animal);
        log.info("Nouvel animal a la ferme : {}", enregistre);
        return AnimalMapper.versReponse(enregistre);
    }

    @Transactional
    public AnimalResponse changerEnclos(Long id, String enclos) {
        Animal animal = exigerAnimal(id);
        animal.demenager(enclos);
        return AnimalMapper.versReponse(animal);
    }

    /**
     * Seules les sorties definitives se declarent ici : la vente reste une action de
     * l'eleveur et un animal mort ou disparu ne revient pas a l'etat LIBRE.
     */
    @Transactional
    public AnimalResponse changerEtat(Long id, EtatAnimal etat) {
        if (etat != EtatAnimal.MORT && etat != EtatAnimal.DISPARU) {
            throw new ActionImpossibleException(
                    "Seuls les etats MORT et DISPARU peuvent etre declares ici (recu : %s).".formatted(etat));
        }

        Animal animal = exigerAnimal(id);
        if (!animal.getEtat().estDisponible()) {
            throw new ActionImpossibleException(
                    "%s est deja %s.".formatted(animal.designation(), animal.getEtat()));
        }

        animal.setEtat(etat);
        log.info("{} est desormais {}", animal.designation(), etat);
        return AnimalMapper.versReponse(animal);
    }

    @Transactional
    public void supprimer(Long id) {
        Animal animal = exigerAnimal(id);
        Eleveur eleveur = animal.getEleveur();
        if (eleveur != null) {
            eleveur.getAnimaux().remove(animal);
            animal.setEleveur(null);
        }
        animalRepository.delete(animal);
        log.info("Animal {} retire de la ferme", id);
    }

    private Animal exigerAnimal(Long id) {
        return animalRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Animal", id));
    }
}

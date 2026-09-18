package laFerme.services;

import laFerme.dto.ActionResponse;
import laFerme.dto.CreerEleveurRequest;
import laFerme.dto.EleveurResponse;
import laFerme.exception.ActionImpossibleException;
import laFerme.exception.RessourceIntrouvableException;
import laFerme.model.Animal;
import laFerme.model.Eleveur;
import laFerme.repository.AnimalRepository;
import laFerme.repository.EleveurRepository;
import laFerme.utils.AnimalMapper;
import laFerme.utils.EleveurMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Orchestration des actions de l'eleveur. Les regles metier restent dans les entites
 * ({@link Eleveur} et {@link Animal}), ce service se charge du chargement et de la
 * transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EleveurService {

    private final EleveurRepository eleveurRepository;
    private final AnimalRepository animalRepository;

    public List<EleveurResponse> lister() {
        return eleveurRepository.findAll().stream()
                .sorted(Comparator.comparing(eleveur -> eleveur.getPrenom().toLowerCase()))
                .map(EleveurMapper::versResume)
                .toList();
    }

    public EleveurResponse recupereParId(Long id) {
        Eleveur eleveur = eleveurRepository.findWithAnimauxById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Eleveur", id));
        return EleveurMapper.versReponse(eleveur);
    }

    @Transactional
    public EleveurResponse creer(CreerEleveurRequest requete) {
        String prenom = requete.prenom().trim();
        if (eleveurRepository.existsByPrenomIgnoreCase(prenom)) {
            throw new ActionImpossibleException("Un eleveur nomme %s existe deja.".formatted(prenom));
        }
        Eleveur eleveur = eleveurRepository.save(new Eleveur(prenom));
        log.info("Nouvel eleveur : {}", eleveur);
        return EleveurMapper.versReponse(eleveur);
    }

    @Transactional
    public void supprimer(Long id) {
        Eleveur eleveur = eleveurRepository.findWithAnimauxById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Eleveur", id));
        if (!eleveur.getAnimaux().isEmpty()) {
            throw new ActionImpossibleException(
                    "%s possede encore %d animal(aux) : vendez-les avant de le supprimer."
                            .formatted(eleveur.getPrenom(), eleveur.getAnimaux().size()));
        }
        eleveurRepository.delete(eleveur);
        log.info("Eleveur {} supprime", id);
    }

    // ---------------------------------------------------------------------
    // Actions de l'eleveur sur un animal
    // ---------------------------------------------------------------------

    @Transactional
    public ActionResponse acheter(Long eleveurId, Long animalId) {
        Eleveur eleveur = exigerEleveur(eleveurId);
        Animal animal = exigerAnimal(animalId);
        eleveur.acheter(animal);
        String message = "%s a achete %s.".formatted(eleveur.getPrenom(), animal.designation().toLowerCase());
        return tracer(message, animal);
    }

    @Transactional
    public ActionResponse vendre(Long eleveurId, Long animalId) {
        return agir(eleveurId, animalId, Eleveur::vendre);
    }

    @Transactional
    public ActionResponse nourrir(Long eleveurId, Long animalId) {
        return agir(eleveurId, animalId, Eleveur::nourrir);
    }

    @Transactional
    public ActionResponse soigner(Long eleveurId, Long animalId) {
        return agir(eleveurId, animalId, Eleveur::soigner);
    }

    @Transactional
    public ActionResponse promener(Long eleveurId, Long animalId) {
        return agir(eleveurId, animalId, Eleveur::promener);
    }

    @Transactional
    public ActionResponse recolter(Long eleveurId, Long animalId) {
        return agir(eleveurId, animalId, Eleveur::recolter);
    }

    private ActionResponse agir(Long eleveurId, Long animalId, BiFunction<Eleveur, Animal, String> action) {
        Eleveur eleveur = exigerEleveur(eleveurId);
        Animal animal = exigerAnimal(animalId);
        return tracer(action.apply(eleveur, animal), animal);
    }

    private ActionResponse tracer(String message, Animal animal) {
        log.info(message);
        return new ActionResponse(message, AnimalMapper.versReponse(animal));
    }

    private Eleveur exigerEleveur(Long id) {
        return eleveurRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Eleveur", id));
    }

    private Animal exigerAnimal(Long id) {
        return animalRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Animal", id));
    }
}

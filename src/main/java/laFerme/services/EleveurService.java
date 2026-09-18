package laFerme.services;

import laFerme.dto.ActionResponse;
import laFerme.dto.ClassementResponse;
import laFerme.dto.CreerEleveurRequest;
import laFerme.dto.EleveurResponse;
import laFerme.dto.MouvementResponse;
import laFerme.exception.ActionImpossibleException;
import laFerme.exception.RessourceIntrouvableException;
import laFerme.model.Animal;
import laFerme.model.Eleveur;
import laFerme.model.Mouvement;
import laFerme.model.ResultatAction;
import laFerme.repository.AnimalRepository;
import laFerme.repository.EleveurRepository;
import laFerme.repository.MouvementRepository;
import laFerme.utils.AnimalMapper;
import laFerme.utils.EleveurMapper;
import laFerme.utils.MouvementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

/**
 * Orchestration des actions de l'eleveur. Les regles metier restent dans les entites
 * ({@link Eleveur} et {@link Animal}), ce service se charge du chargement, de la
 * transaction et de l'enregistrement des mouvements d'argent.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EleveurService {

    private static final int MOUVEMENTS_AFFICHES = 50;

    private final EleveurRepository eleveurRepository;
    private final AnimalRepository animalRepository;
    private final MouvementRepository mouvementRepository;

    public List<EleveurResponse> lister() {
        return eleveurRepository.findAll().stream()
                .sorted(Comparator.comparing(eleveur -> eleveur.getPrenom().toLowerCase()))
                .map(EleveurMapper::versResume)
                .toList();
    }

    public EleveurResponse recupereParId(Long id) {
        return EleveurMapper.versReponse(exigerEleveurAvecTroupeau(id));
    }

    /** Classement par fortune : argent en caisse plus valeur de revente du troupeau. */
    public List<ClassementResponse> classement() {
        List<Eleveur> eleveurs = eleveurRepository.findAll().stream()
                .sorted(Comparator.comparing(Eleveur::fortune).reversed())
                .toList();

        return IntStream.range(0, eleveurs.size())
                .mapToObj(rang -> {
                    Eleveur eleveur = eleveurs.get(rang);
                    BigDecimal troupeau = eleveur.fortune().subtract(eleveur.getSolde());
                    return new ClassementResponse(rang + 1, eleveur.getId(), eleveur.getPrenom(),
                            eleveur.getSolde(), troupeau, eleveur.fortune(), eleveur.getAnimaux().size());
                })
                .toList();
    }

    /** Releve de compte : toutes les operations, du plus recent au plus ancien. */
    public List<MouvementResponse> mouvements(Long eleveurId) {
        exigerEleveur(eleveurId);
        return mouvementRepository
                .findByEleveurIdOrderByHorodatageDesc(eleveurId, Limit.of(MOUVEMENTS_AFFICHES))
                .stream()
                .map(MouvementMapper::versReponse)
                .toList();
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
        Eleveur eleveur = exigerEleveurAvecTroupeau(id);
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
        return agir(eleveurId, animalId, Eleveur::acheter);
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

    private ActionResponse agir(Long eleveurId, Long animalId,
                                BiFunction<Eleveur, Animal, ResultatAction> action) {
        Eleveur eleveur = exigerEleveur(eleveurId);
        Animal animal = exigerAnimal(animalId);

        ResultatAction resultat = action.apply(eleveur, animal);
        enregistrer(eleveur, animal, resultat);

        log.info("{} (solde : {} €)", resultat.message(), eleveur.getSolde());
        return new ActionResponse(resultat.message(), resultat.montant(), eleveur.getSolde(),
                AnimalMapper.versReponse(animal));
    }

    /** Une action qui bouge de l'argent laisse une trace dans le releve. */
    private void enregistrer(Eleveur eleveur, Animal animal, ResultatAction resultat) {
        if (resultat.montant().signum() == 0) {
            return;
        }
        mouvementRepository.save(new Mouvement(eleveur, animal.getId(), resultat.type(),
                resultat.montant(), resultat.message()));
    }

    private Eleveur exigerEleveur(Long id) {
        return eleveurRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Eleveur", id));
    }

    private Eleveur exigerEleveurAvecTroupeau(Long id) {
        return eleveurRepository.findWithAnimauxById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Eleveur", id));
    }

    private Animal exigerAnimal(Long id) {
        return animalRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Animal", id));
    }
}

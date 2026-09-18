package laFerme.repository;

import laFerme.model.Eleveur;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EleveurRepository extends JpaRepository<Eleveur, Long> {

    /** Une seule requete pour la liste : evite le N+1 sur le comptage des troupeaux. */
    @EntityGraph(attributePaths = "animaux")
    @Override
    List<Eleveur> findAll();

    Optional<Eleveur> findByPrenomIgnoreCase(String prenom);

    boolean existsByPrenomIgnoreCase(String prenom);

    /** Charge l'eleveur avec son troupeau en une seule requete. */
    @EntityGraph(attributePaths = "animaux")
    Optional<Eleveur> findWithAnimauxById(Long id);
}

package laFerme.repository;

import laFerme.model.Mouvement;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MouvementRepository extends JpaRepository<Mouvement, Long> {

    /** Le releve de compte, du plus recent au plus ancien. */
    List<Mouvement> findByEleveurIdOrderByHorodatageDesc(Long eleveurId, Limit limite);
}

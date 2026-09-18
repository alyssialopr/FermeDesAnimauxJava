package laFerme.repository;

import laFerme.model.Animal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AnimalRepository extends JpaRepository<Animal, Long>, JpaSpecificationExecutor<Animal> {

    Optional<Animal> findByNomIgnoreCase(String nom);

    boolean existsByNomIgnoreCase(String nom);

    long countByEnclos(String enclos);
}

package laFerme.services;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Sert de garde aux annotations {@code @PreAuthorize} : un eleveur ne peut agir
 * qu'en son propre nom, meme s'il presente une cle valide.
 */
@Service("securite")
public class ServiceSecurite {

    /** Vrai si la requete est authentifiee au nom de cet eleveur. */
    public boolean estEleveur(Long eleveurId) {
        return eleveurConnecte().filter(id -> id.equals(eleveurId)).isPresent();
    }

    /** Vrai si la requete est authentifiee, quel que soit l'eleveur. */
    public boolean estConnecte() {
        return eleveurConnecte().isPresent();
    }

    public Optional<Long> eleveurConnecte() {
        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        if (authentification == null || !authentification.isAuthenticated()) {
            return Optional.empty();
        }
        return authentification.getPrincipal() instanceof Long id ? Optional.of(id) : Optional.empty();
    }
}

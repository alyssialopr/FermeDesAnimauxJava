package laFerme.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import laFerme.repository.EleveurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Authentifie l'eleveur a partir de l'en-tete
 * {@code Authorization: Bearer <idEleveur>.<cle>}.
 *
 * <p>La cle n'est jamais stockee en clair : seule son empreinte BCrypt est en base.
 * Le filtre ne renvoie jamais d'erreur lui-meme, il se contente de ne pas
 * authentifier : c'est la chaine de securite qui decide ensuite si la ressource
 * demandee exige une identite.</p>
 */
@RequiredArgsConstructor
@Slf4j
public class FiltreCleEleveur extends OncePerRequestFilter {

    private static final String PREFIXE = "Bearer ";

    /**
     * Empreinte factice : comparer la cle fournie a cette empreinte quand l'eleveur
     * n'existe pas prend le meme temps qu'une vraie verification, ce qui evite de
     * reveler par le temps de reponse quels identifiants existent.
     */
    private static final String EMPREINTE_LEURRE =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOa7t7.Q3.mZ7l5Qb0PQ1cMOnRJGaQmO2";

    private final EleveurRepository eleveurRepository;
    private final PasswordEncoder encodeur;

    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse, FilterChain suite)
            throws ServletException, IOException {

        jetonDe(requete).ifPresent(jeton -> authentifier(jeton, requete));
        suite.doFilter(requete, reponse);
    }

    private Optional<Jeton> jetonDe(HttpServletRequest requete) {
        String entete = requete.getHeader(HttpHeaders.AUTHORIZATION);
        if (entete == null || !entete.startsWith(PREFIXE)) {
            return Optional.empty();
        }

        String valeur = entete.substring(PREFIXE.length()).trim();
        int separateur = valeur.indexOf('.');
        if (separateur <= 0 || separateur == valeur.length() - 1) {
            return Optional.empty();
        }

        try {
            return Optional.of(new Jeton(
                    Long.parseLong(valeur.substring(0, separateur)),
                    valeur.substring(separateur + 1)));
        } catch (NumberFormatException erreur) {
            return Optional.empty();
        }
    }

    private void authentifier(Jeton jeton, HttpServletRequest requete) {
        Optional<String> empreinte = eleveurRepository.findById(jeton.eleveurId())
                .map(eleveur -> eleveur.getCleHachee());

        // Toujours passer par BCrypt, meme sans eleveur : temps de reponse constant.
        boolean valide = encodeur.matches(jeton.cle(), empreinte.orElse(EMPREINTE_LEURRE))
                && empreinte.isPresent();

        if (!valide) {
            log.warn("Cle refusee pour l'eleveur {} depuis {}", jeton.eleveurId(), requete.getRemoteAddr());
            return;
        }

        var authentification = new UsernamePasswordAuthenticationToken(
                jeton.eleveurId(), null, List.of(new SimpleGrantedAuthority("ROLE_ELEVEUR")));
        SecurityContextHolder.getContext().setAuthentication(authentification);
    }

    private record Jeton(Long eleveurId, String cle) {
    }
}

package laFerme.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import laFerme.repository.EleveurRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Securite de l'API.
 *
 * <p>Principes retenus :</p>
 * <ul>
 *   <li><b>Lecture ouverte, ecriture authentifiee</b> : consulter la ferme ne demande
 *       rien, agir dessus exige la cle de l'eleveur concerne.</li>
 *   <li><b>Sans session</b> : aucun cookie, donc pas de CSRF possible ; la protection
 *       CSRF est desactivee en connaissance de cause.</li>
 *   <li><b>Cle hachee</b> : seule l'empreinte BCrypt est stockee.</li>
 *   <li><b>Erreurs normalisees</b> : 401 et 403 sortent en {@code problem+json} comme
 *       le reste de l'API, sans page de connexion ni detail interne.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class ConfigurationSecurite {

    private static final String[] LECTURE_PUBLIQUE = {
            "/api/animaux/**", "/api/eleveurs/**", "/api/especes/**",
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
    };

    @Bean
    PasswordEncoder encodeurDeCle() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    FiltreCleEleveur filtreCleEleveur(EleveurRepository eleveurRepository, PasswordEncoder encodeur) {
        return new FiltreCleEleveur(eleveurRepository, encodeur);
    }

    @Bean
    SecurityFilterChain chaineDeSecurite(HttpSecurity http, FiltreCleEleveur filtreCleEleveur) throws Exception {
        return http
                // API sans cookie ni session : rien a proteger contre le CSRF.
                .csrf(AbstractHttpConfigurer::disable)
                // Aucun traitement CORS : l'API ne renvoie jamais d'en-tete
                // Access-Control-Allow-Origin, donc aucun site tiers ne peut lire ses
                // reponses depuis un navigateur. Le jeu, lui, est servi par nginx sur
                // la meme origine que l'API et n'en a pas besoin.
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .anonymous(Customizer.withDefaults())

                // Pas de formulaire de connexion ni d'authentification HTTP basique :
                // la seule facon de s'authentifier est la cle d'eleveur.
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                .headers(entetes -> entetes
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .referrerPolicy(politique -> politique
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                        .permissionsPolicyHeader(permissions -> permissions
                                .policy("camera=(), microphone=(), geolocation=(), interest-cohort=()")))

                .authorizeHttpRequests(acces -> acces
                        // Le releve de compte est prive : il passe avant la regle de
                        // lecture publique, sinon elle l'attraperait.
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/eleveurs/*/mouvements")
                        .authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, LECTURE_PUBLIQUE).permitAll()
                        // La creation d'un eleveur est le point d'entree : il faut bien
                        // pouvoir obtenir une premiere cle.
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/eleveurs").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())

                .exceptionHandling(erreurs -> erreurs
                        .authenticationEntryPoint((requete, reponse, exception) -> ecrire(reponse, requete,
                                HttpStatus.UNAUTHORIZED, "Authentification requise",
                                "Cette action demande la cle de l'eleveur "
                                        + "(en-tete Authorization: Bearer <idEleveur>.<cle>)."))
                        .accessDeniedHandler((requete, reponse, exception) -> ecrire(reponse, requete,
                                HttpStatus.FORBIDDEN, "Acces refuse",
                                "Cette cle ne permet pas d'agir au nom de cet eleveur.")))

                .addFilterBefore(filtreCleEleveur, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Les refus de securite sortent au meme format que les autres erreurs de l'API.
     * Le corps est ecrit a la main : ces refus surviennent dans la chaine de filtres,
     * avant que la serialisation de Spring MVC ne soit disponible.
     */
    private void ecrire(HttpServletResponse reponse, HttpServletRequest requete,
                        HttpStatus statut, String titre, String detail) throws IOException {
        reponse.setStatus(statut.value());
        reponse.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        reponse.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String corps = """
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s","instance":"%s","horodatage":"%s"}"""
                .formatted(echapper(titre), statut.value(), echapper(detail),
                        echapper(requete.getRequestURI()), Instant.now());

        reponse.getWriter().write(corps);
    }

    /** Echappement JSON minimal : ces valeurs finissent dans une chaine. */
    private static String echapper(String valeur) {
        return valeur.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ");
    }
}

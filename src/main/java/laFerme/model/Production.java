package laFerme.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ce que rend un animal quand on le recolte. Le prix unitaire depend de l'espece
 * (le lait de chevre se vend plus cher que celui de vache), il est donc porte par
 * {@link Espece} et pas ici.
 */
@Schema(name = "Production", description = """
        Nature de la production d'un animal. `AUCUNE` pour les especes qui ne se \
        recoltent pas : le cochon prend de la valeur en mangeant, le cheval gagne \
        de l'argent en promenade.""")
public enum Production {

    LAIT("litres de lait", "🥛"),
    OEUFS("oeufs", "🥚"),
    LAINE("kg de laine", "🧶"),
    DUVET("g de duvet", "🪶"),
    LAPEREAUX("lapereaux", "🐇"),
    AUCUNE("rien", "—");

    private final String unite;
    private final String emoji;

    Production(String unite, String emoji) {
        this.unite = unite;
        this.emoji = emoji;
    }

    public String getUnite() {
        return unite;
    }

    public String getEmoji() {
        return emoji;
    }

    public boolean estRecoltable() {
        return this != AUCUNE;
    }
}

package laFerme.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EleveurCreeResponse", description = """
        Eleveur tout juste cree, avec sa cle d'acces. C'est la seule et unique fois \
        que la cle est renvoyee : le serveur n'en garde qu'une empreinte.""")
public record EleveurCreeResponse(

        @Schema(description = "L'eleveur cree")
        EleveurResponse eleveur,

        @Schema(description = "Cle d'acces, a conserver : elle autorise toutes les actions "
                + "au nom de cet eleveur (en-tete Authorization: Bearer <id>.<cle>)",
                example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        String cle
) {
}

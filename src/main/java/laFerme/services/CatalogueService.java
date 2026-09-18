package laFerme.services;

import laFerme.dto.EspeceResponse;
import laFerme.model.Espece;
import laFerme.utils.EspeceMapper;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Le catalogue du marche : ce que coute chaque espece et ce qu'elle rapporte.
 * Tout vient de l'enum {@link Espece}, il n'y a rien en base.
 */
@Service
public class CatalogueService {

    public List<EspeceResponse> especes() {
        return Arrays.stream(Espece.values())
                .sorted(Comparator.comparing(Espece::getPrix))
                .map(EspeceMapper::versReponse)
                .toList();
    }
}

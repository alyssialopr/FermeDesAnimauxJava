package laFerme.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Une ligne du releve de compte d'un eleveur. Chaque achat, vente, repas, soin,
 * recolte ou promenade en laisse une, ce qui donne un historique consultable
 * apres redemarrage.
 */
@Entity
@Table(name = "mouvement")
@Getter
@Setter
@NoArgsConstructor
public class Mouvement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleveur_id", nullable = false)
    private Eleveur eleveur;

    /** L'animal concerne, s'il existe encore. */
    @Column(name = "animal_id")
    private Long animalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeMouvement type;

    /** Negatif pour une depense, positif pour une recette. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montant;

    @Column(name = "solde_apres", nullable = false, precision = 12, scale = 2)
    private BigDecimal soldeApres;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false)
    private Instant horodatage;

    public Mouvement(Eleveur eleveur, Long animalId, TypeMouvement type, BigDecimal montant, String libelle) {
        this.eleveur = eleveur;
        this.animalId = animalId;
        this.type = type;
        this.montant = montant;
        this.soldeApres = eleveur.getSolde();
        this.libelle = libelle;
    }

    @PrePersist
    void avantEnregistrement() {
        if (horodatage == null) {
            horodatage = Instant.now();
        }
    }
}

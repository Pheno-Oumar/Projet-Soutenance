package com.kadi_aon.mon_salon.prestation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.prestation.entity.Prestation;
import com.kadi_aon.mon_salon.prestation.enums.StatutPrestation;

@Repository
public interface PrestationRepository extends JpaRepository<Prestation, Long> {

    List<Prestation> findBySalonSlugOrderByDateHeureDebutDesc(String slugSalon);

    List<Prestation> findBySalonSlugAndStatutOrderByDateHeureDebutDesc(String slugSalon, StatutPrestation statut);

    Optional<Prestation> findByIdAndSalonSlug(Long id, String slugSalon);

    Optional<Prestation> findByRendezVousId(Long rendezVousId);

    List<Prestation> findByClientEmailAndSalonSlugOrderByDateHeureDebutDesc(String email, String slugSalon);

    long countByStatut(StatutPrestation statut);

    long countBySalonSlug(String slugSalon);

    long countBySalonSlugAndStatut(String slugSalon, StatutPrestation statut);

    List<Prestation> findByClientIdOrderByDateHeureDebutDesc(Long clientId);

    List<Prestation> findByClientIdAndSalonSlugOrderByDateHeureDebutDesc(Long clientId, String slugSalon);

    List<Prestation> findByClientEmailOrderByDateHeureDebutDesc(String email);

    @Query("SELECT COUNT(p) FROM Prestation p WHERE p.coiffeur.id = :coiffeurId AND p.salon.slug = :slugSalon AND p.statut = :statut AND p.dateHeureDebut >= :debutMois")
    long countPrestationsMoisCoiffeur(
            @Param("coiffeurId") Long coiffeurId,
            @Param("slugSalon") String slugSalon,
            @Param("statut") StatutPrestation statut,
            @Param("debutMois") java.time.LocalDateTime debutMois);

    boolean existsByCoiffeurIdAndStatut(Long coiffeurId, StatutPrestation statut);

    boolean existsByCoiffeurIdAndStatutAndIdNot(Long coiffeurId, StatutPrestation statut, Long id);

    // Prochain numéro d'ordre dans la file d'attente pour le salon aujourd'hui
    @Query("SELECT COALESCE(MAX(p.ordreFileAttente), 0) + 1 FROM Prestation p WHERE p.salon.slug = :slugSalon AND p.statut = com.kadi_aon.mon_salon.prestation.enums.StatutPrestation.EN_ATTENTE")
    Integer prochainOrdreFileAttente(@Param("slugSalon") String slugSalon);

    // File d'attente ordonnée
    @Query("SELECT p FROM Prestation p WHERE p.salon.slug = :slugSalon AND p.statut = com.kadi_aon.mon_salon.prestation.enums.StatutPrestation.EN_ATTENTE ORDER BY p.ordreFileAttente ASC")
    List<Prestation> findFileAttenteOrdronnee(@Param("slugSalon") String slugSalon);
}

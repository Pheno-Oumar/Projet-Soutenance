package com.kadi_aon.mon_salon.avis.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.avis.entity.AvisPrestation;

@Repository
public interface AvisPrestationRepository extends JpaRepository<AvisPrestation, Long> {

    Optional<AvisPrestation> findByClientIdAndLignePrestationId(Long clientId, Long lignePrestationId);

    boolean existsByClientIdAndLignePrestationId(Long clientId, Long lignePrestationId);

    Optional<AvisPrestation> findByIdAndLignePrestationPrestationSalonSlug(Long id, String slugSalon);

    Optional<AvisPrestation> findByIdAndClientEmailAndLignePrestationPrestationSalonSlug(Long id, String clientEmail, String slugSalon);

    List<AvisPrestation> findByClientEmailAndLignePrestationPrestationSalonSlugOrderByDateCreationDesc(String clientEmail, String slugSalon);

    List<AvisPrestation> findByLignePrestationIdAndStatutTrueOrderByDateCreationDesc(Long lignePrestationId);

    List<AvisPrestation> findByLignePrestationPrestationSalonSlugOrderByDateCreationDesc(String slugSalon);

    List<AvisPrestation> findByLignePrestationPrestationSalonSlugAndStatutOrderByDateCreationDesc(String slugSalon, boolean statut);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM AvisPrestation a WHERE a.lignePrestation.prestation.coiffeur.compte.email = :coiffeurEmail AND a.lignePrestation.prestation.salon.slug = :slugSalon ORDER BY a.dateCreation DESC")
    List<AvisPrestation> findByCoiffeurEmailAndSalonSlug(
            @org.springframework.data.repository.query.Param("coiffeurEmail") String coiffeurEmail,
            @org.springframework.data.repository.query.Param("slugSalon") String slugSalon);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM AvisPrestation a WHERE a.lignePrestation.prestation.coiffeur.compte.email = :coiffeurEmail AND a.lignePrestation.prestation.salon.slug = :slugSalon AND a.statut = :statut ORDER BY a.dateCreation DESC")
    List<AvisPrestation> findByCoiffeurEmailAndSalonSlugAndStatut(
            @org.springframework.data.repository.query.Param("coiffeurEmail") String coiffeurEmail,
            @org.springframework.data.repository.query.Param("slugSalon") String slugSalon,
            @org.springframework.data.repository.query.Param("statut") boolean statut);

    List<AvisPrestation> findByClientEmailOrderByDateCreationDesc(String clientEmail);
}



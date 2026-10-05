package com.kadi_aon.mon_salon.facturation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.facturation.entity.Paiement;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    Optional<Paiement> findByNumeroPaiement(String numeroPaiement);

    List<Paiement> findByFactureIdOrderByDatePaiementAsc(Long factureId);

    List<Paiement> findByClientEmailAndSalonSlugOrderByDatePaiementDesc(String email, String slugSalon);

    Optional<Paiement> findByIdAndSalonSlug(Long id, String slugSalon);

    Optional<Paiement> findByIdAndClientEmailAndSalonSlug(Long id, String email, String slugSalon);

    Optional<Paiement> findByIdAndClientEmail(Long id, String email);

    List<Paiement> findByClientEmailOrderByDatePaiementDesc(String email);

    List<Paiement> findBySalonSlugOrderByDatePaiementDesc(String slugSalon);


    List<Paiement> findByClientIdOrderByDatePaiementDesc(Long clientId);

    List<Paiement> findByClientIdAndSalonSlugOrderByDatePaiementDesc(Long clientId, String slugSalon);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.montant), 0) FROM Paiement p WHERE p.statut = com.kadi_aon.mon_salon.facturation.enums.StatutPaiement.PAYE")
    java.math.BigDecimal totalPaiementsPlateforme();

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.montant), 0) FROM Paiement p WHERE p.salon.slug = :slugSalon AND p.statut = com.kadi_aon.mon_salon.facturation.enums.StatutPaiement.PAYE")
    java.math.BigDecimal totalPaiementsSalon(@org.springframework.data.repository.query.Param("slugSalon") String slugSalon);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.montant), 0) FROM Paiement p WHERE p.salon.slug = :slugSalon AND p.statut = com.kadi_aon.mon_salon.facturation.enums.StatutPaiement.REMBOURSE AND p.paiementOrigine IS NOT NULL")
    java.math.BigDecimal totalRemboursementsSalon(@org.springframework.data.repository.query.Param("slugSalon") String slugSalon);


    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.montant), 0) FROM Paiement p WHERE p.salon.slug = :slugSalon AND p.statut = :statut AND p.datePaiement BETWEEN :debut AND :fin")
    java.math.BigDecimal totalPaiementsSalonEntreDates(
            @org.springframework.data.repository.query.Param("slugSalon") String slugSalon,
            @org.springframework.data.repository.query.Param("statut") com.kadi_aon.mon_salon.facturation.enums.StatutPaiement statut,
            @org.springframework.data.repository.query.Param("debut") java.time.LocalDateTime debut,
            @org.springframework.data.repository.query.Param("fin") java.time.LocalDateTime fin);

    List<Paiement> findBySalonSlugAndStatutAndDatePaiementBetweenOrderByDatePaiementDesc(
            String slugSalon, com.kadi_aon.mon_salon.facturation.enums.StatutPaiement statut, java.time.LocalDateTime debut, java.time.LocalDateTime fin);

    List<Paiement> findBySalonSlugAndStatutAndTypeAndDatePaiementBetweenOrderByDatePaiementDesc(
            String slugSalon, com.kadi_aon.mon_salon.facturation.enums.StatutPaiement statut, com.kadi_aon.mon_salon.facturation.enums.TypePaiement type, java.time.LocalDateTime debut, java.time.LocalDateTime fin);

    List<Paiement> findBySalonSlugAndDatePaiementBetweenOrderByDatePaiementDesc(
            String slugSalon, java.time.LocalDateTime debut, java.time.LocalDateTime fin);
}


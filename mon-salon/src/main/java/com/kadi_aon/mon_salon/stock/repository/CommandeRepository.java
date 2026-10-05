package com.kadi_aon.mon_salon.stock.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.stock.entity.Commande;
import com.kadi_aon.mon_salon.stock.enums.StatutCommande;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {

    Optional<Commande> findByNumeroCommande(String numeroCommande);

    Optional<Commande> findByIdAndClientAffectationSalonSlug(Long id, String slug);

    Optional<Commande> findByIdAndClientAffectationCompteEmailAndClientAffectationSalonSlug(Long id, String email, String slug);

    List<Commande> findByClientAffectationSalonSlugOrderByDateCommandeDesc(String slug);

    List<Commande> findByClientAffectationSalonSlugAndStatutOrderByDateCommandeDesc(String slug, StatutCommande statut);

    List<Commande> findByClientAffectationSalonSlugAndDateCommandeBetweenOrderByDateCommandeDesc(String slug, LocalDateTime debut, LocalDateTime fin);

    List<Commande> findByClientAffectationSalonSlugAndStatutAndDateCommandeBetweenOrderByDateCommandeDesc(String slug, StatutCommande statut, LocalDateTime debut, LocalDateTime fin);

    List<Commande> findByClientAffectationCompteEmailAndClientAffectationSalonSlugOrderByDateCommandeDesc(String email, String slug);

    List<Commande> findByClientAffectationCompteEmailOrderByDateCommandeDesc(String email);

    Optional<Commande> findByIdAndClientAffectationCompteEmail(Long id, String email);

    long countByClientAffectationSalonSlug(String slug);


    long countByClientAffectationSalonSlugAndStatut(String slug, StatutCommande statut);

    @Query("SELECT COALESCE(SUM(c.montantTotal), 0) FROM Commande c WHERE c.clientAffectation.salon.slug = :slug AND c.statut IN ('VALIDEE', 'RECUPEREE')")
    BigDecimal totalChiffreAffairesCommandesValidees(@Param("slug") String slug);
}

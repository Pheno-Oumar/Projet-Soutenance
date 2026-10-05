package com.kadi_aon.mon_salon.stock.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.stock.entity.Produit;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    List<Produit> findByCategorieSalonSlug(String slugSalon);

    List<Produit> findByCategorieSalonSlugAndStatut(String slugSalon, boolean statut);

    List<Produit> findByCategorieId(Long categorieId);

    List<Produit> findByCategorieIdAndStatut(Long categorieId, boolean statut);

    List<Produit> findByStatutTrue();

    List<Produit> findByStatutTrueAndCategorieId(Long categorieId);

    Optional<Produit> findByIdAndCategorieSalonSlug(Long id, String slugSalon);


    boolean existsByCategorieIdAndNomIgnoreCase(Long categorieId, String nom);

    boolean existsByCategorieIdAndNomIgnoreCaseAndIdNot(Long categorieId, String nom, Long id);

    long countByCategorieSalonSlugAndStatut(String slugSalon, boolean statut);

    @Query("SELECT p FROM Produit p WHERE p.categorie.salon.slug = :slug AND p.statut = true AND p.stock.quantiteDisponible <= p.stock.seuilMinimum ORDER BY p.stock.quantiteDisponible ASC")
    List<Produit> findProduitsEnAlerteStock(@Param("slug") String slugSalon);

    @Query("SELECT COUNT(p) FROM Produit p WHERE p.categorie.salon.slug = :slug AND p.statut = true AND p.stock.quantiteDisponible = 0")
    long countProduitsEnRupture(@Param("slug") String slugSalon);

    @Query("SELECT COUNT(p) FROM Produit p WHERE p.categorie.salon.slug = :slug AND p.statut = true AND p.stock.quantiteDisponible <= p.stock.seuilMinimum")
    long countProduitsEnAlerte(@Param("slug") String slugSalon);

    @Query("SELECT COALESCE(SUM(p.stock.quantiteDisponible * p.prixVente), 0) FROM Produit p WHERE p.categorie.salon.slug = :slug AND p.statut = true")
    BigDecimal calculateValeurTotaleStock(@Param("slug") String slugSalon);
}

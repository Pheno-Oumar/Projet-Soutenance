package com.kadi_aon.mon_salon.depense.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.depense.entity.Depense;
import com.kadi_aon.mon_salon.depense.enums.CategorieDepense;

@Repository
public interface DepenseRepository extends JpaRepository<Depense, Long> {

    List<Depense> findBySalonSlugOrderByDateDepenseDesc(String slugSalon);

    List<Depense> findBySalonSlugAndStatutOrderByDateDepenseDesc(String slugSalon, boolean statut);

    List<Depense> findBySalonSlugAndCategorieOrderByDateDepenseDesc(String slugSalon, CategorieDepense categorie);

    List<Depense> findBySalonSlugAndCategorieAndStatutOrderByDateDepenseDesc(String slugSalon, CategorieDepense categorie, boolean statut);

    Optional<Depense> findByIdAndSalonSlug(Long id, String slugSalon);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(d.montant), 0) FROM Depense d WHERE d.salon.slug = :slugSalon AND d.statut = true")
    java.math.BigDecimal totalDepensesSalon(@org.springframework.data.repository.query.Param("slugSalon") String slugSalon);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(d.montant), 0) FROM Depense d WHERE d.salon.slug = :slugSalon AND d.statut = true AND d.dateDepense BETWEEN :debut AND :fin")
    java.math.BigDecimal totalDepensesSalonEntreDates(
            @org.springframework.data.repository.query.Param("slugSalon") String slugSalon,
            @org.springframework.data.repository.query.Param("debut") java.time.LocalDateTime debut,
            @org.springframework.data.repository.query.Param("fin") java.time.LocalDateTime fin);

    @org.springframework.data.jpa.repository.Query("SELECT d.categorie, COALESCE(SUM(d.montant), 0) FROM Depense d WHERE d.salon.slug = :slugSalon AND d.statut = true AND d.dateDepense BETWEEN :debut AND :fin GROUP BY d.categorie")
    List<Object[]> totalDepensesParCategorieEntreDates(
            @org.springframework.data.repository.query.Param("slugSalon") String slugSalon,
            @org.springframework.data.repository.query.Param("debut") java.time.LocalDateTime debut,
            @org.springframework.data.repository.query.Param("fin") java.time.LocalDateTime fin);

    List<Depense> findBySalonSlugAndStatutAndDateDepenseBetweenOrderByDateDepenseDesc(
            String slugSalon, boolean statut, java.time.LocalDateTime debut, java.time.LocalDateTime fin);

    List<Depense> findBySalonSlugAndCategorieAndStatutAndDateDepenseBetweenOrderByDateDepenseDesc(
            String slugSalon, CategorieDepense categorie, boolean statut, java.time.LocalDateTime debut, java.time.LocalDateTime fin);
}


package com.kadi_aon.mon_salon.salon.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.salon.entity.Salon;

@Repository
public interface SalonRepository extends JpaRepository<Salon, Long> {

    Optional<Salon> findBySlug(String slug);

    Optional<Salon> findBySlugAndStatutTrue(String slug);

    boolean existsBySlug(String slug);

    long countByStatut(Boolean statut);

    Page<Salon> findByStatutTrue(Pageable pageable);

    List<Salon> findByStatutTrueOrderByNomAsc();

    @Query("SELECT s FROM Salon s WHERE s.statut = true AND (LOWER(s.nom) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.adresse) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Salon> searchSalons(@Param("query") String query);

    @Query("SELECT DISTINCT s.salon FROM ServiceSalon s WHERE s.id = :serviceId AND s.statut = true AND s.salon.statut = true")
    Optional<Salon> findSalonByServiceId(@Param("serviceId") Long serviceId);

    @Query("SELECT DISTINCT s.salon FROM ServiceSalon s WHERE LOWER(s.nom) LIKE LOWER(CONCAT('%', :nom, '%')) AND s.statut = true AND s.salon.statut = true")
    List<Salon> findSalonsByServiceNom(@Param("nom") String nom);
}


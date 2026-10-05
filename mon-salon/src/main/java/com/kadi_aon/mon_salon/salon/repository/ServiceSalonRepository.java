package com.kadi_aon.mon_salon.salon.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.salon.entity.ServiceSalon;

@Repository
public interface ServiceSalonRepository extends JpaRepository<ServiceSalon, Long> {

    @Query("SELECT DISTINCT s FROM ServiceSalon s LEFT JOIN FETCH s.variantes WHERE s.salon.id = :salonId ORDER BY s.nom ASC")
    List<ServiceSalon> findBySalonIdWithVariantes(@Param("salonId") Long salonId);

    @Query("SELECT DISTINCT s FROM ServiceSalon s LEFT JOIN FETCH s.variantes WHERE s.salon.slug = :slug ORDER BY s.nom ASC")
    List<ServiceSalon> findBySalonSlugWithVariantes(@Param("slug") String slug);

    @Query("SELECT DISTINCT s FROM ServiceSalon s LEFT JOIN FETCH s.variantes WHERE s.salon.slug = :slug AND s.statut = true ORDER BY s.nom ASC")
    List<ServiceSalon> findBySalonSlugActiveWithVariantes(@Param("slug") String slug);

    Optional<ServiceSalon> findByIdAndSalonSlug(Long id, String slug);

    boolean existsBySalonIdAndNomIgnoreCase(Long salonId, String nom);

    @Query("SELECT DISTINCT s FROM ServiceSalon s WHERE LOWER(s.nom) LIKE LOWER(CONCAT('%', :nom, '%')) AND s.statut = true")
    List<ServiceSalon> findByNomContainingIgnoreCase(@Param("nom") String nom);
}

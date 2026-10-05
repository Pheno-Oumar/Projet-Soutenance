package com.kadi_aon.mon_salon.avis.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.avis.entity.AvisSalon;

@Repository
public interface AvisSalonRepository extends JpaRepository<AvisSalon, Long> {

    Optional<AvisSalon> findByClientIdAndSalonId(Long clientId, Long salonId);

    boolean existsByClientIdAndSalonId(Long clientId, Long salonId);

    Optional<AvisSalon> findByClientEmailAndSalonSlug(String clientEmail, String slugSalon);

    Optional<AvisSalon> findByIdAndSalonSlug(Long id, String slugSalon);

    List<AvisSalon> findBySalonSlugAndStatutTrueOrderByDateCreationDesc(String slugSalon);

    List<AvisSalon> findBySalonSlugOrderByDateCreationDesc(String slugSalon);

    List<AvisSalon> findBySalonSlugAndStatutOrderByDateCreationDesc(String slugSalon, boolean statut);

    List<AvisSalon> findByClientEmailOrderByDateCreationDesc(String clientEmail);
}


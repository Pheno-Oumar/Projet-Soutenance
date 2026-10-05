package com.kadi_aon.mon_salon.realisation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.realisation.entity.Realisation;

@Repository
public interface RealisationRepository extends JpaRepository<Realisation, Long> {

    List<Realisation> findBySalonSlugAndStatutPublicationTrueOrderByDatePublicationDesc(String slugSalon);

    List<Realisation> findBySalonSlugOrderByDateCreationDesc(String slugSalon);

    List<Realisation> findBySalonSlugAndStatutPublicationOrderByDateCreationDesc(String slugSalon, boolean statutPublication);

    Optional<Realisation> findByIdAndSalonSlug(Long id, String slugSalon);

    Optional<Realisation> findByIdAndSalonSlugAndStatutPublicationTrue(Long id, String slugSalon);

    List<Realisation> findByStatutPublicationTrueOrderByDatePublicationDesc();

    org.springframework.data.domain.Page<Realisation> findByStatutPublicationTrueOrderByDatePublicationDesc(org.springframework.data.domain.Pageable pageable);

    Optional<Realisation> findByIdAndStatutPublicationTrue(Long id);
}


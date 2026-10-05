package com.kadi_aon.mon_salon.reclamation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.reclamation.entity.Reclamation;
import com.kadi_aon.mon_salon.reclamation.enums.StatutReclamation;

@Repository
public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {

    List<Reclamation> findBySalonSlugOrderByDateCreationDesc(String slugSalon);

    List<Reclamation> findBySalonSlugAndStatutOrderByDateCreationDesc(String slugSalon, StatutReclamation statut);

    List<Reclamation> findByAffectationClientCompteEmailAndSalonSlugOrderByDateCreationDesc(String email, String slugSalon);

    Optional<Reclamation> findByIdAndSalonSlug(Long id, String slugSalon);

    Optional<Reclamation> findByIdAndAffectationClientCompteEmailAndSalonSlug(Long id, String email, String slugSalon);

    List<Reclamation> findByAffectationClientCompteEmailOrderByDateCreationDesc(String email);

    Optional<Reclamation> findByIdAndAffectationClientCompteEmail(Long id, String email);
}


package com.kadi_aon.mon_salon.coiffeur.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.coiffeur.entity.ProfilCoiffeur;

@Repository
public interface ProfilCoiffeurRepository extends JpaRepository<ProfilCoiffeur, Long> {

    Optional<ProfilCoiffeur> findByAffectationId(Long affectationId);

    Optional<ProfilCoiffeur> findByAffectationCompteEmailAndAffectationSalonSlug(String email, String slugSalon);
}

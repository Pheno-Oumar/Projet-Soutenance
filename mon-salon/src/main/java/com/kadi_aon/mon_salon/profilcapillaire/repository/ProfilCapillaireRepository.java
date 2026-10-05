package com.kadi_aon.mon_salon.profilcapillaire.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.profilcapillaire.entity.ProfilCapillaire;

@Repository
public interface ProfilCapillaireRepository extends JpaRepository<ProfilCapillaire, Long> {

    Optional<ProfilCapillaire> findByCompteId(Long compteId);

    boolean existsByCompteId(Long compteId);

    Optional<ProfilCapillaire> findByCompteEmail(String email);
}

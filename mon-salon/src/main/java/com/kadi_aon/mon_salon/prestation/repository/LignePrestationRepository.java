package com.kadi_aon.mon_salon.prestation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.prestation.entity.LignePrestation;

@Repository
public interface LignePrestationRepository extends JpaRepository<LignePrestation, Long> {

    List<LignePrestation> findByPrestationId(Long prestationId);

    @org.springframework.data.jpa.repository.Query("SELECT lp FROM LignePrestation lp WHERE lp.prestation.coiffeur.compte.id = :coiffeurId AND lp.prestation.salon.slug = :slugSalon AND lp.prestation.statut = com.kadi_aon.mon_salon.prestation.enums.StatutPrestation.TERMINEE")
    List<LignePrestation> findLignesTermineesByCoiffeurAndSalon(
            @org.springframework.data.repository.query.Param("coiffeurId") Long coiffeurId,
            @org.springframework.data.repository.query.Param("slugSalon") String slugSalon
    );
}

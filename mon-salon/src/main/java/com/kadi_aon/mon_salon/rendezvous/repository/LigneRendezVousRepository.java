package com.kadi_aon.mon_salon.rendezvous.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.rendezvous.entity.LigneRendezVous;
import com.kadi_aon.mon_salon.rendezvous.enums.StatutRendezVous;

@Repository
public interface LigneRendezVousRepository extends JpaRepository<LigneRendezVous, Long> {


    @Query("SELECT l FROM LigneRendezVous l WHERE l.rendezVous.salon.id = :salonId AND l.rendezVous.statut != :statutAnnule AND l.rendezVous.statut != com.kadi_aon.mon_salon.rendezvous.enums.StatutRendezVous.NO_SHOW AND l.dateHeureDebut < :fin AND l.dateHeureFin > :debut")
    List<LigneRendezVous> findLignesActivesBySalonAndPeriode(
            @Param("salonId") Long salonId,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin,
            @Param("statutAnnule") StatutRendezVous statutAnnule);
}

package com.kadi_aon.mon_salon.coiffeur.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.coiffeur.entity.IndisponibiliteCoiffeur;

@Repository
public interface IndisponibiliteCoiffeurRepository extends JpaRepository<IndisponibiliteCoiffeur, Long> {

    List<IndisponibiliteCoiffeur> findByCoiffeurIdOrderByDateDebutAsc(Long coiffeurAffectationId);

    @Query("SELECT i FROM IndisponibiliteCoiffeur i WHERE i.coiffeur.salon.id = :salonId AND i.dateDebut < :fin AND i.dateFin > :debut")
    List<IndisponibiliteCoiffeur> findBySalonIdAndPeriode(
            @Param("salonId") Long salonId,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin);

    @Query("SELECT i FROM IndisponibiliteCoiffeur i WHERE i.coiffeur.id = :coiffeurId AND i.dateDebut < :fin AND i.dateFin > :debut")
    List<IndisponibiliteCoiffeur> findByCoiffeurIdAndPeriode(
            @Param("coiffeurId") Long coiffeurId,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin);

    List<IndisponibiliteCoiffeur> findByCoiffeurSalonSlugOrderByDateDebutAsc(String slugSalon);

    java.util.Optional<IndisponibiliteCoiffeur> findByIdAndCoiffeurSalonSlug(Long id, String slugSalon);
}

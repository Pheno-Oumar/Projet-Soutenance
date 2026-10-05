package com.kadi_aon.mon_salon.salon.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.salon.entity.FermetureExceptionnelle;

@Repository
public interface FermetureExceptionnelleRepository extends JpaRepository<FermetureExceptionnelle, Long> {

    List<FermetureExceptionnelle> findBySalonIdOrderByDateDebutAsc(Long salonId);

    List<FermetureExceptionnelle> findBySalonSlugOrderByDateDebutAsc(String slug);

    @Query("SELECT f FROM FermetureExceptionnelle f WHERE f.salon.id = :salonId AND f.dateDebut <= :dateFin AND f.dateFin >= :dateDebut")
    List<FermetureExceptionnelle> findChevauchements(
            @Param("salonId") Long salonId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    @Query("SELECT f FROM FermetureExceptionnelle f WHERE f.salon.id = :salonId AND f.id <> :id AND f.dateDebut <= :dateFin AND f.dateFin >= :dateDebut")
    List<FermetureExceptionnelle> findChevauchementsExcluant(
            @Param("salonId") Long salonId,
            @Param("id") Long id,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );
}

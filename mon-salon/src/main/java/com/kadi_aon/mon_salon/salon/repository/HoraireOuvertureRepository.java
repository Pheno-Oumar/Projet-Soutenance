package com.kadi_aon.mon_salon.salon.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.salon.entity.HoraireOuverture;
import com.kadi_aon.mon_salon.salon.enums.JourSemaine;

@Repository
public interface HoraireOuvertureRepository extends JpaRepository<HoraireOuverture, Long> {

    List<HoraireOuverture> findBySalonIdOrderByJourSemaineAsc(Long salonId);

    List<HoraireOuverture> findBySalonSlugOrderByJourSemaineAsc(String slug);

    Optional<HoraireOuverture> findBySalonIdAndJourSemaine(Long salonId, JourSemaine jourSemaine);

    Optional<HoraireOuverture> findBySalonSlugAndJourSemaine(String slug, JourSemaine jourSemaine);

    boolean existsBySalonIdAndJourSemaine(Long salonId, JourSemaine jourSemaine);
}

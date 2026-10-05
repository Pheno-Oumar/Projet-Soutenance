package com.kadi_aon.mon_salon.regle.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.regle.entity.ReglePlateforme;

@Repository
public interface ReglePlateformeRepository extends JpaRepository<ReglePlateforme, Long> {

    List<ReglePlateforme> findAllByOrderByDateCreationDesc();

    List<ReglePlateforme> findByActifTrueOrderByDateCreationDesc();
}

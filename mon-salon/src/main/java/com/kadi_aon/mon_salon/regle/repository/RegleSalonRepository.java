package com.kadi_aon.mon_salon.regle.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.regle.entity.RegleSalon;

@Repository
public interface RegleSalonRepository extends JpaRepository<RegleSalon, Long> {

    List<RegleSalon> findBySalonSlugOrderByDateCreationDesc(String slugSalon);

    Optional<RegleSalon> findByIdAndSalonSlug(Long id, String slugSalon);
}

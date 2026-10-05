package com.kadi_aon.mon_salon.salon.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.salon.entity.VarianteService;

@Repository
public interface VarianteServiceRepository extends JpaRepository<VarianteService, Long> {

    List<VarianteService> findByServiceSalonId(Long serviceSalonId);

    Optional<VarianteService> findByIdAndServiceSalonId(Long id, Long serviceSalonId);

    boolean existsByServiceSalonIdAndNomIgnoreCase(Long serviceSalonId, String nom);
}

package com.kadi_aon.scheduler.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.scheduler.entity.DemandeExportDonnees;
import com.kadi_aon.scheduler.enums.StatutExportDonnees;

@Repository
public interface DemandeExportDonneesRepository extends JpaRepository<DemandeExportDonnees, Long> {

    List<DemandeExportDonnees> findByStatutAndDateExpirationBefore(StatutExportDonnees statut, LocalDateTime now);

    List<DemandeExportDonnees> findByStatutAndDateExpirationBefore(StatutExportDonnees statut, LocalDateTime now, org.springframework.data.domain.Pageable pageable);
}

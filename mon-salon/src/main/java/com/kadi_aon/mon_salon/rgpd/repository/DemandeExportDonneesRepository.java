package com.kadi_aon.mon_salon.rgpd.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.rgpd.entity.DemandeExportDonnees;
import com.kadi_aon.mon_salon.rgpd.enums.StatutExportDonnees;

@Repository
public interface DemandeExportDonneesRepository extends JpaRepository<DemandeExportDonnees, Long> {

    List<DemandeExportDonnees> findByCompteEmailOrderByDateDemandeDesc(String email);

    List<DemandeExportDonnees> findByStatutAndDateExpirationBefore(StatutExportDonnees statut, LocalDateTime now);

    List<DemandeExportDonnees> findByStatut(StatutExportDonnees statut);
}

package com.kadi_aon.scheduler.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.kadi_aon.scheduler.entity.DemandeExportDonnees;
import com.kadi_aon.scheduler.enums.StatutExportDonnees;
import com.kadi_aon.scheduler.repository.DemandeExportDonneesRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryPurgeScheduler {

    private static final int BATCH_SIZE = 50;

    private final DemandeExportDonneesRepository demandeExportDonneesRepository;
    private final CloudinaryWorkerService cloudinaryWorkerService;

    @Scheduled(fixedDelay = 60000)
    public void purgerFichiersExpires() {
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        List<DemandeExportDonnees> demandesExpirees = demandeExportDonneesRepository
                .findByStatutAndDateExpirationBefore(StatutExportDonnees.DISPONIBLE, now, pageable);

        if (demandesExpirees.isEmpty()) {
            return;
        }

        log.info("[CLOUDINARY PURGE] Snapshot capturé : {} export(s) RGPD expiré(s) transféré(s) au worker asynchrone.",
                demandesExpirees.size());

        for (DemandeExportDonnees demande : demandesExpirees) {
            cloudinaryWorkerService.purgerExportAsync(demande.getId(), demande.getCloudinaryPublicId());
        }
    }
}


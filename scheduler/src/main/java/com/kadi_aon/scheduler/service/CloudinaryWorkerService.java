package com.kadi_aon.scheduler.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.scheduler.enums.StatutExportDonnees;
import com.kadi_aon.scheduler.repository.DemandeExportDonneesRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryWorkerService {

    private final DemandeExportDonneesRepository demandeExportDonneesRepository;
    private final CloudinaryService cloudinaryService;

    @Async("cloudinaryTaskExecutor")
    public void purgerExportAsync(Long demandeId, String cloudinaryPublicId) {
        log.debug("[CLOUDINARY WORKER] Début de purge asynchrone pour l'export #{}", demandeId);
        try {
            if (cloudinaryPublicId != null && !cloudinaryPublicId.isBlank()) {
                cloudinaryService.deleteRawFile(cloudinaryPublicId);
            }
            marquerExpire(demandeId);
            log.info("[CLOUDINARY WORKER] Demande #{} marquée comme expirée et fichier raw supprimé.", demandeId);
        } catch (Exception e) {
            log.error("[CLOUDINARY WORKER] Erreur lors de la purge asynchrone de la demande #{} : {}", demandeId, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marquerExpire(Long demandeId) {
        demandeExportDonneesRepository.findById(demandeId).ifPresent(demande -> {
            demande.setStatut(StatutExportDonnees.EXPIRE);
            demandeExportDonneesRepository.save(demande);
        });
    }
}

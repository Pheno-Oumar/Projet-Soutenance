package com.kadi_aon.scheduler.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.scheduler.entity.NotificationEmail;
import com.kadi_aon.scheduler.enums.StatutNotification;
import com.kadi_aon.scheduler.repository.NotificationEmailRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationScheduler {

    private static final int BATCH_SIZE = 5;
    private static final int STALE_MINUTES = 15;

    private final NotificationEmailRepository notificationEmailRepository;
    private final EmailWorkerService emailWorkerService;

    @Scheduled(fixedDelay = 5000)
    public void dispatcherEmailsEnAttente() {
        // 1. Récupération & marquage atomique du snapshot (très rapide, quelques millisecondes)
        List<NotificationEmail> snapshot = capturerEtVerrouillerSnapshot();

        if (snapshot.isEmpty()) {
            return;
        }

        log.info("[EMAIL SCHEDULER] Snapshot capturé : {} email(s) transféré(s) aux workers asynchrones.", snapshot.size());

        // 2. Délégation immédiate aux workers sans bloquer le thread du scheduler
        for (NotificationEmail email : snapshot) {
            emailWorkerService.traiterEmailAsync(
                    email.getId(),
                    email.getDestinataire(),
                    email.getSujet(),
                    email.getContenu()
            );
        }
    }

    @Transactional
    public List<NotificationEmail> capturerEtVerrouillerSnapshot() {
        // Récupération fail-safe des tâches EN_COURS abandonnées (ex: crash antérieur)
        LocalDateTime staleThreshold = LocalDateTime.now().minusMinutes(STALE_MINUTES);
        List<NotificationEmail> staleEmails = notificationEmailRepository.findStaleEnCoursEmails(staleThreshold);
        if (!staleEmails.isEmpty()) {
            log.warn("[EMAIL SCHEDULER] Récupération de {} notification(s) bloquée(s) en EN_COURS depuis plus de {} minutes.",
                    staleEmails.size(), STALE_MINUTES);
            for (NotificationEmail stale : staleEmails) {
                stale.setStatut(StatutNotification.EN_ATTENTE);
            }
            notificationEmailRepository.saveAll(staleEmails);
        }

        // Capture d'un lot limité
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        List<NotificationEmail> snapshot = notificationEmailRepository
                .findByStatutOrderByDateCreationAsc(StatutNotification.EN_ATTENTE, pageable);

        if (snapshot.isEmpty()) {
            return List.of();
        }

        // Bascule instantanée en EN_COURS pour isoler le snapshot
        for (NotificationEmail email : snapshot) {
            email.setStatut(StatutNotification.EN_COURS);
        }

        return notificationEmailRepository.saveAll(snapshot);
    }
}

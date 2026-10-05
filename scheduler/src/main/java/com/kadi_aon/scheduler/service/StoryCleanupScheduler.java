package com.kadi_aon.scheduler.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.scheduler.entity.StorySalon;
import com.kadi_aon.scheduler.repository.StorySalonRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryCleanupScheduler {

    private final StorySalonRepository storySalonRepository;
    private final CloudinaryService cloudinaryService;

    /**
     * Purge périodique (toutes les 10 minutes) des stories de salons ayant dépassé
     * leur durée de vie de 24 heures. Supprime le média sur Cloudinary et la ligne en BDD.
     */
    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void purgerStoriesExpirees() {
        LocalDateTime now = LocalDateTime.now();
        List<StorySalon> expiredStories = storySalonRepository.findByDateExpirationBefore(now);

        if (expiredStories.isEmpty()) {
            return;
        }

        log.info("[STORIES PURGE] Début du nettoyage des stories expirées : {} story(ies) à supprimer.", expiredStories.size());

        for (StorySalon story : expiredStories) {
            try {
                if (story.getMediaUrl() != null && !story.getMediaUrl().isBlank()) {
                    String resourceType = story.getTypeMedia() != null ? story.getTypeMedia().name().toLowerCase() : "image";
                    cloudinaryService.deleteMediaByUrl(story.getMediaUrl(), resourceType);
                }
            } catch (Exception e) {
                log.warn("[STORIES PURGE] Erreur lors de la suppression Cloudinary pour la story ID {} : {}", story.getId(), e.getMessage());
            }
        }

        storySalonRepository.deleteAll(expiredStories);
        log.info("[STORIES PURGE] Nettoyage terminé : {} story(ies) expirées purgées avec succès.", expiredStories.size());
    }
}

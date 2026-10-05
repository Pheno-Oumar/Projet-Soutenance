package com.kadi_aon.mon_salon.story.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.common.service.CloudinaryService;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;
import com.kadi_aon.mon_salon.story.dto.SalonStoriesDTOResponse;
import com.kadi_aon.mon_salon.story.dto.StoryDTOResponse;
import com.kadi_aon.mon_salon.story.entity.StorySalon;
import com.kadi_aon.mon_salon.story.enums.TypeMediaStory;
import com.kadi_aon.mon_salon.story.repository.StorySalonRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorySalonService {

    private final StorySalonRepository storySalonRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final SalonRepository salonRepository;
    private final CloudinaryService cloudinaryService;
    private final AuditLogService auditLogService;

    @Transactional
    public StoryDTOResponse publierStory(String slugSalon, String managerEmail, MultipartFile mediaFile) throws IOException {
        AffectationSalon affectation = validerManager(slugSalon, managerEmail);
        Salon salon = affectation.getSalon();

        if (mediaFile == null || mediaFile.isEmpty()) {
            throw new IllegalArgumentException("Le fichier média de la story est obligatoire.");
        }

        String contentType = mediaFile.getContentType();
        TypeMediaStory typeMedia = (contentType != null && contentType.startsWith("video"))
                ? TypeMediaStory.VIDEO
                : TypeMediaStory.IMAGE;

        String mediaUrl = cloudinaryService.uploadMediaStory(mediaFile, slugSalon);
        LocalDateTime now = LocalDateTime.now();

        StorySalon story = StorySalon.builder()
                .salon(salon)
                .auteur(affectation.getCompte())
                .mediaUrl(mediaUrl)
                .typeMedia(typeMedia)
                .dateCreation(now)
                .dateExpiration(now.plusHours(24))
                .actif(true)
                .build();

        StorySalon saved = storySalonRepository.save(story);

        auditLogService.logActionSalon(
                TypeActionAudit.CREATION,
                "StorySalon",
                String.valueOf(saved.getId()),
                null,
                String.format("Story 24h (%s) publiée pour le salon '%s'", typeMedia, slugSalon),
                affectation,
                TypeRoleSalon.MANAGER.name()
        );

        log.info("Story ID {} publiée avec succès par {} pour le salon {} (expire à {})",
                saved.getId(), managerEmail, slugSalon, saved.getDateExpiration());

        return mapToStoryResponse(saved);
    }

    @Transactional
    public void supprimerStory(String slugSalon, Long storyId, String managerEmail) {
        AffectationSalon affectation = validerManager(slugSalon, managerEmail);

        StorySalon story = storySalonRepository.findByIdAndSalonSlug(storyId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Story introuvable ID " + storyId + " dans le salon " + slugSalon));

        // Purge Cloudinary immédiate
        if (story.getMediaUrl() != null && !story.getMediaUrl().isBlank()) {
            cloudinaryService.deleteMediaByUrl(story.getMediaUrl(), story.getTypeMedia().name().toLowerCase());
        }

        storySalonRepository.delete(story);

        auditLogService.logActionSalon(
                TypeActionAudit.SUPPRESSION,
                "StorySalon",
                String.valueOf(storyId),
                story.getMediaUrl(),
                "SUPPRIMEE",
                affectation,
                TypeRoleSalon.MANAGER.name()
        );

        log.info("Story ID {} supprimée manuellement par le manager {}", storyId, managerEmail);
    }

    @Transactional(readOnly = true)
    public List<StoryDTOResponse> listerStoriesSalon(String slugSalon) {
        if (!salonRepository.existsBySlug(slugSalon)) {
            throw new EntityNotFoundException("Salon introuvable pour le slug : " + slugSalon);
        }

        LocalDateTime now = LocalDateTime.now();
        List<StorySalon> list = storySalonRepository.findActiveStoriesBySalonSlug(slugSalon, now);
        return list.stream().map(this::mapToStoryResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SalonStoriesDTOResponse> listerStoriesTousSalonsPourKadys() {
        LocalDateTime now = LocalDateTime.now();
        List<StorySalon> activeStories = storySalonRepository.findAllActiveStoriesWithSalon(now);

        Map<Salon, List<StoryDTOResponse>> groupedBySalon = new LinkedHashMap<>();
        for (StorySalon story : activeStories) {
            Salon s = story.getSalon();
            groupedBySalon.computeIfAbsent(s, k -> new ArrayList<>()).add(mapToStoryResponse(story));
        }

        List<SalonStoriesDTOResponse> result = new ArrayList<>();
        for (Map.Entry<Salon, List<StoryDTOResponse>> entry : groupedBySalon.entrySet()) {
            Salon s = entry.getKey();
            List<StoryDTOResponse> stories = entry.getValue();
            result.add(new SalonStoriesDTOResponse(
                    s.getId(),
                    s.getNom(),
                    s.getSlug(),
                    s.getLogoUrl(),
                    stories.size(),
                    stories
            ));
        }

        return result;
    }

    // ==========================================
    // HELPERS & MAPPERS
    // ==========================================

    private AffectationSalon validerManager(String slugSalon, String email) {
        AffectationSalon affectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Aucune affectation active trouvée pour " + email + " dans le salon " + slugSalon));

        boolean isManager = affectation.getRoles().stream()
                .anyMatch(r -> r.getRole() == TypeRoleSalon.MANAGER);

        if (!isManager) {
            throw new IllegalArgumentException("L'utilisateur " + email + " n'a pas le rôle MANAGER dans ce salon.");
        }

        return affectation;
    }

    public StoryDTOResponse mapToStoryResponse(StorySalon s) {
        Salon sal = s.getSalon();
        return new StoryDTOResponse(
                s.getId(),
                s.getMediaUrl(),
                s.getTypeMedia(),
                s.getDateCreation(),
                s.getDateExpiration(),
                sal != null ? sal.getSlug() : null,
                sal != null ? sal.getNom() : null,
                sal != null ? sal.getLogoUrl() : null
        );
    }
}

package com.kadi_aon.mon_salon.realisation.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.common.service.CloudinaryService;
import com.kadi_aon.mon_salon.realisation.dto.RealisationCreateDTORequest;
import com.kadi_aon.mon_salon.realisation.dto.RealisationDTOResponse;
import com.kadi_aon.mon_salon.realisation.dto.RealisationUpdateDTORequest;
import com.kadi_aon.mon_salon.realisation.entity.Realisation;
import com.kadi_aon.mon_salon.realisation.repository.RealisationRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealisationSalonService {

    private final RealisationRepository realisationRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final SalonRepository salonRepository;
    private final CompteRepository compteRepository;
    private final CloudinaryService cloudinaryService;
    private final AuditLogService auditLogService;

    // ==========================================
    // 1. GESTION PAR LE MANAGER
    // ==========================================

    @Transactional
    public RealisationDTOResponse creerRealisation(
            String slugSalon,
            String managerEmail,
            RealisationCreateDTORequest request,
            MultipartFile videoFile) throws IOException {

        AffectationSalon affectation = validerManager(slugSalon, managerEmail);
        Salon salon = affectation.getSalon();

        if (videoFile == null || videoFile.isEmpty()) {
            throw new IllegalArgumentException("Une réalisation doit obligatoirement être accompagnée d'une vidéo.");
        }

        Compte coiffeur = null;
        if (request.coiffeurId() != null) {
            coiffeur = validerCoiffeur(slugSalon, request.coiffeurId());
        }

        String videoUrl = cloudinaryService.uploadVideoRealisation(videoFile, slugSalon);

        boolean publier = Boolean.TRUE.equals(request.publierImmediatement());
        LocalDateTime datePublication = publier ? LocalDateTime.now() : null;

        Realisation realisation = Realisation.builder()
                .salon(salon)
                .coiffeur(coiffeur)
                .titre(request.titre().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .urlVideo(videoUrl)
                .dateRealisation(request.dateRealisation())
                .datePublication(datePublication)
                .statutPublication(publier)
                .dateCreation(LocalDateTime.now())
                .build();

        Realisation saved = realisationRepository.save(realisation);

        auditLogService.logActionSalon(
                TypeActionAudit.CREATION,
                "Realisation",
                String.valueOf(saved.getId()),
                null,
                "Création: " + saved.getTitre() + " (publié: " + saved.isStatutPublication() + ")",
                affectation,
                TypeRoleSalon.MANAGER.name()
        );

        log.info("Réalisation ID {} créée avec succès par le manager {} dans le salon {}", saved.getId(), managerEmail, slugSalon);
        return mapToResponse(saved);
    }

    @Transactional
    public RealisationDTOResponse modifierRealisation(
            String slugSalon,
            Long realisationId,
            String managerEmail,
            RealisationUpdateDTORequest request,
            MultipartFile videoFile) throws IOException {

        AffectationSalon affectation = validerManager(slugSalon, managerEmail);

        Realisation realisation = realisationRepository.findByIdAndSalonSlug(realisationId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Réalisation introuvable ID " + realisationId + " dans le salon " + slugSalon));

        String ancienneValeur = "Titre=" + realisation.getTitre() + ", Video=" + realisation.getUrlVideo();

        if (request.coiffeurId() != null) {
            Compte coiffeur = validerCoiffeur(slugSalon, request.coiffeurId());
            realisation.setCoiffeur(coiffeur);
        }

        if (request.titre() != null && !request.titre().isBlank()) {
            realisation.setTitre(request.titre().trim());
        }

        if (request.description() != null) {
            realisation.setDescription(request.description().trim());
        }

        if (request.dateRealisation() != null) {
            realisation.setDateRealisation(request.dateRealisation());
        }

        // Si une nouvelle vidéo est envoyée, on supprime l'ancienne de Cloudinary puis on upload la nouvelle
        if (videoFile != null && !videoFile.isEmpty()) {
            String ancienneVideo = realisation.getUrlVideo();
            String nouvelleVideoUrl = cloudinaryService.uploadVideoRealisation(videoFile, slugSalon);
            realisation.setUrlVideo(nouvelleVideoUrl);

            if (ancienneVideo != null && !ancienneVideo.isBlank()) {
                cloudinaryService.deleteMediaByUrl(ancienneVideo, "video");
            }
        }

        realisation.setDateModification(LocalDateTime.now());
        Realisation saved = realisationRepository.save(realisation);

        String nouvelleValeur = "Titre=" + saved.getTitre() + ", Video=" + saved.getUrlVideo();
        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "Realisation",
                String.valueOf(saved.getId()),
                ancienneValeur,
                nouvelleValeur,
                affectation,
                TypeRoleSalon.MANAGER.name()
        );

        log.info("Réalisation ID {} mise à jour par le manager {}", saved.getId(), managerEmail);
        return mapToResponse(saved);
    }

    @Transactional
    public RealisationDTOResponse modifierRealisation(
            String slugSalon,
            Long realisationId,
            String managerEmail,
            RealisationUpdateDTORequest request) throws IOException {
        return modifierRealisation(slugSalon, realisationId, managerEmail, request, null);
    }

    @Transactional
    public RealisationDTOResponse uploadVideoRealisation(
            String slugSalon,
            Long realisationId,
            String managerEmail,
            MultipartFile videoFile) throws IOException {

        AffectationSalon affectation = validerManager(slugSalon, managerEmail);

        if (videoFile == null || videoFile.isEmpty()) {
            throw new IllegalArgumentException("Le fichier vidéo est obligatoire pour la mise à jour.");
        }

        Realisation realisation = realisationRepository.findByIdAndSalonSlug(realisationId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Réalisation introuvable ID " + realisationId + " dans le salon " + slugSalon));

        String ancienneVideo = realisation.getUrlVideo();
        String nouvelleVideoUrl = cloudinaryService.uploadVideoRealisation(videoFile, slugSalon);
        realisation.setUrlVideo(nouvelleVideoUrl);
        realisation.setDateModification(LocalDateTime.now());

        if (ancienneVideo != null && !ancienneVideo.isBlank()) {
            cloudinaryService.deleteMediaByUrl(ancienneVideo, "video");
        }

        Realisation saved = realisationRepository.save(realisation);

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "Realisation (Video)",
                String.valueOf(saved.getId()),
                "Ancienne video: " + ancienneVideo,
                "Nouvelle video: " + nouvelleVideoUrl,
                affectation,
                TypeRoleSalon.MANAGER.name()
        );

        log.info("Vidéo de la réalisation ID {} mise à jour par le manager {}", saved.getId(), managerEmail);
        return mapToResponse(saved);
    }

    @Transactional
    public RealisationDTOResponse modifierStatutPublication(
            String slugSalon,
            Long realisationId,
            String managerEmail,
            boolean statutPublication) {

        AffectationSalon affectation = validerManager(slugSalon, managerEmail);

        Realisation realisation = realisationRepository.findByIdAndSalonSlug(realisationId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Réalisation introuvable ID " + realisationId + " dans le salon " + slugSalon));

        boolean ancienStatut = realisation.isStatutPublication();
        realisation.setStatutPublication(statutPublication);

        if (statutPublication && realisation.getDatePublication() == null) {
            realisation.setDatePublication(LocalDateTime.now());
        }

        realisation.setDateModification(LocalDateTime.now());
        Realisation saved = realisationRepository.save(realisation);

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "Realisation (Publication)",
                String.valueOf(saved.getId()),
                "Statut: " + ancienStatut,
                "Statut: " + saved.isStatutPublication(),
                affectation,
                TypeRoleSalon.MANAGER.name()
        );

        log.info("Statut publication réalisation ID {} modifié: {} -> {}", saved.getId(), ancienStatut, statutPublication);
        return mapToResponse(saved);
    }

    @Transactional
    public void supprimerRealisation(String slugSalon, Long realisationId, String managerEmail) {
        AffectationSalon affectation = validerManager(slugSalon, managerEmail);

        Realisation realisation = realisationRepository.findByIdAndSalonSlug(realisationId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Réalisation introuvable ID " + realisationId + " dans le salon " + slugSalon));

        // Supprimer la vidéo Cloudinary associée
        if (realisation.getUrlVideo() != null && !realisation.getUrlVideo().isBlank()) {
            cloudinaryService.deleteMediaByUrl(realisation.getUrlVideo(), "video");
        }

        realisationRepository.delete(realisation);

        auditLogService.logActionSalon(
                TypeActionAudit.SUPPRESSION,
                "Realisation",
                String.valueOf(realisationId),
                realisation.getTitre(),
                "SUPPRIMEE",
                affectation,
                TypeRoleSalon.MANAGER.name()
        );

        log.info("Réalisation ID {} supprimée par le manager {}", realisationId, managerEmail);
    }

    @Transactional(readOnly = true)
    public RealisationDTOResponse obtenirRealisationPourManager(String slugSalon, Long realisationId, String managerEmail) {
        validerManager(slugSalon, managerEmail);
        Realisation r = realisationRepository.findByIdAndSalonSlug(realisationId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Réalisation introuvable ID " + realisationId + " dans le salon " + slugSalon));
        return mapToResponse(r);
    }

    @Transactional(readOnly = true)
    public List<RealisationDTOResponse> listerRealisationsPourManager(String slugSalon, String managerEmail, Boolean statutPublicationOpt) {
        validerManager(slugSalon, managerEmail);
        List<Realisation> list;
        if (statutPublicationOpt != null) {
            list = realisationRepository.findBySalonSlugAndStatutPublicationOrderByDateCreationDesc(slugSalon, statutPublicationOpt);
        } else {
            list = realisationRepository.findBySalonSlugOrderByDateCreationDesc(slugSalon);
        }
        return list.stream().map(this::mapToResponse).toList();
    }

    // ==========================================
    // 2. CONSULTATION CLIENT (PUBLIEES UNIQUEMENT)
    // ==========================================

    @Transactional(readOnly = true)
    public List<RealisationDTOResponse> listerRealisationsPubliees(String slugSalon) {
        if (!salonRepository.existsBySlug(slugSalon)) {
            throw new EntityNotFoundException("Salon introuvable pour le slug : " + slugSalon);
        }
        // 🔥 Seulement si statutPublication = true
        return realisationRepository.findBySalonSlugAndStatutPublicationTrueOrderByDatePublicationDesc(slugSalon).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RealisationDTOResponse> listerToutesLesRealisationsPubliees() {
        return realisationRepository.findByStatutPublicationTrueOrderByDatePublicationDesc().stream()
                .map(this::mapToResponse)
                .toList();
    }


    @Transactional(readOnly = true)
    public RealisationDTOResponse obtenirRealisationPubliee(String slugSalon, Long realisationId) {
        // 🔥 Seulement si statutPublication = true
        Realisation r = realisationRepository.findByIdAndSalonSlugAndStatutPublicationTrue(realisationId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Réalisation publiée introuvable ID " + realisationId + " pour le salon " + slugSalon));
        return mapToResponse(r);
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

    private Compte validerCoiffeur(String slugSalon, Long coiffeurId) {
        Compte coiffeur = compteRepository.findById(coiffeurId)
                .orElseThrow(() -> new EntityNotFoundException("Compte coiffeur introuvable ID " + coiffeurId));

        boolean isCoiffeurValide = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(coiffeur.getEmail(), slugSalon)
                .map(aff -> aff.getRoles().stream().anyMatch(r -> r.getRole() == TypeRoleSalon.COIFFEUR))
                .orElse(false);

        if (!isCoiffeurValide) {
            throw new IllegalArgumentException("L'employé ID " + coiffeurId + " n'a pas le rôle COIFFEUR actif dans ce salon.");
        }

        return coiffeur;
    }

    private RealisationDTOResponse mapToResponse(Realisation r) {
        Salon s = r.getSalon();
        Compte c = r.getCoiffeur();
        String coiffeurNomComplet = c != null ? c.getPrenom() + " " + c.getNom() : null;

        return new RealisationDTOResponse(
                r.getId(),
                r.getTitre(),
                r.getDescription(),
                r.getUrlVideo(),
                r.getDateRealisation(),
                r.getDatePublication(),
                r.isStatutPublication(),
                s != null ? s.getSlug() : null,
                s != null ? s.getNom() : null,
                c != null ? c.getId() : null,
                coiffeurNomComplet,
                r.getTotalLikes(),
                r.getTotalCommentaires(),
                r.getTotalVues(),
                r.getDateCreation(),
                r.getDateModification()
        );
    }
}

package com.kadi_aon.mon_salon.reclamation.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.notification.service.NotificationEmailService;
import com.kadi_aon.mon_salon.reclamation.dto.ReclamationCreateDTORequest;
import com.kadi_aon.mon_salon.reclamation.dto.ReclamationDTOResponse;
import com.kadi_aon.mon_salon.reclamation.dto.ReclamationTraiterDTORequest;
import com.kadi_aon.mon_salon.reclamation.entity.Reclamation;
import com.kadi_aon.mon_salon.reclamation.enums.StatutReclamation;
import com.kadi_aon.mon_salon.reclamation.repository.ReclamationRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReclamationSalonService {

    private final ReclamationRepository reclamationRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final SalonRepository salonRepository;
    private final NotificationEmailService notificationEmailService;
    private final AuditLogService auditLogService;

    @Transactional
    public ReclamationDTOResponse deposerReclamation(String slugSalon, String clientEmail, ReclamationCreateDTORequest request) {
        AffectationSalon affectation = affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue(clientEmail, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Affectation client active introuvable pour " + clientEmail + " sur le salon " + slugSalon));

        Reclamation reclamation = Reclamation.builder()
                .affectationClient(affectation)
                .salon(affectation.getSalon())
                .objet(request.objet())
                .description(request.description())
                .statut(StatutReclamation.EN_ATTENTE)
                .dateCreation(LocalDateTime.now())
                .build();

        Reclamation saved = reclamationRepository.save(reclamation);

        auditLogService.logActionSalon(
                TypeActionAudit.CREATION,
                "Reclamation",
                String.valueOf(saved.getId()),
                null,
                "Dépôt réclamation: " + saved.getObjet(),
                affectation,
                TypeRoleSalon.CLIENT.name()
        );

        log.info("Réclamation {} déposée par le client {} sur le salon {}", saved.getId(), clientEmail, slugSalon);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReclamationDTOResponse> listerReclamationsClient(String slugSalon, String clientEmail) {
        if (!salonRepository.existsBySlug(slugSalon)) {
            throw new EntityNotFoundException("Salon introuvable pour le slug : " + slugSalon);
        }
        return reclamationRepository.findByAffectationClientCompteEmailAndSalonSlugOrderByDateCreationDesc(clientEmail, slugSalon).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReclamationDTOResponse obtenirReclamationClient(String slugSalon, Long reclamationId, String clientEmail) {
        Reclamation reclamation = reclamationRepository.findByIdAndAffectationClientCompteEmailAndSalonSlug(reclamationId, clientEmail, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Réclamation introuvable avec l'ID " + reclamationId + " pour ce client sur ce salon"));
        return mapToResponse(reclamation);
    }

    @Transactional(readOnly = true)
    public List<ReclamationDTOResponse> listerToutesReclamationsClient(String clientEmail) {
        return reclamationRepository.findByAffectationClientCompteEmailOrderByDateCreationDesc(clientEmail).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReclamationDTOResponse obtenirReclamationClientGlobal(Long reclamationId, String clientEmail) {
        Reclamation reclamation = reclamationRepository.findByIdAndAffectationClientCompteEmail(reclamationId, clientEmail)
                .orElseThrow(() -> new EntityNotFoundException("Réclamation introuvable avec l'ID " + reclamationId));
        return mapToResponse(reclamation);
    }


    @Transactional(readOnly = true)
    public List<ReclamationDTOResponse> listerReclamationsSalon(String slugSalon, StatutReclamation statut) {
        if (!salonRepository.existsBySlug(slugSalon)) {
            throw new EntityNotFoundException("Salon introuvable pour le slug : " + slugSalon);
        }

        List<Reclamation> reclamations = (statut != null)
                ? reclamationRepository.findBySalonSlugAndStatutOrderByDateCreationDesc(slugSalon, statut)
                : reclamationRepository.findBySalonSlugOrderByDateCreationDesc(slugSalon);

        return reclamations.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReclamationDTOResponse obtenirReclamationSalon(String slugSalon, Long reclamationId) {
        Reclamation reclamation = reclamationRepository.findByIdAndSalonSlug(reclamationId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Réclamation introuvable avec l'ID " + reclamationId + " pour le salon " + slugSalon));
        return mapToResponse(reclamation);
    }

    @Transactional
    public ReclamationDTOResponse traiterReclamation(String slugSalon, Long reclamationId, String staffEmail, ReclamationTraiterDTORequest request) {
        AffectationSalon staffAffectation = validerManagerOuProprietaire(slugSalon, staffEmail);

        Reclamation reclamation = reclamationRepository.findByIdAndSalonSlug(reclamationId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Réclamation introuvable avec l'ID " + reclamationId + " pour le salon " + slugSalon));

        if (reclamation.getStatut() == StatutReclamation.RESOLUE || reclamation.getStatut() == StatutReclamation.REJETEE) {
            throw new IllegalStateException("Cette réclamation a déjà été clôturée (statut: " + reclamation.getStatut() + ").");
        }

        String ancienStatut = reclamation.getStatut().name();
        reclamation.setStatut(request.nouveauStatut());
        reclamation.setReponseTraitement(request.reponse());
        reclamation.setDateTraitement(LocalDateTime.now());
        reclamation.setTraitePar(staffAffectation);

        Reclamation updated = reclamationRepository.save(reclamation);

        // Notification envoyée au client
        String clientEmail = reclamation.getAffectationClient().getCompte().getEmail();
        String clientPrenom = reclamation.getAffectationClient().getCompte().getPrenom();
        String sujet = "Mise à jour de votre réclamation - " + reclamation.getSalon().getNom();
        String contenu = String.format(
                "Bonjour %s,\n\n" +
                "Votre réclamation concernant : \"%s\" a été traitée par la direction du salon.\n\n" +
                "Statut : %s\n" +
                "Réponse : %s\n\n" +
                "Cordialement,\n" +
                "L'équipe %s.",
                clientPrenom,
                reclamation.getObjet(),
                request.nouveauStatut().name(),
                request.reponse(),
                reclamation.getSalon().getNom()
        );

        notificationEmailService.queueNotificationEmail(clientEmail, sujet, contenu);

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "Reclamation",
                String.valueOf(updated.getId()),
                ancienStatut,
                "Traitement réclamation passé à " + request.nouveauStatut().name(),
                staffAffectation,
                staffAffectation.getRoles().stream().findFirst().map(r -> r.getRole().name()).orElse("STAFF")
        );

        log.info("Réclamation {} traitée par {} avec statut {}", updated.getId(), staffEmail, request.nouveauStatut());
        return mapToResponse(updated);
    }

    private AffectationSalon validerManagerOuProprietaire(String slugSalon, String email) {
        AffectationSalon affectation = affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Affectation active introuvable pour l'utilisateur " + email + " sur le salon " + slugSalon));

        boolean hasRole = affectation.getRoles().stream()
                .anyMatch(r -> r.getRole() == TypeRoleSalon.MANAGER || r.getRole() == TypeRoleSalon.PROPRIETAIRE);

        if (!hasRole) {
            throw new IllegalArgumentException("L'utilisateur " + email + " n'a pas les droits pour traiter les réclamations sur ce salon (MANAGER ou PROPRIETAIRE requis).");
        }
        return affectation;
    }

    private ReclamationDTOResponse mapToResponse(Reclamation r) {
        String clientNom = r.getAffectationClient() != null && r.getAffectationClient().getCompte() != null
                ? r.getAffectationClient().getCompte().getPrenom() + " " + r.getAffectationClient().getCompte().getNom()
                : "N/A";
        String clientEmail = r.getAffectationClient() != null && r.getAffectationClient().getCompte() != null
                ? r.getAffectationClient().getCompte().getEmail()
                : "N/A";

        String traiteParNom = r.getTraitePar() != null && r.getTraitePar().getCompte() != null
                ? r.getTraitePar().getCompte().getPrenom() + " " + r.getTraitePar().getCompte().getNom()
                : null;

        return new ReclamationDTOResponse(
                r.getId(),
                r.getObjet(),
                r.getDescription(),
                r.getStatut().name(),
                r.getReponseTraitement(),
                r.getDateCreation(),
                r.getDateTraitement(),
                clientNom,
                clientEmail,
                traiteParNom,
                r.getSalon().getSlug()
        );
    }
}

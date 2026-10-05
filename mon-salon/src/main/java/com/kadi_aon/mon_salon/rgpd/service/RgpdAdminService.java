package com.kadi_aon.mon_salon.rgpd.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.notification.service.NotificationEmailService;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeSuppressionDecisionDTORequest;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeSuppressionDTOResponse;
import com.kadi_aon.mon_salon.rgpd.entity.DemandeSuppressionCompte;
import com.kadi_aon.mon_salon.rgpd.enums.StatutDemandeSuppression;
import com.kadi_aon.mon_salon.rgpd.repository.DemandeSuppressionCompteRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RgpdAdminService {

    private final DemandeSuppressionCompteRepository demandeSuppressionRepository;
    private final CompteRepository compteRepository;
    private final NotificationEmailService notificationEmailService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<DemandeSuppressionDTOResponse> listerDemandesSuppression(StatutDemandeSuppression statut) {
        List<DemandeSuppressionCompte> list = (statut != null)
                ? demandeSuppressionRepository.findByStatutOrderByDateDemandeDesc(statut)
                : demandeSuppressionRepository.findAllByOrderByDateDemandeDesc();

        return list.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public DemandeSuppressionDTOResponse obtenirDemandeSuppression(Long demandeId) {
        DemandeSuppressionCompte demande = demandeSuppressionRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande de suppression introuvable avec l'ID : " + demandeId));
        return mapToResponse(demande);
    }

    @Transactional
    public DemandeSuppressionDTOResponse traiterDemandeSuppression(Long demandeId, String adminEmail, DemandeSuppressionDecisionDTORequest request) {
        Compte adminCompte = compteRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new EntityNotFoundException("Compte administrateur introuvable pour : " + adminEmail));

        DemandeSuppressionCompte demande = demandeSuppressionRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande de suppression introuvable avec l'ID : " + demandeId));

        if (demande.getStatut() != StatutDemandeSuppression.EN_ATTENTE) {
            throw new IllegalStateException("Cette demande de suppression a déjà été traitée (statut: " + demande.getStatut() + ").");
        }

        Compte compteCible = demande.getCompte();
        String originalEmail = compteCible.getEmail();

        if (Boolean.TRUE.equals(request.approuvee())) {
            demande.setStatut(StatutDemandeSuppression.APPROUVEE);

            // Anonymisation RGPD du compte et désactivation
            compteCible.setNom("ANONYME");
            compteCible.setPrenom("ANONYME");
            compteCible.setEmail("anonyme_" + compteCible.getId() + "@rgpd.supprime");
            compteCible.setTelephone("0000000000");
            compteCible.setStatut(false);
            compteRepository.save(compteCible);

            // Notification envoyée au client à son adresse email d'origine
            String sujet = "Confirmation d'anonymisation et suppression de votre compte (RGPD)";
            String contenu = String.format(
                    "Bonjour,\n\n" +
                    "Votre demande de suppression de compte a été validée par l'administrateur système.\n\n" +
                    "Conformément au RGPD (droit à l'effacement), vos données personnelles nominatives ont été définitivement anonymisées " +
                    "et votre compte a été désactivé.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe Mon Salon."
            );
            notificationEmailService.queueNotificationEmail(originalEmail, sujet, contenu);

            log.info("Compte {} anonymisé avec succès suite à approbation RGPD #{}", originalEmail, demandeId);

        } else {
            demande.setStatut(StatutDemandeSuppression.REJETEE);

            // Notification de refus
            String sujet = "Décision concernant votre demande de suppression de compte (RGPD)";
            String contenu = String.format(
                    "Bonjour %s,\n\n" +
                    "Votre demande de suppression de compte a été examinée par l'administrateur système et ne peut être acceptée à ce stade.\n\n" +
                    "Motif : %s\n\n" +
                    "Pour toute question complémentaire, vous pouvez contacter notre support.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe Mon Salon.",
                    compteCible.getPrenom(),
                    request.motifDecision() != null ? request.motifDecision() : "Non spécifié"
            );
            notificationEmailService.queueNotificationEmail(originalEmail, sujet, contenu);

            log.info("Demande de suppression RGPD #{} rejetée par l'admin {}", demandeId, adminEmail);
        }

        demande.setDateDecision(LocalDateTime.now());
        demande.setMotifDecision(request.motifDecision());
        demande.setTraitePar(adminCompte);

        DemandeSuppressionCompte updated = demandeSuppressionRepository.save(demande);

        auditLogService.logActionPlateforme(
                TypeActionAudit.MODIFICATION,
                "DemandeSuppressionCompte",
                String.valueOf(updated.getId()),
                "EN_ATTENTE",
                "Décision admin : " + updated.getStatut().name() + " (motif: " + request.motifDecision() + ")",
                adminCompte,
                "ADMIN_SYSTEME"
        );

        return mapToResponse(updated);
    }

    private DemandeSuppressionDTOResponse mapToResponse(DemandeSuppressionCompte d) {
        String adminEmail = d.getTraitePar() != null ? d.getTraitePar().getEmail() : null;
        return new DemandeSuppressionDTOResponse(
                d.getId(),
                d.getCompte().getId(),
                d.getCompte().getEmail(),
                d.getMotif(),
                d.getStatut().name(),
                d.getDateDemande(),
                d.getDateDecision(),
                d.getMotifDecision(),
                adminEmail
        );
    }
}

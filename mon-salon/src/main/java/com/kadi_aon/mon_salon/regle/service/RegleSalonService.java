package com.kadi_aon.mon_salon.regle.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.notification.service.NotificationEmailService;
import com.kadi_aon.mon_salon.regle.dto.RegleSalonCreateDTORequest;
import com.kadi_aon.mon_salon.regle.dto.RegleSalonDTOResponse;
import com.kadi_aon.mon_salon.regle.dto.RegleSalonUpdateDTORequest;
import com.kadi_aon.mon_salon.regle.entity.RegleSalon;
import com.kadi_aon.mon_salon.regle.repository.RegleSalonRepository;
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
public class RegleSalonService {

    private final RegleSalonRepository regleSalonRepository;
    private final SalonRepository salonRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final NotificationEmailService notificationEmailService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<RegleSalonDTOResponse> listerReglesDuSalon(String slugSalon) {
        validerExistenceSalon(slugSalon);
        return regleSalonRepository.findBySalonSlugOrderByDateCreationDesc(slugSalon).stream()
                .map(RegleSalonDTOResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RegleSalonDTOResponse obtenirRegle(String slugSalon, Long id) {
        validerExistenceSalon(slugSalon);
        RegleSalon regle = regleSalonRepository.findByIdAndSalonSlug(id, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Règle non trouvée pour ce salon avec l'id : " + id));
        return RegleSalonDTOResponse.fromEntity(regle);
    }

    @Transactional
    public RegleSalonDTOResponse creerRegle(String slugSalon, RegleSalonCreateDTORequest request, String proprietaireEmail) {
        Salon salon = salonRepository.findBySlug(slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Salon non trouvé avec le slug : " + slugSalon));

        RegleSalon regle = RegleSalon.builder()
                .titre(request.titre())
                .description(request.description())
                .salon(salon)
                .build();

        RegleSalon saved = regleSalonRepository.save(regle);

        AffectationSalon affectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(proprietaireEmail, slugSalon)
                .orElse(null);

        auditLogService.logActionSalon(
                TypeActionAudit.CREATION,
                "RegleSalon",
                String.valueOf(saved.getId()),
                null,
                "Titre: " + saved.getTitre(),
                affectation,
                "PROPRIETAIRE"
        );

        notifierClientsDuSalon(slugSalon, salon.getNom(),
                "Nouvelle règle dans votre salon " + salon.getNom() + " : " + saved.getTitre(),
                "Bonjour,\n\nUne nouvelle règle a été mise en place dans votre salon '" + salon.getNom() + "' :\n\n"
                        + saved.getTitre() + "\n\n" + saved.getDescription()
                        + "\n\nCordialement,\nL'équipe du salon " + salon.getNom() + ".");

        return RegleSalonDTOResponse.fromEntity(saved);
    }

    @Transactional
    public RegleSalonDTOResponse modifierRegle(String slugSalon, Long id, RegleSalonUpdateDTORequest request, String proprietaireEmail) {
        RegleSalon regle = regleSalonRepository.findByIdAndSalonSlug(id, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Règle non trouvée pour ce salon avec l'id : " + id));

        String ancienneValeur = "Titre: " + regle.getTitre() + ", Description: " + regle.getDescription();

        regle.setTitre(request.titre());
        regle.setDescription(request.description());

        RegleSalon updated = regleSalonRepository.save(regle);

        String nouvelleValeur = "Titre: " + updated.getTitre() + ", Description: " + updated.getDescription();

        AffectationSalon affectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(proprietaireEmail, slugSalon)
                .orElse(null);

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "RegleSalon",
                String.valueOf(updated.getId()),
                ancienneValeur,
                nouvelleValeur,
                affectation,
                "PROPRIETAIRE"
        );

        notifierClientsDuSalon(slugSalon, regle.getSalon().getNom(),
                "Mise à jour d'une règle dans votre salon " + regle.getSalon().getNom() + " : " + updated.getTitre(),
                "Bonjour,\n\nUne règle a été modifiée dans votre salon '" + regle.getSalon().getNom() + "' :\n\n"
                        + updated.getTitre() + "\n\n" + updated.getDescription()
                        + "\n\nCordialement,\nL'équipe du salon " + regle.getSalon().getNom() + ".");

        return RegleSalonDTOResponse.fromEntity(updated);
    }

    @Transactional
    public void supprimerRegle(String slugSalon, Long id, String proprietaireEmail) {
        RegleSalon regle = regleSalonRepository.findByIdAndSalonSlug(id, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Règle non trouvée pour ce salon avec l'id : " + id));

        AffectationSalon affectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(proprietaireEmail, slugSalon)
                .orElse(null);

        auditLogService.logActionSalon(
                TypeActionAudit.SUPPRESSION,
                "RegleSalon",
                String.valueOf(regle.getId()),
                "Titre: " + regle.getTitre(),
                null,
                affectation,
                "PROPRIETAIRE"
        );

        regleSalonRepository.delete(regle);
        log.info("Règle de salon {} supprimée avec succès par {}", id, proprietaireEmail);
    }

    private void notifierClientsDuSalon(String slugSalon, String nomSalon, String sujet, String contenu) {
        List<String> clientsEmails = affectationSalonRepository.findDistinctEmailsBySalonSlugAndRole(slugSalon, TypeRoleSalon.CLIENT);
        for (String email : clientsEmails) {
            notificationEmailService.queueNotificationEmail(email, sujet, contenu);
        }
        log.info("Notifications de règle de salon envoyées à {} client(s) unique(s) pour le salon {}", clientsEmails.size(), slugSalon);
    }

    private void validerExistenceSalon(String slugSalon) {
        if (!salonRepository.existsBySlug(slugSalon)) {
            throw new EntityNotFoundException("Salon non trouvé avec le slug : " + slugSalon);
        }
    }
}

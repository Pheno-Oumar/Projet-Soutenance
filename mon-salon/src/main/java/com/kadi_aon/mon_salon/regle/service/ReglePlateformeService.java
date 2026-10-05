package com.kadi_aon.mon_salon.regle.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.notification.service.NotificationEmailService;
import com.kadi_aon.mon_salon.regle.dto.ReglePlateformeCreateDTORequest;
import com.kadi_aon.mon_salon.regle.dto.ReglePlateformeDTOResponse;
import com.kadi_aon.mon_salon.regle.dto.ReglePlateformeUpdateDTORequest;
import com.kadi_aon.mon_salon.regle.entity.ReglePlateforme;
import com.kadi_aon.mon_salon.regle.repository.ReglePlateformeRepository;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReglePlateformeService {

    private final ReglePlateformeRepository reglePlateformeRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final NotificationEmailService notificationEmailService;
    private final CompteRepository compteRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<ReglePlateformeDTOResponse> listerRegles() {
        return reglePlateformeRepository.findAllByOrderByDateCreationDesc().stream()
                .map(ReglePlateformeDTOResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReglePlateformeDTOResponse> listerReglesActives() {
        return reglePlateformeRepository.findByActifTrueOrderByDateCreationDesc().stream()
                .map(ReglePlateformeDTOResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReglePlateformeDTOResponse obtenirRegle(Long id) {
        ReglePlateforme regle = reglePlateformeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Règle de plateforme non trouvée avec l'id : " + id));
        return ReglePlateformeDTOResponse.fromEntity(regle);
    }

    @Transactional
    public ReglePlateformeDTOResponse creerRegle(ReglePlateformeCreateDTORequest request, String adminEmail) {
        ReglePlateforme regle = ReglePlateforme.builder()
                .titre(request.titre())
                .description(request.description())
                .actif(true)
                .build();

        ReglePlateforme saved = reglePlateformeRepository.save(regle);

        Compte admin = compteRepository.findByEmail(adminEmail).orElse(null);
        auditLogService.logActionPlateforme(
                TypeActionAudit.CREATION,
                "ReglePlateforme",
                String.valueOf(saved.getId()),
                null,
                "Titre: " + saved.getTitre(),
                admin,
                "ADMIN_SYSTEME"
        );

        notifierProprietaires("Nouvelle règle de plateforme publiée : " + saved.getTitre(),
                "Bonjour,\n\nUne nouvelle règle de la plateforme Mon Salon a été publiée :\n\n"
                        + saved.getTitre() + "\n\n" + saved.getDescription()
                        + "\n\nMerci d'en prendre connaissance.\nL'administration Mon Salon.");

        return ReglePlateformeDTOResponse.fromEntity(saved);
    }

    @Transactional
    public ReglePlateformeDTOResponse modifierRegle(Long id, ReglePlateformeUpdateDTORequest request, String adminEmail) {
        ReglePlateforme regle = reglePlateformeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Règle de plateforme non trouvée avec l'id : " + id));

        String ancienneValeur = "Titre: " + regle.getTitre() + ", Description: " + regle.getDescription();

        regle.setTitre(request.titre());
        regle.setDescription(request.description());

        ReglePlateforme updated = reglePlateformeRepository.save(regle);

        String nouvelleValeur = "Titre: " + updated.getTitre() + ", Description: " + updated.getDescription();

        Compte admin = compteRepository.findByEmail(adminEmail).orElse(null);
        auditLogService.logActionPlateforme(
                TypeActionAudit.MODIFICATION,
                "ReglePlateforme",
                String.valueOf(updated.getId()),
                ancienneValeur,
                nouvelleValeur,
                admin,
                "ADMIN_SYSTEME"
        );

        notifierProprietaires("Mise à jour d'une règle de la plateforme : " + updated.getTitre(),
                "Bonjour,\n\nUne règle de la plateforme Mon Salon a été mise à jour :\n\n"
                        + updated.getTitre() + "\n\n" + updated.getDescription()
                        + "\n\nMerci d'en prendre connaissance.\nL'administration Mon Salon.");

        return ReglePlateformeDTOResponse.fromEntity(updated);
    }

    @Transactional
    public ReglePlateformeDTOResponse activerRegle(Long id, String adminEmail) {
        ReglePlateforme regle = reglePlateformeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Règle de plateforme non trouvée avec l'id : " + id));

        regle.setActif(true);
        ReglePlateforme updated = reglePlateformeRepository.save(regle);

        Compte admin = compteRepository.findByEmail(adminEmail).orElse(null);
        auditLogService.logActionPlateforme(
                TypeActionAudit.REACTIVATION,
                "ReglePlateforme",
                String.valueOf(updated.getId()),
                "actif: false",
                "actif: true",
                admin,
                "ADMIN_SYSTEME"
        );

        notifierProprietaires("Réactivation d'une règle de plateforme : " + updated.getTitre(),
                "Bonjour,\n\nLa règle suivante est à nouveau en vigueur sur la plateforme Mon Salon :\n\n"
                        + updated.getTitre() + "\n\n" + updated.getDescription()
                        + "\n\nCordialement,\nL'administration Mon Salon.");

        return ReglePlateformeDTOResponse.fromEntity(updated);
    }

    @Transactional
    public ReglePlateformeDTOResponse desactiverRegle(Long id, String adminEmail) {
        ReglePlateforme regle = reglePlateformeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Règle de plateforme non trouvée avec l'id : " + id));

        regle.setActif(false);
        ReglePlateforme updated = reglePlateformeRepository.save(regle);

        Compte admin = compteRepository.findByEmail(adminEmail).orElse(null);
        auditLogService.logActionPlateforme(
                TypeActionAudit.DESACTIVATION,
                "ReglePlateforme",
                String.valueOf(updated.getId()),
                "actif: true",
                "actif: false",
                admin,
                "ADMIN_SYSTEME"
        );

        notifierProprietaires("Désactivation d'une règle de plateforme : " + updated.getTitre(),
                "Bonjour,\n\nLa règle suivante de la plateforme Mon Salon a été désactivée :\n\n"
                        + updated.getTitre()
                        + "\n\nCordialement,\nL'administration Mon Salon.");

        return ReglePlateformeDTOResponse.fromEntity(updated);
    }

    private void notifierProprietaires(String sujet, String contenu) {
        List<String> proprietairesEmails = affectationSalonRepository.findDistinctEmailsByRole(TypeRoleSalon.PROPRIETAIRE);
        for (String email : proprietairesEmails) {
            notificationEmailService.queueNotificationEmail(email, sujet, contenu);
        }
        log.info("Notifications de règle de plateforme envoyées à {} propriétaire(s) unique(s)", proprietairesEmails.size());
    }
}

package com.kadi_aon.mon_salon.audit.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.audit.dto.AuditLogDTOResponse;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.repository.AuditLogRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogAdminService {

    private final AuditLogRepository auditLogRepository;
    private final SalonRepository salonRepository;

    private static final Set<TypeActionAudit> ACTIONS_SENSIBLES = Set.of(
            TypeActionAudit.CHANGEMENT_MDP,
            TypeActionAudit.DESACTIVATION,
            TypeActionAudit.SUPPRESSION
    );

    private static final Set<String> ENTITES_SENSIBLES = Set.of(
            "Compte",
            "Salon",
            "DemandeSuppressionCompte"
    );

    // =========================================================================
    // CONSULTATION AUDIT - ADMIN SYSTEME (PLATEFORME COMPLETE)
    // =========================================================================

    public List<AuditLogDTOResponse> listerTousLesLogs() {
        return auditLogRepository.findAllByOrderByDateHeureDesc().stream()
                .map(AuditLogDTOResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AuditLogDTOResponse> filtrerLogsPlateforme(
            TypeActionAudit action,
            String entite,
            LocalDateTime dateDebut,
            LocalDateTime dateFin) {

        return auditLogRepository.filtrerLogsPlateforme(action, entite, dateDebut, dateFin).stream()
                .map(AuditLogDTOResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AuditLogDTOResponse> rechercherLogsPlateforme(String motCle) {
        if (motCle == null || motCle.isBlank()) {
            return listerTousLesLogs();
        }
        return auditLogRepository.rechercherLogsPlateforme(motCle.trim()).stream()
                .map(AuditLogDTOResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AuditLogDTOResponse> listerActionsSensiblesPlateforme() {
        return auditLogRepository.findActionsSensibles(ACTIONS_SENSIBLES, ENTITES_SENSIBLES).stream()
                .map(AuditLogDTOResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public AuditLogDTOResponse obtenirLogPlateforme(Long id) {
        return auditLogRepository.findById(id)
                .map(AuditLogDTOResponse::fromEntity)
                .orElseThrow(() -> new EntityNotFoundException("Log d'audit non trouvé avec l'id : " + id));
    }

    // =========================================================================
    // CONSULTATION AUDIT - PROPRIETAIRE (SALON SPECIFIQUE)
    // =========================================================================

    public List<AuditLogDTOResponse> listerLogsSalon(String slugSalon) {
        validerExistenceSalon(slugSalon);
        return auditLogRepository.findByAffectationSalonSalonSlugOrderByDateHeureDesc(slugSalon).stream()
                .map(AuditLogDTOResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AuditLogDTOResponse> filtrerLogsSalon(
            String slugSalon,
            TypeActionAudit action,
            String entite,
            LocalDateTime dateDebut,
            LocalDateTime dateFin) {

        validerExistenceSalon(slugSalon);
        return auditLogRepository.filtrerLogsSalon(slugSalon, action, entite, dateDebut, dateFin).stream()
                .map(AuditLogDTOResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AuditLogDTOResponse> rechercherLogsSalon(String slugSalon, String motCle) {
        validerExistenceSalon(slugSalon);
        if (motCle == null || motCle.isBlank()) {
            return listerLogsSalon(slugSalon);
        }
        return auditLogRepository.rechercherLogsSalon(slugSalon, motCle.trim()).stream()
                .map(AuditLogDTOResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AuditLogDTOResponse> listerActionsSensiblesSalon(String slugSalon) {
        validerExistenceSalon(slugSalon);
        return auditLogRepository.findActionsSensiblesSalon(slugSalon, ACTIONS_SENSIBLES, ENTITES_SENSIBLES).stream()
                .map(AuditLogDTOResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public AuditLogDTOResponse obtenirLogSalon(String slugSalon, Long id) {
        validerExistenceSalon(slugSalon);
        return auditLogRepository.findByIdAndAffectationSalonSalonSlug(id, slugSalon)
                .map(AuditLogDTOResponse::fromEntity)
                .orElseThrow(() -> new EntityNotFoundException("Log d'audit non trouvé pour ce salon avec l'id : " + id));
    }

    private void validerExistenceSalon(String slugSalon) {
        if (!salonRepository.existsBySlug(slugSalon)) {
            throw new EntityNotFoundException("Salon non trouvé avec le slug : " + slugSalon);
        }
    }
}

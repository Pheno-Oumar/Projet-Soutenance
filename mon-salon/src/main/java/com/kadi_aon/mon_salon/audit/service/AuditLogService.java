package com.kadi_aon.mon_salon.audit.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.audit.entity.AuditLog;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.repository.AuditLogRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public AuditLog logActionPlateforme(
            TypeActionAudit action,
            String entite,
            String entiteId,
            String ancienneValeur,
            String nouvelleValeur,
            Compte compte,
            String rolePlateforme) {

        HttpServletRequest request = getCurrentHttpRequest();

        AuditLog auditLog = AuditLog.builder()
                .dateHeure(LocalDateTime.now())
                .action(action)
                .entite(entite)
                .entiteId(entiteId)
                .ancienneValeur(ancienneValeur)
                .nouvelleValeur(nouvelleValeur)
                .adresseIP(extractClientIp(request))
                .userAgent(extractUserAgent(request))
                .roleUtilise(rolePlateforme != null ? rolePlateforme : "ADMIN_SYSTEME")
                .compte(compte)
                .affectationSalon(null)
                .build();

        log.info("[AUDIT PLATEFORME] Action: {}, Entité: {} #{}, Auteur: {}, IP: {}",
                action, entite, entiteId, compte != null ? compte.getEmail() : "SYSTEME", auditLog.getAdresseIP());

        return auditLogRepository.save(auditLog);
    }

    @Transactional
    public AuditLog logActionSalon(
            TypeActionAudit action,
            String entite,
            String entiteId,
            String ancienneValeur,
            String nouvelleValeur,
            AffectationSalon affectationSalon,
            String roleSalon) {

        HttpServletRequest request = getCurrentHttpRequest();

        AuditLog auditLog = AuditLog.builder()
                .dateHeure(LocalDateTime.now())
                .action(action)
                .entite(entite)
                .entiteId(entiteId)
                .ancienneValeur(ancienneValeur)
                .nouvelleValeur(nouvelleValeur)
                .adresseIP(extractClientIp(request))
                .userAgent(extractUserAgent(request))
                .roleUtilise(roleSalon)
                .affectationSalon(affectationSalon)
                .compte(null) // L'auteur est rattaché à l'affectationSalon
                .build();

        String salonNom = (affectationSalon != null && affectationSalon.getSalon() != null)
                ? affectationSalon.getSalon().getNom() : "Inconnu";
        String auteurEmail = (affectationSalon != null && affectationSalon.getCompte() != null)
                ? affectationSalon.getCompte().getEmail() : "Inconnu";

        log.info("[AUDIT SALON] Action: {}, Salon: {}, Rôle: {}, Auteur: {}, Entité: {} #{}",
                action, salonNom, roleSalon, auteurEmail, entite, entiteId);

        return auditLogRepository.save(auditLog);
    }

    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }
        String ua = request.getHeader("User-Agent");
        if (ua != null && ua.length() > 250) {
            return ua.substring(0, 250);
        }
        return ua != null ? ua : "UNKNOWN";
    }
}

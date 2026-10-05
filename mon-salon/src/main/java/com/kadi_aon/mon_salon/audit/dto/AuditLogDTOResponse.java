package com.kadi_aon.mon_salon.audit.dto;

import java.time.LocalDateTime;

import com.kadi_aon.mon_salon.audit.entity.AuditLog;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTOResponse {

    private Long id;
    private LocalDateTime dateHeure;
    private TypeActionAudit action;
    private String entite;
    private String entiteId;
    private String ancienneValeur;
    private String nouvelleValeur;
    private String adresseIP;
    private String userAgent;
    private String roleUtilise;

    private Long compteId;
    private String compteEmail;
    private String compteNom;

    private String salonSlug;
    private String salonNom;

    public static AuditLogDTOResponse fromEntity(AuditLog log) {
        if (log == null) {
            return null;
        }

        Long compteId = null;
        String compteEmail = null;
        String compteNom = null;
        String salonSlug = null;
        String salonNom = null;

        if (log.getCompte() != null) {
            compteId = log.getCompte().getId();
            compteEmail = log.getCompte().getEmail();
            compteNom = (log.getCompte().getPrenom() != null ? log.getCompte().getPrenom() + " " : "")
                    + (log.getCompte().getNom() != null ? log.getCompte().getNom() : "");
        } else if (log.getAffectationSalon() != null) {
            if (log.getAffectationSalon().getCompte() != null) {
                compteId = log.getAffectationSalon().getCompte().getId();
                compteEmail = log.getAffectationSalon().getCompte().getEmail();
                compteNom = (log.getAffectationSalon().getCompte().getPrenom() != null ? log.getAffectationSalon().getCompte().getPrenom() + " " : "")
                        + (log.getAffectationSalon().getCompte().getNom() != null ? log.getAffectationSalon().getCompte().getNom() : "");
            }
            if (log.getAffectationSalon().getSalon() != null) {
                salonSlug = log.getAffectationSalon().getSalon().getSlug();
                salonNom = log.getAffectationSalon().getSalon().getNom();
            }
        }

        return AuditLogDTOResponse.builder()
                .id(log.getId())
                .dateHeure(log.getDateHeure())
                .action(log.getAction())
                .entite(log.getEntite())
                .entiteId(log.getEntiteId())
                .ancienneValeur(log.getAncienneValeur())
                .nouvelleValeur(log.getNouvelleValeur())
                .adresseIP(log.getAdresseIP())
                .userAgent(log.getUserAgent())
                .roleUtilise(log.getRoleUtilise())
                .compteId(compteId)
                .compteEmail(compteEmail)
                .compteNom(compteNom != null && !compteNom.isBlank() ? compteNom.trim() : null)
                .salonSlug(salonSlug)
                .salonNom(salonNom)
                .build();
    }
}

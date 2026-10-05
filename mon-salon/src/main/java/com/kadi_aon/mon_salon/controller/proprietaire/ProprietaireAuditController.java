package com.kadi_aon.mon_salon.controller.proprietaire;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kadi_aon.mon_salon.audit.dto.AuditLogDTOResponse;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogAdminService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/{slugSalon}/proprietaire/audit")
@PreAuthorize("@salonSecurity.hasRoleInSalon(#slugSalon, 'PROPRIETAIRE')")
@RequiredArgsConstructor
@Tag(name = "Propriétaire - Audit", description = "Consultation des journaux d'audit du salon par le propriétaire")
public class ProprietaireAuditController {

    private final AuditLogAdminService auditLogAdminService;

    @GetMapping
    @Operation(summary = "Lister ou filtrer les journaux d'audit du salon")
    public ResponseEntity<APIResponse<List<AuditLogDTOResponse>>> listerLogs(
            @PathVariable String slugSalon,
            @RequestParam(required = false) TypeActionAudit action,
            @RequestParam(required = false) String entite,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin) {

        List<AuditLogDTOResponse> logs;
        if (action != null || entite != null || dateDebut != null || dateFin != null) {
            logs = auditLogAdminService.filtrerLogsSalon(slugSalon, action, entite, dateDebut, dateFin);
        } else {
            logs = auditLogAdminService.listerLogsSalon(slugSalon);
        }
        return ResponseEntity.ok(new APIResponse<>(true, "Logs d'audit du salon récupérés avec succès", logs));
    }

    @GetMapping("/recherche")
    @Operation(summary = "Rechercher dans les journaux d'audit du salon par mot-clé")
    public ResponseEntity<APIResponse<List<AuditLogDTOResponse>>> rechercherLogs(
            @PathVariable String slugSalon,
            @RequestParam(required = false) String motCle) {

        List<AuditLogDTOResponse> logs = auditLogAdminService.rechercherLogsSalon(slugSalon, motCle);
        return ResponseEntity.ok(new APIResponse<>(true, "Résultats de recherche récupérés avec succès", logs));
    }

    @GetMapping("/actions-sensibles")
    @Operation(summary = "Consulter les actions sensibles effectuées dans le salon")
    public ResponseEntity<APIResponse<List<AuditLogDTOResponse>>> listerActionsSensibles(
            @PathVariable String slugSalon) {

        List<AuditLogDTOResponse> logs = auditLogAdminService.listerActionsSensiblesSalon(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Actions sensibles du salon récupérées avec succès", logs));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter le détail d'un journal d'audit du salon par son identifiant")
    public ResponseEntity<APIResponse<AuditLogDTOResponse>> getLogById(
            @PathVariable String slugSalon,
            @PathVariable Long id) {

        AuditLogDTOResponse log = auditLogAdminService.obtenirLogSalon(slugSalon, id);
        return ResponseEntity.ok(new APIResponse<>(true, "Détail du log d'audit récupéré avec succès", log));
    }
}

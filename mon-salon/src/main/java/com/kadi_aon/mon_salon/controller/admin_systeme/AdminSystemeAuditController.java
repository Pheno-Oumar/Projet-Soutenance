package com.kadi_aon.mon_salon.controller.admin_systeme;

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
@RequestMapping("/admin-systeme/audit")
@PreAuthorize("hasRole('ADMIN_SYSTEME')")
@RequiredArgsConstructor
@Tag(name = "Admin Système - Audit", description = "Consultation des journaux d'audit de la plateforme")
public class AdminSystemeAuditController {

    private final AuditLogAdminService auditLogAdminService;

    @GetMapping
    @Operation(summary = "Lister ou filtrer les journaux d'audit de l'ensemble de la plateforme")
    public ResponseEntity<APIResponse<List<AuditLogDTOResponse>>> listerLogs(
            @RequestParam(required = false) TypeActionAudit action,
            @RequestParam(required = false) String entite,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin) {

        List<AuditLogDTOResponse> logs;
        if (action != null || entite != null || dateDebut != null || dateFin != null) {
            logs = auditLogAdminService.filtrerLogsPlateforme(action, entite, dateDebut, dateFin);
        } else {
            logs = auditLogAdminService.listerTousLesLogs();
        }
        return ResponseEntity.ok(new APIResponse<>(true, "Logs d'audit récupérés avec succès", logs));
    }

    @GetMapping("/recherche")
    @Operation(summary = "Rechercher dans les journaux d'audit par mot-clé")
    public ResponseEntity<APIResponse<List<AuditLogDTOResponse>>> rechercherLogs(
            @RequestParam(required = false) String motCle) {

        List<AuditLogDTOResponse> logs = auditLogAdminService.rechercherLogsPlateforme(motCle);
        return ResponseEntity.ok(new APIResponse<>(true, "Résultats de recherche récupérés avec succès", logs));
    }

    @GetMapping("/actions-sensibles")
    @Operation(summary = "Consulter les actions sensibles effectuées sur la plateforme")
    public ResponseEntity<APIResponse<List<AuditLogDTOResponse>>> listerActionsSensibles() {
        List<AuditLogDTOResponse> logs = auditLogAdminService.listerActionsSensiblesPlateforme();
        return ResponseEntity.ok(new APIResponse<>(true, "Actions sensibles récupérées avec succès", logs));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter le détail d'un journal d'audit par son identifiant")
    public ResponseEntity<APIResponse<AuditLogDTOResponse>> getLogById(@PathVariable Long id) {
        AuditLogDTOResponse log = auditLogAdminService.obtenirLogPlateforme(id);
        return ResponseEntity.ok(new APIResponse<>(true, "Détail du log d'audit récupéré avec succès", log));
    }
}

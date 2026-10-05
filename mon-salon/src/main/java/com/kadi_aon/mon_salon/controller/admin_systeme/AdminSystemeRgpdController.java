package com.kadi_aon.mon_salon.controller.admin_systeme;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeSuppressionDecisionDTORequest;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeSuppressionDTOResponse;
import com.kadi_aon.mon_salon.rgpd.enums.StatutDemandeSuppression;
import com.kadi_aon.mon_salon.rgpd.service.RgpdAdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin-systeme/rgpd")
@PreAuthorize("hasRole('ADMIN_SYSTEME')")
@RequiredArgsConstructor
@Tag(name = "Admin Système - RGPD", description = "Traitement des demandes de suppression de compte et droit à l'oubli")
public class AdminSystemeRgpdController {

    private final RgpdAdminService rgpdAdminService;

    @GetMapping("/suppressions")
    @Operation(summary = "Lister les demandes de suppression de compte (avec filtre optionnel par statut)")
    public ResponseEntity<APIResponse<List<DemandeSuppressionDTOResponse>>> listerDemandesSuppression(
            @RequestParam(required = false) StatutDemandeSuppression statut) {
        List<DemandeSuppressionDTOResponse> response = rgpdAdminService.listerDemandesSuppression(statut);
        return ResponseEntity.ok(new APIResponse<>(true, "Demandes de suppression récupérées avec succès", response));
    }

    @GetMapping("/suppressions/{id}")
    @Operation(summary = "Consulter le détail d'une demande de suppression de compte")
    public ResponseEntity<APIResponse<DemandeSuppressionDTOResponse>> obtenirDemandeSuppression(
            @PathVariable Long id) {
        DemandeSuppressionDTOResponse response = rgpdAdminService.obtenirDemandeSuppression(id);
        return ResponseEntity.ok(new APIResponse<>(true, "Détail de la demande de suppression récupéré", response));
    }

    @PatchMapping("/suppressions/{id}/decision")
    @Operation(summary = "Traiter une demande de suppression (approbation avec anonymisation ou rejet)")
    public ResponseEntity<APIResponse<DemandeSuppressionDTOResponse>> traiterDemandeSuppression(
            @PathVariable Long id,
            @Valid @RequestBody DemandeSuppressionDecisionDTORequest request,
            Principal principal) {
        DemandeSuppressionDTOResponse response = rgpdAdminService.traiterDemandeSuppression(id, principal.getName(), request);
        return ResponseEntity.ok(new APIResponse<>(true, "Décision enregistrée avec succès", response));
    }
}

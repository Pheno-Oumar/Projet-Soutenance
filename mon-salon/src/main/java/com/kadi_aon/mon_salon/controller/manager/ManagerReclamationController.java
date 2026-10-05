package com.kadi_aon.mon_salon.controller.manager;

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
import com.kadi_aon.mon_salon.reclamation.dto.ReclamationDTOResponse;
import com.kadi_aon.mon_salon.reclamation.dto.ReclamationTraiterDTORequest;
import com.kadi_aon.mon_salon.reclamation.enums.StatutReclamation;
import com.kadi_aon.mon_salon.reclamation.service.ReclamationSalonService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/{slugSalon}/manager/reclamations")
@PreAuthorize("@salonSecurity.hasRoleInSalon(#slugSalon, 'MANAGER') or @salonSecurity.hasRoleInSalon(#slugSalon, 'PROPRIETAIRE')")
@RequiredArgsConstructor
@Tag(name = "Espace Manager/Propriétaire - Réclamations", description = "Consultation et traitement des réclamations clients du salon")
public class ManagerReclamationController {

    private final ReclamationSalonService reclamationSalonService;

    @GetMapping
    @Operation(summary = "Lister les réclamations du salon (avec filtre optionnel par statut)")
    public ResponseEntity<APIResponse<List<ReclamationDTOResponse>>> listerReclamations(
            @PathVariable String slugSalon,
            @RequestParam(required = false) StatutReclamation statut) {
        List<ReclamationDTOResponse> response = reclamationSalonService.listerReclamationsSalon(slugSalon, statut);
        return ResponseEntity.ok(new APIResponse<>(true, "Réclamations du salon récupérées avec succès", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter le détail d'une réclamation")
    public ResponseEntity<APIResponse<ReclamationDTOResponse>> obtenirReclamation(
            @PathVariable String slugSalon,
            @PathVariable Long id) {
        ReclamationDTOResponse response = reclamationSalonService.obtenirReclamationSalon(slugSalon, id);
        return ResponseEntity.ok(new APIResponse<>(true, "Détail de la réclamation récupéré avec succès", response));
    }

    @PatchMapping("/{id}/traiter")
    @Operation(summary = "Traiter une réclamation (résolution ou rejet avec réponse)")
    public ResponseEntity<APIResponse<ReclamationDTOResponse>> traiterReclamation(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @Valid @RequestBody ReclamationTraiterDTORequest request,
            Principal principal) {
        ReclamationDTOResponse response = reclamationSalonService.traiterReclamation(slugSalon, id, principal.getName(), request);
        return ResponseEntity.ok(new APIResponse<>(true, "Réclamation traitée avec succès", response));
    }
}

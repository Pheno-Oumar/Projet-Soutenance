package com.kadi_aon.mon_salon.controller.client;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.reclamation.dto.ReclamationCreateDTORequest;
import com.kadi_aon.mon_salon.reclamation.dto.ReclamationDTOResponse;
import com.kadi_aon.mon_salon.reclamation.service.ReclamationSalonService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/{slugSalon}/client/reclamations")
@PreAuthorize("@salonSecurity.hasRoleInSalon(#slugSalon, 'CLIENT')")
@RequiredArgsConstructor
@Tag(name = "Espace Client - Réclamations", description = "Dépôt et suivi des réclamations client auprès du salon")
public class ClientReclamationController {

    private final ReclamationSalonService reclamationSalonService;

    @PostMapping
    @Operation(summary = "Déposer une nouvelle réclamation")
    public ResponseEntity<APIResponse<ReclamationDTOResponse>> deposerReclamation(
            @PathVariable String slugSalon,
            @Valid @RequestBody ReclamationCreateDTORequest request,
            Principal principal) {
        ReclamationDTOResponse response = reclamationSalonService.deposerReclamation(slugSalon, principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Réclamation déposée avec succès", response));
    }

    @GetMapping
    @Operation(summary = "Lister mes réclamations pour ce salon")
    public ResponseEntity<APIResponse<List<ReclamationDTOResponse>>> listerMesReclamations(
            @PathVariable String slugSalon,
            Principal principal) {
        List<ReclamationDTOResponse> response = reclamationSalonService.listerReclamationsClient(slugSalon, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Réclamations récupérées avec succès", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter le détail d'une de mes réclamations")
    public ResponseEntity<APIResponse<ReclamationDTOResponse>> obtenirMaReclamation(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {
        ReclamationDTOResponse response = reclamationSalonService.obtenirReclamationClient(slugSalon, id, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Détail de la réclamation récupéré avec succès", response));
    }
}

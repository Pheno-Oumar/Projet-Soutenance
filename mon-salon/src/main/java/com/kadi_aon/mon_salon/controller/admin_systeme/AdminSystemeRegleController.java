package com.kadi_aon.mon_salon.controller.admin_systeme;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.regle.dto.ReglePlateformeCreateDTORequest;
import com.kadi_aon.mon_salon.regle.dto.ReglePlateformeDTOResponse;
import com.kadi_aon.mon_salon.regle.dto.ReglePlateformeUpdateDTORequest;
import com.kadi_aon.mon_salon.regle.service.ReglePlateformeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin-systeme/regles")
@PreAuthorize("hasRole('ADMIN_SYSTEME')")
@RequiredArgsConstructor
@Tag(name = "Admin Système - Règles", description = "Gestion des règles globales de la plateforme par l'administrateur système")
public class AdminSystemeRegleController {

    private final ReglePlateformeService reglePlateformeService;

    @PostMapping
    @Operation(summary = "Créer une nouvelle règle de la plateforme et notifier les propriétaires")
    public ResponseEntity<APIResponse<ReglePlateformeDTOResponse>> creerRegle(
            @Valid @RequestBody ReglePlateformeCreateDTORequest request,
            Principal principal) {
        ReglePlateformeDTOResponse response = reglePlateformeService.creerRegle(request, principal != null ? principal.getName() : null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Règle de plateforme créée avec succès", response));
    }

    @GetMapping
    @Operation(summary = "Lister toutes les règles de la plateforme (actives et inactives)")
    public ResponseEntity<APIResponse<List<ReglePlateformeDTOResponse>>> listerRegles() {
        List<ReglePlateformeDTOResponse> regles = reglePlateformeService.listerRegles();
        return ResponseEntity.ok(new APIResponse<>(true, "Règles de plateforme récupérées avec succès", regles));
    }

    @GetMapping("/actives")
    @Operation(summary = "Lister uniquement les règles de plateforme en vigueur")
    public ResponseEntity<APIResponse<List<ReglePlateformeDTOResponse>>> listerReglesActives() {
        List<ReglePlateformeDTOResponse> regles = reglePlateformeService.listerReglesActives();
        return ResponseEntity.ok(new APIResponse<>(true, "Règles actives récupérées avec succès", regles));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter le détail d'une règle de plateforme")
    public ResponseEntity<APIResponse<ReglePlateformeDTOResponse>> getRegle(@PathVariable Long id) {
        ReglePlateformeDTOResponse regle = reglePlateformeService.obtenirRegle(id);
        return ResponseEntity.ok(new APIResponse<>(true, "Détail de la règle récupéré avec succès", regle));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une règle de la plateforme et notifier les propriétaires")
    public ResponseEntity<APIResponse<ReglePlateformeDTOResponse>> modifierRegle(
            @PathVariable Long id,
            @Valid @RequestBody ReglePlateformeUpdateDTORequest request,
            Principal principal) {
        ReglePlateformeDTOResponse response = reglePlateformeService.modifierRegle(id, request, principal != null ? principal.getName() : null);
        return ResponseEntity.ok(new APIResponse<>(true, "Règle modifiée avec succès", response));
    }

    @PatchMapping("/{id}/activer")
    @Operation(summary = "Réactiver une règle de la plateforme")
    public ResponseEntity<APIResponse<ReglePlateformeDTOResponse>> activerRegle(
            @PathVariable Long id,
            Principal principal) {
        ReglePlateformeDTOResponse response = reglePlateformeService.activerRegle(id, principal != null ? principal.getName() : null);
        return ResponseEntity.ok(new APIResponse<>(true, "Règle réactivée avec succès", response));
    }

    @PatchMapping("/{id}/desactiver")
    @Operation(summary = "Désactiver une règle de la plateforme")
    public ResponseEntity<APIResponse<ReglePlateformeDTOResponse>> desactiverRegle(
            @PathVariable Long id,
            Principal principal) {
        ReglePlateformeDTOResponse response = reglePlateformeService.desactiverRegle(id, principal != null ? principal.getName() : null);
        return ResponseEntity.ok(new APIResponse<>(true, "Règle désactivée avec succès", response));
    }
}

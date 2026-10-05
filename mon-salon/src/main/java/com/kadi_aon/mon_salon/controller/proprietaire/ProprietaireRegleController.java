package com.kadi_aon.mon_salon.controller.proprietaire;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.regle.dto.RegleSalonCreateDTORequest;
import com.kadi_aon.mon_salon.regle.dto.RegleSalonDTOResponse;
import com.kadi_aon.mon_salon.regle.dto.RegleSalonUpdateDTORequest;
import com.kadi_aon.mon_salon.regle.service.RegleSalonService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/{slugSalon}/proprietaire/regles")
@PreAuthorize("@salonSecurity.hasRoleInSalon(#slugSalon, 'PROPRIETAIRE')")
@RequiredArgsConstructor
@Tag(name = "Propriétaire - Règles du Salon", description = "Gestion des règles internes du salon par le propriétaire")
public class ProprietaireRegleController {

    private final RegleSalonService regleSalonService;

    @PostMapping
    @Operation(summary = "Créer une nouvelle règle pour le salon et notifier ses clients")
    public ResponseEntity<APIResponse<RegleSalonDTOResponse>> creerRegle(
            @PathVariable String slugSalon,
            @Valid @RequestBody RegleSalonCreateDTORequest request,
            Principal principal) {
        RegleSalonDTOResponse response = regleSalonService.creerRegle(slugSalon, request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Règle de salon créée avec succès", response));
    }

    @GetMapping
    @Operation(summary = "Lister toutes les règles du salon")
    public ResponseEntity<APIResponse<List<RegleSalonDTOResponse>>> listerRegles(
            @PathVariable String slugSalon) {
        List<RegleSalonDTOResponse> regles = regleSalonService.listerReglesDuSalon(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Règles du salon récupérées avec succès", regles));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter le détail d'une règle du salon")
    public ResponseEntity<APIResponse<RegleSalonDTOResponse>> getRegle(
            @PathVariable String slugSalon,
            @PathVariable Long id) {
        RegleSalonDTOResponse regle = regleSalonService.obtenirRegle(slugSalon, id);
        return ResponseEntity.ok(new APIResponse<>(true, "Détail de la règle récupéré avec succès", regle));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une règle du salon et notifier ses clients")
    public ResponseEntity<APIResponse<RegleSalonDTOResponse>> modifierRegle(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @Valid @RequestBody RegleSalonUpdateDTORequest request,
            Principal principal) {
        RegleSalonDTOResponse response = regleSalonService.modifierRegle(slugSalon, id, request, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Règle du salon modifiée avec succès", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une règle du salon")
    public ResponseEntity<APIResponse<Void>> supprimerRegle(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {
        regleSalonService.supprimerRegle(slugSalon, id, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Règle du salon supprimée avec succès", null));
    }
}

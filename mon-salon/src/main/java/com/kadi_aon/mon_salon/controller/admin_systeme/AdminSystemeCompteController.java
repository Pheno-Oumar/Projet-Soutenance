package com.kadi_aon.mon_salon.controller.admin_systeme;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kadi_aon.mon_salon.account.dto.ChangementMotDePasseDTORequest;
import com.kadi_aon.mon_salon.account.dto.CompteDTOResponse;
import com.kadi_aon.mon_salon.account.dto.CompteUpdateDTORequest;
import com.kadi_aon.mon_salon.account.service.CompteService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin-systeme/compte")
@PreAuthorize("hasRole('ADMIN_SYSTEME')")
@RequiredArgsConstructor
@Tag(name = "Admin Système - Compte", description = "Gestion du profil et des accès de l'administrateur système")
public class AdminSystemeCompteController {

    private final CompteService compteService;

    @GetMapping
    @Operation(summary = "Consulter les informations du profil administrateur système")
    public ResponseEntity<APIResponse<CompteDTOResponse>> getProfil(Principal principal) {
        CompteDTOResponse response = compteService.getProfil(principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Profil administrateur récupéré avec succès", response));
    }

    @PutMapping
    @Operation(summary = "Modifier les informations personnelles de l'administrateur système")
    public ResponseEntity<APIResponse<CompteDTOResponse>> updateProfil(
            @Valid @RequestBody CompteUpdateDTORequest request,
            Principal principal) {
        CompteDTOResponse response = compteService.updateProfil(principal.getName(), request, null, "ADMIN_SYSTEME");
        return ResponseEntity.ok(new APIResponse<>(true, "Profil administrateur mis à jour avec succès", response));
    }

    @PatchMapping("/mot-de-passe")
    @Operation(summary = "Modifier le mot de passe de l'administrateur système")
    public ResponseEntity<APIResponse<Void>> changerMotDePasse(
            @Valid @RequestBody ChangementMotDePasseDTORequest request,
            Principal principal) {
        compteService.changerMotDePasse(principal.getName(), request, null, "ADMIN_SYSTEME");
        return ResponseEntity.ok(new APIResponse<>(true, "Mot de passe mis à jour avec succès", null));
    }
}

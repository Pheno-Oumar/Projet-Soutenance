package com.kadi_aon.mon_salon.controller.comptable;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping("/{slugSalon}/comptable/compte")
@PreAuthorize("@salonSecurity.hasRoleInSalon(#slugSalon, 'COMPTABLE')")
@RequiredArgsConstructor
@Tag(name = "Espace Comptable - Compte", description = "Consultation et modification du profil personnel et mot de passe du comptable")
public class ComptableCompteController {

    private final CompteService compteService;

    @GetMapping
    @Operation(summary = "Consulter ses informations personnelles de compte")
    public ResponseEntity<APIResponse<CompteDTOResponse>> getCompte(Principal principal) {
        CompteDTOResponse response = compteService.getProfil(principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Profil comptable récupéré avec succès", response));
    }

    @PutMapping
    @Operation(summary = "Modifier ses informations personnelles (nom, prénom, date de naissance, téléphone)")
    public ResponseEntity<APIResponse<CompteDTOResponse>> updateCompte(
            @PathVariable String slugSalon,
            @Valid @RequestBody CompteUpdateDTORequest request,
            Principal principal) {

        CompteDTOResponse response = compteService.updateProfil(principal.getName(), request, slugSalon, "COMPTABLE");
        return ResponseEntity.ok(new APIResponse<>(true, "Profil comptable mis à jour avec succès", response));
    }

    @PatchMapping("/mot-de-passe")
    @Operation(summary = "Modifier son mot de passe en fournissant l'ancien et le nouveau")
    public ResponseEntity<APIResponse<Void>> changerMotDePasse(
            @PathVariable String slugSalon,
            @Valid @RequestBody ChangementMotDePasseDTORequest request,
            Principal principal) {

        compteService.changerMotDePasse(principal.getName(), request, slugSalon, "COMPTABLE");
        return ResponseEntity.ok(new APIResponse<>(true, "Mot de passe modifié avec succès", null));
    }
}

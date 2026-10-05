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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.salon.dto.SalonCreateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.SalonDTOResponse;
import com.kadi_aon.mon_salon.salon.service.AdminSalonService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin-systeme/salons")
@PreAuthorize("hasRole('ADMIN_SYSTEME')")
@RequiredArgsConstructor
@Tag(name = "Admin Système - Salons", description = "Gestion globale des salons par l'administrateur système")
public class AdminSystemeController {

    private final AdminSalonService adminSalonService;

    @PostMapping
    @Operation(summary = "Créer un nouveau salon et lui associer son propriétaire")
    public ResponseEntity<APIResponse<SalonDTOResponse>> creerSalon(
            @Valid @RequestBody SalonCreateDTORequest request,
            Principal principal) {

        SalonDTOResponse response = adminSalonService.creerSalon(request, principal != null ? principal.getName() : null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Salon créé avec succès", response));
    }

    @GetMapping
    @Operation(summary = "Lister tous les salons de la plateforme")
    public ResponseEntity<APIResponse<List<SalonDTOResponse>>> listerSalons() {
        List<SalonDTOResponse> salons = adminSalonService.listerSalons();
        return ResponseEntity.ok(new APIResponse<>(true, "Liste des salons récupérée avec succès", salons));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Consulter les informations détaillées d'un salon")
    public ResponseEntity<APIResponse<SalonDTOResponse>> getSalon(@PathVariable String slug) {
        SalonDTOResponse salon = adminSalonService.getSalonBySlug(slug);
        return ResponseEntity.ok(new APIResponse<>(true, "Salon trouvé", salon));
    }

    @PatchMapping("/{slug}/desactiver")
    @Operation(summary = "Désactiver un salon")
    public ResponseEntity<APIResponse<SalonDTOResponse>> desactiverSalon(
            @PathVariable String slug,
            Principal principal) {

        SalonDTOResponse salon = adminSalonService.desactiverSalon(slug, principal != null ? principal.getName() : null);
        return ResponseEntity.ok(new APIResponse<>(true, "Salon désactivé avec succès", salon));
    }

    @PatchMapping("/{slug}/reactiver")
    @Operation(summary = "Réactiver un salon")
    public ResponseEntity<APIResponse<SalonDTOResponse>> reactiverSalon(
            @PathVariable String slug,
            Principal principal) {

        SalonDTOResponse salon = adminSalonService.reactiverSalon(slug, principal != null ? principal.getName() : null);
        return ResponseEntity.ok(new APIResponse<>(true, "Salon réactivé avec succès", salon));
    }
}

package com.kadi_aon.mon_salon.controller.comptable;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kadi_aon.mon_salon.caisse.dto.SessionCaisseClotureDTORequest;
import com.kadi_aon.mon_salon.caisse.dto.SessionCaisseDTOResponse;
import com.kadi_aon.mon_salon.caisse.dto.SessionCaisseOuvertureDTORequest;
import com.kadi_aon.mon_salon.caisse.service.CaisseSalonService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/{slugSalon}/comptable/caisse")
@PreAuthorize("@salonSecurity.hasRoleInSalon(#slugSalon, 'COMPTABLE')")
@RequiredArgsConstructor
@Tag(name = "Espace Comptable - Caisse", description = "Ouverture, clôture et suivi des sessions et écritures de caisse du salon")
public class ComptableCaisseController {

    private final CaisseSalonService caisseSalonService;

    @PostMapping("/sessions")
    @Operation(summary = "Ouvrir une nouvelle session de caisse")
    public ResponseEntity<APIResponse<SessionCaisseDTOResponse>> ouvrirSession(
            @PathVariable String slugSalon,
            @Valid @RequestBody SessionCaisseOuvertureDTORequest request,
            Principal principal) {
        SessionCaisseDTOResponse response = caisseSalonService.ouvrirSessionCaisse(slugSalon, principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Session de caisse ouverte avec succès", response));
    }

    @PutMapping("/sessions/courante/cloturer")
    @Operation(summary = "Clôturer la session de caisse en cours")
    public ResponseEntity<APIResponse<SessionCaisseDTOResponse>> cloturerSession(
            @PathVariable String slugSalon,
            @Valid @RequestBody SessionCaisseClotureDTORequest request,
            Principal principal) {
        SessionCaisseDTOResponse response = caisseSalonService.cloturerSessionCaisse(slugSalon, principal.getName(), request);
        return ResponseEntity.ok(new APIResponse<>(true, "Session de caisse clôturée avec succès", response));
    }

    @GetMapping("/sessions/courante")
    @Operation(summary = "Consulter la session de caisse active et le cumul des opérations")
    public ResponseEntity<APIResponse<SessionCaisseDTOResponse>> getSessionCourante(
            @PathVariable String slugSalon) {
        SessionCaisseDTOResponse response = caisseSalonService.obtenirSessionCourante(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Session courante récupérée", response));
    }

    @GetMapping("/sessions")
    @Operation(summary = "Consulter l'historique de toutes les sessions de caisse du salon")
    public ResponseEntity<APIResponse<List<SessionCaisseDTOResponse>>> getHistoriqueSessions(
            @PathVariable String slugSalon) {
        List<SessionCaisseDTOResponse> response = caisseSalonService.listerHistoriqueSessions(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Historique des sessions récupéré", response));
    }

    @GetMapping("/sessions/{sessionId}/operations")
    @Operation(summary = "Consulter la liste détaillée des opérations d'une session de caisse spécifique")
    public ResponseEntity<APIResponse<List<com.kadi_aon.mon_salon.caisse.dto.OperationCaisseDTOResponse>>> getOperationsSession(
            @PathVariable String slugSalon,
            @PathVariable Long sessionId) {
        List<com.kadi_aon.mon_salon.caisse.dto.OperationCaisseDTOResponse> response =
                caisseSalonService.listerOperationsSession(slugSalon, sessionId);
        return ResponseEntity.ok(new APIResponse<>(true, "Opérations de la session de caisse récupérées avec succès", response));
    }
}


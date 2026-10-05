package com.kadi_aon.mon_salon.controller.client;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.stock.dto.CommandeDTOResponse;
import com.kadi_aon.mon_salon.stock.service.CommandeSalonService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/{slugSalon}/client/commandes")
@PreAuthorize("@salonSecurity.hasRoleInSalon(#slugSalon, 'CLIENT')")
@RequiredArgsConstructor
@Tag(name = "Espace Client - Commandes", description = "Passage et suivi des commandes de produits d'un client dans un salon")
public class ClientCommandeController {

    private final CommandeSalonService commandeSalonService;

    @PostMapping
    @Operation(summary = "Passer commande à partir du panier courant (le panier est vidé et un bon de commande est généré)")
    public ResponseEntity<APIResponse<CommandeDTOResponse>> passerCommande(
            @PathVariable String slugSalon,
            Principal principal) {
        CommandeDTOResponse response = commandeSalonService.passerCommande(slugSalon, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Commande passée avec succès. Vous recevrez une notification par email.", response));
    }

    @GetMapping
    @Operation(summary = "Lister l'historique de ses commandes passées dans ce salon")
    public ResponseEntity<APIResponse<List<CommandeDTOResponse>>> listerCommandes(
            @PathVariable String slugSalon,
            Principal principal) {
        List<CommandeDTOResponse> response = commandeSalonService.listerCommandesClient(slugSalon, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Commandes récupérées avec succès", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter le détail d'une de ses commandes")
    public ResponseEntity<APIResponse<CommandeDTOResponse>> obtenirCommande(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {
        CommandeDTOResponse response = commandeSalonService.obtenirCommandeClient(slugSalon, principal.getName(), id);
        return ResponseEntity.ok(new APIResponse<>(true, "Détails de la commande récupérés avec succès", response));
    }
}

package com.kadi_aon.mon_salon.controller.client;

import java.security.Principal;

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
import com.kadi_aon.mon_salon.stock.dto.AjoutPanierDTORequest;
import com.kadi_aon.mon_salon.stock.dto.ModificationQuantiteDTORequest;
import com.kadi_aon.mon_salon.stock.dto.PanierDTOResponse;
import com.kadi_aon.mon_salon.stock.service.PanierService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/{slugSalon}/client/panier")
@PreAuthorize("@salonSecurity.hasRoleInSalon(#slugSalon, 'CLIENT')")
@RequiredArgsConstructor
@Tag(name = "Espace Client - Panier", description = "Gestion du panier d'achat de produits dans un salon")
public class ClientPanierController {

    private final PanierService panierService;

    @GetMapping
    @Operation(summary = "Consulter son panier actuel pour ce salon")
    public ResponseEntity<APIResponse<PanierDTOResponse>> getPanier(
            @PathVariable String slugSalon,
            Principal principal) {
        PanierDTOResponse response = panierService.obtenirPanier(slugSalon, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Panier récupéré avec succès", response));
    }

    @PostMapping({"/articles", "/items"})
    @Operation(summary = "Ajouter un produit à son panier")
    public ResponseEntity<APIResponse<PanierDTOResponse>> ajouterArticle(
            @PathVariable String slugSalon,
            @Valid @RequestBody AjoutPanierDTORequest request,
            Principal principal) {
        PanierDTOResponse response = panierService.ajouterArticle(slugSalon, principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Article ajouté au panier avec succès", response));
    }

    @PutMapping({"/articles/{produitId}", "/items/{produitId}"})
    @Operation(summary = "Modifier la quantité d'un produit dans son panier")
    public ResponseEntity<APIResponse<PanierDTOResponse>> modifierQuantite(
            @PathVariable String slugSalon,
            @PathVariable Long produitId,
            @Valid @RequestBody ModificationQuantiteDTORequest request,
            Principal principal) {
        PanierDTOResponse response = panierService.modifierQuantite(slugSalon, principal.getName(), produitId, request);
        return ResponseEntity.ok(new APIResponse<>(true, "Quantité mise à jour avec succès", response));
    }

    @DeleteMapping({"/articles/{produitId}", "/items/{produitId}"})
    @Operation(summary = "Supprimer un produit de son panier")
    public ResponseEntity<APIResponse<PanierDTOResponse>> supprimerArticle(
            @PathVariable String slugSalon,
            @PathVariable Long produitId,
            Principal principal) {
        PanierDTOResponse response = panierService.supprimerArticle(slugSalon, principal.getName(), produitId);
        return ResponseEntity.ok(new APIResponse<>(true, "Article retiré du panier avec succès", response));
    }

    @DeleteMapping
    @Operation(summary = "Vider intégralement son panier")
    public ResponseEntity<APIResponse<Void>> viderPanier(
            @PathVariable String slugSalon,
            Principal principal) {
        panierService.viderPanier(slugSalon, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Panier vidé avec succès", null));
    }
}

package com.kadi_aon.mon_salon.controller.responsable_stock;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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
import com.kadi_aon.mon_salon.stock.dto.CommandeDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.KpiStockDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.ProduitAlerteStockDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.RejetCommandeDTORequest;
import com.kadi_aon.mon_salon.stock.dto.RetraitCommandeDTORequest;
import com.kadi_aon.mon_salon.stock.enums.StatutCommande;
import com.kadi_aon.mon_salon.stock.service.CommandeSalonService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/{slugSalon}/responsable-stock")
@PreAuthorize("@salonSecurity.hasRoleInSalon(#slugSalon, 'RESPONSABLE_STOCK')")
@RequiredArgsConstructor
@Tag(name = "Espace Responsable Stock - Commandes & Alertes", description = "Gestion des commandes clients, alertes de rupture et indicateurs de performance")
public class ResponsableStockCommandeController {

    private final CommandeSalonService commandeSalonService;

    @GetMapping("/produits/bientot-en-rupture")
    @Operation(summary = "Consulter les produits bientôt en rupture ou en rupture totale de stock")
    public ResponseEntity<APIResponse<List<ProduitAlerteStockDTOResponse>>> getProduitsBientotEnRupture(
            @PathVariable String slugSalon,
            Principal principal) {
        List<ProduitAlerteStockDTOResponse> response = commandeSalonService.obtenirProduitsBientotEnRupture(slugSalon, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Produits en alerte de rupture récupérés avec succès", response));
    }

    @GetMapping("/commandes")
    @Operation(summary = "Lister les commandes clients du salon avec filtres par statut et dates")
    public ResponseEntity<APIResponse<List<CommandeDTOResponse>>> listerCommandes(
            @PathVariable String slugSalon,
            @RequestParam(required = false) StatutCommande statut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            Principal principal) {
        List<CommandeDTOResponse> response = commandeSalonService.listerCommandesSalon(slugSalon, principal.getName(), statut, dateDebut, dateFin);
        return ResponseEntity.ok(new APIResponse<>(true, "Commandes récupérées avec succès", response));
    }

    @GetMapping("/commandes/{id}")
    @Operation(summary = "Consulter les détails d'une commande client")
    public ResponseEntity<APIResponse<CommandeDTOResponse>> obtenirCommande(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {
        CommandeDTOResponse response = commandeSalonService.obtenirCommandeSalon(slugSalon, principal.getName(), id);
        return ResponseEntity.ok(new APIResponse<>(true, "Détails de la commande récupérés avec succès", response));
    }

    @PatchMapping("/commandes/{id}/valider")
    @Operation(summary = "Valider une commande client (génère la facture, décrémente les stocks en VENTE et envoie le code de retrait par email)")
    public ResponseEntity<APIResponse<CommandeDTOResponse>> validerCommande(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {
        CommandeDTOResponse response = commandeSalonService.validerCommande(slugSalon, principal.getName(), id);
        return ResponseEntity.ok(new APIResponse<>(true, "Commande validée avec succès. Facture et code de retrait générés.", response));
    }

    @PatchMapping("/commandes/{id}/rejeter")
    @Operation(summary = "Rejeter une commande client avec motif obligatoire (notifie le client par email)")
    public ResponseEntity<APIResponse<CommandeDTOResponse>> rejeterCommande(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @Valid @RequestBody RejetCommandeDTORequest request,
            Principal principal) {
        CommandeDTOResponse response = commandeSalonService.rejeterCommande(slugSalon, principal.getName(), id, request);
        return ResponseEntity.ok(new APIResponse<>(true, "Commande rejetée avec succès. Notification envoyée au client.", response));
    }

    @PatchMapping("/commandes/{id}/retrait")
    @Operation(summary = "Confirmer la remise en main propre de la commande au salon (statut RECUPEREE)")
    public ResponseEntity<APIResponse<CommandeDTOResponse>> confirmerRetrait(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @RequestBody(required = false) RetraitCommandeDTORequest request,
            Principal principal) {
        CommandeDTOResponse response = commandeSalonService.confirmerRetrait(slugSalon, principal.getName(), id, request);
        return ResponseEntity.ok(new APIResponse<>(true, "Retrait de la commande confirmé avec succès", response));
    }

    @GetMapping("/kpi")
    @Operation(summary = "Consulter les KPI de performance du stock et des ventes de produits")
    public ResponseEntity<APIResponse<KpiStockDTOResponse>> getKpiStock(
            @PathVariable String slugSalon,
            Principal principal) {
        KpiStockDTOResponse response = commandeSalonService.obtenirKpiStock(slugSalon, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "KPIs du stock récupérés avec succès", response));
    }
}

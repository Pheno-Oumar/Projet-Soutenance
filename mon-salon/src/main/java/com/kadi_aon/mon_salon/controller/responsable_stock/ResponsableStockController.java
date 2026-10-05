package com.kadi_aon.mon_salon.controller.responsable_stock;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.stock.dto.CategorieProduitCreateDTORequest;
import com.kadi_aon.mon_salon.stock.dto.CategorieProduitDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.CategorieProduitUpdateDTORequest;
import com.kadi_aon.mon_salon.stock.dto.MouvementStockCreateDTORequest;
import com.kadi_aon.mon_salon.stock.dto.MouvementStockDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.ProduitCreateDTORequest;
import com.kadi_aon.mon_salon.stock.dto.ProduitDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.ProduitUpdateDTORequest;
import com.kadi_aon.mon_salon.stock.service.StockSalonService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/{slugSalon}/responsable-stock")
@PreAuthorize("@salonSecurity.hasRoleInSalon(#slugSalon, 'RESPONSABLE_STOCK')")
@RequiredArgsConstructor
@Tag(name = "Espace Responsable Stock", description = "Gestion des catégories, produits, inventaires et mouvements de stock")
public class ResponsableStockController {

    private final StockSalonService stockSalonService;

    // --- Catégories ---

    @PostMapping(value = "/categories", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Créer une catégorie de produit avec au moins 1 produit initial et image optionnelle (Multipart)")
    public ResponseEntity<APIResponse<CategorieProduitDTOResponse>> creerCategorie(
            @PathVariable String slugSalon,
            @Valid @RequestPart("data") CategorieProduitCreateDTORequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Principal principal) throws IOException {
        CategorieProduitDTOResponse response = stockSalonService.creerCategorie(slugSalon, principal.getName(), request, image);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Catégorie de produit créée avec succès", response));
    }

    @GetMapping("/categories")
    @Operation(summary = "Lister les catégories du salon")
    public ResponseEntity<APIResponse<List<CategorieProduitDTOResponse>>> listerCategories(
            @PathVariable String slugSalon,
            @RequestParam(required = false) Boolean statut) {
        List<CategorieProduitDTOResponse> response = stockSalonService.listerCategories(slugSalon, statut);
        return ResponseEntity.ok(new APIResponse<>(true, "Catégories récupérées avec succès", response));
    }

    @GetMapping("/categories/{id}")
    @Operation(summary = "Détails d'une catégorie et ses produits")
    public ResponseEntity<APIResponse<CategorieProduitDTOResponse>> obtenirCategorie(
            @PathVariable String slugSalon,
            @PathVariable Long id) {
        CategorieProduitDTOResponse response = stockSalonService.obtenirCategorie(slugSalon, id);
        return ResponseEntity.ok(new APIResponse<>(true, "Détails de la catégorie récupérés avec succès", response));
    }

    @PutMapping(value = "/categories/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modifier les attributs d'une catégorie (nom, description) (JSON)")
    public ResponseEntity<APIResponse<CategorieProduitDTOResponse>> modifierCategorie(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @Valid @RequestBody CategorieProduitUpdateDTORequest request,
            Principal principal) {
        CategorieProduitDTOResponse response = stockSalonService.modifierCategorie(slugSalon, id, principal.getName(), request);
        return ResponseEntity.ok(new APIResponse<>(true, "Catégorie modifiée avec succès", response));
    }

    @PatchMapping(value = "/categories/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PutMapping(value = "/categories/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Mettre à jour l'image d'une catégorie (supprime l'ancienne sur Cloudinary)")
    public ResponseEntity<APIResponse<CategorieProduitDTOResponse>> uploadImageCategorie(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Principal principal) throws IOException {
        CategorieProduitDTOResponse response = stockSalonService.uploadImageCategorie(slugSalon, id, principal.getName(), file);
        return ResponseEntity.ok(new APIResponse<>(true, "Image de la catégorie mise à jour avec succès", response));
    }

    @PatchMapping("/categories/{id}/statut")
    @Operation(summary = "Activer ou désactiver une catégorie")
    public ResponseEntity<APIResponse<CategorieProduitDTOResponse>> basculerStatutCategorie(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {
        CategorieProduitDTOResponse response = stockSalonService.basculerStatutCategorie(slugSalon, id, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Statut de la catégorie mis à jour avec succès", response));
    }

    // --- Produits ---

    @PostMapping(value = "/categories/{catId}/produits", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Ajouter un produit à une catégorie avec initialisation de son stock et image optionnelle (Multipart)")
    public ResponseEntity<APIResponse<ProduitDTOResponse>> ajouterProduit(
            @PathVariable String slugSalon,
            @PathVariable Long catId,
            @Valid @RequestPart("data") ProduitCreateDTORequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Principal principal) throws IOException {
        ProduitDTOResponse response = stockSalonService.ajouterProduit(slugSalon, catId, principal.getName(), request, image);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Produit ajouté avec succès", response));
    }

    @GetMapping("/produits")
    @Operation(summary = "Lister les produits du salon")
    public ResponseEntity<APIResponse<List<ProduitDTOResponse>>> listerProduits(
            @PathVariable String slugSalon,
            @RequestParam(required = false) Long categorieId,
            @RequestParam(required = false) Boolean statut) {
        List<ProduitDTOResponse> response = stockSalonService.listerProduits(slugSalon, categorieId, statut);
        return ResponseEntity.ok(new APIResponse<>(true, "Produits récupérés avec succès", response));
    }

    @GetMapping("/produits/{id}")
    @Operation(summary = "Détails d'un produit et son niveau de stock")
    public ResponseEntity<APIResponse<ProduitDTOResponse>> obtenirProduit(
            @PathVariable String slugSalon,
            @PathVariable Long id) {
        ProduitDTOResponse response = stockSalonService.obtenirProduit(slugSalon, id);
        return ResponseEntity.ok(new APIResponse<>(true, "Détails du produit récupérés avec succès", response));
    }

    @PutMapping(value = "/produits/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modifier les informations et les seuils d'un produit (nom, description, prix, seuils) (JSON)")
    public ResponseEntity<APIResponse<ProduitDTOResponse>> modifierProduit(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @Valid @RequestBody ProduitUpdateDTORequest request,
            Principal principal) {
        ProduitDTOResponse response = stockSalonService.modifierProduit(slugSalon, id, principal.getName(), request);
        return ResponseEntity.ok(new APIResponse<>(true, "Produit modifié avec succès", response));
    }

    @PatchMapping(value = "/produits/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PutMapping(value = "/produits/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Mettre à jour l'image d'un produit (supprime l'ancienne sur Cloudinary)")
    public ResponseEntity<APIResponse<ProduitDTOResponse>> uploadImageProduit(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Principal principal) throws IOException {
        ProduitDTOResponse response = stockSalonService.uploadImageProduit(slugSalon, id, principal.getName(), file);
        return ResponseEntity.ok(new APIResponse<>(true, "Image du produit mise à jour avec succès", response));
    }

    @PatchMapping("/produits/{id}/statut")
    @Operation(summary = "Activer ou désactiver un produit")
    public ResponseEntity<APIResponse<ProduitDTOResponse>> basculerStatutProduit(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {
        ProduitDTOResponse response = stockSalonService.basculerStatutProduit(slugSalon, id, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Statut du produit mis à jour avec succès", response));
    }

    // --- Mouvements de stock ---

    @PostMapping("/mouvements")
    @Operation(summary = "Enregistrer un mouvement de stock (ENTREE, VENTE, PERTE, AJUSTEMENT)")
    public ResponseEntity<APIResponse<MouvementStockDTOResponse>> enregistrerMouvement(
            @PathVariable String slugSalon,
            @Valid @RequestBody MouvementStockCreateDTORequest request,
            Principal principal) {
        MouvementStockDTOResponse response = stockSalonService.enregistrerMouvementStock(slugSalon, principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Mouvement de stock enregistré avec succès", response));
    }

    @GetMapping("/mouvements")
    @Operation(summary = "Consulter l'historique des mouvements de stock")
    public ResponseEntity<APIResponse<List<MouvementStockDTOResponse>>> listerMouvements(
            @PathVariable String slugSalon,
            @RequestParam(required = false) Long produitId) {
        List<MouvementStockDTOResponse> response = stockSalonService.listerMouvements(slugSalon, produitId);
        return ResponseEntity.ok(new APIResponse<>(true, "Historique des mouvements récupéré avec succès", response));
    }
}

package com.kadi_aon.mon_salon.stock.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.caisse.entity.SessionCaisse;
import com.kadi_aon.mon_salon.caisse.service.CaisseSalonService;
import com.kadi_aon.mon_salon.common.service.CloudinaryService;
import com.kadi_aon.mon_salon.depense.service.DepenseSalonService;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;
import com.kadi_aon.mon_salon.stock.dto.CategorieProduitCreateDTORequest;
import com.kadi_aon.mon_salon.stock.dto.CategorieProduitDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.CategorieProduitUpdateDTORequest;
import com.kadi_aon.mon_salon.stock.dto.MouvementStockCreateDTORequest;
import com.kadi_aon.mon_salon.stock.dto.MouvementStockDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.ProduitCreateDTORequest;
import com.kadi_aon.mon_salon.stock.dto.ProduitDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.ProduitInitialDTORequest;
import com.kadi_aon.mon_salon.stock.dto.ProduitUpdateDTORequest;
import com.kadi_aon.mon_salon.stock.dto.StockProduitDTOResponse;
import com.kadi_aon.mon_salon.stock.entity.CategorieProduit;
import com.kadi_aon.mon_salon.stock.entity.MouvementStock;
import com.kadi_aon.mon_salon.stock.entity.Produit;
import com.kadi_aon.mon_salon.stock.entity.StockProduit;
import com.kadi_aon.mon_salon.stock.enums.TypeMouvementStock;
import com.kadi_aon.mon_salon.stock.exception.StockInsuffisantException;
import com.kadi_aon.mon_salon.stock.repository.CategorieProduitRepository;
import com.kadi_aon.mon_salon.stock.repository.MouvementStockRepository;
import com.kadi_aon.mon_salon.stock.repository.ProduitRepository;
import com.kadi_aon.mon_salon.stock.repository.StockProduitRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockSalonService {

    private final CategorieProduitRepository categorieProduitRepository;
    private final ProduitRepository produitRepository;
    private final StockProduitRepository stockProduitRepository;
    private final MouvementStockRepository mouvementStockRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final SalonRepository salonRepository;
    private final AuditLogService auditLogService;
    private final CaisseSalonService caisseSalonService;
    private final DepenseSalonService depenseSalonService;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public CategorieProduitDTOResponse creerCategorie(String slugSalon, String responsableEmail, CategorieProduitCreateDTORequest request) {
        try {
            return creerCategorie(slugSalon, responsableEmail, request, null);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'upload de l'image de la catégorie : " + e.getMessage(), e);
        }
    }

    @Transactional
    public CategorieProduitDTOResponse creerCategorie(String slugSalon, String responsableEmail, CategorieProduitCreateDTORequest request, MultipartFile imageFile) throws IOException {
        AffectationSalon affectation = validerResponsableStock(slugSalon, responsableEmail);
        Salon salon = affectation.getSalon();

        if (categorieProduitRepository.existsBySalonIdAndNomIgnoreCase(salon.getId(), request.nom())) {
            throw new IllegalArgumentException("Une catégorie de produit avec le nom '" + request.nom() + "' existe déjà pour ce salon.");
        }

        if (request.produits() != null && !request.produits().isEmpty()) {
            Set<String> nomsProduits = new HashSet<>();
            for (ProduitInitialDTORequest p : request.produits()) {
                if (!nomsProduits.add(p.nom().trim().toLowerCase())) {
                    throw new IllegalArgumentException("Le produit '" + p.nom() + "' est présent en double dans la requête.");
                }
            }
        }

        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = cloudinaryService.uploadImageCategorieProduit(imageFile, slugSalon);
        }

        CategorieProduit categorie = CategorieProduit.builder()
                .nom(request.nom())
                .description(request.description())
                .imageUrl(imageUrl)
                .statut(true)
                .salon(salon)
                .build();

        CategorieProduit savedCategorie = categorieProduitRepository.save(categorie);

        if (request.produits() != null && !request.produits().isEmpty()) {
            for (ProduitInitialDTORequest p : request.produits()) {
                creerProduitEtStock(savedCategorie, p.nom(), p.description(), null, p.prixVente(),
                        p.seuilMinimum(), p.seuilMaximum(), p.quantiteInitiale(), p.prixAchatInitial(), affectation);
            }
        }

        int nbProduits = (request.produits() != null) ? request.produits().size() : 0;

        auditLogService.logActionSalon(
                TypeActionAudit.CREATION,
                "CategorieProduit",
                savedCategorie.getId().toString(),
                null,
                "Création catégorie " + savedCategorie.getNom() + " avec " + nbProduits + " produit(s)",
                affectation,
                TypeRoleSalon.RESPONSABLE_STOCK.name()
        );

        log.info("Catégorie de produit {} créée avec {} produit(s) pour le salon {} par {}",
                savedCategorie.getId(), nbProduits, slugSalon, responsableEmail);

        return obtenirCategorie(slugSalon, savedCategorie.getId());
    }

    @Transactional(readOnly = true)
    public CategorieProduitDTOResponse obtenirCategorie(String slugSalon, Long categorieId) {
        CategorieProduit cat = categorieProduitRepository.findByIdAndSalonSlug(categorieId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie de produit introuvable avec l'ID " + categorieId + " pour le salon " + slugSalon));

        List<ProduitDTOResponse> produits = produitRepository.findByCategorieId(categorieId).stream()
                .map(this::mapProduitToResponse)
                .toList();

        return new CategorieProduitDTOResponse(
                cat.getId(),
                cat.getNom(),
                cat.getDescription(),
                cat.getImageUrl(),
                cat.isStatut(),
                slugSalon,
                produits.size(),
                produits
        );
    }

    @Transactional(readOnly = true)
    public List<CategorieProduitDTOResponse> listerCategories(String slugSalon, Boolean statut) {
        if (!salonRepository.existsBySlug(slugSalon)) {
            throw new EntityNotFoundException("Salon introuvable pour le slug : " + slugSalon);
        }

        List<CategorieProduit> categories = (statut != null)
                ? categorieProduitRepository.findBySalonSlugAndStatut(slugSalon, statut)
                : categorieProduitRepository.findBySalonSlug(slugSalon);

        return categories.stream().map(cat -> {
            List<ProduitDTOResponse> produits = produitRepository.findByCategorieId(cat.getId()).stream()
                    .map(this::mapProduitToResponse)
                    .filter(p -> statut == null || !Boolean.TRUE.equals(statut) || Boolean.TRUE.equals(p.statut()))
                    .toList();
            return new CategorieProduitDTOResponse(
                    cat.getId(),
                    cat.getNom(),
                    cat.getDescription(),
                    cat.getImageUrl(),
                    cat.isStatut(),
                    slugSalon,
                    produits.size(),
                    produits
            );
        })
        .filter(cat -> statut == null || !Boolean.TRUE.equals(statut) || cat.nombreProduits() > 0)
        .toList();
    }

    @Transactional
    public CategorieProduitDTOResponse modifierCategorie(String slugSalon, Long categorieId, String responsableEmail, CategorieProduitUpdateDTORequest request) {
        AffectationSalon affectation = validerResponsableStock(slugSalon, responsableEmail);

        CategorieProduit cat = categorieProduitRepository.findByIdAndSalonSlug(categorieId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie de produit introuvable avec l'ID " + categorieId + " pour le salon " + slugSalon));

        if (categorieProduitRepository.existsBySalonIdAndNomIgnoreCaseAndIdNot(cat.getSalon().getId(), request.nom(), categorieId)) {
            throw new IllegalArgumentException("Une autre catégorie avec le nom '" + request.nom() + "' existe déjà pour ce salon.");
        }

        cat.setNom(request.nom());
        cat.setDescription(request.description());

        CategorieProduit updated = categorieProduitRepository.save(cat);

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "CategorieProduit",
                updated.getId().toString(),
                null,
                "Modification catégorie: " + updated.getNom(),
                affectation,
                TypeRoleSalon.RESPONSABLE_STOCK.name()
        );

        return obtenirCategorie(slugSalon, updated.getId());
    }

    @Transactional
    public CategorieProduitDTOResponse uploadImageCategorie(String slugSalon, Long categorieId, String responsableEmail, MultipartFile file) throws IOException {
        AffectationSalon affectation = validerResponsableStock(slugSalon, responsableEmail);

        CategorieProduit cat = categorieProduitRepository.findByIdAndSalonSlug(categorieId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie de produit introuvable avec l'ID " + categorieId + " pour le salon " + slugSalon));

        String ancienneImage = cat.getImageUrl();
        String nouvelleUrl = cloudinaryService.uploadImageCategorieProduit(file, slugSalon);
        cat.setImageUrl(nouvelleUrl);

        if (ancienneImage != null && !ancienneImage.isBlank()) {
            cloudinaryService.deleteMediaByUrl(ancienneImage, "image");
        }

        CategorieProduit updated = categorieProduitRepository.save(cat);

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "CategorieProduit (Image)",
                updated.getId().toString(),
                ancienneImage,
                nouvelleUrl,
                affectation,
                TypeRoleSalon.RESPONSABLE_STOCK.name()
        );

        return obtenirCategorie(slugSalon, updated.getId());
    }

    @Transactional
    public CategorieProduitDTOResponse basculerStatutCategorie(String slugSalon, Long categorieId, String responsableEmail) {
        AffectationSalon affectation = validerResponsableStock(slugSalon, responsableEmail);

        CategorieProduit cat = categorieProduitRepository.findByIdAndSalonSlug(categorieId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie de produit introuvable avec l'ID " + categorieId + " pour le salon " + slugSalon));

        boolean nouveauStatut = !cat.isStatut();
        cat.setStatut(nouveauStatut);
        CategorieProduit updated = categorieProduitRepository.save(cat);

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "CategorieProduit",
                updated.getId().toString(),
                String.valueOf(!nouveauStatut),
                "Statut passé à " + nouveauStatut,
                affectation,
                TypeRoleSalon.RESPONSABLE_STOCK.name()
        );

        log.info("Statut de la catégorie {} modifié à {} par {}", categorieId, nouveauStatut, responsableEmail);
        return obtenirCategorie(slugSalon, updated.getId());
    }

    @Transactional
    public ProduitDTOResponse ajouterProduit(String slugSalon, Long categorieId, String responsableEmail, ProduitCreateDTORequest request) {
        try {
            return ajouterProduit(slugSalon, categorieId, responsableEmail, request, null);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'ajout du produit : " + e.getMessage(), e);
        }
    }

    @Transactional
    public ProduitDTOResponse ajouterProduit(String slugSalon, Long categorieId, String responsableEmail, ProduitCreateDTORequest request, MultipartFile imageFile) throws IOException {
        AffectationSalon affectation = validerResponsableStock(slugSalon, responsableEmail);

        CategorieProduit cat = categorieProduitRepository.findByIdAndSalonSlug(categorieId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie de produit introuvable avec l'ID " + categorieId + " pour le salon " + slugSalon));

        if (produitRepository.existsByCategorieIdAndNomIgnoreCase(categorieId, request.nom())) {
            throw new IllegalArgumentException("Un produit nommé '" + request.nom() + "' existe déjà dans cette catégorie.");
        }

        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = cloudinaryService.uploadImageProduit(imageFile, slugSalon);
        }

        Produit saved = creerProduitEtStock(cat, request.nom(), request.description(), imageUrl, request.prixVente(),
                request.seuilMinimum(), request.seuilMaximum(), request.quantiteInitiale(), request.prixAchatInitial(), affectation);

        auditLogService.logActionSalon(
                TypeActionAudit.CREATION,
                "Produit",
                saved.getId().toString(),
                null,
                "Ajout produit " + saved.getNom() + " dans catégorie " + cat.getNom(),
                affectation,
                TypeRoleSalon.RESPONSABLE_STOCK.name()
        );

        return mapProduitToResponse(saved);
    }

    @Transactional
    public ProduitDTOResponse modifierProduit(String slugSalon, Long produitId, String responsableEmail, ProduitUpdateDTORequest request) {
        AffectationSalon affectation = validerResponsableStock(slugSalon, responsableEmail);

        Produit prod = produitRepository.findByIdAndCategorieSalonSlug(produitId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable avec l'ID " + produitId + " pour le salon " + slugSalon));

        if (produitRepository.existsByCategorieIdAndNomIgnoreCaseAndIdNot(prod.getCategorie().getId(), request.nom(), produitId)) {
            throw new IllegalArgumentException("Un autre produit avec le nom '" + request.nom() + "' existe déjà dans cette catégorie.");
        }

        prod.setNom(request.nom());
        prod.setDescription(request.description());
        prod.setPrixVente(request.prixVente());
        prod.setDateModification(LocalDateTime.now());

        StockProduit stock = prod.getStock();
        if (stock != null) {
            if (request.seuilMinimum() != null) {
                stock.setSeuilMinimum(request.seuilMinimum());
            }
            if (request.seuilMaximum() != null) {
                stock.setSeuilMaximum(request.seuilMaximum());
            }
            stock.setDateDerniereMiseAJour(LocalDateTime.now());
            stockProduitRepository.save(stock);
        }

        Produit updated = produitRepository.save(prod);

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "Produit",
                updated.getId().toString(),
                null,
                "Modification produit " + updated.getNom(),
                affectation,
                TypeRoleSalon.RESPONSABLE_STOCK.name()
        );

        return mapProduitToResponse(updated);
    }

    @Transactional
    public ProduitDTOResponse uploadImageProduit(String slugSalon, Long produitId, String responsableEmail, MultipartFile file) throws IOException {
        AffectationSalon affectation = validerResponsableStock(slugSalon, responsableEmail);

        Produit prod = produitRepository.findByIdAndCategorieSalonSlug(produitId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable avec l'ID " + produitId + " pour le salon " + slugSalon));

        String ancienneImage = prod.getImageUrl();
        String nouvelleUrl = cloudinaryService.uploadImageProduit(file, slugSalon);
        prod.setImageUrl(nouvelleUrl);
        prod.setDateModification(LocalDateTime.now());

        if (ancienneImage != null && !ancienneImage.isBlank()) {
            cloudinaryService.deleteMediaByUrl(ancienneImage, "image");
        }

        Produit updated = produitRepository.save(prod);

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "Produit (Image)",
                updated.getId().toString(),
                ancienneImage,
                nouvelleUrl,
                affectation,
                TypeRoleSalon.RESPONSABLE_STOCK.name()
        );

        return mapProduitToResponse(updated);
    }

    @Transactional
    public ProduitDTOResponse basculerStatutProduit(String slugSalon, Long produitId, String responsableEmail) {
        AffectationSalon affectation = validerResponsableStock(slugSalon, responsableEmail);

        Produit prod = produitRepository.findByIdAndCategorieSalonSlug(produitId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable avec l'ID " + produitId + " pour le salon " + slugSalon));

        boolean nouveauStatut = !prod.isStatut();
        prod.setStatut(nouveauStatut);
        prod.setDateModification(LocalDateTime.now());
        Produit updated = produitRepository.save(prod);

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "Produit",
                updated.getId().toString(),
                String.valueOf(!nouveauStatut),
                "Statut produit passé à " + nouveauStatut,
                affectation,
                TypeRoleSalon.RESPONSABLE_STOCK.name()
        );

        log.info("Statut du produit {} modifié à {} par {}", produitId, nouveauStatut, responsableEmail);
        return mapProduitToResponse(updated);
    }

    @Transactional(readOnly = true)
    public ProduitDTOResponse obtenirProduit(String slugSalon, Long produitId) {
        Produit prod = produitRepository.findByIdAndCategorieSalonSlug(produitId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable avec l'ID " + produitId + " pour le salon " + slugSalon));
        return mapProduitToResponse(prod);
    }

    @Transactional(readOnly = true)
    public List<ProduitDTOResponse> listerProduits(String slugSalon, Long categorieId, Boolean statut) {
        if (!salonRepository.existsBySlug(slugSalon)) {
            throw new EntityNotFoundException("Salon introuvable pour le slug : " + slugSalon);
        }

        List<Produit> produits;
        if (categorieId != null && statut != null) {
            produits = produitRepository.findByCategorieIdAndStatut(categorieId, statut);
        } else if (categorieId != null) {
            produits = produitRepository.findByCategorieId(categorieId);
        } else if (statut != null) {
            produits = produitRepository.findByCategorieSalonSlugAndStatut(slugSalon, statut);
        } else {
            produits = produitRepository.findByCategorieSalonSlug(slugSalon);
        }

        if (Boolean.TRUE.equals(statut)) {
            produits = produits.stream()
                    .filter(p -> p.getCategorie() != null && p.getCategorie().isStatut())
                    .toList();
        }

        return produits.stream().map(this::mapProduitToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProduitDTOResponse> listerProduitsTransversal(Long categorieId) {
        List<Produit> produits;
        if (categorieId != null) {
            produits = produitRepository.findByStatutTrueAndCategorieId(categorieId);
        } else {
            produits = produitRepository.findByStatutTrue();
        }
        return produits.stream().map(this::mapProduitToResponse).toList();
    }

    @Transactional

    public MouvementStockDTOResponse enregistrerMouvementStock(String slugSalon, String responsableEmail, MouvementStockCreateDTORequest request) {
        AffectationSalon affectation = validerResponsableStock(slugSalon, responsableEmail);

        Produit prod = produitRepository.findByIdAndCategorieSalonSlug(request.produitId(), slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable avec l'ID " + request.produitId() + " pour le salon " + slugSalon));

        StockProduit stock = stockProduitRepository.findByProduitId(prod.getId())
                .orElseThrow(() -> new IllegalStateException("Fiche stock introuvable pour le produit " + prod.getNom()));

        int quantiteMouvement = request.quantite();
        TypeMouvementStock type = request.type();

        switch (type) {
            case ENTREE -> stock.setQuantiteDisponible(stock.getQuantiteDisponible() + quantiteMouvement);
            case VENTE, PERTE -> {
                if (stock.getQuantiteDisponible() < quantiteMouvement) {
                    throw new StockInsuffisantException("Stock insuffisant pour le produit '" + prod.getNom()
                            + "' : disponible=" + stock.getQuantiteDisponible() + ", demandé=" + quantiteMouvement);
                }
                stock.setQuantiteDisponible(stock.getQuantiteDisponible() - quantiteMouvement);
            }
            case AJUSTEMENT -> {
                // Pour un ajustement d'inventaire, quantite représente le nouveau stock constaté
                stock.setQuantiteDisponible(quantiteMouvement);
            }
        }

        stock.setDateDerniereMiseAJour(LocalDateTime.now());
        stockProduitRepository.save(stock);

        MouvementStock mouvement = MouvementStock.builder()
                .produit(prod)
                .quantite(quantiteMouvement)
                .type(type)
                .prixUnitaire(request.prixUnitaire())
                .dateMouvement(LocalDateTime.now())
                .motif(request.motif())
                .auteur(affectation)
                .build();

        MouvementStock savedMouvement = mouvementStockRepository.save(mouvement);

        // Si ENTREE avec prix d'achat, générer automatiquement la dépense ACHAT_STOCK et la sortie de caisse
        if (type == TypeMouvementStock.ENTREE && request.prixUnitaire() != null && request.prixUnitaire().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal montantTotal = request.prixUnitaire().multiply(BigDecimal.valueOf(quantiteMouvement));
            try {
                SessionCaisse sessionCaisse = caisseSalonService.obtenirSessionActive(slugSalon);
                depenseSalonService.creerDepenseAchatStock(
                        affectation,
                        sessionCaisse,
                        montantTotal,
                        "Achat de " + quantiteMouvement + "x " + prod.getNom() + " (" + request.motif() + ")",
                        savedMouvement
                );
            } catch (Exception ex) {
                log.warn("Impossible d'enregistrer la dépense de caisse pour le réapprovisionnement de stock (aucune caisse ouverte ou erreur): {}", ex.getMessage());
            }
        }

        auditLogService.logActionSalon(
                TypeActionAudit.CREATION,
                "MouvementStock",
                String.valueOf(savedMouvement.getId()),
                null,
                "Mouvement " + type.name() + " de " + quantiteMouvement + " unité(s) sur le produit " + prod.getNom()
                        + " (nouveau stock disponible: " + stock.getQuantiteDisponible() + ")",
                affectation,
                TypeRoleSalon.RESPONSABLE_STOCK.name()
        );

        return mapMouvementToResponse(savedMouvement, stock.getQuantiteDisponible());
    }

    @Transactional(readOnly = true)
    public List<MouvementStockDTOResponse> listerMouvements(String slugSalon, Long produitId) {
        if (!salonRepository.existsBySlug(slugSalon)) {
            throw new EntityNotFoundException("Salon introuvable pour le slug : " + slugSalon);
        }

        List<MouvementStock> mouvements = (produitId != null)
                ? mouvementStockRepository.findByProduitIdAndProduitCategorieSalonSlugOrderByDateMouvementDesc(produitId, slugSalon)
                : mouvementStockRepository.findByProduitCategorieSalonSlugOrderByDateMouvementDesc(slugSalon);

        return mouvements.stream().map(m -> {
            Integer restant = m.getProduit().getStock() != null ? m.getProduit().getStock().getQuantiteDisponible() : null;
            return mapMouvementToResponse(m, restant);
        }).toList();
    }

    private Produit creerProduitEtStock(CategorieProduit categorie, String nom, String description, BigDecimal prixVente,
                                        Integer seuilMin, Integer seuilMax, Integer quantiteInit, BigDecimal prixAchatInit, AffectationSalon auteur) {
        return creerProduitEtStock(categorie, nom, description, null, prixVente, seuilMin, seuilMax, quantiteInit, prixAchatInit, auteur);
    }

    private Produit creerProduitEtStock(CategorieProduit categorie, String nom, String description, String imageUrl, BigDecimal prixVente,
                                        Integer seuilMin, Integer seuilMax, Integer quantiteInit, BigDecimal prixAchatInit, AffectationSalon auteur) {
        int qteInit = quantiteInit != null ? quantiteInit : 0;
        int sMin = seuilMin != null ? seuilMin : 0;

        Produit produit = Produit.builder()
                .nom(nom)
                .description(description)
                .imageUrl(imageUrl)
                .prixVente(prixVente)
                .statut(true)
                .dateCreation(LocalDateTime.now())
                .categorie(categorie)
                .build();

        Produit savedProduit = produitRepository.save(produit);

        StockProduit stock = StockProduit.builder()
                .produit(savedProduit)
                .quantiteDisponible(qteInit)
                .seuilMinimum(sMin)
                .seuilMaximum(seuilMax)
                .dateDerniereMiseAJour(LocalDateTime.now())
                .build();

        StockProduit savedStock = stockProduitRepository.save(stock);
        savedProduit.setStock(savedStock);

        if (qteInit > 0) {
            MouvementStock mouvement = MouvementStock.builder()
                    .produit(savedProduit)
                    .quantite(qteInit)
                    .type(TypeMouvementStock.ENTREE)
                    .prixUnitaire(prixAchatInit)
                    .dateMouvement(LocalDateTime.now())
                    .motif("Stock initial lors de la création")
                    .auteur(auteur)
                    .build();

            MouvementStock savedMvt = mouvementStockRepository.save(mouvement);

            if (prixAchatInit != null && prixAchatInit.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal totalAchat = prixAchatInit.multiply(BigDecimal.valueOf(qteInit));
                try {
                    SessionCaisse sessionCaisse = caisseSalonService.obtenirSessionActive(categorie.getSalon().getSlug());
                    depenseSalonService.creerDepenseAchatStock(
                            auteur,
                            sessionCaisse,
                            totalAchat,
                            "Stock initial de " + qteInit + "x " + nom,
                            savedMvt
                    );
                } catch (Exception ex) {
                    log.warn("Session de caisse non disponible pour l'achat initial de stock: {}", ex.getMessage());
                }
            }
        }

        return savedProduit;
    }

    private AffectationSalon validerResponsableStock(String slugSalon, String email) {
        AffectationSalon affectation = affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Affectation active introuvable pour l'utilisateur " + email + " sur le salon " + slugSalon));

        boolean hasRole = affectation.getRoles().stream()
                .anyMatch(r -> r.getRole() == TypeRoleSalon.RESPONSABLE_STOCK 
                        || r.getRole() == TypeRoleSalon.MANAGER 
                        || r.getRole() == TypeRoleSalon.RECEPTIONNISTE);

        if (!hasRole) {
            throw new IllegalArgumentException("L'utilisateur " + email + " n'a pas les droits pour gérer le stock sur le salon " + slugSalon);
        }
        return affectation;
    }

    private ProduitDTOResponse mapProduitToResponse(Produit p) {
        StockProduit stock = p.getStock();
        StockProduitDTOResponse stockDTO = null;
        if (stock != null) {
            boolean alerte = stock.getQuantiteDisponible() <= stock.getSeuilMinimum();
            stockDTO = new StockProduitDTOResponse(
                    stock.getId(),
                    stock.getQuantiteDisponible(),
                    stock.getSeuilMinimum(),
                    stock.getSeuilMaximum(),
                    alerte,
                    stock.getDateDerniereMiseAJour()
            );
        }

        return new ProduitDTOResponse(
                p.getId(),
                p.getNom(),
                p.getDescription(),
                p.getPrixVente(),
                p.getImageUrl(),
                p.isStatut(),
                p.getCategorie().getId(),
                p.getCategorie().getNom(),
                stockDTO,
                p.getDateCreation(),
                p.getDateModification()
        );
    }

    private MouvementStockDTOResponse mapMouvementToResponse(MouvementStock m, Integer restant) {
        String auteurNom = (m.getAuteur() != null && m.getAuteur().getCompte() != null)
                ? m.getAuteur().getCompte().getPrenom() + " " + m.getAuteur().getCompte().getNom()
                : "N/A";

        return new MouvementStockDTOResponse(
                m.getId(),
                m.getProduit().getId(),
                m.getProduit().getNom(),
                m.getQuantite(),
                m.getType().name(),
                m.getPrixUnitaire(),
                m.getDateMouvement(),
                m.getMotif(),
                auteurNom,
                restant
        );
    }
}

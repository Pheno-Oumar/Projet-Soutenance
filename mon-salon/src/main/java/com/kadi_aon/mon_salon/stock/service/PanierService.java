package com.kadi_aon.mon_salon.stock.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.stock.dto.AjoutPanierDTORequest;
import com.kadi_aon.mon_salon.stock.dto.LignePanierDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.ModificationQuantiteDTORequest;
import com.kadi_aon.mon_salon.stock.dto.PanierDTOResponse;
import com.kadi_aon.mon_salon.stock.entity.LignePanier;
import com.kadi_aon.mon_salon.stock.entity.Panier;
import com.kadi_aon.mon_salon.stock.entity.Produit;
import com.kadi_aon.mon_salon.stock.entity.StockProduit;
import com.kadi_aon.mon_salon.stock.exception.StockInsuffisantException;
import com.kadi_aon.mon_salon.stock.repository.LignePanierRepository;
import com.kadi_aon.mon_salon.stock.repository.PanierRepository;
import com.kadi_aon.mon_salon.stock.repository.ProduitRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PanierService {

    private final PanierRepository panierRepository;
    private final LignePanierRepository lignePanierRepository;
    private final ProduitRepository produitRepository;
    private final AffectationSalonRepository affectationSalonRepository;

    @Transactional
    public PanierDTOResponse obtenirPanier(String slugSalon, String clientEmail) {
        AffectationSalon affectation = validerClient(slugSalon, clientEmail);
        Panier panier = getOrCreatePanier(affectation);
        return mapPanierToResponse(panier);
    }

    @Transactional
    public PanierDTOResponse ajouterArticle(String slugSalon, String clientEmail, AjoutPanierDTORequest request) {
        AffectationSalon affectation = validerClient(slugSalon, clientEmail);
        Panier panier = getOrCreatePanier(affectation);

        Produit produit = produitRepository.findByIdAndCategorieSalonSlug(request.produitId(), slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Produit ID " + request.produitId() + " introuvable dans ce salon."));

        if (!produit.isStatut()) {
            throw new IllegalArgumentException("Le produit '" + produit.getNom() + "' n'est plus actif à la vente.");
        }

        StockProduit stock = produit.getStock();
        int stockDispo = (stock != null) ? stock.getQuantiteDisponible() : 0;

        LignePanier ligneExistante = panier.getLignes().stream()
                .filter(l -> l.getProduit().getId().equals(produit.getId()))
                .findFirst()
                .orElse(null);

        int quantiteDemandee = (ligneExistante != null ? ligneExistante.getQuantite() : 0) + request.quantite();

        if (quantiteDemandee > stockDispo) {
            throw new StockInsuffisantException("Stock insuffisant pour le produit '" + produit.getNom() + "'. Disponible : " + stockDispo + ", demandé au total : " + quantiteDemandee);
        }

        if (ligneExistante != null) {
            ligneExistante.setQuantite(quantiteDemandee);
        } else {
            LignePanier nouvelleLigne = LignePanier.builder()
                    .panier(panier)
                    .produit(produit)
                    .quantite(request.quantite())
                    .build();
            panier.getLignes().add(nouvelleLigne);
        }

        panier.setDateModification(LocalDateTime.now());
        Panier savedPanier = panierRepository.save(panier);
        log.info("Article {} (qte: {}) ajouté au panier de {}", produit.getNom(), request.quantite(), clientEmail);
        return mapPanierToResponse(savedPanier);
    }

    @Transactional
    public PanierDTOResponse modifierQuantite(String slugSalon, String clientEmail, Long produitId, ModificationQuantiteDTORequest request) {
        AffectationSalon affectation = validerClient(slugSalon, clientEmail);
        Panier panier = getOrCreatePanier(affectation);

        LignePanier ligne = panier.getLignes().stream()
                .filter(l -> l.getProduit().getId().equals(produitId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Le produit ID " + produitId + " n'est pas dans votre panier."));

        StockProduit stock = ligne.getProduit().getStock();
        int stockDispo = (stock != null) ? stock.getQuantiteDisponible() : 0;

        if (request.quantite() > stockDispo) {
            throw new StockInsuffisantException("Stock insuffisant pour le produit '" + ligne.getProduit().getNom() + "'. Disponible : " + stockDispo + ", demandé : " + request.quantite());
        }

        ligne.setQuantite(request.quantite());
        panier.setDateModification(LocalDateTime.now());
        Panier savedPanier = panierRepository.save(panier);
        log.info("Quantité modifiée à {} pour le produit {} dans le panier de {}", request.quantite(), produitId, clientEmail);
        return mapPanierToResponse(savedPanier);
    }

    @Transactional
    public PanierDTOResponse supprimerArticle(String slugSalon, String clientEmail, Long produitId) {
        AffectationSalon affectation = validerClient(slugSalon, clientEmail);
        Panier panier = getOrCreatePanier(affectation);

        boolean removed = panier.getLignes().removeIf(l -> l.getProduit().getId().equals(produitId));
        if (!removed) {
            throw new EntityNotFoundException("Le produit ID " + produitId + " n'est pas dans votre panier.");
        }

        panier.setDateModification(LocalDateTime.now());
        Panier savedPanier = panierRepository.save(panier);
        log.info("Produit ID {} retiré du panier de {}", produitId, clientEmail);
        return mapPanierToResponse(savedPanier);
    }

    @Transactional
    public void viderPanier(String slugSalon, String clientEmail) {
        AffectationSalon affectation = validerClient(slugSalon, clientEmail);
        Panier panier = getOrCreatePanier(affectation);
        panier.vider();
        panier.setDateModification(LocalDateTime.now());
        panierRepository.save(panier);
        log.info("Panier vidé pour {}", clientEmail);
    }

    public Panier getOrCreatePanier(AffectationSalon affectation) {
        return panierRepository.findByClientAffectationId(affectation.getId())
                .orElseGet(() -> {
                    Panier p = Panier.builder()
                            .clientAffectation(affectation)
                            .dateCreation(LocalDateTime.now())
                            .dateModification(LocalDateTime.now())
                            .lignes(new ArrayList<>())
                            .build();
                    return panierRepository.save(p);
                });
    }

    private AffectationSalon validerClient(String slugSalon, String email) {
        AffectationSalon affectation = affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Affectation active introuvable pour le client " + email + " sur le salon " + slugSalon));

        boolean isClient = affectation.getRoles().stream()
                .anyMatch(r -> r.getRole() == TypeRoleSalon.CLIENT);

        if (!isClient) {
            throw new IllegalArgumentException("L'utilisateur " + email + " ne possède pas le rôle CLIENT sur le salon " + slugSalon);
        }
        return affectation;
    }

    public PanierDTOResponse mapPanierToResponse(Panier panier) {
        List<LignePanierDTOResponse> lignesDTO = panier.getLignes().stream()
                .map(l -> {
                    StockProduit stock = l.getProduit().getStock();
                    int dispo = (stock != null) ? stock.getQuantiteDisponible() : 0;
                    int seuilMin = (stock != null) ? stock.getSeuilMinimum() : 0;
                    boolean enAlerte = dispo <= seuilMin;
                    return new LignePanierDTOResponse(
                            l.getId(),
                            l.getProduit().getId(),
                            l.getProduit().getNom(),
                            l.getProduit().getPrixVente(),
                            l.getQuantite(),
                            l.calculerSousTotal(),
                            dispo,
                            enAlerte
                    );
                })
                .toList();

        BigDecimal total = panier.calculerTotal();
        int nombreArticles = panier.getLignes().stream().mapToInt(LignePanier::getQuantite).sum();
        String slug = panier.getClientAffectation().getSalon().getSlug();
        String email = panier.getClientAffectation().getCompte().getEmail();

        return new PanierDTOResponse(
                panier.getId(),
                slug,
                email,
                lignesDTO,
                total,
                nombreArticles,
                panier.getDateModification()
        );
    }

    @Transactional(readOnly = true)
    public List<PanierDTOResponse> listerTousMesPaniers(String clientEmail) {
        return panierRepository.findByClientAffectationCompteEmail(clientEmail).stream()
                .filter(p -> p.getLignes() != null && !p.getLignes().isEmpty())
                .map(this::mapPanierToResponse)
                .toList();
    }

    @Transactional
    public void viderTousMesPaniers(String clientEmail) {
        List<Panier> paniers = panierRepository.findByClientAffectationCompteEmail(clientEmail);
        for (Panier p : paniers) {
            p.getLignes().clear();
            p.setDateModification(LocalDateTime.now());
            panierRepository.save(p);
        }
    }
}


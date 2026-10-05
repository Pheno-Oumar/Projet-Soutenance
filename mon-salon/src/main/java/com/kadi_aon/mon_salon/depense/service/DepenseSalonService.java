package com.kadi_aon.mon_salon.depense.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.caisse.entity.OperationCaisse;
import com.kadi_aon.mon_salon.caisse.entity.SessionCaisse;
import com.kadi_aon.mon_salon.caisse.service.CaisseSalonService;
import com.kadi_aon.mon_salon.depense.dto.DepenseCreateDTORequest;
import com.kadi_aon.mon_salon.depense.dto.DepenseDTOResponse;
import com.kadi_aon.mon_salon.depense.entity.Depense;
import com.kadi_aon.mon_salon.depense.enums.CategorieDepense;
import com.kadi_aon.mon_salon.depense.repository.DepenseRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
//import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;
import com.kadi_aon.mon_salon.stock.entity.MouvementStock;
import com.kadi_aon.mon_salon.stock.entity.StockProduit;
import com.kadi_aon.mon_salon.stock.enums.TypeMouvementStock;
import com.kadi_aon.mon_salon.stock.repository.MouvementStockRepository;
import com.kadi_aon.mon_salon.stock.repository.StockProduitRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepenseSalonService {

    private final DepenseRepository depenseRepository;
    private final CaisseSalonService caisseSalonService;
    private final AffectationSalonRepository affectationSalonRepository;
    private final SalonRepository salonRepository;
    private final AuditLogService auditLogService;
    private final StockProduitRepository stockProduitRepository;
    private final MouvementStockRepository mouvementStockRepository;

    @Transactional
    public DepenseDTOResponse creerDepense(String slugSalon, String comptableEmail, DepenseCreateDTORequest request) {
        AffectationSalon affectation = validerComptable(slugSalon, comptableEmail);

        // Récupérer la session de caisse active (ou lève SessionCaisseFermeeException)
        SessionCaisse sessionCaisse = caisseSalonService.obtenirSessionActive(slugSalon);

        // Enregistrer l'opération de sortie dans la caisse
        String libelle = "Dépense [" + request.categorie().name() + "]" +
                (request.description() != null && !request.description().isBlank() ? " : " + request.description() : "");
        OperationCaisse opSortie = caisseSalonService.enregistrerOperationSortie(sessionCaisse, request.montant(), libelle, null);

        Depense depense = Depense.builder()
                .montant(request.montant())
                .dateDepense(LocalDateTime.now())
                .description(request.description())
                .categorie(request.categorie())
                .statut(true)
                .salon(affectation.getSalon())
                .comptable(affectation)
                .operationCaisse(opSortie)
                .build();

        Depense saved = depenseRepository.save(depense);

        auditLogService.logActionSalon(
                TypeActionAudit.CREATION,
                "Depense",
                saved.getId().toString(),
                null,
                "Création dépense " + saved.getCategorie() + " d'un montant de " + saved.getMontant(),
                affectation,
                TypeRoleSalon.COMPTABLE.name()
        );

        log.info("Dépense {} créée pour le salon {} par {}", saved.getId(), slugSalon, comptableEmail);
        return mapToResponse(saved);
    }

    @Transactional
    public Depense creerDepenseAchatStock(AffectationSalon auteur, SessionCaisse sessionCaisse, BigDecimal montant, String description, MouvementStock mouvementStock) {
        String libelle = "Achat Stock : " + (description != null ? description : "");
        OperationCaisse opSortie = caisseSalonService.enregistrerOperationSortie(sessionCaisse, montant, libelle, null);

        Depense depense = Depense.builder()
                .montant(montant)
                .dateDepense(LocalDateTime.now())
                .description(description)
                .categorie(CategorieDepense.ACHAT_STOCK)
                .statut(true)
                .salon(auteur.getSalon())
                .comptable(auteur)
                .operationCaisse(opSortie)
                .mouvementStock(mouvementStock)
                .build();

        Depense saved = depenseRepository.save(depense);

        auditLogService.logActionSalon(
                TypeActionAudit.CREATION,
                "Depense",
                saved.getId().toString(),
                null,
                "Achat stock automatique pour mouvement #" + (mouvementStock != null ? mouvementStock.getId() : "init") + ", montant: " + montant,
                auteur,
                "STOCK"
        );

        return saved;
    }

    @Transactional
    public DepenseDTOResponse annulerDepense(String slugSalon, Long depenseId, String comptableEmail) {
        AffectationSalon affectation = validerComptable(slugSalon, comptableEmail);

        Depense depense = depenseRepository.findByIdAndSalonSlug(depenseId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Dépense introuvable avec l'ID " + depenseId + " pour le salon " + slugSalon));

        if (!depense.isStatut()) {
            throw new IllegalStateException("La dépense #" + depenseId + " est déjà annulée.");
        }

        depense.setStatut(false);

        if (depense.getOperationCaisse() != null) {
            depense.getOperationCaisse().setStatut(false);
        }

        // Si la dépense est liée à un achat de stock, annuler le stock ajouté
        if (depense.getMouvementStock() != null) {
            MouvementStock mvt = depense.getMouvementStock();
            if (mvt.getType() == TypeMouvementStock.ENTREE && mvt.getProduit() != null) {
                stockProduitRepository.findByProduitId(mvt.getProduit().getId()).ifPresent(stock -> {
                    int ancienneQte = stock.getQuantiteDisponible();
                    int nouvelleQte = Math.max(0, ancienneQte - mvt.getQuantite());
                    stock.setQuantiteDisponible(nouvelleQte);
                    stock.setDateDerniereMiseAJour(LocalDateTime.now());
                    stockProduitRepository.save(stock);
                    log.info("Stock du produit {} mis à jour suite à l'annulation de la dépense {} : {} -> {}",
                            mvt.getProduit().getNom(), depense.getId(), ancienneQte, nouvelleQte);
                });
                mvt.setMotif((mvt.getMotif() != null ? mvt.getMotif() + " " : "") + "[ANNULE PAR DEPENSE #" + depense.getId() + "]");
                mouvementStockRepository.save(mvt);
            }
        }

        Depense updated = depenseRepository.save(depense);

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "Depense",
                updated.getId().toString(),
                "ACTIVE",
                "ANNULEE",
                affectation,
                TypeRoleSalon.COMPTABLE.name()
        );

        log.info("Dépense {} annulée pour le salon {} par {}", updated.getId(), slugSalon, comptableEmail);
        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public DepenseDTOResponse obtenirDepense(String slugSalon, Long depenseId) {
        Depense depense = depenseRepository.findByIdAndSalonSlug(depenseId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Dépense introuvable avec l'ID " + depenseId + " pour le salon " + slugSalon));
        return mapToResponse(depense);
    }

    @Transactional(readOnly = true)
    public List<DepenseDTOResponse> listerDepenses(String slugSalon, CategorieDepense categorie, Boolean statut) {
        if (!salonRepository.existsBySlug(slugSalon)) {
            throw new EntityNotFoundException("Salon introuvable pour le slug : " + slugSalon);
        }

        List<Depense> depenses;
        if (categorie != null && statut != null) {
            depenses = depenseRepository.findBySalonSlugAndCategorieAndStatutOrderByDateDepenseDesc(slugSalon, categorie, statut);
        } else if (categorie != null) {
            depenses = depenseRepository.findBySalonSlugAndCategorieOrderByDateDepenseDesc(slugSalon, categorie);
        } else if (statut != null) {
            depenses = depenseRepository.findBySalonSlugAndStatutOrderByDateDepenseDesc(slugSalon, statut);
        } else {
            depenses = depenseRepository.findBySalonSlugOrderByDateDepenseDesc(slugSalon);
        }

        return depenses.stream().map(this::mapToResponse).toList();
    }

    private AffectationSalon validerComptable(String slugSalon, String email) {
        AffectationSalon affectation = affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Affectation active introuvable pour l'utilisateur " + email + " sur le salon " + slugSalon));

        boolean hasComptableRole = affectation.getRoles().stream()
                .anyMatch(r -> r.getRole() == TypeRoleSalon.COMPTABLE);

        if (!hasComptableRole) {
            throw new IllegalArgumentException("L'utilisateur " + email + " ne possède pas le rôle COMPTABLE sur le salon " + slugSalon);
        }
        return affectation;
    }

    private DepenseDTOResponse mapToResponse(Depense d) {
        String auteurNom = d.getComptable() != null && d.getComptable().getCompte() != null
                ? d.getComptable().getCompte().getPrenom() + " " + d.getComptable().getCompte().getNom()
                : "N/A";

        return new DepenseDTOResponse(
                d.getId(),
                d.getMontant(),
                d.getDateDepense(),
                d.getDescription(),
                d.getCategorie().name(),
                d.isStatut(),
                d.getSalon().getSlug(),
                auteurNom,
                d.getOperationCaisse() != null ? d.getOperationCaisse().getId() : null,
                d.getMouvementStock() != null ? d.getMouvementStock().getId() : null
        );
    }
}

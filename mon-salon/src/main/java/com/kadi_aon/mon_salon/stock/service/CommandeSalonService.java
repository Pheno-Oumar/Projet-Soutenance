package com.kadi_aon.mon_salon.stock.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.facturation.entity.Facture;
import com.kadi_aon.mon_salon.facturation.repository.FactureRepository;
import com.kadi_aon.mon_salon.notification.service.NotificationEmailService;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.RoleSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;
import com.kadi_aon.mon_salon.stock.dto.CommandeDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.KpiStockDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.LigneCommandeDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.ProduitAlerteStockDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.RejetCommandeDTORequest;
import com.kadi_aon.mon_salon.stock.dto.RetraitCommandeDTORequest;
import com.kadi_aon.mon_salon.stock.dto.VitrineCommandeDTORequest;
import com.kadi_aon.mon_salon.stock.entity.Commande;
import com.kadi_aon.mon_salon.stock.entity.LigneCommande;
import com.kadi_aon.mon_salon.stock.entity.LignePanier;
import com.kadi_aon.mon_salon.stock.entity.MouvementStock;
import com.kadi_aon.mon_salon.stock.entity.Panier;
import com.kadi_aon.mon_salon.stock.entity.Produit;
import com.kadi_aon.mon_salon.stock.entity.StockProduit;
import com.kadi_aon.mon_salon.stock.enums.StatutCommande;
import com.kadi_aon.mon_salon.stock.enums.TypeMouvementStock;
import com.kadi_aon.mon_salon.stock.exception.StockInsuffisantException;
import com.kadi_aon.mon_salon.stock.repository.CommandeRepository;
import com.kadi_aon.mon_salon.stock.repository.MouvementStockRepository;
import com.kadi_aon.mon_salon.stock.repository.ProduitRepository;
import com.kadi_aon.mon_salon.stock.repository.StockProduitRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandeSalonService {

    private final CommandeRepository commandeRepository;
    private final PanierService panierService;
    private final ProduitRepository produitRepository;
    private final StockProduitRepository stockProduitRepository;
    private final MouvementStockRepository mouvementStockRepository;
    private final FactureRepository factureRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final NotificationEmailService notificationEmailService;
    private final AuditLogService auditLogService;
    private final SalonRepository salonRepository;
    private final CompteRepository compteRepository;
    private final RoleSalonRepository roleSalonRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Random RANDOM = new Random();

    @Transactional
    public CommandeDTOResponse passerCommande(String slugSalon, String clientEmail) {
        AffectationSalon clientAffectation = validerClient(slugSalon, clientEmail);
        Panier panier = panierService.getOrCreatePanier(clientAffectation);

        if (panier.getLignes() == null || panier.getLignes().isEmpty()) {
            throw new IllegalStateException("Votre panier est vide. Impossible de passer une commande.");
        }

        // Vérification des stocks pour chaque produit
        for (LignePanier lp : panier.getLignes()) {
            Produit p = lp.getProduit();
            if (!p.isStatut()) {
                throw new IllegalArgumentException("Le produit '" + p.getNom() + "' n'est plus actif à la vente.");
            }
            StockProduit sp = p.getStock();
            int dispo = (sp != null) ? sp.getQuantiteDisponible() : 0;
            if (lp.getQuantite() > dispo) {
                throw new StockInsuffisantException("Stock insuffisant pour le produit '" + p.getNom() + "'. Disponible : " + dispo + ", demandé : " + lp.getQuantite());
            }
        }

        String numeroCmd = "CMD-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        BigDecimal montantTotal = panier.calculerTotal();

        Commande commande = Commande.builder()
                .numeroCommande(numeroCmd)
                .dateCommande(LocalDateTime.now())
                .statut(StatutCommande.EN_ATTENTE)
                .montantTotal(montantTotal)
                .clientAffectation(clientAffectation)
                .build();

        for (LignePanier lp : panier.getLignes()) {
            LigneCommande lc = LigneCommande.builder()
                    .commande(commande)
                    .produit(lp.getProduit())
                    .quantite(lp.getQuantite())
                    .prixUnitaire(lp.getProduit().getPrixVente())
                    .sousTotal(lp.calculerSousTotal())
                    .build();
            commande.getLignes().add(lc);
        }

        Commande savedCommande = commandeRepository.save(commande);

        // Vider le panier
        panierService.viderPanier(slugSalon, clientEmail);

        // Notification email au client
        String nomSalon = clientAffectation.getSalon().getNom();
        String sujet = "Confirmation de votre commande " + savedCommande.getNumeroCommande() + " - " + nomSalon;
        String corps = String.format(
                "Bonjour %s,\n\n" +
                "Votre commande %s a été enregistrée avec succès auprès du salon %s.\n\n" +
                "Récapitulatif :\n" +
                "- Montant total : %s FCFA\n" +
                "- Statut : En attente de préparation\n\n" +
                "Notre équipe prépare votre commande. Vous recevrez un nouvel email dès qu'elle sera validée avec votre facture et votre code de retrait.\n\n" +
                "Cordialement,\n%s",
                clientAffectation.getCompte().getPrenom(),
                savedCommande.getNumeroCommande(),
                nomSalon,
                savedCommande.getMontantTotal(),
                nomSalon
        );
        notificationEmailService.queueNotificationEmail(clientEmail, sujet, corps);

        auditLogService.logActionSalon(
                TypeActionAudit.CREATION,
                "Commande",
                savedCommande.getId().toString(),
                null,
                "EN_ATTENTE (N° " + savedCommande.getNumeroCommande() + ", Montant : " + savedCommande.getMontantTotal() + ")",
                clientAffectation,
                "CLIENT"
        );

        log.info("Commande {} créée avec succès par {} pour le salon {}", savedCommande.getNumeroCommande(), clientEmail, slugSalon);
        return mapCommandeToResponse(savedCommande);
    }

    @Transactional
    public CommandeDTOResponse passerCommandeDepuisVitrine(String slugSalon, VitrineCommandeDTORequest request) {
        Salon salon = salonRepository.findBySlug(slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Salon introuvable avec le slug : " + slugSalon));

        if (!Boolean.TRUE.equals(salon.getStatut())) {
            throw new IllegalArgumentException("Impossible de passer commande : ce salon est actuellement inactif.");
        }

        if (request.lignes() == null || request.lignes().isEmpty()) {
            throw new IllegalStateException("Votre panier est vide. Impossible de passer une commande.");
        }

        String rawEmail = (request.email() != null && !request.email().isBlank())
                ? request.email().trim().toLowerCase(Locale.ROOT)
                : null;

        String email = rawEmail != null
                ? rawEmail
                : "visiteur." + request.telephone().trim().replaceAll("[^0-9]", "") + "@" + slugSalon + ".client";

        Compte compte = compteRepository.findByEmail(email).orElse(null);

        if (compte == null) {
            String rawPassword = (request.password() != null && !request.password().isBlank())
                    ? request.password()
                    : UUID.randomUUID().toString().substring(0, 12);

            String nom = (request.nom() != null) ? request.nom().trim() : "Client";
            String prenom = (request.prenom() != null && !request.prenom().isBlank())
                    ? request.prenom().trim()
                    : "";
            if (prenom.isBlank() && nom.contains(" ")) {
                String[] parts = nom.split(" ", 2);
                prenom = parts[0];
                nom = parts[1];
            } else if (prenom.isBlank()) {
                prenom = nom;
            }

            compte = Compte.builder()
                    .nom(nom)
                    .prenom(prenom)
                    .email(email)
                    .telephone(request.telephone().trim())
                    .password(passwordEncoder.encode(rawPassword))
                    .statut(true)
                    .build();
            compte = compteRepository.save(compte);
        }

        final Compte clientCompte = compte;
        AffectationSalon clientAffectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon)
                .orElseGet(() -> {
                    RoleSalon roleClient = roleSalonRepository.findByRole(TypeRoleSalon.CLIENT)
                            .orElseThrow(() -> new IllegalStateException("Rôle CLIENT non initialisé"));
                    Set<RoleSalon> roles = new HashSet<>();
                    roles.add(roleClient);

                    AffectationSalon nouvelle = AffectationSalon.builder()
                            .compte(clientCompte)
                            .salon(salon)
                            .roles(roles)
                            .statut(true)
                            .dateDebut(LocalDate.now())
                            .build();
                    return affectationSalonRepository.save(nouvelle);
                });

        BigDecimal montantTotal = BigDecimal.ZERO;
        List<LigneCommande> lignesCommande = new ArrayList<>();

        String numeroCmd = "CMD-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Commande commande = Commande.builder()
                .numeroCommande(numeroCmd)
                .dateCommande(LocalDateTime.now())
                .statut(StatutCommande.EN_ATTENTE)
                .clientAffectation(clientAffectation)
                .build();

        for (VitrineCommandeDTORequest.LigneVitrineCommandeDTORequest item : request.lignes()) {
            if (item.quantite() <= 0) continue;
            Produit p = produitRepository.findByIdAndCategorieSalonSlug(item.produitId(), slugSalon)
                    .orElseThrow(() -> new EntityNotFoundException("Produit ID " + item.produitId() + " introuvable dans ce salon."));

            if (!p.isStatut()) {
                throw new IllegalArgumentException("Le produit '" + p.getNom() + "' n'est plus actif à la vente.");
            }
            StockProduit sp = p.getStock();
            int dispo = (sp != null) ? sp.getQuantiteDisponible() : 0;
            if (item.quantite() > dispo) {
                throw new StockInsuffisantException("Stock insuffisant pour le produit '" + p.getNom() + "'. Disponible : " + dispo + ", demandé : " + item.quantite());
            }

            BigDecimal sousTotal = p.getPrixVente().multiply(BigDecimal.valueOf(item.quantite()));
            montantTotal = montantTotal.add(sousTotal);

            LigneCommande lc = LigneCommande.builder()
                    .commande(commande)
                    .produit(p)
                    .quantite(item.quantite())
                    .prixUnitaire(p.getPrixVente())
                    .sousTotal(sousTotal)
                    .build();
            lignesCommande.add(lc);
        }

        if (lignesCommande.isEmpty()) {
            throw new IllegalStateException("Aucun article valide dans la commande.");
        }

        commande.setLignes(lignesCommande);
        commande.setMontantTotal(montantTotal);

        Commande savedCommande = commandeRepository.save(commande);

        // Vider le panier éventuel du client
        try {
            panierService.viderPanier(slugSalon, email);
        } catch (Exception ignored) {}

        // Notification email
        if (rawEmail != null && rawEmail.contains("@")) {
            String nomSalon = salon.getNom();
            String sujet = "Confirmation de votre commande " + savedCommande.getNumeroCommande() + " - " + nomSalon;
            String corps = String.format(
                    "Bonjour %s,\n\n" +
                    "Votre commande Click & Collect %s a été enregistrée avec succès auprès du salon %s.\n\n" +
                    "Récapitulatif :\n" +
                    "- Montant total : %s FCFA\n" +
                    "- Mode de retrait : Click & Collect au comptoir du salon\n" +
                    "- Statut : En attente de préparation\n\n" +
                    "Vous recevrez une notification dès que votre commande sera prête au salon.\n\n" +
                    "Cordialement,\n%s",
                    clientCompte.getPrenom(),
                    savedCommande.getNumeroCommande(),
                    nomSalon,
                    savedCommande.getMontantTotal(),
                    nomSalon
            );
            notificationEmailService.queueNotificationEmail(rawEmail, sujet, corps);
        }

        auditLogService.logActionSalon(
                TypeActionAudit.CREATION,
                "Commande",
                savedCommande.getId().toString(),
                null,
                "EN_ATTENTE Click & Collect (N° " + savedCommande.getNumeroCommande() + ", Montant : " + savedCommande.getMontantTotal() + ")",
                clientAffectation,
                "CLIENT"
        );

        log.info("Commande vitrine {} créée avec succès pour {} (salon {})", savedCommande.getNumeroCommande(), email, slugSalon);
        return mapCommandeToResponse(savedCommande);
    }

    @Transactional
    public CommandeDTOResponse validerCommande(String slugSalon, String responsableEmail, Long commandeId) {
        AffectationSalon responsable = validerResponsableStock(slugSalon, responsableEmail);

        Commande commande = commandeRepository.findByIdAndClientAffectationSalonSlug(commandeId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Commande ID " + commandeId + " introuvable dans ce salon."));

        if (commande.getStatut() == StatutCommande.REJETEE) {
            throw new IllegalStateException("Une commande rejetée ne peut plus être validée.");
        }
        if (commande.getStatut() != StatutCommande.EN_ATTENTE) {
            throw new IllegalStateException("Seule une commande en attente peut être validée. Statut actuel : " + commande.getStatut());
        }

        // Décrémenter le stock et créer les mouvements VENTE
        for (LigneCommande lc : commande.getLignes()) {
            Produit p = lc.getProduit();
            StockProduit stock = p.getStock();
            if (stock == null || stock.getQuantiteDisponible() < lc.getQuantite()) {
                throw new StockInsuffisantException("Stock insuffisant pour valider la commande sur le produit " + p.getNom());
            }

            stock.setQuantiteDisponible(stock.getQuantiteDisponible() - lc.getQuantite());
            stock.setDateDerniereMiseAJour(LocalDateTime.now());
            stockProduitRepository.save(stock);

            MouvementStock mvt = MouvementStock.builder()
                    .produit(p)
                    .quantite(lc.getQuantite())
                    .type(TypeMouvementStock.VENTE)
                    .prixUnitaire(lc.getPrixUnitaire())
                    .dateMouvement(LocalDateTime.now())
                    .motif("Vente commande " + commande.getNumeroCommande())
                    .auteur(responsable)
                    .build();
            mouvementStockRepository.save(mvt);
        }

        // Code de retrait sécurisé (6 chiffres)
        String codeRetrait = "RET-" + (100000 + RANDOM.nextInt(900000));

        // Génération automatique de la Facture
        String numFacture = "FAC-CMD-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Facture facture = Facture.builder()
                .numeroFacture(numFacture)
                .dateEmission(LocalDateTime.now())
                .montantTotal(commande.getMontantTotal())
                .commande(commande)
                .clientAffectation(commande.getClientAffectation())
                .build();
        Facture savedFacture = factureRepository.save(facture);

        commande.setStatut(StatutCommande.VALIDEE);
        commande.setTraiteParAffectation(responsable);
        commande.setDateTraitement(LocalDateTime.now());
        commande.setCodeRetrait(codeRetrait);
        commande.setFacture(savedFacture);
        Commande updated = commandeRepository.save(commande);

        // Notification email au client avec facture et code de retrait
        String emailClient = commande.getClientAffectation().getCompte().getEmail();
        String nomSalon = commande.getClientAffectation().getSalon().getNom();
        String sujet = "Votre commande " + commande.getNumeroCommande() + " est prête ! - " + nomSalon;
        String corps = String.format(
                "Bonjour %s,\n\n" +
                "Bonne nouvelle ! Votre commande %s a été validée et est prête pour votre retrait au salon %s.\n\n" +
                "Informations de retrait (Click & Collect) :\n" +
                "- Code de retrait sécurisé : %s\n" +
                "- Facture n° : %s\n" +
                "- Montant total : %s FCFA\n\n" +
                "Veuillez présenter ce code lors de votre passage au salon pour récupérer vos produits.\n\n" +
                "À bientôt,\n%s",
                commande.getClientAffectation().getCompte().getPrenom(),
                commande.getNumeroCommande(),
                nomSalon,
                codeRetrait,
                savedFacture.getNumeroFacture(),
                commande.getMontantTotal(),
                nomSalon
        );
        if (emailClient != null && emailClient.contains("@") && !emailClient.endsWith(".client")) {
            notificationEmailService.queueNotificationEmail(emailClient, sujet, corps);
        }

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "Commande",
                updated.getId().toString(),
                "EN_ATTENTE",
                "VALIDEE (Code retrait: " + codeRetrait + ", Facture: " + savedFacture.getNumeroFacture() + ")",
                responsable,
                "RESPONSABLE_STOCK"
        );

        log.info("Commande {} validée par {}. Facture {} générée.", updated.getNumeroCommande(), responsableEmail, savedFacture.getNumeroFacture());
        return mapCommandeToResponse(updated);
    }

    @Transactional
    public CommandeDTOResponse rejeterCommande(String slugSalon, String responsableEmail, Long commandeId, RejetCommandeDTORequest request) {
        AffectationSalon responsable = validerResponsableStock(slugSalon, responsableEmail);

        Commande commande = commandeRepository.findByIdAndClientAffectationSalonSlug(commandeId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Commande ID " + commandeId + " introuvable dans ce salon."));

        if (commande.getStatut() == StatutCommande.VALIDEE) {
            throw new IllegalStateException("Une commande validée ne peut pas être rejetée.");
        }
        if (commande.getStatut() != StatutCommande.EN_ATTENTE) {
            throw new IllegalStateException("Seule une commande en attente peut être rejetée. Statut actuel : " + commande.getStatut());
        }

        commande.setStatut(StatutCommande.REJETEE);
        commande.setMotifRejet(request.motif().trim());
        commande.setTraiteParAffectation(responsable);
        commande.setDateTraitement(LocalDateTime.now());
        Commande updated = commandeRepository.save(commande);

        // Notification email au client avec motif de rejet
        String emailClient = commande.getClientAffectation().getCompte().getEmail();
        String nomSalon = commande.getClientAffectation().getSalon().getNom();
        String sujet = "Information concernant votre commande " + commande.getNumeroCommande() + " - " + nomSalon;
        String corps = String.format(
                "Bonjour %s,\n\n" +
                "Nous vous informons que votre commande %s auprès du salon %s n'a pas pu être validée.\n\n" +
                "Motif du refus :\n\"%s\"\n\n" +
                "Aucun montant n'a été facturé. Nous vous prions de nous excuser pour ce désagrément et restons à votre disposition au salon.\n\n" +
                "Cordialement,\n%s",
                commande.getClientAffectation().getCompte().getPrenom(),
                commande.getNumeroCommande(),
                nomSalon,
                request.motif().trim(),
                nomSalon
        );
        if (emailClient != null && emailClient.contains("@") && !emailClient.endsWith(".client")) {
            notificationEmailService.queueNotificationEmail(emailClient, sujet, corps);
        }

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "Commande",
                updated.getId().toString(),
                "EN_ATTENTE",
                "REJETEE (Motif: " + request.motif().trim() + ")",
                responsable,
                "RESPONSABLE_STOCK"
        );

        log.info("Commande {} rejetée par {}. Motif : {}", updated.getNumeroCommande(), responsableEmail, request.motif());
        return mapCommandeToResponse(updated);
    }

    @Transactional
    public CommandeDTOResponse confirmerRetrait(String slugSalon, String employeEmail, Long commandeId, RetraitCommandeDTORequest request) {
        AffectationSalon affectation = validerPersonnelSalon(slugSalon, employeEmail);

        Commande commande = commandeRepository.findByIdAndClientAffectationSalonSlug(commandeId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Commande ID " + commandeId + " introuvable dans ce salon."));

        if (commande.getStatut() != StatutCommande.VALIDEE) {
            throw new IllegalStateException("Seule une commande validée peut être marquée comme retirée. Statut actuel : " + commande.getStatut());
        }

        if (request != null && request.codeRetrait() != null && !request.codeRetrait().isBlank()) {
            if (!request.codeRetrait().trim().equalsIgnoreCase(commande.getCodeRetrait())) {
                throw new IllegalArgumentException("Code de retrait invalide pour cette commande.");
            }
        }

        commande.setStatut(StatutCommande.RECUPEREE);
        commande.setDateTraitement(LocalDateTime.now());
        Commande updated = commandeRepository.save(commande);

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "Commande",
                updated.getId().toString(),
                "VALIDEE",
                "RECUPEREE (Retrait confirmé)",
                affectation,
                "RESPONSABLE_STOCK"
        );

        log.info("Commande {} marquée comme RECUPEREE par {}", updated.getNumeroCommande(), employeEmail);
        return mapCommandeToResponse(updated);
    }

    // --- Consultations Client ---

    @Transactional(readOnly = true)
    public List<CommandeDTOResponse> listerCommandesClient(String slugSalon, String clientEmail) {
        validerClient(slugSalon, clientEmail);
        return commandeRepository.findByClientAffectationCompteEmailAndClientAffectationSalonSlugOrderByDateCommandeDesc(clientEmail, slugSalon).stream()
                .map(this::mapCommandeToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CommandeDTOResponse obtenirCommandeClient(String slugSalon, String clientEmail, Long commandeId) {
        validerClient(slugSalon, clientEmail);
        Commande c = commandeRepository.findByIdAndClientAffectationCompteEmailAndClientAffectationSalonSlug(commandeId, clientEmail, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Commande ID " + commandeId + " introuvable pour votre compte sur ce salon."));
        return mapCommandeToResponse(c);
    }

    @Transactional(readOnly = true)
    public List<CommandeDTOResponse> listerToutesCommandesClient(String clientEmail) {
        return commandeRepository.findByClientAffectationCompteEmailOrderByDateCommandeDesc(clientEmail).stream()
                .map(this::mapCommandeToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CommandeDTOResponse obtenirCommandeClientGlobal(String clientEmail, Long commandeId) {
        Commande c = commandeRepository.findByIdAndClientAffectationCompteEmail(commandeId, clientEmail)
                .orElseThrow(() -> new EntityNotFoundException("Commande ID " + commandeId + " introuvable."));
        return mapCommandeToResponse(c);
    }


    // --- Consultations Responsable Stock ---

    @Transactional(readOnly = true)
    public List<CommandeDTOResponse> listerCommandesSalon(String slugSalon, String responsableEmail, StatutCommande statut, LocalDateTime debut, LocalDateTime fin) {
        validerResponsableStock(slugSalon, responsableEmail);

        List<Commande> liste;
        if (statut != null && debut != null && fin != null) {
            liste = commandeRepository.findByClientAffectationSalonSlugAndStatutAndDateCommandeBetweenOrderByDateCommandeDesc(slugSalon, statut, debut, fin);
        } else if (statut != null) {
            liste = commandeRepository.findByClientAffectationSalonSlugAndStatutOrderByDateCommandeDesc(slugSalon, statut);
        } else if (debut != null && fin != null) {
            liste = commandeRepository.findByClientAffectationSalonSlugAndDateCommandeBetweenOrderByDateCommandeDesc(slugSalon, debut, fin);
        } else {
            liste = commandeRepository.findByClientAffectationSalonSlugOrderByDateCommandeDesc(slugSalon);
        }

        return liste.stream().map(this::mapCommandeToResponse).toList();
    }

    @Transactional(readOnly = true)
    public CommandeDTOResponse obtenirCommandeSalon(String slugSalon, String responsableEmail, Long commandeId) {
        validerResponsableStock(slugSalon, responsableEmail);
        Commande c = commandeRepository.findByIdAndClientAffectationSalonSlug(commandeId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Commande ID " + commandeId + " introuvable dans ce salon."));
        return mapCommandeToResponse(c);
    }

    @Transactional(readOnly = true)
    public List<ProduitAlerteStockDTOResponse> obtenirProduitsBientotEnRupture(String slugSalon, String responsableEmail) {
        validerResponsableStock(slugSalon, responsableEmail);
        List<Produit> alertes = produitRepository.findProduitsEnAlerteStock(slugSalon);

        return alertes.stream().map(p -> {
            StockProduit s = p.getStock();
            int dispo = (s != null) ? s.getQuantiteDisponible() : 0;
            int seuilMin = (s != null) ? s.getSeuilMinimum() : 0;
            Integer seuilMax = (s != null) ? s.getSeuilMaximum() : null;
            return new ProduitAlerteStockDTOResponse(
                    p.getId(),
                    p.getNom(),
                    p.getCategorie().getId(),
                    p.getCategorie().getNom(),
                    p.getPrixVente(),
                    dispo,
                    seuilMin,
                    seuilMax,
                    dispo == 0
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public KpiStockDTOResponse obtenirKpiStock(String slugSalon, String responsableEmail) {
        validerResponsableStock(slugSalon, responsableEmail);

        long totalProduitsActifs = produitRepository.countByCategorieSalonSlugAndStatut(slugSalon, true);
        long totalProduitsEnAlerte = produitRepository.countProduitsEnAlerte(slugSalon);
        long totalProduitsEnRupture = produitRepository.countProduitsEnRupture(slugSalon);
        BigDecimal valeurTotaleStock = produitRepository.calculateValeurTotaleStock(slugSalon);

        long totalCommandes = commandeRepository.countByClientAffectationSalonSlug(slugSalon);
        long commandesEnAttente = commandeRepository.countByClientAffectationSalonSlugAndStatut(slugSalon, StatutCommande.EN_ATTENTE);
        long commandesValidees = commandeRepository.countByClientAffectationSalonSlugAndStatut(slugSalon, StatutCommande.VALIDEE);
        long commandesRejetees = commandeRepository.countByClientAffectationSalonSlugAndStatut(slugSalon, StatutCommande.REJETEE);
        long commandesRecuperees = commandeRepository.countByClientAffectationSalonSlugAndStatut(slugSalon, StatutCommande.RECUPEREE);
        BigDecimal caVentes = commandeRepository.totalChiffreAffairesCommandesValidees(slugSalon);

        return new KpiStockDTOResponse(
                totalProduitsActifs,
                totalProduitsEnAlerte,
                totalProduitsEnRupture,
                valeurTotaleStock != null ? valeurTotaleStock : BigDecimal.ZERO,
                totalCommandes,
                commandesEnAttente,
                commandesValidees,
                commandesRejetees,
                commandesRecuperees,
                caVentes != null ? caVentes : BigDecimal.ZERO
        );
    }

    // --- Helpers de validation d'affectation ---

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

    private AffectationSalon validerResponsableStock(String slugSalon, String email) {
        AffectationSalon affectation = affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Affectation active introuvable pour l'utilisateur " + email + " sur le salon " + slugSalon));

        boolean hasRole = affectation.getRoles().stream()
                .anyMatch(r -> r.getRole() == TypeRoleSalon.RESPONSABLE_STOCK || r.getRole() == TypeRoleSalon.MANAGER);

        if (!hasRole) {
            throw new IllegalArgumentException("L'utilisateur " + email + " ne possède pas le rôle RESPONSABLE_STOCK sur le salon " + slugSalon);
        }
        return affectation;
    }

    private AffectationSalon validerPersonnelSalon(String slugSalon, String email) {
        AffectationSalon affectation = affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Affectation active introuvable pour l'utilisateur " + email + " sur le salon " + slugSalon));

        boolean hasRole = affectation.getRoles().stream()
                .anyMatch(r -> r.getRole() == TypeRoleSalon.RESPONSABLE_STOCK || r.getRole() == TypeRoleSalon.RECEPTIONNISTE || r.getRole() == TypeRoleSalon.MANAGER);

        if (!hasRole) {
            throw new IllegalArgumentException("L'utilisateur " + email + " n'est pas autorisé à confirmer les retraits sur le salon " + slugSalon);
        }
        return affectation;
    }

    public CommandeDTOResponse mapCommandeToResponse(Commande c) {
        List<LigneCommandeDTOResponse> lignesDTO = c.getLignes().stream()
                .map(l -> new LigneCommandeDTOResponse(
                        l.getId(),
                        l.getProduit().getId(),
                        l.getProduit().getNom(),
                        l.getQuantite(),
                        l.getPrixUnitaire(),
                        l.getSousTotal()
                ))
                .toList();

        String clientNom = (c.getClient() != null) ? c.getClient().getPrenom() + " " + c.getClient().getNom() : "Client";
        String clientEmail = (c.getClient() != null) ? c.getClient().getEmail() : "N/A";
        String traiteParNom = (c.getTraiteParAffectation() != null && c.getTraiteParAffectation().getCompte() != null)
                ? c.getTraiteParAffectation().getCompte().getPrenom() + " " + c.getTraiteParAffectation().getCompte().getNom()
                : null;

        Long factureId = (c.getFacture() != null) ? c.getFacture().getId() : null;
        String numFacture = (c.getFacture() != null) ? c.getFacture().getNumeroFacture() : null;

        return new CommandeDTOResponse(
                c.getId(),
                c.getNumeroCommande(),
                c.getDateCommande(),
                c.getStatut().name(),
                c.getMontantTotal(),
                clientNom,
                clientEmail,
                c.getCodeRetrait(),
                c.getMotifRejet(),
                c.getDateTraitement(),
                traiteParNom,
                factureId,
                numFacture,
                lignesDTO
        );
    }
}

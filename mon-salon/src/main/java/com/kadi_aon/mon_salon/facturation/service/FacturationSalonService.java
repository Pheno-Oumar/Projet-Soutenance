package com.kadi_aon.mon_salon.facturation.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.caisse.entity.OperationCaisse;
import com.kadi_aon.mon_salon.caisse.entity.SessionCaisse;
import com.kadi_aon.mon_salon.caisse.service.CaisseSalonService;
import com.kadi_aon.mon_salon.facturation.dto.FactureDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.PaiementDTORequest;
import com.kadi_aon.mon_salon.facturation.dto.PaiementDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.RemboursementDTORequest;
import com.kadi_aon.mon_salon.facturation.entity.Facture;
import com.kadi_aon.mon_salon.facturation.entity.Paiement;
import com.kadi_aon.mon_salon.facturation.enums.MoyenPaiement;
import com.kadi_aon.mon_salon.facturation.enums.StatutFacture;
import com.kadi_aon.mon_salon.facturation.enums.StatutPaiement;
import com.kadi_aon.mon_salon.facturation.enums.TypePaiement;
import com.kadi_aon.mon_salon.facturation.repository.FactureRepository;
import com.kadi_aon.mon_salon.facturation.repository.PaiementRepository;
import com.kadi_aon.mon_salon.prestation.entity.Prestation;
import com.kadi_aon.mon_salon.prestation.enums.StatutPrestation;
import com.kadi_aon.mon_salon.prestation.repository.PrestationRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacturationSalonService {

    private final FactureRepository factureRepository;
    private final PaiementRepository paiementRepository;
    private final PrestationRepository prestationRepository;
    private final CaisseSalonService caisseSalonService;
    private final AffectationSalonRepository affectationSalonRepository;
    private final AuditLogService auditLogService;

    // ═══════════════════════════════════════════════════════════════════
    // CRÉATION DE FACTURE
    // ═══════════════════════════════════════════════════════════════════

    @Transactional
    public Facture creerFactureInitiale(Prestation prestation) {
        String slug = (prestation.getSalon() != null) ? prestation.getSalon().getSlug() : "SALON";

        Facture facture = Facture.builder()
                .numeroFacture(genererNumeroFacture(slug))
                .dateEmission(LocalDateTime.now())
                .montantTotal(prestation.getMontantTotal())
                .montantPaye(BigDecimal.ZERO)
                .resteAPayer(prestation.getMontantTotal())
                .remise(BigDecimal.ZERO)
                .statut(StatutFacture.EMISE)
                .salon(prestation.getSalon())
                .prestation(prestation)
                .build();

        Facture saved = factureRepository.save(facture);
        log.info("Facture {} créée pour la prestation ID {} (Montant : {})",
                saved.getNumeroFacture(), prestation.getId(), saved.getMontantTotal());
        return saved;
    }

    // ═══════════════════════════════════════════════════════════════════
    // REMISE SUR FACTURE
    // ═══════════════════════════════════════════════════════════════════

    @Transactional
    public FactureDTOResponse appliquerRemise(String slugSalon, Long factureId, String employeEmail,
            BigDecimal montantRemise, String motifRemise) {
        AffectationSalon affectation = validerReceptionniste(slugSalon, employeEmail);

        Facture facture = factureRepository.findByIdAndPrestationSalonSlug(factureId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Facture introuvable ID " + factureId));

        if (facture.getStatut() == StatutFacture.ANNULEE || facture.getStatut() == StatutFacture.REMBOURSEE) {
            throw new IllegalStateException("Impossible d'appliquer une remise sur une facture " + facture.getStatut());
        }

        if (facture.getStatut() == StatutFacture.PAYEE) {
            throw new IllegalStateException("Impossible d'appliquer une remise sur une facture déjà soldée. Utilisez la procédure de remboursement.");
        }

        if (montantRemise == null || montantRemise.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le montant de la remise doit être positif ou nul.");
        }

        if (montantRemise.compareTo(facture.getMontantTotal()) > 0) {
            throw new IllegalArgumentException("La remise (" + montantRemise
                    + ") ne peut pas dépasser le montant total (" + facture.getMontantTotal() + ").");
        }

        BigDecimal nouveauNet = facture.getMontantTotal().subtract(montantRemise);
        if (facture.getMontantPaye() != null && nouveauNet.compareTo(facture.getMontantPaye()) < 0) {
            throw new IllegalArgumentException("La remise (" + montantRemise
                    + " FCFA) ne peut pas réduire le montant net en dessous du montant déjà encaissé ("
                    + facture.getMontantPaye() + " FCFA). Pour un geste commercial a posteriori, utilisez le remboursement.");
        }

        facture.setRemise(montantRemise);
        facture.setMotifRemise(motifRemise);

        recalculerFacture(facture);
        factureRepository.save(facture);

        auditLogService.logActionSalon(TypeActionAudit.MODIFICATION, "Facture",
                facture.getNumeroFacture(), null,
                "Remise de " + montantRemise + " appliquée : " + motifRemise,
                affectation, "RECEPTIONNISTE");

        log.info("Remise de {} appliquée sur la facture {}", montantRemise, facture.getNumeroFacture());
        return mapFactureToResponse(facture);
    }

    // ═══════════════════════════════════════════════════════════════════
    // ENCAISSEMENT (PAIEMENT)
    // ═══════════════════════════════════════════════════════════════════

    @Transactional
    public PaiementDTOResponse encaisserPaiement(String slugSalon, Long factureId,
            String employeEmail, PaiementDTORequest request) {
        AffectationSalon affectation = validerReceptionniste(slugSalon, employeEmail);

        SessionCaisse sessionActive = caisseSalonService.obtenirSessionActive(slugSalon);

        Facture facture = factureRepository.findByIdAndPrestationSalonSlug(factureId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Facture introuvable ID " + factureId + " dans le salon " + slugSalon));

        if (facture.getStatut() == StatutFacture.ANNULEE) {
            throw new IllegalStateException("Impossible d'encaisser un paiement sur une facture annulée.");
        }

        // Calcul dynamique du reste à payer basé sur le montant net (après remise)
        BigDecimal montantPayeExistant = calculerMontantPaye(facture);
        BigDecimal montantNet = facture.getMontantNet();
        BigDecimal resteAPayer = montantNet.subtract(montantPayeExistant);

        if (resteAPayer.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Cette facture est déjà intégralement payée.");
        }

        if (request.montant().compareTo(resteAPayer) > 0) {
            throw new IllegalArgumentException("Le montant saisi (" + request.montant() +
                    ") est supérieur au reste à payer (" + resteAPayer + ").");
        }

        // Auto-détection du type : ACOMPTE si partiel, SOLDE si total
        TypePaiement type = (request.montant().compareTo(resteAPayer) < 0)
                ? TypePaiement.ACOMPTE
                : TypePaiement.SOLDE;

        MoyenPaiement moyen = parseMoyenPaiement(request.moyenPaiement());

        String numeroPaiement = genererNumeroPaiement(slugSalon);

        Paiement paiement = Paiement.builder()
                .numeroPaiement(numeroPaiement)
                .montant(request.montant())
                .datePaiement(LocalDateTime.now())
                .type(type)
                .statut(StatutPaiement.PAYE)
                .moyenPaiement(moyen)
                .reference(request.reference())
                .encaissePar(affectation)
                .facture(facture)
                .client(facture.getPrestation() != null ? facture.getPrestation().getClient() : null)
                .salon(facture.getPrestation() != null ? facture.getPrestation().getSalon() : affectation.getSalon())
                .build();

        // Écriture d'entrée en caisse
        OperationCaisse opEntree = caisseSalonService.enregistrerOperationEntree(
                sessionActive, request.montant(),
                "Encaissement " + type.name() + " facture " + facture.getNumeroFacture() + " [" + numeroPaiement + "]",
                paiement);
        paiement.setOperationCaisse(opEntree);

        Paiement savedPaiement = paiementRepository.save(paiement);
        facture.getPaiements().add(savedPaiement);

        // Recalcul complet de la facture
        recalculerFacture(facture);

        // Si la totalité est payée → prestation TERMINEE
        if (facture.getStatut() == StatutFacture.PAYEE && facture.getPrestation() != null) {
            Prestation prestation = facture.getPrestation();
            prestation.setStatut(StatutPrestation.TERMINEE);
            if (prestation.getDateHeureFin() == null) {
                prestation.setDateHeureFin(LocalDateTime.now());
            }
            prestationRepository.save(prestation);
            log.info("Prestation ID {} soldée et marquée TERMINEE", prestation.getId());
        }

        factureRepository.save(facture);

        auditLogService.logActionSalon(TypeActionAudit.CREATION, "Paiement",
                savedPaiement.getNumeroPaiement(), null,
                "Paiement " + savedPaiement.getNumeroPaiement() + " encaissé de " + savedPaiement.getMontant() +
                        " pour la facture " + facture.getNumeroFacture(),
                affectation, affectation.getRoles().iterator().next().getRole().name());

        log.info("Paiement {} enregistré pour la facture {}", savedPaiement.getNumeroPaiement(),
                facture.getNumeroFacture());
        return mapPaiementToResponse(savedPaiement);
    }

    // ═══════════════════════════════════════════════════════════════════
    // REMBOURSEMENT (avec traçabilité FK)
    // ═══════════════════════════════════════════════════════════════════

    @Transactional
    public PaiementDTOResponse effectuerRemboursement(String slugSalon, Long paiementId,
            String employeEmail, RemboursementDTORequest request) {
        AffectationSalon affectation = validerReceptionniste(slugSalon, employeEmail);

        SessionCaisse sessionActive = caisseSalonService.obtenirSessionActive(slugSalon);

        Paiement paiementOrigine = paiementRepository.findByIdAndSalonSlug(paiementId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Paiement introuvable ID " + paiementId));

        // Vérification stricte : seul un paiement PAYE ou PARTIELLEMENT_REMBOURSE peut
        // être remboursé
        if (!paiementOrigine.isRemboursable()) {
            throw new IllegalStateException("Ce paiement (statut: " + paiementOrigine.getStatut() +
                    ") ne peut plus être remboursé. Montant restant remboursable : "
                    + paiementOrigine.getMontantRemboursable());
        }

        BigDecimal maxRemboursable = paiementOrigine.getMontantRemboursable();

        if (request.montant().compareTo(maxRemboursable) > 0) {
            throw new IllegalArgumentException("Le montant du remboursement (" + request.montant() +
                    " FCFA) excède le montant maximal remboursable (" + maxRemboursable + " FCFA).");
        }

        String numeroRemboursement = genererNumeroRemboursement(slugSalon);

        // Création du paiement de remboursement avec FK vers l'original
        Paiement remboursement = Paiement.builder()
                .numeroPaiement(numeroRemboursement)
                .montant(request.montant())
                .datePaiement(LocalDateTime.now())
                .type(paiementOrigine.getType())
                .statut(StatutPaiement.REMBOURSE)
                .moyenPaiement(paiementOrigine.getMoyenPaiement())
                .reference(request.motif())
                .paiementOrigine(paiementOrigine)
                .encaissePar(affectation)
                .facture(paiementOrigine.getFacture())
                .client(paiementOrigine.getClient())
                .salon(paiementOrigine.getSalon())
                .build();

        // Écriture de sortie en caisse
        OperationCaisse opSortie = caisseSalonService.enregistrerOperationSortie(
                sessionActive, request.montant(),
                "Remboursement " + numeroRemboursement + " (Réf " + paiementOrigine.getNumeroPaiement() + ") : "
                        + request.motif(),
                remboursement);
        remboursement.setOperationCaisse(opSortie);

        Paiement savedRemboursement = paiementRepository.save(remboursement);

        // Mise à jour du paiement d'origine : tracker le montant remboursé et mettre à
        // jour le statut
        BigDecimal nouveauMontantRembourse = paiementOrigine.getMontantRembourse().add(request.montant());
        paiementOrigine.setMontantRembourse(nouveauMontantRembourse);

        if (nouveauMontantRembourse.compareTo(paiementOrigine.getMontant()) >= 0) {
            paiementOrigine.setStatut(StatutPaiement.REMBOURSE);
        } else {
            paiementOrigine.setStatut(StatutPaiement.PARTIELLEMENT_REMBOURSE);
        }
        paiementRepository.save(paiementOrigine);

        // Recalcul de la facture
        Facture facture = paiementOrigine.getFacture();
        facture.getPaiements().add(savedRemboursement);

        // Option : annuler la prestation si demandé
        boolean annulerPrestation = Boolean.TRUE.equals(request.annulerPrestation())
                || (facture.getPrestation() != null && facture.getPrestation().getStatut() == StatutPrestation.ANNULEE);

        if (annulerPrestation) {
            facture.setStatut(StatutFacture.ANNULEE);
            facture.setResteAPayer(BigDecimal.ZERO);
            facture.setMontantPaye(BigDecimal.ZERO);
            if (facture.getPrestation() != null && facture.getPrestation().getStatut() != StatutPrestation.ANNULEE) {
                facture.getPrestation().setStatut(StatutPrestation.ANNULEE);
                prestationRepository.save(facture.getPrestation());
            }
        } else {
            recalculerFacture(facture);
        }

        factureRepository.save(facture);

        auditLogService.logActionSalon(TypeActionAudit.MODIFICATION, "Paiement",
                savedRemboursement.getNumeroPaiement(), paiementOrigine.getNumeroPaiement(),
                "Remboursement " + savedRemboursement.getNumeroPaiement() + " de " + request.montant() +
                        " sur paiement " + paiementOrigine.getNumeroPaiement() + " (Motif : " + request.motif() + ")",
                affectation, affectation.getRoles().iterator().next().getRole().name());

        log.info("Remboursement {} créé pour le paiement {}", savedRemboursement.getNumeroPaiement(),
                paiementOrigine.getNumeroPaiement());
        return mapPaiementToResponse(savedRemboursement);
    }

    // ═══════════════════════════════════════════════════════════════════
    // CONSULTATION
    // ═══════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<PaiementDTOResponse> listerPaiementsClient(String slugSalon, String clientEmail) {
        return paiementRepository.findByClientEmailAndSalonSlugOrderByDatePaiementDesc(clientEmail, slugSalon).stream()
                .map(this::mapPaiementToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaiementDTOResponse obtenirDetailPaiementClient(String slugSalon, Long paiementId, String clientEmail) {
        Paiement p = paiementRepository.findByIdAndClientEmailAndSalonSlug(paiementId, clientEmail, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Paiement introuvable ID " + paiementId));
        return mapPaiementToResponse(p);
    }

    @Transactional(readOnly = true)
    public List<PaiementDTOResponse> listerTousPaiementsClient(String clientEmail) {
        return paiementRepository.findByClientEmailOrderByDatePaiementDesc(clientEmail).stream()
                .map(this::mapPaiementToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaiementDTOResponse obtenirDetailPaiementClientGlobal(Long paiementId, String clientEmail) {
        Paiement p = paiementRepository.findByIdAndClientEmail(paiementId, clientEmail)
                .orElseThrow(() -> new EntityNotFoundException("Paiement introuvable ID " + paiementId));
        return mapPaiementToResponse(p);
    }

    @Transactional(readOnly = true)
    public FactureDTOResponse obtenirFactureDetail(String slugSalon, Long factureId) {
        Facture f = factureRepository.findByIdAndPrestationSalonSlug(factureId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Facture introuvable ID " + factureId));
        return mapFactureToResponse(f);
    }

    @Transactional(readOnly = true)
    public FactureDTOResponse rechercherFactureParNumero(String slugSalon, String numero) {
        if (numero == null || numero.isBlank()) {
            throw new IllegalArgumentException("Le numéro de facture est obligatoire.");
        }
        Facture f = factureRepository.findByNumeroFacture(numero.trim())
                .orElseThrow(() -> new EntityNotFoundException("Facture introuvable pour le numéro " + numero));
        if (f.getPrestation() != null && f.getPrestation().getSalon() != null) {
            if (!f.getPrestation().getSalon().getSlug().equalsIgnoreCase(slugSalon)) {
                throw new EntityNotFoundException("Facture " + numero + " introuvable dans ce salon.");
            }
        }
        return mapFactureToResponse(f);
    }

    @Transactional(readOnly = true)
    public List<FactureDTOResponse> listerFacturesSalon(String slugSalon, String statutOpt) {
        List<Facture> list = factureRepository.findByPrestationSalonSlugOrderByDateEmissionDesc(slugSalon);
        if (statutOpt != null && !statutOpt.isBlank()) {
            String s = statutOpt.trim().toUpperCase();
            if ("IMPAYEE".equals(s) || "IMPAYEES".equals(s)) {
                list = list.stream()
                        .filter(f -> f.getStatut() != StatutFacture.PAYEE
                                && f.getStatut() != StatutFacture.ANNULEE
                                && f.getStatut() != StatutFacture.REMBOURSEE)
                        .toList();
            } else {
                try {
                    StatutFacture sf = StatutFacture.valueOf(s);
                    list = list.stream().filter(f -> f.getStatut() == sf).toList();
                } catch (IllegalArgumentException ignored) {
                    log.warn("Statut de facture non reconnu dans le filtre : {}", statutOpt);
                }
            }
        }
        return list.stream().map(this::mapFactureToResponse).toList();
    }

    // ═══════════════════════════════════════════════════════════════════
    // LOGIQUE DE CALCUL CENTRALISÉE
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Source de vérité pour le montant effectivement payé sur une facture.
     * Somme des paiements PAYE/PARTIELLEMENT_REMBOURSE - Somme des remboursements.
     */
    private BigDecimal calculerMontantPaye(Facture f) {
        BigDecimal totalPaye = BigDecimal.ZERO;
        BigDecimal totalRembourse = BigDecimal.ZERO;

        if (f.getPaiements() != null) {
            for (Paiement p : f.getPaiements()) {
                if (p.getStatut() == StatutPaiement.PAYE
                        || p.getStatut() == StatutPaiement.PARTIELLEMENT_REMBOURSE
                        || p.getStatut() == StatutPaiement.REMBOURSE) {
                    // C'est un paiement d'encaissement (pas un remboursement enfant)
                    if (p.getPaiementOrigine() == null) {
                        totalPaye = totalPaye.add(p.getMontant());
                    }
                }
                // Paiement enfant = remboursement
                if (p.getPaiementOrigine() != null && p.getStatut() == StatutPaiement.REMBOURSE) {
                    totalRembourse = totalRembourse.add(p.getMontant());
                }
            }
        }
        return totalPaye.subtract(totalRembourse);
    }

    /**
     * Recalcul centralisé : met à jour montantPaye, resteAPayer et statut de la
     * facture.
     */
    private void recalculerFacture(Facture facture) {
        BigDecimal montantPaye = calculerMontantPaye(facture);
        BigDecimal montantNet = facture.getMontantNet();
        BigDecimal resteAPayer = montantNet.subtract(montantPaye);

        if (resteAPayer.compareTo(BigDecimal.ZERO) < 0) {
            resteAPayer = BigDecimal.ZERO;
        }

        facture.setMontantPaye(montantPaye);
        facture.setResteAPayer(resteAPayer);

        // Mise à jour du statut
        if (facture.getStatut() == StatutFacture.ANNULEE) {
            return; // On ne touche pas au statut annulé
        }

        if (montantPaye.compareTo(BigDecimal.ZERO) <= 0) {
            facture.setStatut(StatutFacture.EMISE);
        } else if (resteAPayer.compareTo(BigDecimal.ZERO) == 0) {
            facture.setStatut(StatutFacture.PAYEE);
        } else {
            facture.setStatut(StatutFacture.PARTIELLEMENT_PAYEE);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // MAPPERS
    // ═══════════════════════════════════════════════════════════════════

    public FactureDTOResponse mapFactureToResponse(Facture f) {
        BigDecimal montantPaye = calculerMontantPaye(f);
        BigDecimal montantNet = f.getMontantNet();
        BigDecimal resteAPayer = montantNet.subtract(montantPaye);
        if (resteAPayer.compareTo(BigDecimal.ZERO) < 0) {
            resteAPayer = BigDecimal.ZERO;
        }

        List<PaiementDTOResponse> paiementsDTO = f.getPaiements().stream()
                .map(this::mapPaiementToResponse)
                .toList();

        Long prestationId = f.getPrestation() != null ? f.getPrestation().getId() : null;
        String clientNom = extraireClientNom(f);
        String clientTelephone = null;
        String coiffeurNom = null;
        List<com.kadi_aon.mon_salon.prestation.dto.LignePrestationDTOResponse> lignesDTO = null;

        if (f.getPrestation() != null) {
            clientTelephone = f.getPrestation().getTelephoneClient();
            if (f.getPrestation().getCoiffeur() != null && f.getPrestation().getCoiffeur().getCompte() != null) {
                String pre = f.getPrestation().getCoiffeur().getCompte().getPrenom();
                String nom = f.getPrestation().getCoiffeur().getCompte().getNom();
                coiffeurNom = ((pre != null ? pre + " " : "") + (nom != null ? nom : "")).trim();
            }
            if (f.getPrestation().getLignes() != null && !f.getPrestation().getLignes().isEmpty()) {
                lignesDTO = f.getPrestation().getLignes().stream()
                        .map(l -> new com.kadi_aon.mon_salon.prestation.dto.LignePrestationDTOResponse(
                                l.getId(),
                                l.getVarianteService() != null ? l.getVarianteService().getId() : null,
                                l.getVarianteService() != null ? l.getVarianteService().getNom() : null,
                                l.getPrixReel()))
                        .toList();
            }
        }

        return new FactureDTOResponse(
                f.getId(),
                f.getNumeroFacture(),
                f.getDateEmission(),
                f.getMontantTotal(),
                f.getRemise(),
                f.getMotifRemise(),
                montantNet,
                montantPaye,
                resteAPayer,
                prestationId,
                null,
                paiementsDTO,
                f.getStatut() != null ? f.getStatut().name() : null,
                clientNom,
                clientTelephone,
                coiffeurNom,
                lignesDTO);
    }

    public PaiementDTOResponse mapPaiementToResponse(Paiement p) {
        String nomClient = null;
        String prenomClient = null;
        Long prestationId = null;

        if (p.getFacture() != null && p.getFacture().getPrestation() != null) {
            prestationId = p.getFacture().getPrestation().getId();
            nomClient = p.getFacture().getPrestation().getNomClient();
            prenomClient = p.getFacture().getPrestation().getPrenomClient();
        }
        if (p.getClient() != null) {
            nomClient = p.getClient().getNom();
            prenomClient = p.getClient().getPrenom();
        }

        Long paiementOrigineId = (p.getPaiementOrigine() != null) ? p.getPaiementOrigine().getId() : null;
        String paiementOrigineNumero = (p.getPaiementOrigine() != null) ? p.getPaiementOrigine().getNumeroPaiement()
                : null;

        return new PaiementDTOResponse(
                p.getId(),
                p.getNumeroPaiement(),
                p.getMontant(),
                p.getDatePaiement(),
                p.getType().name(),
                p.getStatut().name(),
                p.getFacture() != null ? p.getFacture().getId() : null,
                p.getFacture() != null ? p.getFacture().getNumeroFacture() : null,
                prestationId,
                p.getSalon() != null ? p.getSalon().getSlug() : null,
                nomClient,
                prenomClient,
                p.getMoyenPaiement() != null ? p.getMoyenPaiement().name() : null,
                p.getReference(),
                paiementOrigineId,
                paiementOrigineNumero,
                p.getMontantRembourse(),
                p.getMontantRemboursable());
    }

    // ═══════════════════════════════════════════════════════════════════
    // UTILITAIRES
    // ═══════════════════════════════════════════════════════════════════

    private AffectationSalon validerReceptionniste(String slugSalon, String email) {
        AffectationSalon affectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Aucune affectation active pour " + email + " sur le salon " + slugSalon));

        boolean isReceptionniste = affectation.getRoles().stream()
                .anyMatch(r -> r.getRole() == TypeRoleSalon.RECEPTIONNISTE);
        if (!isReceptionniste) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Seul un réceptionniste est habilité à effectuer cette opération.");
        }

        return affectation;
    }

    private MoyenPaiement parseMoyenPaiement(String moyenStr) {
        if (moyenStr != null && !moyenStr.isBlank()) {
            try {
                return MoyenPaiement.valueOf(moyenStr.trim().toUpperCase());
            } catch (Exception e) {
                return MoyenPaiement.ESPECES;
            }
        }
        return MoyenPaiement.ESPECES;
    }

    private String extraireClientNom(Facture f) {
        if (f.getPrestation() != null) {
            if (f.getPrestation().getPrenomClient() != null || f.getPrestation().getNomClient() != null) {
                return ((f.getPrestation().getPrenomClient() != null ? f.getPrestation().getPrenomClient() + " " : "")
                        + (f.getPrestation().getNomClient() != null ? f.getPrestation().getNomClient() : "")).trim();
            } else if (f.getPrestation().getClient() != null) {
                return (f.getPrestation().getClient().getPrenom() + " " + f.getPrestation().getClient().getNom())
                        .trim();
            }
        }
        return null;
    }

    private String genererNumeroFacture(String slugSalon) {
        String cleanSlug = (slugSalon != null ? slugSalon.toUpperCase() : "SALON");
        String month = DateTimeFormatter.ofPattern("yyyyMM").format(LocalDateTime.now());
        String code = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "FAC-" + cleanSlug + "-" + month + "-" + code;
    }

    private String genererNumeroPaiement(String slugSalon) {
        String cleanSlug = (slugSalon != null ? slugSalon.toUpperCase() : "SALON");
        String month = DateTimeFormatter.ofPattern("yyyyMM").format(LocalDateTime.now());
        String code = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "PAY-" + cleanSlug + "-" + month + "-" + code;
    }

    private String genererNumeroRemboursement(String slugSalon) {
        String cleanSlug = (slugSalon != null ? slugSalon.toUpperCase() : "SALON");
        String month = DateTimeFormatter.ofPattern("yyyyMM").format(LocalDateTime.now());
        String code = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "REM-" + cleanSlug + "-" + month + "-" + code;
    }
}

                 
                        
package com.kadi_aon.mon_salon.prestation.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.facturation.entity.Facture;
import com.kadi_aon.mon_salon.facturation.service.FacturationSalonService;
import com.kadi_aon.mon_salon.prestation.dto.LignePrestationDTORequest;
import com.kadi_aon.mon_salon.prestation.dto.LignePrestationDTOResponse;
import com.kadi_aon.mon_salon.prestation.dto.PrestationCreateDTORequest;
import com.kadi_aon.mon_salon.prestation.dto.PrestationDTOResponse;
import com.kadi_aon.mon_salon.prestation.entity.LignePrestation;
import com.kadi_aon.mon_salon.prestation.entity.Prestation;
import com.kadi_aon.mon_salon.prestation.enums.StatutPrestation;
import com.kadi_aon.mon_salon.prestation.repository.PrestationRepository;
import com.kadi_aon.mon_salon.rendezvous.entity.RendezVous;
import com.kadi_aon.mon_salon.rendezvous.enums.StatutRendezVous;
import com.kadi_aon.mon_salon.rendezvous.repository.RendezVousRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.entity.VarianteService;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;
import com.kadi_aon.mon_salon.salon.repository.VarianteServiceRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrestationSalonService {

    private final PrestationRepository prestationRepository;
    private final RendezVousRepository rendezVousRepository;
    private final SalonRepository salonRepository;
    private final CompteRepository compteRepository;
    private final VarianteServiceRepository varianteServiceRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final FacturationSalonService facturationSalonService;
    private final AuditLogService auditLogService;

    // ═══════════════════════════════════════════════════════════════════
    //  CRÉATION DE PRESTATION
    // ═══════════════════════════════════════════════════════════════════

    @Transactional
    public PrestationDTOResponse creerPrestation(String slugSalon, String employeEmail,
            PrestationCreateDTORequest request) {
        AffectationSalon affectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(employeEmail, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Aucune affectation active trouvée pour " + employeEmail + " sur le salon " + slugSalon));

        Salon salon = affectation.getSalon();
        RendezVous rdv = null;
        Compte client = null;
        String nomClient = request.nomClient();
        String prenomClient = request.prenomClient();
        String telephoneClient = request.telephoneClient();
        AffectationSalon coiffeurPrestation = null;

        StatutPrestation statutInitial = (request.statut() != null) ? request.statut() : StatutPrestation.EN_COURS;

        // --- Cas RDV ---
        if (request.rendezVousId() != null) {
            rdv = rendezVousRepository.findById(request.rendezVousId())
                    .orElseThrow(() -> new EntityNotFoundException("Rendez-vous introuvable ID " + request.rendezVousId()));

            if (!rdv.getSalon().getId().equals(salon.getId())) {
                throw new IllegalArgumentException("Le rendez-vous " + rdv.getId() + " n'appartient pas au salon " + slugSalon);
            }

            // Validation croisée : RDV doit être CONFIRME ou EN_COURS
            if (rdv.getStatut() == StatutRendezVous.ANNULE || rdv.getStatut() == StatutRendezVous.NO_SHOW) {
                throw new IllegalStateException("Impossible de créer une prestation pour un rendez-vous au statut " + rdv.getStatut());
            }

            if (prestationRepository.findByRendezVousId(rdv.getId()).isPresent()) {
                throw new IllegalStateException("Une prestation a déjà été créée pour le rendez-vous ID " + rdv.getId());
            }

            if (statutInitial == StatutPrestation.EN_COURS) {
                rdv.setStatut(StatutRendezVous.EN_COURS);
                rdv.setDateModification(LocalDateTime.now());
                rendezVousRepository.save(rdv);
            }

            client = rdv.getClient();
            if (client != null) {
                nomClient = client.getNom();
                prenomClient = client.getPrenom();
                telephoneClient = client.getTelephone();
            }

            coiffeurPrestation = (request.coiffeurAffectationId() != null)
                    ? validerCoiffeur(request.coiffeurAffectationId(), salon, slugSalon)
                    : rdv.getCoiffeur();
        }
        // --- Cas Walk-in ---
        else {
            if (request.coiffeurAffectationId() != null) {
                coiffeurPrestation = validerCoiffeur(request.coiffeurAffectationId(), salon, slugSalon);
            } else if (statutInitial == StatutPrestation.EN_COURS) {
                throw new IllegalArgumentException(
                        "L'affectation du coiffeur est obligatoire pour démarrer directement une prestation au fauteuil.");
            }

            if (request.clientCompteId() != null) {
                client = compteRepository.findById(request.clientCompteId())
                        .orElseThrow(() -> new EntityNotFoundException("Compte client introuvable ID " + request.clientCompteId()));
                if (nomClient == null || nomClient.isBlank()) nomClient = client.getNom();
                if (prenomClient == null || prenomClient.isBlank()) prenomClient = client.getPrenom();
                if (telephoneClient == null || telephoneClient.isBlank()) telephoneClient = client.getTelephone();
            }
        }

        // Validation coiffeur exclusivité
        if (coiffeurPrestation != null && statutInitial == StatutPrestation.EN_COURS) {
            validerExclusiviteCoiffeur(coiffeurPrestation, null);
        }

        if (coiffeurPrestation == null && statutInitial == StatutPrestation.EN_COURS) {
            throw new IllegalArgumentException("Le coiffeur exécutant la prestation est obligatoire pour démarrer au fauteuil.");
        }

        // Validation informations client
        if (nomClient == null || nomClient.isBlank() || prenomClient == null || prenomClient.isBlank()) {
            throw new IllegalArgumentException("Le nom et le prénom du client sont obligatoires pour créer une prestation.");
        }
        if (telephoneClient == null || telephoneClient.isBlank()) {
            throw new IllegalArgumentException("Le numéro de téléphone du client est obligatoire pour créer une prestation.");
        }

        // Attribution du numéro de file d'attente pour les EN_ATTENTE
        Integer ordreFile = null;
        if (statutInitial == StatutPrestation.EN_ATTENTE) {
            ordreFile = prestationRepository.prochainOrdreFileAttente(slugSalon);
        }

        Prestation prestation = Prestation.builder()
                .salon(salon)
                .coiffeur(coiffeurPrestation)
                .client(client)
                .rendezVous(rdv)
                .nomClient(nomClient)
                .prenomClient(prenomClient)
                .telephoneClient(telephoneClient)
                .dateHeureDebut(LocalDateTime.now())
                .statut(statutInitial)
                .ordreFileAttente(ordreFile)
                .build();

        List<LignePrestation> lignes = new ArrayList<>();
        BigDecimal montantTotal = BigDecimal.ZERO;

        for (LignePrestationDTORequest lReq : request.lignes()) {
            VarianteService variante = varianteServiceRepository.findById(lReq.varianteServiceId())
                    .orElseThrow(() -> new EntityNotFoundException("Variante de service introuvable ID " + lReq.varianteServiceId()));

            if (!variante.getServiceSalon().getSalon().getId().equals(salon.getId())) {
                throw new IllegalArgumentException("La variante " + variante.getId() + " n'appartient pas au salon " + slugSalon);
            }

            LignePrestation lp = LignePrestation.builder()
                    .prestation(prestation)
                    .varianteService(variante)
                    .prixReel(lReq.prixReel())
                    .build();

            lignes.add(lp);
            montantTotal = montantTotal.add(lReq.prixReel());
        }

        prestation.setLignes(lignes);
        prestation.setMontantTotal(montantTotal);

        Prestation savedPrestation = prestationRepository.save(prestation);

        // La facture n'est créée que lorsque la prestation est TERMINEE (générée dans terminerPrestation)
        Facture facture = null;
        if (statutInitial == StatutPrestation.TERMINEE) {
            facture = facturationSalonService.creerFactureInitiale(savedPrestation);
            savedPrestation.setFacture(facture);
        }

        auditLogService.logActionSalon(TypeActionAudit.CREATION, "Prestation",
                savedPrestation.getId() != null ? savedPrestation.getId().toString() : "N/A", null,
                "Prestation créée (statut: " + statutInitial + ", montant: " + montantTotal
                        + (facture != null ? ", facture: " + facture.getNumeroFacture() : "") + ")",
                affectation, affectation.getRoles().iterator().next().getRole().name());

        log.info("Prestation ID {} créée (statut: {}) pour le salon {}",
                savedPrestation.getId(), statutInitial, slugSalon);

        return mapToResponse(savedPrestation);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  TRANSITIONS DE STATUT (Machine à états stricte)
    // ═══════════════════════════════════════════════════════════════════

    @Transactional
    public PrestationDTOResponse demarrerPrestation(String slugSalon, Long prestationId,
                                                     Long coiffeurAffectationId, String employeEmail) {
        AffectationSalon affectation = getAffectation(employeEmail, slugSalon);
        Prestation prestation = getPrestation(prestationId, slugSalon);

        // Machine à états : seule EN_ATTENTE → EN_COURS est autorisé
        validerTransition(prestation, StatutPrestation.EN_COURS);

        // Assignation du coiffeur au moment du passage au fauteuil
        if (coiffeurAffectationId != null) {
            AffectationSalon coiffeurChoisi = validerCoiffeur(coiffeurAffectationId, prestation.getSalon(), slugSalon);
            prestation.setCoiffeur(coiffeurChoisi);
        }

        if (prestation.getCoiffeur() == null) {
            throw new IllegalArgumentException("Veuillez sélectionner un coiffeur pour installer le client au fauteuil.");
        }

        // Exclusivité coiffeur
        validerExclusiviteCoiffeur(prestation.getCoiffeur(), prestation.getId());

        prestation.setStatut(StatutPrestation.EN_COURS);
        prestation.setDateHeureDebut(LocalDateTime.now());

        // Synchronisation RDV
        if (prestation.getRendezVous() != null) {
            RendezVous rdv = prestation.getRendezVous();
            rdv.setStatut(StatutRendezVous.EN_COURS);
            rdv.setDateModification(LocalDateTime.now());
            rendezVousRepository.save(rdv);
        }

        Prestation saved = prestationRepository.save(prestation);

        auditLogService.logActionSalon(TypeActionAudit.MODIFICATION, "Prestation",
                String.valueOf(saved.getId()), "EN_ATTENTE", "EN_COURS",
                affectation, affectation.getRoles().iterator().next().getRole().name());

        log.info("Prestation ID {} démarrée pour le salon {} avec coiffeur {}", saved.getId(), slugSalon, prestation.getCoiffeur().getId());
        return mapToResponse(saved);
    }

    @Transactional
    public PrestationDTOResponse demarrerPrestation(String slugSalon, Long prestationId, String employeEmail) {
        return demarrerPrestation(slugSalon, prestationId, null, employeEmail);
    }

    @Transactional
    public PrestationDTOResponse terminerPrestation(String slugSalon, Long prestationId, String employeEmail) {
        AffectationSalon affectation = getAffectation(employeEmail, slugSalon);
        Prestation prestation = getPrestation(prestationId, slugSalon);

        // Machine à états : seule EN_COURS → TERMINEE est autorisé
        validerTransition(prestation, StatutPrestation.TERMINEE);

        prestation.setStatut(StatutPrestation.TERMINEE);
        prestation.setDateHeureFin(LocalDateTime.now());

        // Synchronisation RDV
        if (prestation.getRendezVous() != null) {
            RendezVous rdv = prestation.getRendezVous();
            rdv.setStatut(StatutRendezVous.TERMINE);
            rdv.setDateModification(LocalDateTime.now());
            rendezVousRepository.save(rdv);
        }

        // Si la facture n'était pas encore générée, la générer
        if (prestation.getFacture() == null) {
            Facture facture = facturationSalonService.creerFactureInitiale(prestation);
            prestation.setFacture(facture);
        }

        Prestation saved = prestationRepository.save(prestation);

        auditLogService.logActionSalon(TypeActionAudit.MODIFICATION, "Prestation",
                String.valueOf(saved.getId()), "EN_COURS", "TERMINEE",
                affectation, affectation.getRoles().iterator().next().getRole().name());

        log.info("Prestation ID {} marquée comme TERMINEE pour le salon {}", saved.getId(), slugSalon);
        return mapToResponse(saved);
    }

    @Transactional
    public PrestationDTOResponse annulerPrestation(String slugSalon, Long prestationId, String employeEmail, String motif) {
        AffectationSalon affectation = getAffectation(employeEmail, slugSalon);
        Prestation prestation = getPrestation(prestationId, slugSalon);

        // Machine à états : EN_ATTENTE ou EN_COURS → ANNULEE
        validerTransition(prestation, StatutPrestation.ANNULEE);

        StatutPrestation ancienStatut = prestation.getStatut();
        prestation.setStatut(StatutPrestation.ANNULEE);
        prestation.setDateHeureFin(LocalDateTime.now());

        // Synchronisation RDV
        if (prestation.getRendezVous() != null) {
            RendezVous rdv = prestation.getRendezVous();
            rdv.setStatut(StatutRendezVous.ANNULE);
            rdv.setMotifAnnulation(motif != null ? motif : "Annulation de prestation par la réception");
            rdv.setDateModification(LocalDateTime.now());
            rendezVousRepository.save(rdv);
        }

        Prestation saved = prestationRepository.save(prestation);

        auditLogService.logActionSalon(TypeActionAudit.MODIFICATION, "Prestation",
                String.valueOf(saved.getId()), ancienStatut.name(), "ANNULEE",
                affectation, affectation.getRoles().iterator().next().getRole().name());

        log.info("Prestation ID {} annulée pour le salon {}", saved.getId(), slugSalon);
        return mapToResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CONSULTATION
    // ═══════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public PrestationDTOResponse obtenirPrestation(String slugSalon, Long prestationId) {
        return mapToResponse(getPrestation(prestationId, slugSalon));
    }

    @Transactional(readOnly = true)
    public List<PrestationDTOResponse> listerPrestations(String slugSalon, StatutPrestation statutOpt) {
        if (!salonRepository.existsBySlug(slugSalon)) {
            throw new EntityNotFoundException("Salon introuvable pour le slug : " + slugSalon);
        }
        List<Prestation> list = (statutOpt != null)
                ? prestationRepository.findBySalonSlugAndStatutOrderByDateHeureDebutDesc(slugSalon, statutOpt)
                : prestationRepository.findBySalonSlugOrderByDateHeureDebutDesc(slugSalon);
        return list.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PrestationDTOResponse> listerPrestationsClientSalon(String slugSalon, String clientEmail) {
        if (!salonRepository.existsBySlug(slugSalon)) {
            throw new EntityNotFoundException("Salon introuvable pour le slug : " + slugSalon);
        }
        return prestationRepository.findByClientEmailAndSalonSlugOrderByDateHeureDebutDesc(clientEmail, slugSalon)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PrestationDTOResponse> listerToutesPrestationsClient(String clientEmail) {
        return prestationRepository.findByClientEmailOrderByDateHeureDebutDesc(clientEmail)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public PrestationDTOResponse obtenirDetailPrestationClient(Long prestationId, String clientEmail) {
        Prestation p = prestationRepository.findById(prestationId)
                .orElseThrow(() -> new EntityNotFoundException("Prestation introuvable ID : " + prestationId));
        if (p.getClient() == null || !p.getClient().getEmail().equalsIgnoreCase(clientEmail)) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à consulter cette prestation.");
        }
        return mapToResponse(p);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  VALIDATIONS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Valide la transition de statut via la machine à états de Prestation.
     */
    private void validerTransition(Prestation prestation, StatutPrestation nouveauStatut) {
        if (!prestation.isTransitionAutorisee(nouveauStatut)) {
            throw new IllegalStateException(
                    "Transition interdite : " + prestation.getStatut() + " → " + nouveauStatut +
                    ". Transitions autorisées depuis " + prestation.getStatut() + " : " + getTransitionsAutorisees(prestation.getStatut()));
        }
    }

    private String getTransitionsAutorisees(StatutPrestation statut) {
        return switch (statut) {
            case EN_ATTENTE -> "EN_COURS, ANNULEE";
            case EN_COURS -> "TERMINEE, ANNULEE";
            case TERMINEE -> "(aucune - état final)";
            case ANNULEE -> "(aucune - état final)";
        };
    }

    /**
     * Valide qu'un coiffeur est actif et appartient au salon.
     */
    private AffectationSalon validerCoiffeur(Long coiffeurAffectationId, Salon salon, String slugSalon) {
        AffectationSalon coiffeur = affectationSalonRepository.findById(coiffeurAffectationId)
                .orElseThrow(() -> new EntityNotFoundException("Affectation coiffeur introuvable ID " + coiffeurAffectationId));

        if (!coiffeur.getSalon().getId().equals(salon.getId())) {
            throw new IllegalArgumentException("Le coiffeur sélectionné n'appartient pas au salon " + slugSalon);
        }

        boolean coiffeurActif = Boolean.TRUE.equals(coiffeur.getStatut()) &&
                coiffeur.getRoles().stream().anyMatch(r -> r.getRole() == TypeRoleSalon.COIFFEUR);
        if (!coiffeurActif) {
            throw new IllegalArgumentException("L'employé sélectionné n'a pas le rôle COIFFEUR actif sur ce salon.");
        }

        return coiffeur;
    }

    /**
     * Un coiffeur ne doit pas coiffer deux personnes en même temps au fauteuil.
     */
    private void validerExclusiviteCoiffeur(AffectationSalon coiffeur, Long prestationIdExclure) {
        boolean occupe = (prestationIdExclure != null)
                ? prestationRepository.existsByCoiffeurIdAndStatutAndIdNot(coiffeur.getId(), StatutPrestation.EN_COURS, prestationIdExclure)
                : prestationRepository.existsByCoiffeurIdAndStatut(coiffeur.getId(), StatutPrestation.EN_COURS);

        if (occupe) {
            String nomCoiff = (coiffeur.getCompte() != null)
                    ? coiffeur.getCompte().getPrenom() + " " + coiffeur.getCompte().getNom()
                    : "sélectionné";
            throw new IllegalStateException("Le coiffeur " + nomCoiff +
                    " est actuellement occupé au fauteuil avec un autre client. Veuillez choisir un coiffeur libre ou placer le client en salle d'attente.");
        }
    }

    private AffectationSalon getAffectation(String email, String slugSalon) {
        return affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Aucune affectation active pour " + email + " sur le salon " + slugSalon));
    }

    private Prestation getPrestation(Long prestationId, String slugSalon) {
        return prestationRepository.findByIdAndSalonSlug(prestationId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Prestation introuvable ID " + prestationId + " dans le salon " + slugSalon));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  MAPPER
    // ═══════════════════════════════════════════════════════════════════

    public PrestationDTOResponse mapToResponse(Prestation p) {
        List<LignePrestationDTOResponse> lignesDTO = p.getLignes().stream()
                .map(l -> new LignePrestationDTOResponse(
                        l.getId(),
                        l.getVarianteService().getId(),
                        l.getVarianteService().getNom(),
                        l.getPrixReel()))
                .toList();

        var factureDTO = (p.getFacture() != null)
                ? facturationSalonService.mapFactureToResponse(p.getFacture())
                : null;

        Long coiffeurAffectationId = p.getCoiffeur() != null ? p.getCoiffeur().getId() : null;
        String coiffeurNom = (p.getCoiffeur() != null && p.getCoiffeur().getCompte() != null)
                ? p.getCoiffeur().getCompte().getNom() : null;
        String coiffeurPrenom = (p.getCoiffeur() != null && p.getCoiffeur().getCompte() != null)
                ? p.getCoiffeur().getCompte().getPrenom() : null;

        Long clientCompteId = (p.getClient() != null) ? p.getClient().getId() : null;
        Long rendezVousId = (p.getRendezVous() != null) ? p.getRendezVous().getId() : null;

        return new PrestationDTOResponse(
                p.getId(),
                p.getSalon().getSlug(),
                coiffeurAffectationId,
                coiffeurNom,
                coiffeurPrenom,
                clientCompteId,
                p.getNomClient(),
                p.getPrenomClient(),
                p.getTelephoneClient(),
                rendezVousId,
                p.getDateHeureDebut(),
                p.getDateHeureFin(),
                p.getMontantTotal(),
                p.getStatut().name(),
                factureDTO,
                lignesDTO);
    }
}

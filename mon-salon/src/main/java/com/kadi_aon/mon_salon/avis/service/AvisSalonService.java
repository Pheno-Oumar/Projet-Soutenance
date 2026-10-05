package com.kadi_aon.mon_salon.avis.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.avis.dto.AvisModerationDTORequest;
import com.kadi_aon.mon_salon.avis.dto.AvisPrestationCreateDTORequest;
import com.kadi_aon.mon_salon.avis.dto.AvisPrestationDTOResponse;
import com.kadi_aon.mon_salon.avis.dto.AvisPrestationUpdateDTORequest;
import com.kadi_aon.mon_salon.avis.dto.AvisSalonCreateDTORequest;
import com.kadi_aon.mon_salon.avis.dto.AvisSalonDTOResponse;
import com.kadi_aon.mon_salon.avis.dto.AvisSalonUpdateDTORequest;
import com.kadi_aon.mon_salon.avis.entity.AvisPrestation;
import com.kadi_aon.mon_salon.avis.entity.AvisSalon;
import com.kadi_aon.mon_salon.avis.repository.AvisPrestationRepository;
import com.kadi_aon.mon_salon.avis.repository.AvisSalonRepository;
import com.kadi_aon.mon_salon.prestation.entity.LignePrestation;
import com.kadi_aon.mon_salon.prestation.entity.Prestation;
import com.kadi_aon.mon_salon.prestation.enums.StatutPrestation;
import com.kadi_aon.mon_salon.prestation.repository.LignePrestationRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvisSalonService {

    private final AvisPrestationRepository avisPrestationRepository;
    private final AvisSalonRepository avisSalonRepository;
    private final LignePrestationRepository lignePrestationRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final SalonRepository salonRepository;
    private final AuditLogService auditLogService;

    // ==========================================
    // 1. AVIS SUR LIGNE DE PRESTATION (CLIENT)
    // ==========================================

    @Transactional
    public AvisPrestationDTOResponse creerAvisPrestation(String slugSalon, String clientEmail, AvisPrestationCreateDTORequest request) {
        AffectationSalon affectationClient = validerRole(slugSalon, clientEmail, TypeRoleSalon.CLIENT);
        Compte client = affectationClient.getCompte();

        LignePrestation ligne = lignePrestationRepository.findById(request.lignePrestationId())
                .orElseThrow(() -> new EntityNotFoundException("Ligne de prestation introuvable ID " + request.lignePrestationId()));

        Prestation prestation = ligne.getPrestation();
        if (!prestation.getSalon().getSlug().equalsIgnoreCase(slugSalon)) {
            throw new IllegalArgumentException("Cette ligne de prestation n'appartient pas au salon " + slugSalon);
        }

        if (prestation.getStatut() != StatutPrestation.TERMINEE) {
            throw new IllegalStateException("Impossible de déposer un avis sur une prestation qui n'est pas encore terminée.");
        }

        if (prestation.getClient() != null && !prestation.getClient().getId().equals(client.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas le client associé à cette prestation.");
        }

        if (avisPrestationRepository.existsByClientIdAndLignePrestationId(client.getId(), ligne.getId())) {
            throw new IllegalStateException("Vous avez déjà déposé un avis pour cette prestation. Vous pouvez le modifier si vous le souhaitez.");
        }

        AvisPrestation avis = AvisPrestation.builder()
                .client(client)
                .lignePrestation(ligne)
                .note(request.note())
                .commentaire(request.commentaire())
                .statut(false) // Par défaut false en attente de modération manager
                .dateCreation(LocalDateTime.now())
                .build();

        AvisPrestation saved = avisPrestationRepository.save(avis);

        auditLogService.logActionSalon(
                TypeActionAudit.CREATION,
                "AvisPrestation",
                String.valueOf(saved.getId()),
                null,
                "Note: " + saved.getNote() + " (en attente modération)",
                affectationClient,
                TypeRoleSalon.CLIENT.name()
        );

        log.info("Avis sur prestation ID {} créé par le client {}", saved.getId(), clientEmail);
        return mapPrestationToResponse(saved);
    }

    @Transactional
    public AvisPrestationDTOResponse modifierAvisPrestation(String slugSalon, Long avisId, String clientEmail, AvisPrestationUpdateDTORequest request) {
        AffectationSalon affectationClient = validerRole(slugSalon, clientEmail, TypeRoleSalon.CLIENT);

        AvisPrestation avis = avisPrestationRepository.findByIdAndClientEmailAndLignePrestationPrestationSalonSlug(avisId, clientEmail, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Avis introuvable ID " + avisId + " pour ce client dans ce salon."));

        String ancienneValeur = "Note=" + avis.getNote() + ", Commentaire=" + avis.getCommentaire();

        avis.setNote(request.note());
        avis.setCommentaire(request.commentaire());
        avis.setStatut(false); // 🔥 Après modification du client, le statut redevient false jusqu'à ce que le manager le re-valide
        avis.setDateModification(LocalDateTime.now());

        AvisPrestation saved = avisPrestationRepository.save(avis);

        String nouvelleValeur = "Note=" + saved.getNote() + ", Commentaire=" + saved.getCommentaire();
        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "AvisPrestation",
                String.valueOf(saved.getId()),
                ancienneValeur,
                nouvelleValeur,
                affectationClient,
                TypeRoleSalon.CLIENT.name()
        );

        log.info("Avis prestation ID {} modifié par le client {} (statut réinitialisé à false)", saved.getId(), clientEmail);
        return mapPrestationToResponse(saved);
    }

    @Transactional
    public void supprimerAvisPrestation(String slugSalon, Long avisId, String clientEmail) {
        AffectationSalon affectationClient = validerRole(slugSalon, clientEmail, TypeRoleSalon.CLIENT);

        AvisPrestation avis = avisPrestationRepository.findByIdAndClientEmailAndLignePrestationPrestationSalonSlug(avisId, clientEmail, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Avis introuvable ID " + avisId + " pour ce client dans ce salon."));

        avisPrestationRepository.delete(avis);

        auditLogService.logActionSalon(
                TypeActionAudit.SUPPRESSION,
                "AvisPrestation",
                String.valueOf(avisId),
                "Note=" + avis.getNote(),
                "SUPPRIME",
                affectationClient,
                TypeRoleSalon.CLIENT.name()
        );

        log.info("Avis prestation ID {} supprimé par le client {}", avisId, clientEmail);
    }

    @Transactional(readOnly = true)
    public List<AvisPrestationDTOResponse> listerMesAvisPrestations(String slugSalon, String clientEmail) {
        validerRole(slugSalon, clientEmail, TypeRoleSalon.CLIENT);
        return avisPrestationRepository.findByClientEmailAndLignePrestationPrestationSalonSlugOrderByDateCreationDesc(clientEmail, slugSalon).stream()
                .map(this::mapPrestationToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvisPrestationDTOResponse> listerAvisValidesLignePrestation(String slugSalon, Long lignePrestationId) {
        return avisPrestationRepository.findByLignePrestationIdAndStatutTrueOrderByDateCreationDesc(lignePrestationId).stream()
                .map(this::mapPrestationToResponse)
                .toList();
    }

    // ==========================================
    // 2. MODERATION AVIS PRESTATION (MANAGER)
    // ==========================================

    @Transactional(readOnly = true)
    public List<AvisPrestationDTOResponse> listerAvisPrestationsPourManager(String slugSalon, String managerEmail, Boolean statutOpt) {
        validerRole(slugSalon, managerEmail, TypeRoleSalon.MANAGER);
        List<AvisPrestation> list;
        if (statutOpt != null) {
            list = avisPrestationRepository.findByLignePrestationPrestationSalonSlugAndStatutOrderByDateCreationDesc(slugSalon, statutOpt);
        } else {
            list = avisPrestationRepository.findByLignePrestationPrestationSalonSlugOrderByDateCreationDesc(slugSalon);
        }
        return list.stream().map(this::mapPrestationToResponse).toList();
    }

    @Transactional
    public AvisPrestationDTOResponse modererAvisPrestation(String slugSalon, Long avisId, String managerEmail, AvisModerationDTORequest request) {
        AffectationSalon affectationManager = validerRole(slugSalon, managerEmail, TypeRoleSalon.MANAGER);

        AvisPrestation avis = avisPrestationRepository.findByIdAndLignePrestationPrestationSalonSlug(avisId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Avis de prestation introuvable ID " + avisId + " dans le salon " + slugSalon));

        boolean ancienStatut = avis.isStatut();
        avis.setStatut(request.statut());
        avis.setDateModification(LocalDateTime.now());

        AvisPrestation saved = avisPrestationRepository.save(avis);

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "AvisPrestation (Modération)",
                String.valueOf(saved.getId()),
                "Statut: " + ancienStatut,
                "Statut: " + saved.isStatut(),
                affectationManager,
                TypeRoleSalon.MANAGER.name()
        );

        log.info("Avis prestation ID {} modéré par le manager {} -> statut: {}", saved.getId(), managerEmail, saved.isStatut());
        return mapPrestationToResponse(saved);
    }

    // ==========================================
    // 3. AVIS SUR LE SALON (CLIENT)
    // ==========================================

    @Transactional
    public AvisSalonDTOResponse creerAvisSalon(String slugSalon, String clientEmail, AvisSalonCreateDTORequest request) {
        AffectationSalon affectationClient = validerRole(slugSalon, clientEmail, TypeRoleSalon.CLIENT);
        Salon salon = affectationClient.getSalon();
        Compte client = affectationClient.getCompte();

        if (avisSalonRepository.existsByClientIdAndSalonId(client.getId(), salon.getId())) {
            throw new IllegalStateException("Vous avez déjà déposé un avis pour ce salon. Vous pouvez le modifier si vous le souhaitez.");
        }

        AvisSalon avis = AvisSalon.builder()
                .client(client)
                .salon(salon)
                .note(request.note())
                .commentaire(request.commentaire())
                .statut(false) // En attente de modération manager
                .dateCreation(LocalDateTime.now())
                .build();

        AvisSalon saved = avisSalonRepository.save(avis);

        auditLogService.logActionSalon(
                TypeActionAudit.CREATION,
                "AvisSalon",
                String.valueOf(saved.getId()),
                null,
                "Note: " + saved.getNote() + " (en attente modération)",
                affectationClient,
                TypeRoleSalon.CLIENT.name()
        );

        log.info("Avis sur salon ID {} créé par le client {}", saved.getId(), clientEmail);
        return mapSalonToResponse(saved);
    }

    @Transactional
    public AvisSalonDTOResponse modifierAvisSalon(String slugSalon, String clientEmail, AvisSalonUpdateDTORequest request) {
        AffectationSalon affectationClient = validerRole(slugSalon, clientEmail, TypeRoleSalon.CLIENT);

        AvisSalon avis = avisSalonRepository.findByClientEmailAndSalonSlug(clientEmail, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Aucun avis déposé par vous trouvé pour le salon " + slugSalon));

        String ancienneValeur = "Note=" + avis.getNote() + ", Commentaire=" + avis.getCommentaire();

        avis.setNote(request.note());
        avis.setCommentaire(request.commentaire());
        avis.setStatut(false); // 🔥 Après modification du client, le statut redevient false
        avis.setDateModification(LocalDateTime.now());

        AvisSalon saved = avisSalonRepository.save(avis);

        String nouvelleValeur = "Note=" + saved.getNote() + ", Commentaire=" + saved.getCommentaire();
        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "AvisSalon",
                String.valueOf(saved.getId()),
                ancienneValeur,
                nouvelleValeur,
                affectationClient,
                TypeRoleSalon.CLIENT.name()
        );

        log.info("Avis salon ID {} modifié par le client {} (statut réinitialisé à false)", saved.getId(), clientEmail);
        return mapSalonToResponse(saved);
    }

    @Transactional
    public void supprimerAvisSalon(String slugSalon, String clientEmail) {
        AffectationSalon affectationClient = validerRole(slugSalon, clientEmail, TypeRoleSalon.CLIENT);

        AvisSalon avis = avisSalonRepository.findByClientEmailAndSalonSlug(clientEmail, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Aucun avis déposé par vous trouvé pour le salon " + slugSalon));

        avisSalonRepository.delete(avis);

        auditLogService.logActionSalon(
                TypeActionAudit.SUPPRESSION,
                "AvisSalon",
                String.valueOf(avis.getId()),
                "Note=" + avis.getNote(),
                "SUPPRIME",
                affectationClient,
                TypeRoleSalon.CLIENT.name()
        );

        log.info("Avis salon ID {} supprimé par le client {}", avis.getId(), clientEmail);
    }

    @Transactional(readOnly = true)
    public List<AvisSalonDTOResponse> listerAvisPubliesSalon(String slugSalon) {
        if (!salonRepository.existsBySlug(slugSalon)) {
            throw new EntityNotFoundException("Salon introuvable pour le slug : " + slugSalon);
        }
        return avisSalonRepository.findBySalonSlugAndStatutTrueOrderByDateCreationDesc(slugSalon).stream()
                .map(this::mapSalonToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AvisSalonDTOResponse obtenirMonAvisSalon(String slugSalon, String clientEmail) {
        validerRole(slugSalon, clientEmail, TypeRoleSalon.CLIENT);
        return avisSalonRepository.findByClientEmailAndSalonSlug(clientEmail, slugSalon)
                .map(this::mapSalonToResponse)
                .orElse(null);
    }

    // ==========================================
    // 4. MODERATION AVIS SALON (MANAGER)
    // ==========================================

    @Transactional(readOnly = true)
    public List<AvisSalonDTOResponse> listerAvisSalonPourManager(String slugSalon, String managerEmail, Boolean statutOpt) {
        validerRole(slugSalon, managerEmail, TypeRoleSalon.MANAGER);
        List<AvisSalon> list;
        if (statutOpt != null) {
            list = avisSalonRepository.findBySalonSlugAndStatutOrderByDateCreationDesc(slugSalon, statutOpt);
        } else {
            list = avisSalonRepository.findBySalonSlugOrderByDateCreationDesc(slugSalon);
        }
        return list.stream().map(this::mapSalonToResponse).toList();
    }

    @Transactional
    public AvisSalonDTOResponse modererAvisSalon(String slugSalon, Long avisId, String managerEmail, AvisModerationDTORequest request) {
        AffectationSalon affectationManager = validerRole(slugSalon, managerEmail, TypeRoleSalon.MANAGER);

        AvisSalon avis = avisSalonRepository.findByIdAndSalonSlug(avisId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Avis de salon introuvable ID " + avisId + " dans le salon " + slugSalon));

        boolean ancienStatut = avis.isStatut();
        avis.setStatut(request.statut());
        avis.setDateModification(LocalDateTime.now());

        AvisSalon saved = avisSalonRepository.save(avis);

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "AvisSalon (Modération)",
                String.valueOf(saved.getId()),
                "Statut: " + ancienStatut,
                "Statut: " + saved.isStatut(),
                affectationManager,
                TypeRoleSalon.MANAGER.name()
        );

        log.info("Avis salon ID {} modéré par le manager {} -> statut: {}", saved.getId(), managerEmail, saved.isStatut());
        return mapSalonToResponse(saved);
    }

    // ==========================================
    // 4. AVIS COIFFEUR (COIFFEUR)
    // ==========================================

    @Transactional(readOnly = true)
    public List<AvisPrestationDTOResponse> listerAvisPourCoiffeur(String slugSalon, String coiffeurEmail, Boolean statut) {
        validerRole(slugSalon, coiffeurEmail, TypeRoleSalon.COIFFEUR);

        List<AvisPrestation> list;
        if (statut != null) {
            list = avisPrestationRepository
                    .findByCoiffeurEmailAndSalonSlugAndStatut(
                            coiffeurEmail, slugSalon, statut);
        } else {
            list = avisPrestationRepository
                    .findByCoiffeurEmailAndSalonSlug(
                            coiffeurEmail, slugSalon);
        }

        return list.stream()
                .map(this::mapPrestationToResponse)
                .toList();
    }

    // ==========================================
    // HELPERS & MAPPERS
    // ==========================================


    private AffectationSalon validerRole(String slugSalon, String email, TypeRoleSalon roleAttendu) {
        AffectationSalon affectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Aucune affectation active trouvée pour " + email + " dans le salon " + slugSalon));

        boolean aRole = affectation.getRoles().stream()
                .anyMatch(r -> r.getRole() == roleAttendu);

        if (!aRole) {
            throw new IllegalArgumentException("L'utilisateur " + email + " n'a pas le rôle " + roleAttendu.name() + " dans ce salon.");
        }

        return affectation;
    }

    private AvisPrestationDTOResponse mapPrestationToResponse(AvisPrestation a) {
        LignePrestation lp = a.getLignePrestation();
        String serviceNom = lp.getVarianteService() != null && lp.getVarianteService().getServiceSalon() != null
                ? lp.getVarianteService().getServiceSalon().getNom() : "Service inconnu";
        String varianteNom = lp.getVarianteService() != null ? lp.getVarianteService().getNom() : "Variante inconnue";
        AffectationSalon coiffeurAff = (lp.getPrestation() != null) ? lp.getPrestation().getCoiffeur() : null;
        Long coiffeurId = (coiffeurAff != null && coiffeurAff.getCompte() != null)
                ? coiffeurAff.getCompte().getId()
                : (coiffeurAff != null ? coiffeurAff.getId() : null);
        String coiffeurNomComplet = (coiffeurAff != null && coiffeurAff.getCompte() != null)
                ? coiffeurAff.getCompte().getPrenom() + " " + coiffeurAff.getCompte().getNom() : "Coiffeur inconnu";

        Long clientId = a.getClient() != null ? a.getClient().getId() : null;
        String clientNomComplet = a.getClient() != null
                ? a.getClient().getPrenom() + " " + a.getClient().getNom() : "Client anonyme";

        return new AvisPrestationDTOResponse(
                a.getId(),
                lp.getId(),
                lp.getPrestation() != null ? lp.getPrestation().getId() : null,
                serviceNom,
                varianteNom,
                coiffeurId,
                coiffeurNomComplet,
                clientId,
                clientNomComplet,
                a.getNote(),
                a.getCommentaire(),
                a.isStatut(),
                a.getDateCreation(),
                a.getDateModification()
        );
    }

    private AvisSalonDTOResponse mapSalonToResponse(AvisSalon a) {
        String salonSlug = a.getSalon() != null ? a.getSalon().getSlug() : null;
        String salonNom = a.getSalon() != null ? a.getSalon().getNom() : null;
        Long clientId = a.getClient() != null ? a.getClient().getId() : null;
        String clientNomComplet = a.getClient() != null
                ? a.getClient().getPrenom() + " " + a.getClient().getNom() : "Client anonyme";

        return new AvisSalonDTOResponse(
                a.getId(),
                salonSlug,
                salonNom,
                clientId,
                clientNomComplet,
                a.getNote(),
                a.getCommentaire(),
                a.isStatut(),
                a.getDateCreation(),
                a.getDateModification()
        );
    }

    @Transactional(readOnly = true)
    public List<AvisSalonDTOResponse> listerTousMesAvisSalons(String clientEmail) {
        return avisSalonRepository.findByClientEmailOrderByDateCreationDesc(clientEmail).stream()
                .map(this::mapSalonToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvisPrestationDTOResponse> listerTousMesAvisPrestations(String clientEmail) {
        return avisPrestationRepository.findByClientEmailOrderByDateCreationDesc(clientEmail).stream()
                .map(this::mapPrestationToResponse)
                .toList();
    }
}


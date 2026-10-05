package com.kadi_aon.mon_salon.favori.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.coiffeur.repository.ProfilCoiffeurRepository;
import com.kadi_aon.mon_salon.favori.dto.FavoriCoiffeurDTOResponse;
import com.kadi_aon.mon_salon.favori.dto.FavoriSalonDTOResponse;
import com.kadi_aon.mon_salon.favori.entity.FavoriCoiffeur;
import com.kadi_aon.mon_salon.favori.entity.FavoriSalon;
import com.kadi_aon.mon_salon.favori.repository.FavoriCoiffeurRepository;
import com.kadi_aon.mon_salon.favori.repository.FavoriSalonRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriSalonService {

    private final FavoriSalonRepository favoriSalonRepository;
    private final FavoriCoiffeurRepository favoriCoiffeurRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final CompteRepository compteRepository;
    private final ProfilCoiffeurRepository profilCoiffeurRepository;
    private final AuditLogService auditLogService;

    // ==========================================
    // 1. SALON FAVORI
    // ==========================================

    @Transactional
    public FavoriSalonDTOResponse ajouterSalonFavori(String slugSalon, String clientEmail) {
        AffectationSalon affectationClient = validerClient(slugSalon, clientEmail);
        Compte client = affectationClient.getCompte();
        Salon salon = affectationClient.getSalon();

        FavoriSalon favori = favoriSalonRepository.findByClientIdAndSalonId(client.getId(), salon.getId())
                .orElseGet(() -> {
                    FavoriSalon f = FavoriSalon.builder()
                            .client(client)
                            .salon(salon)
                            .dateAjout(LocalDateTime.now())
                            .build();
                    FavoriSalon saved = favoriSalonRepository.save(f);

                    auditLogService.logActionSalon(
                            TypeActionAudit.CREATION,
                            "FavoriSalon",
                            String.valueOf(saved.getId()),
                            null,
                            "Ajout salon aux favoris",
                            affectationClient,
                            TypeRoleSalon.CLIENT.name()
                    );
                    log.info("Salon {} ajouté aux favoris du client {}", slugSalon, clientEmail);
                    return saved;
                });

        return mapSalonToResponse(favori);
    }

    @Transactional
    public void retirerSalonFavori(String slugSalon, String clientEmail) {
        AffectationSalon affectationClient = validerClient(slugSalon, clientEmail);
        Compte client = affectationClient.getCompte();
        Salon salon = affectationClient.getSalon();

        if (favoriSalonRepository.existsByClientIdAndSalonId(client.getId(), salon.getId())) {
            favoriSalonRepository.deleteByClientIdAndSalonId(client.getId(), salon.getId());

            auditLogService.logActionSalon(
                    TypeActionAudit.SUPPRESSION,
                    "FavoriSalon",
                    String.valueOf(salon.getId()),
                    "FAVORI",
                    "SUPPRIME",
                    affectationClient,
                    TypeRoleSalon.CLIENT.name()
            );
            log.info("Salon {} retiré des favoris du client {}", slugSalon, clientEmail);
        }
    }

    @Transactional(readOnly = true)
    public boolean isSalonFavori(String slugSalon, String clientEmail) {
        return favoriSalonRepository.findByClientEmailAndSalonSlug(clientEmail, slugSalon).isPresent();
    }

    @Transactional(readOnly = true)
    public List<FavoriSalonDTOResponse> listerMesSalonsFavoris(String clientEmail) {
        return favoriSalonRepository.findByClientEmailOrderByDateAjoutDesc(clientEmail).stream()
                .map(this::mapSalonToResponse)
                .toList();
    }

    // ==========================================
    // 2. COIFFEUR FAVORI
    // ==========================================

    @Transactional
    public FavoriCoiffeurDTOResponse ajouterCoiffeurFavori(String slugSalon, Long coiffeurId, String clientEmail) {
        AffectationSalon affectationClient = validerClient(slugSalon, clientEmail);
        Compte client = affectationClient.getCompte();
        Salon salon = affectationClient.getSalon();

        Compte coiffeur = compteRepository.findById(coiffeurId)
                .orElseThrow(() -> new EntityNotFoundException("Compte coiffeur introuvable ID " + coiffeurId));

        // Vérifier que le coiffeur a une affectation active avec le rôle COIFFEUR sur ce salon
        boolean isCoiffeurValide = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(coiffeur.getEmail(), slugSalon)
                .map(aff -> aff.getRoles().stream().anyMatch(r -> r.getRole() == TypeRoleSalon.COIFFEUR))
                .orElse(false);

        if (!isCoiffeurValide) {
            throw new IllegalArgumentException("L'employé ID " + coiffeurId + " n'a pas le rôle COIFFEUR actif dans ce salon.");
        }

        FavoriCoiffeur favori = favoriCoiffeurRepository.findByClientIdAndCoiffeurIdAndSalonId(client.getId(), coiffeur.getId(), salon.getId())
                .orElseGet(() -> {
                    FavoriCoiffeur f = FavoriCoiffeur.builder()
                            .client(client)
                            .coiffeur(coiffeur)
                            .salon(salon)
                            .dateAjout(LocalDateTime.now())
                            .build();
                    FavoriCoiffeur saved = favoriCoiffeurRepository.save(f);

                    auditLogService.logActionSalon(
                            TypeActionAudit.CREATION,
                            "FavoriCoiffeur",
                            String.valueOf(saved.getId()),
                            null,
                            "Ajout coiffeur ID " + coiffeur.getId() + " aux favoris",
                            affectationClient,
                            TypeRoleSalon.CLIENT.name()
                    );
                    log.info("Coiffeur {} ajouté aux favoris du client {} dans le salon {}", coiffeurId, clientEmail, slugSalon);
                    return saved;
                });

        return mapCoiffeurToResponse(favori);
    }

    @Transactional
    public void retirerCoiffeurFavori(String slugSalon, Long coiffeurId, String clientEmail) {
        AffectationSalon affectationClient = validerClient(slugSalon, clientEmail);
        Compte client = affectationClient.getCompte();
        Salon salon = affectationClient.getSalon();

        if (favoriCoiffeurRepository.existsByClientIdAndCoiffeurIdAndSalonId(client.getId(), coiffeurId, salon.getId())) {
            favoriCoiffeurRepository.deleteByClientIdAndCoiffeurIdAndSalonId(client.getId(), coiffeurId, salon.getId());

            auditLogService.logActionSalon(
                    TypeActionAudit.SUPPRESSION,
                    "FavoriCoiffeur",
                    String.valueOf(coiffeurId),
                    "FAVORI",
                    "SUPPRIME",
                    affectationClient,
                    TypeRoleSalon.CLIENT.name()
            );
            log.info("Coiffeur {} retiré des favoris du client {} dans le salon {}", coiffeurId, clientEmail, slugSalon);
        }
    }

    @Transactional(readOnly = true)
    public List<FavoriCoiffeurDTOResponse> listerMesCoiffeursFavoris(String slugSalon, String clientEmail) {
        validerClient(slugSalon, clientEmail);
        return favoriCoiffeurRepository.findByClientEmailAndSalonSlugOrderByDateAjoutDesc(clientEmail, slugSalon).stream()
                .map(this::mapCoiffeurToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FavoriCoiffeurDTOResponse> listerTousMesCoiffeursFavoris(String clientEmail) {
        return favoriCoiffeurRepository.findByClientEmailOrderByDateAjoutDesc(clientEmail).stream()
                .map(this::mapCoiffeurToResponse)
                .toList();
    }

    // ==========================================
    // HELPERS & MAPPERS
    // ==========================================

    private AffectationSalon validerClient(String slugSalon, String clientEmail) {
        AffectationSalon affectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(clientEmail, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Aucune affectation active trouvée pour " + clientEmail + " dans le salon " + slugSalon));

        boolean isClient = affectation.getRoles().stream()
                .anyMatch(r -> r.getRole() == TypeRoleSalon.CLIENT);

        if (!isClient) {
            throw new IllegalArgumentException("L'utilisateur " + clientEmail + " n'a pas le rôle CLIENT dans ce salon.");
        }

        return affectation;
    }

    private FavoriSalonDTOResponse mapSalonToResponse(FavoriSalon f) {
        Salon s = f.getSalon();
        return new FavoriSalonDTOResponse(
                f.getId(),
                s != null ? s.getSlug() : null,
                s != null ? s.getNom() : null,
                s != null ? s.getLogoUrl() : null,
                f.getClient() != null ? f.getClient().getId() : null,
                f.getDateAjout()
        );
    }

    private FavoriCoiffeurDTOResponse mapCoiffeurToResponse(FavoriCoiffeur f) {
        Compte c = f.getCoiffeur();
        Salon s = f.getSalon();
        String coiffeurNomComplet = c != null ? c.getPrenom() + " " + c.getNom() : "Coiffeur inconnu";

        String photoUrl = null;
        if (c != null && s != null) {
            photoUrl = profilCoiffeurRepository.findByAffectationCompteEmailAndAffectationSalonSlug(c.getEmail(), s.getSlug())
                    .map(p -> p.getPhotoProfilUrl())
                    .orElse(null);
        }

        return new FavoriCoiffeurDTOResponse(
                f.getId(),
                c != null ? c.getId() : null,
                coiffeurNomComplet,
                photoUrl,
                s != null ? s.getSlug() : null,
                s != null ? s.getNom() : null,
                f.getClient() != null ? f.getClient().getId() : null,
                f.getDateAjout()
        );
    }
}

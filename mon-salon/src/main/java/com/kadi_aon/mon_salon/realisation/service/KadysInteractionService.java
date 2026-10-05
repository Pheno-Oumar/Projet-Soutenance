package com.kadi_aon.mon_salon.realisation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.realisation.dto.CommentaireCreateDTORequest;
import com.kadi_aon.mon_salon.realisation.dto.CommentaireDTOResponse;
import com.kadi_aon.mon_salon.realisation.dto.KadysRealisationDTOResponse;
import com.kadi_aon.mon_salon.realisation.dto.LikeToggleDTOResponse;
import com.kadi_aon.mon_salon.realisation.entity.CommentaireRealisation;
import com.kadi_aon.mon_salon.realisation.entity.LikeRealisation;
import com.kadi_aon.mon_salon.realisation.entity.Realisation;
import com.kadi_aon.mon_salon.realisation.enums.StatutCommentaire;
import com.kadi_aon.mon_salon.realisation.repository.CommentaireRealisationRepository;
import com.kadi_aon.mon_salon.realisation.repository.LikeRealisationRepository;
import com.kadi_aon.mon_salon.realisation.repository.RealisationRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.RoleSalonRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KadysInteractionService {

    private final RealisationRepository realisationRepository;
    private final LikeRealisationRepository likeRealisationRepository;
    private final CommentaireRealisationRepository commentaireRealisationRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final CompteRepository compteRepository;
    private final RoleSalonRepository roleSalonRepository;
    private final AuditLogService auditLogService;

    // ==========================================
    // 1. RATTACHEMENT CLIENT & AFFECTATION
    // ==========================================

    @Transactional
    public AffectationSalon validerOuCreerAffectationClient(Salon salon, String clientEmail) {
        Compte compte = compteRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new EntityNotFoundException("Compte client introuvable avec l'email : " + clientEmail));

        AffectationSalon affectation = affectationSalonRepository.findByCompteAndSalon(compte, salon).orElse(null);

        if (affectation != null) {
            if (Boolean.FALSE.equals(affectation.getStatut())) {
                log.warn("Tentative d'interaction par le client {} désactivé dans le salon {}", clientEmail, salon.getSlug());
                throw new IllegalStateException("Votre compte client a été désactivé par ce salon. Pour réactiver votre accès, veuillez vous rendre directement au salon.");
            }
            return affectation;
        }

        if (!Boolean.TRUE.equals(salon.getStatut())) {
            throw new IllegalArgumentException("Impossible d'interagir : ce salon est actuellement inactif.");
        }

        RoleSalon roleClient = roleSalonRepository.findByRole(TypeRoleSalon.CLIENT)
                .orElseThrow(() -> new IllegalStateException("Rôle CLIENT non initialisé"));

        Set<RoleSalon> roles = new HashSet<>();
        roles.add(roleClient);

        AffectationSalon nouvelleAffectation = AffectationSalon.builder()
                .compte(compte)
                .salon(salon)
                .roles(roles)
                .statut(true)
                .dateDebut(LocalDate.now())
                .build();

        AffectationSalon saved = affectationSalonRepository.save(nouvelleAffectation);

        auditLogService.logActionSalon(
                TypeActionAudit.CREATION,
                "AffectationSalon (Client Kady's)",
                String.valueOf(saved.getId()),
                null,
                String.format("Rattachement automatique client '%s' dans le salon '%s' via interaction Kady's", clientEmail, salon.getSlug()),
                saved,
                "CLIENT"
        );

        log.info("Client {} automatiquement rattaché au salon {} suite à une interaction Kady's", clientEmail, salon.getSlug());
        return saved;
    }

    // ==========================================
    // 2. LIKES & UNLIKES (TOGGLE)
    // ==========================================

    @Transactional
    public LikeToggleDTOResponse toggleLike(Long realisationId, String clientEmail) {
        Realisation realisation = realisationRepository.findByIdAndStatutPublicationTrue(realisationId)
                .orElseThrow(() -> new EntityNotFoundException("Réalisation introuvable ou non publiée ID " + realisationId));

        AffectationSalon affectation = validerOuCreerAffectationClient(realisation.getSalon(), clientEmail);

        LikeRealisation existingLike = likeRealisationRepository
                .findByRealisationIdAndAffectationSalonId(realisationId, affectation.getId())
                .orElse(null);

        if (existingLike != null) {
            likeRealisationRepository.delete(existingLike);
            int nouveauTotal = Math.max(0, realisation.getTotalLikes() - 1);
            realisation.setTotalLikes(nouveauTotal);
            realisationRepository.save(realisation);

            log.info("Client {} a retiré son like sur la réalisation ID {}", clientEmail, realisationId);
            return new LikeToggleDTOResponse(false, nouveauTotal);
        } else {
            LikeRealisation like = LikeRealisation.builder()
                    .realisation(realisation)
                    .affectationSalon(affectation)
                    .dateCreation(LocalDateTime.now())
                    .build();
            likeRealisationRepository.save(like);

            int nouveauTotal = realisation.getTotalLikes() + 1;
            realisation.setTotalLikes(nouveauTotal);
            realisationRepository.save(realisation);

            log.info("Client {} a liké la réalisation ID {}", clientEmail, realisationId);
            return new LikeToggleDTOResponse(true, nouveauTotal);
        }
    }

    // ==========================================
    // 3. COMMENTAIRES
    // ==========================================

    @Transactional
    public CommentaireDTOResponse ajouterCommentaire(Long realisationId, String clientEmail, CommentaireCreateDTORequest request) {
        Realisation realisation = realisationRepository.findByIdAndStatutPublicationTrue(realisationId)
                .orElseThrow(() -> new EntityNotFoundException("Réalisation introuvable ou non publiée ID " + realisationId));

        AffectationSalon affectation = validerOuCreerAffectationClient(realisation.getSalon(), clientEmail);

        CommentaireRealisation commentaire = CommentaireRealisation.builder()
                .realisation(realisation)
                .affectationSalon(affectation)
                .contenu(request.contenu().trim())
                .statut(StatutCommentaire.ACTIF)
                .dateCreation(LocalDateTime.now())
                .build();

        CommentaireRealisation saved = commentaireRealisationRepository.save(commentaire);

        realisation.setTotalCommentaires(realisation.getTotalCommentaires() + 1);
        realisationRepository.save(realisation);

        log.info("Commentaire ID {} ajouté par client {} sur la réalisation ID {}", saved.getId(), clientEmail, realisationId);
        return mapToCommentaireResponse(saved, clientEmail);
    }

    @Transactional
    public void supprimerCommentaire(Long commentaireId, String clientEmail) {
        CommentaireRealisation commentaire = commentaireRealisationRepository.findByIdAndCompteEmail(commentaireId, clientEmail)
                .orElseThrow(() -> new EntityNotFoundException("Commentaire introuvable ou vous n'êtes pas l'auteur."));

        Realisation realisation = commentaire.getRealisation();
        commentaireRealisationRepository.delete(commentaire);

        int nouveauTotal = Math.max(0, realisation.getTotalCommentaires() - 1);
        realisation.setTotalCommentaires(nouveauTotal);
        realisationRepository.save(realisation);

        log.info("Commentaire ID {} supprimé par son auteur {}", commentaireId, clientEmail);
    }

    @Transactional
    public void masquerCommentaire(String slugSalon, Long commentaireId, String managerEmail) {
        validerManager(slugSalon, managerEmail);

        CommentaireRealisation commentaire = commentaireRealisationRepository.findByIdAndRealisationSalonSlug(commentaireId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Commentaire introuvable ID " + commentaireId + " dans le salon " + slugSalon));

        if (commentaire.getStatut() == StatutCommentaire.ACTIF) {
            commentaire.setStatut(StatutCommentaire.MASQUE);
            commentaire.setDateModification(LocalDateTime.now());
            commentaireRealisationRepository.save(commentaire);

            Realisation realisation = commentaire.getRealisation();
            realisation.setTotalCommentaires(Math.max(0, realisation.getTotalCommentaires() - 1));
            realisationRepository.save(realisation);

            log.info("Commentaire ID {} masqué par le manager {} dans le salon {}", commentaireId, managerEmail, slugSalon);
        }
    }

    @Transactional
    public void deMasquerCommentaire(String slugSalon, Long commentaireId, String managerEmail) {
        validerManager(slugSalon, managerEmail);

        CommentaireRealisation commentaire = commentaireRealisationRepository.findByIdAndRealisationSalonSlug(commentaireId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Commentaire introuvable ID " + commentaireId + " dans le salon " + slugSalon));

        if (commentaire.getStatut() == StatutCommentaire.MASQUE) {
            commentaire.setStatut(StatutCommentaire.ACTIF);
            commentaire.setDateModification(LocalDateTime.now());
            commentaireRealisationRepository.save(commentaire);

            Realisation realisation = commentaire.getRealisation();
            realisation.setTotalCommentaires(realisation.getTotalCommentaires() + 1);
            realisationRepository.save(realisation);

            log.info("Commentaire ID {} réactivé par le manager {} dans le salon {}", commentaireId, managerEmail, slugSalon);
        }
    }

    @Transactional
    public void supprimerCommentaireParManager(String slugSalon, Long commentaireId, String managerEmail) {
        validerManager(slugSalon, managerEmail);

        CommentaireRealisation commentaire = commentaireRealisationRepository.findByIdAndRealisationSalonSlug(commentaireId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Commentaire introuvable ID " + commentaireId + " dans le salon " + slugSalon));

        Realisation realisation = commentaire.getRealisation();
        if (commentaire.getStatut() == StatutCommentaire.ACTIF) {
            realisation.setTotalCommentaires(Math.max(0, realisation.getTotalCommentaires() - 1));
            realisationRepository.save(realisation);
        }
        commentaireRealisationRepository.delete(commentaire);

        log.info("Commentaire ID {} supprimé définitivement par le manager {} dans le salon {}", commentaireId, managerEmail, slugSalon);
    }

    @Transactional(readOnly = true)
    public List<CommentaireDTOResponse> listerCommentairesSalon(String slugSalon, String managerEmail) {
        validerManager(slugSalon, managerEmail);

        List<CommentaireRealisation> list = commentaireRealisationRepository
                .findByRealisationSalonSlugWithCompteAndRealisationOrderByDateCreationDesc(slugSalon);

        return list.stream()
                .map(c -> mapToCommentaireResponse(c, managerEmail))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommentaireDTOResponse> listerCommentaires(Long realisationId, String currentUserEmailOpt) {
        List<CommentaireRealisation> list = commentaireRealisationRepository
                .findByRealisationIdAndStatutWithCompteOrderByDateCreationDesc(realisationId, StatutCommentaire.ACTIF);

        return list.stream()
                .map(c -> mapToCommentaireResponse(c, currentUserEmailOpt))
                .toList();
    }

    // ==========================================
    // 4. VISIONNAGES & FEED KADY'S
    // ==========================================

    @Transactional
    public void enregistrerVue(Long realisationId) {
        Realisation realisation = realisationRepository.findByIdAndStatutPublicationTrue(realisationId)
                .orElseThrow(() -> new EntityNotFoundException("Réalisation introuvable ID " + realisationId));

        realisation.setTotalVues(realisation.getTotalVues() + 1);
        realisationRepository.save(realisation);
    }

    @Transactional(readOnly = true)
    public Page<KadysRealisationDTOResponse> listerFeedKadys(String currentUserEmailOpt, Pageable pageable) {
        Page<Realisation> page = realisationRepository.findByStatutPublicationTrueOrderByDatePublicationDesc(pageable);

        Set<Long> likedIds = (currentUserEmailOpt != null && !currentUserEmailOpt.isBlank())
                ? likeRealisationRepository.findRealisationIdsLikedByEmail(currentUserEmailOpt)
                : Collections.emptySet();

        return page.map(r -> mapToKadysResponse(r, likedIds.contains(r.getId())));
    }

    @Transactional(readOnly = true)
    public KadysRealisationDTOResponse obtenirRealisationKadys(Long realisationId, String currentUserEmailOpt) {
        Realisation r = realisationRepository.findByIdAndStatutPublicationTrue(realisationId)
                .orElseThrow(() -> new EntityNotFoundException("Réalisation introuvable ID " + realisationId));

        boolean isLiked = false;
        if (currentUserEmailOpt != null && !currentUserEmailOpt.isBlank()) {
            isLiked = likeRealisationRepository.findRealisationIdsLikedByEmail(currentUserEmailOpt).contains(r.getId());
        }

        return mapToKadysResponse(r, isLiked);
    }

    @Transactional(readOnly = true)
    public Page<KadysRealisationDTOResponse> listerMesInspirations(String clientEmail, Pageable pageable) {
        Page<Realisation> page = likeRealisationRepository.findRealisationLikesByEmail(clientEmail, pageable);
        return page.map(r -> mapToKadysResponse(r, true));
    }

    // ==========================================
    // HELPERS & MAPPERS
    // ==========================================

    private AffectationSalon validerManager(String slugSalon, String email) {
        AffectationSalon affectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Aucune affectation active trouvée pour " + email + " dans le salon " + slugSalon));

        boolean isManager = affectation.getRoles().stream()
                .anyMatch(r -> r.getRole() == TypeRoleSalon.MANAGER);

        if (!isManager) {
            throw new IllegalArgumentException("L'utilisateur " + email + " n'a pas le rôle MANAGER dans ce salon.");
        }

        return affectation;
    }

    private CommentaireDTOResponse mapToCommentaireResponse(CommentaireRealisation c, String currentUserEmailOpt) {
        Compte auteur = c.getAffectationSalon().getCompte();
        boolean isMine = currentUserEmailOpt != null && auteur != null && currentUserEmailOpt.equalsIgnoreCase(auteur.getEmail());
        String nomComplet = auteur != null ? auteur.getPrenom() + " " + auteur.getNom() : "Utilisateur";

        return new CommentaireDTOResponse(
                c.getId(),
                c.getRealisation().getId(),
                auteur != null ? auteur.getId() : null,
                nomComplet,
                null, // Photo profil si disponible
                c.getContenu(),
                c.getStatut(),
                c.getDateCreation(),
                isMine
        );
    }

    private KadysRealisationDTOResponse mapToKadysResponse(Realisation r, boolean isLiked) {
        Salon s = r.getSalon();
        Compte c = r.getCoiffeur();
        String coiffeurNomComplet = c != null ? c.getPrenom() + " " + c.getNom() : null;

        return new KadysRealisationDTOResponse(
                r.getId(),
                r.getTitre(),
                r.getDescription(),
                r.getUrlVideo(),
                r.getDateRealisation(),
                r.getDatePublication(),
                s != null ? s.getSlug() : null,
                s != null ? s.getNom() : null,
                s != null ? s.getLogoUrl() : null,
                c != null ? c.getId() : null,
                coiffeurNomComplet,
                r.getTotalLikes(),
                r.getTotalCommentaires(),
                r.getTotalVues(),
                isLiked
        );
    }
}

package com.kadi_aon.mon_salon.salon.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.account.service.RefreshTokenService;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.common.service.CloudinaryService;
import com.kadi_aon.mon_salon.salon.dto.EmployeDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.EmployeUpdateRolesDTORequest;
import com.kadi_aon.mon_salon.salon.dto.SalonDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.SalonUpdateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.TransfertProprieteDTORequest;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.mapper.EmployeDTOResponseMapper;
import com.kadi_aon.mon_salon.salon.mapper.SalonDTOResponseMapper;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.RoleSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProprietaireSalonService {

    private final SalonRepository salonRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final SalonDTOResponseMapper salonDTOResponseMapper;
    private final AdminSalonService adminSalonService;
    private final CloudinaryService cloudinaryService;
    private final AuditLogService auditLogService;
    private final EmployeSalonService employeSalonService;
    private final EmployeDTOResponseMapper employeDTOResponseMapper;
    private final CompteRepository compteRepository;
    private final RoleSalonRepository roleSalonRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public SalonDTOResponse getSalon(String slugSalon, String proprietaireEmail) {
        Salon salon = salonRepository.findBySlug(slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Salon introuvable avec le slug : " + slugSalon));

        // Vérification de sécurité supplémentaire
        affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue(proprietaireEmail, slugSalon)
                .orElseThrow(() -> new IllegalArgumentException("Vous n'avez pas d'affectation active dans ce salon."));

        return salonDTOResponseMapper.apply(salon);
    }

    @Transactional
    public SalonDTOResponse updateSalon(String slugSalon, SalonUpdateDTORequest request, String proprietaireEmail) {
        Salon salon = salonRepository.findBySlug(slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Salon introuvable avec le slug : " + slugSalon));

        AffectationSalon affectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(proprietaireEmail, slugSalon)
                .orElseThrow(() -> new IllegalArgumentException("Vous n'avez pas d'affectation active dans ce salon."));

        String ancienneValeur = String.format("nom: %s, slug: %s, telephone: %s, adresse: %s",
                salon.getNom(), salon.getSlug(), salon.getTelephone(), salon.getAdresse());

        // Si le nom change, le slug est régénéré automatiquement avec garantie d'unicité
        if (request.nom() != null && !request.nom().isBlank() && !request.nom().trim().equalsIgnoreCase(salon.getNom())) {
            String nouveauSlug = adminSalonService.generateUniqueSlug(request.nom());
            log.info("Changement de nom de salon : '{}' -> '{}'. Nouveau slug généré : '{}'",
                    salon.getNom(), request.nom(), nouveauSlug);
            salon.setNom(request.nom().trim());
            salon.setSlug(nouveauSlug);
        }

        if (request.description() != null) {
            salon.setDescription(request.description());
        }
        if (request.adresse() != null) {
            salon.setAdresse(request.adresse());
        }
        if (request.telephone() != null) {
            salon.setTelephone(request.telephone());
        }
        if (request.email() != null) {
            salon.setEmail(request.email());
        }
        if (request.latitude() != null && request.longitude() != null) {
            salon.setCoordonnees(request.latitude(), request.longitude());
        }

        Salon updated = salonRepository.save(salon);

        String nouvelleValeur = String.format("nom: %s, slug: %s, telephone: %s, adresse: %s",
                updated.getNom(), updated.getSlug(), updated.getTelephone(), updated.getAdresse());

        // Audit lié à l'affectation du salon et au rôle PROPRIETAIRE
        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "Salon",
                String.valueOf(updated.getId()),
                ancienneValeur,
                nouvelleValeur,
                affectation,
                "PROPRIETAIRE"
        );

        return salonDTOResponseMapper.apply(updated);
    }

    @Transactional
    public SalonDTOResponse uploadLogo(String slugSalon, MultipartFile file, String proprietaireEmail) throws IOException {
        Salon salon = salonRepository.findBySlug(slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Salon introuvable avec le slug : " + slugSalon));

        AffectationSalon affectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(proprietaireEmail, slugSalon)
                .orElseThrow(() -> new IllegalArgumentException("Vous n'avez pas d'affectation active dans ce salon."));

        String ancienLogo = salon.getLogoUrl();

        // Upload Cloudinary avec conversion forcée en WebP
        String webpUrl = cloudinaryService.uploadLogo(file, salon.getSlug());
        salon.setLogoUrl(webpUrl);

        Salon updated = salonRepository.save(salon);

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "Salon (Logo)",
                String.valueOf(updated.getId()),
                ancienLogo != null ? ancienLogo : "aucun",
                webpUrl,
                affectation,
                "PROPRIETAIRE"
        );

        return salonDTOResponseMapper.apply(updated);
    }

    @Transactional(readOnly = true)
    public EmployeDTOResponse getMesRoles(String slugSalon, String proprietaireEmail) {
        AffectationSalon affectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(proprietaireEmail, slugSalon)
                .orElseThrow(() -> new IllegalArgumentException("Vous n'avez pas d'affectation active dans ce salon."));

        return employeDTOResponseMapper.apply(affectation);
    }

    @Transactional
    public EmployeDTOResponse updateMesRoles(
            String slugSalon,
            EmployeUpdateRolesDTORequest request,
            String proprietaireEmail) {

        AffectationSalon affectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(proprietaireEmail, slugSalon)
                .orElseThrow(() -> new IllegalArgumentException("Vous n'avez pas d'affectation active dans ce salon."));

        return employeSalonService.updateRoles(slugSalon, affectation.getId(), request, proprietaireEmail);
    }

    @Transactional
    public SalonDTOResponse transfererPropriete(
            String slugSalon,
            TransfertProprieteDTORequest request,
            String proprietaireEmail) {

        Salon salon = salonRepository.findBySlug(slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Salon introuvable avec le slug : " + slugSalon));

        AffectationSalon proprioAffectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(proprietaireEmail, slugSalon)
                .orElseThrow(() -> new IllegalArgumentException("Vous n'avez pas d'affectation active dans ce salon."));

        boolean isProprio = proprioAffectation.getRoles().stream()
                .anyMatch(r -> r.getRole() == TypeRoleSalon.PROPRIETAIRE);
        if (!isProprio) {
            throw new IllegalArgumentException("Seul le propriétaire actuel peut céder la propriété de ce salon.");
        }

        if (!passwordEncoder.matches(request.motDePasseConfirmation(), proprioAffectation.getCompte().getPassword())) {
            throw new IllegalArgumentException("Mot de passe de confirmation incorrect.");
        }

        String nouveauEmail = request.nouvelEmailProprietaire().trim().toLowerCase(Locale.ROOT);
        if (nouveauEmail.equalsIgnoreCase(proprietaireEmail)) {
            throw new IllegalArgumentException("Vous êtes déjà le propriétaire de ce salon.");
        }

        Compte nouveauCompte = compteRepository.findByEmail(nouveauEmail)
                .orElseThrow(() -> new EntityNotFoundException("Aucun compte utilisateur trouvé avec l'email : " + nouveauEmail));

        if (!Boolean.TRUE.equals(nouveauCompte.getStatut())) {
            throw new IllegalArgumentException("Le compte du nouveau propriétaire est désactivé.");
        }

        RoleSalon roleProprio = roleSalonRepository.findByRole(TypeRoleSalon.PROPRIETAIRE)
                .orElseThrow(() -> new IllegalStateException("Rôle PROPRIETAIRE non initialisé"));
        RoleSalon roleClient = roleSalonRepository.findByRole(TypeRoleSalon.CLIENT)
                .orElseThrow(() -> new IllegalStateException("Rôle CLIENT non initialisé"));

        // 1. Affectation du nouveau propriétaire
        AffectationSalon nouveauAffectation = affectationSalonRepository
                .findByCompteAndSalon(nouveauCompte, salon)
                .orElse(null);

        if (nouveauAffectation == null) {
            Set<RoleSalon> roles = new HashSet<>();
            roles.add(roleProprio);
            roles.add(roleClient);

            nouveauAffectation = AffectationSalon.builder()
                    .compte(nouveauCompte)
                    .salon(salon)
                    .roles(roles)
                    .statut(true)
                    .dateDebut(LocalDate.now())
                    .build();
        } else {
            nouveauAffectation.setStatut(true);
            nouveauAffectation.getRoles().add(roleProprio);
            nouveauAffectation.getRoles().add(roleClient);
        }
        affectationSalonRepository.save(nouveauAffectation);

        // 2. Retrait du rôle PROPRIETAIRE pour l'ancien propriétaire (seul cas autorisé de retrait du rôle PROPRIETAIRE)
        proprioAffectation.getRoles().removeIf(r -> r.getRole() == TypeRoleSalon.PROPRIETAIRE);
        if (proprioAffectation.getRoles().isEmpty()) {
            proprioAffectation.getRoles().add(roleClient);
        }
        affectationSalonRepository.save(proprioAffectation);

        // 3. Révocation des sessions pour forcer le rafraîchissement des tokens et droits
        refreshTokenService.revokeAllByCompte(proprioAffectation.getCompte());
        refreshTokenService.revokeAllByCompte(nouveauCompte);

        // 4. Audit
        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "Salon (Transfert Propriétaire)",
                String.valueOf(salon.getId()),
                proprietaireEmail,
                nouveauEmail,
                proprioAffectation,
                "PROPRIETAIRE"
        );

        log.info("Propriété du salon '{}' ({}) transférée de '{}' vers '{}'",
                salon.getNom(), slugSalon, proprietaireEmail, nouveauEmail);

        return salonDTOResponseMapper.apply(salon);
    }
}

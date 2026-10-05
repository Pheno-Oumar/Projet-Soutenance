package com.kadi_aon.mon_salon.salon.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.account.service.RefreshTokenService;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.notification.service.NotificationEmailService;
import com.kadi_aon.mon_salon.salon.dto.EmployeCreateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.EmployeDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.EmployeUpdateRolesDTORequest;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.mapper.EmployeDTOResponseMapper;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.RoleSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeSalonService {

    private final SalonRepository salonRepository;
    private final CompteRepository compteRepository;
    private final RoleSalonRepository roleSalonRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final EmployeDTOResponseMapper employeDTOResponseMapper;
    private final NotificationEmailService notificationEmailService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;

    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public EmployeDTOResponse creerEmploye(String slugSalon, EmployeCreateDTORequest request, String proprietaireEmail) {
        Salon salon = salonRepository.findBySlug(slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Salon introuvable avec le slug : " + slugSalon));

        AffectationSalon proprioAffectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(proprietaireEmail, slugSalon)
                .orElseThrow(() -> new IllegalArgumentException("Vous n'avez pas d'affectation active dans ce salon."));

        // Interdiction d'attribuer le rôle PROPRIETAIRE lors de la création d'un employé
        if (request.roles().contains(TypeRoleSalon.PROPRIETAIRE)) {
            throw new IllegalArgumentException("Le rôle PROPRIETAIRE ne peut pas être attribué à un employé.");
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        // Récupération des rôles salon
        Set<RoleSalon> roles = new HashSet<>();
        for (TypeRoleSalon typeRole : request.roles()) {
            RoleSalon roleSalon = roleSalonRepository.findByRole(typeRole)
                    .orElseThrow(() -> new IllegalStateException("Rôle " + typeRole + " non initialisé en base"));
            roles.add(roleSalon);
        }

        // Tout employé bénéficie également automatiquement du rôle CLIENT en plus de ses rôles métier
        if (!request.roles().contains(TypeRoleSalon.CLIENT)) {
            RoleSalon roleClient = roleSalonRepository.findByRole(TypeRoleSalon.CLIENT)
                    .orElseThrow(() -> new IllegalStateException("Rôle CLIENT non initialisé en base"));
            roles.add(roleClient);
        }

        Compte compte = compteRepository.findByEmail(email).orElse(null);

        if (compte != null) {
            // Vérifier si une affectation existe déjà pour ce compte dans ce salon (active ou inactive)
            List<AffectationSalon> affectationsExistantes = affectationSalonRepository
                    .findByCompteAndSalonOrderByStatutDescIdDesc(compte, salon);

            if (!affectationsExistantes.isEmpty()) {
                AffectationSalon affectation = affectationsExistantes.get(0);
                if (Boolean.TRUE.equals(affectation.getStatut())) {
                    throw new IllegalArgumentException("Cet utilisateur possède déjà une affectation active dans ce salon.");
                }

                // Réactivation propre de l'affectation existante pour éviter tout doublon
                affectation.setStatut(true);
                affectation.setDateDebut(LocalDate.now());
                affectation.setDateFin(null);
                affectation.setRoles(roles);
                AffectationSalon updated = affectationSalonRepository.save(affectation);

                auditLogService.logActionSalon(
                        TypeActionAudit.REACTIVATION,
                        "AffectationSalon",
                        String.valueOf(updated.getId()),
                        "statut=false",
                        String.format("Réactivation de l'employé '%s' avec rôles %s", email, request.roles()),
                        proprioAffectation,
                        "PROPRIETAIRE"
                );

                log.info("Affectation existante réactivée pour l'employé {} dans le salon {}", email, slugSalon);
                return employeDTOResponseMapper.apply(updated);
            }
        } else {
            // Création d'un nouveau compte
            String tempPassword = generateRandomPassword(10);
            compte = Compte.builder()
                    .email(email)
                    .nom(request.nom().trim())
                    .prenom(request.prenom().trim())
                    .telephone(request.telephone().trim())
                    .password(passwordEncoder.encode(tempPassword))
                    .statut(true)
                    .build();
            compte = compteRepository.save(compte);

            notificationEmailService.queueNouveauProprietaireEmail(email, salon.getNom(), tempPassword);
            log.info("Nouveau compte employé créé pour {} avec mot de passe généré.", email);
        }

        AffectationSalon affectation = AffectationSalon.builder()
                .compte(compte)
                .salon(salon)
                .roles(roles)
                .statut(true)
                .dateDebut(LocalDate.now())
                .build();

        AffectationSalon saved = affectationSalonRepository.save(affectation);

        auditLogService.logActionSalon(
                TypeActionAudit.CREATION,
                "AffectationSalon",
                String.valueOf(saved.getId()),
                null,
                String.format("Embauche de l'employé '%s' avec rôles %s", email, request.roles()),
                proprioAffectation,
                "PROPRIETAIRE"
        );

        return employeDTOResponseMapper.apply(saved);
    }

    @Transactional(readOnly = true)
    public List<EmployeDTOResponse> listerEmployes(String slugSalon) {
        Salon salon = salonRepository.findBySlug(slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Salon introuvable avec le slug : " + slugSalon));

        return affectationSalonRepository.findEmployesBySalonId(salon.getId()).stream()
                .map(employeDTOResponseMapper)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeDTOResponse getEmploye(String slugSalon, Long affectationId) {
        AffectationSalon affectation = affectationSalonRepository.findById(affectationId)
                .orElseThrow(() -> new EntityNotFoundException("Employé introuvable avec l'identifiant : " + affectationId));

        if (!affectation.getSalon().getSlug().equals(slugSalon)) {
            throw new IllegalArgumentException("Cet employé n'appartient pas au salon spécifié.");
        }

        return employeDTOResponseMapper.apply(affectation);
    }

    @Transactional
    public EmployeDTOResponse updateRoles(
            String slugSalon,
            Long affectationId,
            EmployeUpdateRolesDTORequest request,
            String proprietaireEmail) {

        AffectationSalon affectation = affectationSalonRepository.findById(affectationId)
                .orElseThrow(() -> new EntityNotFoundException("Employé introuvable avec l'identifiant : " + affectationId));

        if (!affectation.getSalon().getSlug().equals(slugSalon)) {
            throw new IllegalArgumentException("Cet employé n'appartient pas au salon spécifié.");
        }

        AffectationSalon proprioAffectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(proprietaireEmail, slugSalon)
                .orElseThrow(() -> new IllegalArgumentException("Vous n'avez pas d'affectation active dans ce salon."));

        boolean isSelfModification = affectation.getCompte().getEmail().equalsIgnoreCase(proprietaireEmail);
        boolean targetHasProprioRole = affectation.getRoles().stream()
                .anyMatch(r -> r.getRole() == TypeRoleSalon.PROPRIETAIRE);

        // Si l'employé cible n'est PAS le propriétaire, interdiction formelle d'attribuer le rôle PROPRIETAIRE
        if (!isSelfModification && !targetHasProprioRole) {
            if (request.roles().contains(TypeRoleSalon.PROPRIETAIRE)) {
                throw new IllegalArgumentException("Le rôle PROPRIETAIRE ne peut pas être attribué à un employé.");
            }
        }

        String anciensRoles = affectation.getRoles().stream()
                .map(r -> r.getRole().name())
                .collect(Collectors.joining(", "));

        Set<RoleSalon> newRoles = new HashSet<>();
        for (TypeRoleSalon typeRole : request.roles()) {
            if (typeRole == TypeRoleSalon.PROPRIETAIRE && !isSelfModification && !targetHasProprioRole) {
                continue;
            }
            RoleSalon roleSalon = roleSalonRepository.findByRole(typeRole)
                    .orElseThrow(() -> new IllegalStateException("Rôle " + typeRole + " non initialisé"));
            newRoles.add(roleSalon);
        }

        // RÈGLE : Le propriétaire peut s'attribuer des rôles en plus du rôle PROPRIETAIRE,
        // mais il ne peut JAMAIS s'enlever le rôle PROPRIETAIRE (sauf transfert explicite de propriété).
        if (isSelfModification || targetHasProprioRole) {
            RoleSalon roleProprio = roleSalonRepository.findByRole(TypeRoleSalon.PROPRIETAIRE)
                    .orElseThrow(() -> new IllegalStateException("Rôle PROPRIETAIRE non initialisé"));
            newRoles.add(roleProprio);
        }

        // Tout membre (propriétaire ou employé) conserve automatiquement le rôle CLIENT en plus de ses rôles métier
        if (!request.roles().contains(TypeRoleSalon.CLIENT)) {
            RoleSalon roleClient = roleSalonRepository.findByRole(TypeRoleSalon.CLIENT)
                    .orElseThrow(() -> new IllegalStateException("Rôle CLIENT non initialisé"));
            newRoles.add(roleClient);
        }

        affectation.setRoles(newRoles);
        AffectationSalon updated = affectationSalonRepository.save(affectation);

        String nouveauxRoles = newRoles.stream()
                .map(r -> r.getRole().name())
                .collect(Collectors.joining(", "));

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "AffectationSalon (Roles)",
                String.valueOf(updated.getId()),
                anciensRoles,
                nouveauxRoles,
                proprioAffectation,
                "PROPRIETAIRE"
        );

        return employeDTOResponseMapper.apply(updated);
    }

    @Transactional
    public EmployeDTOResponse desactiverEmploye(String slugSalon, Long affectationId, String proprietaireEmail) {
        AffectationSalon affectation = affectationSalonRepository.findById(affectationId)
                .orElseThrow(() -> new EntityNotFoundException("Employé introuvable avec l'identifiant : " + affectationId));

        if (!affectation.getSalon().getSlug().equals(slugSalon)) {
            throw new IllegalArgumentException("Cet employé n'appartient pas au salon spécifié.");
        }

        AffectationSalon proprioAffectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(proprietaireEmail, slugSalon)
                .orElseThrow(() -> new IllegalArgumentException("Vous n'avez pas d'affectation active dans ce salon."));

        if (affectation.getCompte().getEmail().equalsIgnoreCase(proprietaireEmail)) {
            throw new IllegalArgumentException("Le propriétaire ne peut pas se désactiver lui-même.");
        }

        affectation.setStatut(false);
        affectation.setDateFin(LocalDate.now());
        AffectationSalon updated = affectationSalonRepository.save(affectation);

        // Sécurité : Révocation des sessions Refresh Token de l'employé désactivé
        refreshTokenService.revokeAllByCompte(affectation.getCompte());

        auditLogService.logActionSalon(
                TypeActionAudit.DESACTIVATION,
                "AffectationSalon",
                String.valueOf(updated.getId()),
                "statut=true",
                "statut=false",
                proprioAffectation,
                "PROPRIETAIRE"
        );

        log.info("Employé {} désactivé dans le salon {}", affectation.getCompte().getEmail(), slugSalon);
        return employeDTOResponseMapper.apply(updated);
    }

    @Transactional
    public EmployeDTOResponse reactiverEmploye(String slugSalon, Long affectationId, String proprietaireEmail) {
        AffectationSalon affectation = affectationSalonRepository.findById(affectationId)
                .orElseThrow(() -> new EntityNotFoundException("Employé introuvable avec l'identifiant : " + affectationId));

        if (!affectation.getSalon().getSlug().equals(slugSalon)) {
            throw new IllegalArgumentException("Cet employé n'appartient pas au salon spécifié.");
        }

        AffectationSalon proprioAffectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(proprietaireEmail, slugSalon)
                .orElseThrow(() -> new IllegalArgumentException("Vous n'avez pas d'affectation active dans ce salon."));

        affectation.setStatut(true);
        affectation.setDateFin(null);
        AffectationSalon updated = affectationSalonRepository.save(affectation);

        auditLogService.logActionSalon(
                TypeActionAudit.REACTIVATION,
                "AffectationSalon",
                String.valueOf(updated.getId()),
                "statut=false",
                "statut=true",
                proprioAffectation,
                "PROPRIETAIRE"
        );

        log.info("Employé {} réactivé dans le salon {}", affectation.getCompte().getEmail(), slugSalon);
        return employeDTOResponseMapper.apply(updated);
    }

    private String generateRandomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}

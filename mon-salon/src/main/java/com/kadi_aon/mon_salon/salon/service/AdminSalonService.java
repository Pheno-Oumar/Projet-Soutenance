package com.kadi_aon.mon_salon.salon.service;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.notification.service.NotificationEmailService;
import com.kadi_aon.mon_salon.salon.dto.SalonCreateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.SalonDTOResponse;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
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
public class AdminSalonService {

    private final SalonRepository salonRepository;
    private final CompteRepository compteRepository;
    private final RoleSalonRepository roleSalonRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final SalonDTOResponseMapper salonDTOResponseMapper;
    private final NotificationEmailService notificationEmailService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public SalonDTOResponse creerSalon(SalonCreateDTORequest request, String adminEmail) {
        Compte adminCompte = (adminEmail != null) ? compteRepository.findByEmail(adminEmail).orElse(null) : null;

        // 1. Génération automatique du slug sécurisé et unique
        String slug = generateUniqueSlug(request.nom());

        // 2. Création et sauvegarde du Salon
        Salon salon = Salon.builder()
                .nom(request.nom().trim())
                .slug(slug)
                .description(request.description())
                .adresse(request.adresse())
                .telephone(request.telephone())
                .email(request.email())
                .logoUrl(request.logoUrl())
                .statut(true)
                .build();

        if (request.latitude() != null && request.longitude() != null) {
            salon.setCoordonnees(request.latitude(), request.longitude());
        }

        salon = salonRepository.save(salon);

        // 3. Gestion du propriétaire (existant ou création nouveau compte)
        String emailProprio = request.emailProprietaire().trim().toLowerCase(Locale.ROOT);
        Compte proprioCompte = compteRepository.findByEmail(emailProprio).orElse(null);

        if (proprioCompte == null) {
            String tempPassword = generateRandomPassword(10);
            proprioCompte = Compte.builder()
                    .email(emailProprio)
                    .nom("Propriétaire")
                    .prenom(salon.getNom())
                    .telephone("00000000" + RANDOM.nextInt(90 + 10)) // Numéro par défaut modifiable
                    .password(passwordEncoder.encode(tempPassword))
                    .statut(true)
                    .build();
            proprioCompte = compteRepository.save(proprioCompte);

            // Mise en file d'attente de l'e-mail avec mot de passe pour le scheduler
            notificationEmailService.queueNouveauProprietaireEmail(emailProprio, salon.getNom(), tempPassword);
            log.info("Nouveau compte propriétaire créé pour {} avec mot de passe généré.", emailProprio);
        } else {
            log.info("Le compte propriétaire {} existait déjà. Rattachement au nouveau salon.", emailProprio);
        }

        // 4. Attribution de l'affectation active avec le rôle PROPRIETAIRE
        RoleSalon roleProprio = roleSalonRepository.findByRole(TypeRoleSalon.PROPRIETAIRE)
                .orElseThrow(() -> new IllegalStateException("Le rôle PROPRIETAIRE n'est pas initialisé en base."));

        AffectationSalon affectation = AffectationSalon.builder()
                .compte(proprioCompte)
                .salon(salon)
                .roles(new HashSet<>(Collections.singletonList(roleProprio)))
                .statut(true)
                .dateDebut(LocalDate.now())
                .build();
        affectationSalonRepository.save(affectation);

        // 5. Audit de l'action
        auditLogService.logActionPlateforme(
                TypeActionAudit.CREATION,
                "Salon",
                String.valueOf(salon.getId()),
                null,
                String.format("Création du salon '%s' (slug: %s) rattaché au propriétaire '%s'", salon.getNom(), slug, emailProprio),
                adminCompte,
                "ADMIN_SYSTEME"
        );

        return salonDTOResponseMapper.apply(salon);
    }

    @Transactional(readOnly = true)
    public List<SalonDTOResponse> listerSalons() {
        return salonRepository.findAll().stream()
                .map(salonDTOResponseMapper)
                .toList();
    }

    @Transactional(readOnly = true)
    public SalonDTOResponse getSalonBySlug(String slug) {
        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Salon introuvable avec le slug : " + slug));
        return salonDTOResponseMapper.apply(salon);
    }

    @Transactional
    public SalonDTOResponse desactiverSalon(String slug, String adminEmail) {
        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Salon introuvable avec le slug : " + slug));

        Compte adminCompte = (adminEmail != null) ? compteRepository.findByEmail(adminEmail).orElse(null) : null;

        salon.setStatut(false);
        Salon updated = salonRepository.save(salon);

        auditLogService.logActionPlateforme(
                TypeActionAudit.DESACTIVATION,
                "Salon",
                String.valueOf(updated.getId()),
                "statut=true",
                "statut=false",
                adminCompte,
                "ADMIN_SYSTEME"
        );

        log.info("Salon désactivé : {} (slug: {})", updated.getNom(), slug);
        return salonDTOResponseMapper.apply(updated);
    }

    @Transactional
    public SalonDTOResponse reactiverSalon(String slug, String adminEmail) {
        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Salon introuvable avec le slug : " + slug));

        Compte adminCompte = (adminEmail != null) ? compteRepository.findByEmail(adminEmail).orElse(null) : null;

        salon.setStatut(true);
        Salon updated = salonRepository.save(salon);

        auditLogService.logActionPlateforme(
                TypeActionAudit.REACTIVATION,
                "Salon",
                String.valueOf(updated.getId()),
                "statut=false",
                "statut=true",
                adminCompte,
                "ADMIN_SYSTEME"
        );

        log.info("Salon réactivé : {} (slug: {})", updated.getNom(), slug);
        return salonDTOResponseMapper.apply(updated);
    }

    public String generateUniqueSlug(String nom) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom du salon est obligatoire pour générer le slug.");
        }

        // Remplacement des accents
        String normalized = Normalizer.normalize(nom, Normalizer.Form.NFD);
        String slugBase = normalized.replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");

        if (slugBase.isBlank()) {
            slugBase = "salon";
        }

        String candidateSlug = slugBase;
        int counter = 1;
        while (salonRepository.existsBySlug(candidateSlug)) {
            candidateSlug = slugBase + "-" + counter;
            counter++;
        }
        return candidateSlug;
    }

    private String generateRandomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}

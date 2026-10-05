package com.kadi_aon.mon_salon.security.service;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.account.enums.TypeRolePlateforme;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("salonSecurity")
@RequiredArgsConstructor
public class SalonSecurityService {

    private final SalonRepository salonRepository;
    private final AffectationSalonRepository affectationSalonRepository;

    @Transactional(readOnly = true)
    public boolean hasRoleInSalon(String slugSalon, String requiredRoleName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }

        // Vérification 1 : Si c'est l'administrateur système global, il a tous les droits
        boolean isAdminSysteme = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + TypeRolePlateforme.ADMIN_SYSTEME.name()));
        if (isAdminSysteme) {
            return true;
        }

        // Pipeline Étape 3 : Le salon existe-t-il et est-il actif ?
        Optional<Salon> salonOpt = salonRepository.findBySlug(slugSalon);
        if (salonOpt.isEmpty() || !Boolean.TRUE.equals(salonOpt.get().getStatut())) {
            log.warn("Salon introuvable ou inactif pour le slug : {}", slugSalon);
            return false;
        }

        String email = auth.getName();

        // Pipeline Étape 4 & 5 : L'utilisateur a-t-il une affectation active sur ce salon ?
        Optional<AffectationSalon> affectationOpt =
                affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon);

        if (affectationOpt.isEmpty()) {
            log.warn("Aucune affectation active trouvée pour l'utilisateur {} sur le salon {}", email, slugSalon);
            return false;
        }

        // Pipeline Étape 4 : L'affectation contient-elle le rôle requis ? (Cumul fluide)
        try {
            TypeRoleSalon targetRole = TypeRoleSalon.valueOf(requiredRoleName.toUpperCase());
            boolean hasRole = affectationOpt.get().getRoles().stream()
                    .anyMatch(r -> r.getRole() == targetRole);

            if (!hasRole) {
                log.warn("Utilisateur {} n'a pas le rôle requis {} sur le salon {}", email, requiredRoleName, slugSalon);
            }
            return hasRole;
        } catch (IllegalArgumentException e) {
            log.error("Rôle salon inconnu : {}", requiredRoleName);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean isMemberOfSalon(String slugSalon) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }

        Optional<Salon> salonOpt = salonRepository.findBySlug(slugSalon);
        if (salonOpt.isEmpty() || !Boolean.TRUE.equals(salonOpt.get().getStatut())) {
            return false;
        }

        String email = auth.getName();
        return affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean isClient() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }

        boolean isAdminSysteme = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + TypeRolePlateforme.ADMIN_SYSTEME.name()));
        if (isAdminSysteme) {
            return true;
        }

        String email = auth.getName();
        return affectationSalonRepository.hasClientRole(email);
    }
}


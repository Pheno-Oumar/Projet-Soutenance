package com.kadi_aon.mon_salon.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.entity.RolePlateforme;
import com.kadi_aon.mon_salon.account.enums.TypeRolePlateforme;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.account.repository.RolePlateformeRepository;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.RoleSalonRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class Initialisateur implements CommandLineRunner {

    private final RolePlateformeRepository rolePlateformeRepository;
    private final RoleSalonRepository roleSalonRepository;
    private final CompteRepository compteRepository;
    private final com.kadi_aon.mon_salon.salon.repository.SalonRepository salonRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        initRoles();
        initAdminSysteme();
        initSalonsLogos();
    }

    private void initRoles() {
        // Initialisation des rôles plateforme
        for (TypeRolePlateforme typeRole : TypeRolePlateforme.values()) {
            if (!rolePlateformeRepository.existsByRole(typeRole)) {
                RolePlateforme rolePlateforme = new RolePlateforme(typeRole);
                rolePlateformeRepository.save(rolePlateforme);
                log.info("Rôle plateforme initialisé : {}", typeRole);
            }
        }

        // Initialisation des rôles salon
        for (TypeRoleSalon typeRole : TypeRoleSalon.values()) {
            if (!roleSalonRepository.existsByRole(typeRole)) {
                RoleSalon roleSalon = new RoleSalon(typeRole);
                roleSalonRepository.save(roleSalon);
                log.info("Rôle salon initialisé : {}", typeRole);
            }
        }
    }

    private void initAdminSysteme() {
        String email = "amadou14112004@gmail.com";

        if (!compteRepository.existsByEmail(email)) {
            RolePlateforme roleAdmin = rolePlateformeRepository.findByRole(TypeRolePlateforme.ADMIN_SYSTEME)
                    .orElseThrow(() -> new IllegalStateException("Rôle ADMIN_SYSTEME non trouvé"));

            Compte admin = Compte.builder()
                    .email(email)
                    .nom("AON")
                    .prenom("Amadou")
                    .telephone("76662725")
                    .password(passwordEncoder.encode("123456"))
                    .statut(true)
                    .rolePlateforme(roleAdmin)
                    .build();

            compteRepository.save(admin);
            log.info("Compte administrateur système créé avec succès (email: {})", email);
        } else {
            log.info("Le compte administrateur système existe déjà.");
        }
    }

    private void initSalonsLogos() {
        var salons = salonRepository.findAll();
        String[] defaultLogos = {
            "https://images.unsplash.com/photo-1560066984-138dadb4c035?w=500&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=500&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=500&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1521590832167-7bcbfaa6381f?w=500&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1562322140-8baeececf3df?w=500&auto=format&fit=crop&q=80"
        };

        int idx = 0;
        for (var s : salons) {
            if (s.getLogoUrl() == null || s.getLogoUrl().trim().isBlank()) {
                String assignedLogo = defaultLogos[idx % defaultLogos.length];
                s.setLogoUrl(assignedLogo);
                salonRepository.save(s);
                log.info("Logo automatique assigné au salon '{}' : {}", s.getNom(), assignedLogo);
            }
            idx++;
        }
    }
}

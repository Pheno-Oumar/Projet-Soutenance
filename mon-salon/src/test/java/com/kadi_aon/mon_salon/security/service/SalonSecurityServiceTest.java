package com.kadi_aon.mon_salon.security.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

@ExtendWith(MockitoExtension.class)
class SalonSecurityServiceTest {

    @Mock
    private SalonRepository salonRepository;

    @Mock
    private AffectationSalonRepository affectationSalonRepository;

    @InjectMocks
    private SalonSecurityService salonSecurityService;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCumulFluideRolesSuccess() {
        String email = "employe@salon.com";
        String slug = "salon-star";

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);
        when(authentication.getName()).thenReturn(email);
        when(authentication.getAuthorities()).thenReturn(Collections.emptyList());

        Salon salon = Salon.builder().slug(slug).nom("Salon Star").statut(true).build();
        when(salonRepository.findBySlug(slug)).thenReturn(Optional.of(salon));

        // Affectation avec DEUX rôles : PROPRIETAIRE et MANAGER (Cumul fluide)
        RoleSalon roleProprio = new RoleSalon(TypeRoleSalon.PROPRIETAIRE);
        RoleSalon roleManager = new RoleSalon(TypeRoleSalon.MANAGER);
        AffectationSalon affectation = AffectationSalon.builder()
                .statut(true)
                .roles(Set.of(roleProprio, roleManager))
                .build();

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue(email, slug))
                .thenReturn(Optional.of(affectation));

        // L'utilisateur peut accéder à la fois comme PROPRIETAIRE et MANAGER
        assertTrue(salonSecurityService.hasRoleInSalon(slug, "PROPRIETAIRE"));
        assertTrue(salonSecurityService.hasRoleInSalon(slug, "MANAGER"));

        // Mais n'a pas accès en tant que COIFFEUR
        assertFalse(salonSecurityService.hasRoleInSalon(slug, "COIFFEUR"));
    }

    @Test
    void testHasRoleInSalonInactiveSalonDenied() {
        String slug = "salon-ferme";
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenReturn(Collections.emptyList());

        // Salon inactif (statut = false)
        Salon salon = Salon.builder().slug(slug).nom("Salon Fermé").statut(false).build();
        when(salonRepository.findBySlug(slug)).thenReturn(Optional.of(salon));

        assertFalse(salonSecurityService.hasRoleInSalon(slug, "PROPRIETAIRE"));
    }

    @Test
    void testHasRoleInSalonNoAffectationDenied() {
        String email = "inconnu@salon.com";
        String slug = "salon-star";

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(email);
        when(authentication.getAuthorities()).thenReturn(Collections.emptyList());

        Salon salon = Salon.builder().slug(slug).nom("Salon Star").statut(true).build();
        when(salonRepository.findBySlug(slug)).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue(email, slug))
                .thenReturn(Optional.empty());

        assertFalse(salonSecurityService.hasRoleInSalon(slug, "PROPRIETAIRE"));
    }
}

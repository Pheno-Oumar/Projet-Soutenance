package com.kadi_aon.mon_salon.salon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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

@ExtendWith(MockitoExtension.class)
class AdminSalonServiceTest {

    @Mock
    private SalonRepository salonRepository;
    @Mock
    private CompteRepository compteRepository;
    @Mock
    private RoleSalonRepository roleSalonRepository;
    @Mock
    private AffectationSalonRepository affectationSalonRepository;
    @Mock
    private SalonDTOResponseMapper salonDTOResponseMapper;
    @Mock
    private NotificationEmailService notificationEmailService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AdminSalonService adminSalonService;

    private RoleSalon roleProprietaire;

    @BeforeEach
    void setUp() {
        roleProprietaire = new RoleSalon(TypeRoleSalon.PROPRIETAIRE);
    }

    @Test
    void testCreerSalonNouveauProprietaire() {
        SalonCreateDTORequest request = new SalonCreateDTORequest("Salon Élégance & Beauté", "nouveau.proprio@test.com");

        when(salonRepository.existsBySlug("salon-elegance-beaute")).thenReturn(false);
        when(salonRepository.save(any(Salon.class))).thenAnswer(i -> {
            Salon s = i.getArgument(0);
            s.setId(1L);
            return s;
        });
        when(compteRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(Compte.builder().id(99L).email("admin@test.com").build()));
        when(compteRepository.findByEmail("nouveau.proprio@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashedTempPassword");
        when(compteRepository.save(any(Compte.class))).thenAnswer(i -> {
            Compte c = i.getArgument(0);
            c.setId(2L);
            return c;
        });
        when(roleSalonRepository.findByRole(TypeRoleSalon.PROPRIETAIRE)).thenReturn(Optional.of(roleProprietaire));
        when(salonDTOResponseMapper.apply(any(Salon.class))).thenAnswer(i -> {
            Salon s = i.getArgument(0);
            return new SalonDTOResponse(s.getId(), s.getNom(), s.getSlug(), null, null, null, null, null, null, null, true, null, "nouveau.proprio@test.com");
        });

        SalonDTOResponse response = adminSalonService.creerSalon(request, "admin@test.com");

        assertNotNull(response);
        assertEquals("salon-elegance-beaute", response.slug());
        assertEquals("nouveau.proprio@test.com", response.emailProprietaire());

        verify(notificationEmailService).queueNouveauProprietaireEmail(eq("nouveau.proprio@test.com"), eq("Salon Élégance & Beauté"), any());
        verify(affectationSalonRepository).save(any(AffectationSalon.class));
        verify(auditLogService).logActionPlateforme(eq(TypeActionAudit.CREATION), eq("Salon"), eq("1"), any(), any(), any(), eq("ADMIN_SYSTEME"));
    }

    @Test
    void testGenerateUniqueSlugWithCollision() {
        when(salonRepository.existsBySlug("mon-salon")).thenReturn(true);
        when(salonRepository.existsBySlug("mon-salon-1")).thenReturn(true);
        when(salonRepository.existsBySlug("mon-salon-2")).thenReturn(false);

        String slug = adminSalonService.generateUniqueSlug("Mon Salon !");
        assertEquals("mon-salon-2", slug);
    }

    @Test
    void testDesactiverEtReactiverSalon() {
        Salon salon = Salon.builder().id(5L).nom("Salon Test").slug("salon-test").statut(true).build();
        when(salonRepository.findBySlug("salon-test")).thenReturn(Optional.of(salon));
        when(salonRepository.save(any(Salon.class))).thenAnswer(i -> i.getArgument(0));
        when(salonDTOResponseMapper.apply(any(Salon.class))).thenAnswer(i -> {
            Salon s = i.getArgument(0);
            return new SalonDTOResponse(s.getId(), s.getNom(), s.getSlug(), null, null, null, null, null, null, null, s.getStatut(), null, null);
        });

        SalonDTOResponse desactive = adminSalonService.desactiverSalon("salon-test", "admin@test.com");
        assertFalse(desactive.statut());
        verify(auditLogService).logActionPlateforme(eq(TypeActionAudit.DESACTIVATION), eq("Salon"), eq("5"), any(), any(), any(), eq("ADMIN_SYSTEME"));

        SalonDTOResponse reactive = adminSalonService.reactiverSalon("salon-test", "admin@test.com");
        assertTrue(reactive.statut());
        verify(auditLogService).logActionPlateforme(eq(TypeActionAudit.REACTIVATION), eq("Salon"), eq("5"), any(), any(), any(), eq("ADMIN_SYSTEME"));
    }
}

package com.kadi_aon.mon_salon.regle.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.notification.service.NotificationEmailService;
import com.kadi_aon.mon_salon.regle.dto.ReglePlateformeCreateDTORequest;
import com.kadi_aon.mon_salon.regle.dto.ReglePlateformeDTOResponse;
import com.kadi_aon.mon_salon.regle.dto.ReglePlateformeUpdateDTORequest;
import com.kadi_aon.mon_salon.regle.entity.ReglePlateforme;
import com.kadi_aon.mon_salon.regle.repository.ReglePlateformeRepository;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;

@ExtendWith(MockitoExtension.class)
class ReglePlateformeServiceTest {

    @Mock
    private ReglePlateformeRepository reglePlateformeRepository;

    @Mock
    private AffectationSalonRepository affectationSalonRepository;

    @Mock
    private NotificationEmailService notificationEmailService;

    @Mock
    private CompteRepository compteRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ReglePlateformeService reglePlateformeService;

    private Compte admin;
    private ReglePlateforme regle;

    @BeforeEach
    void setUp() {
        admin = Compte.builder()
                .id(1L)
                .email("admin@test.com")
                .nom("Admin")
                .prenom("Sys")
                .build();

        regle = ReglePlateforme.builder()
                .id(10L)
                .titre("Respect des règles d'hygiène")
                .description("Tous les salons doivent nettoyer le matériel après chaque client.")
                .actif(true)
                .dateCreation(LocalDateTime.now())
                .build();
    }

    @Test
    void creerRegle_succes_notifieProprietairesUniquesEtLogAudit() {
        ReglePlateformeCreateDTORequest request = new ReglePlateformeCreateDTORequest(
                "Respect des règles d'hygiène",
                "Tous les salons doivent nettoyer le matériel après chaque client."
        );

        when(reglePlateformeRepository.save(any(ReglePlateforme.class))).thenReturn(regle);
        when(compteRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(affectationSalonRepository.findDistinctEmailsByRole(TypeRoleSalon.PROPRIETAIRE))
                .thenReturn(List.of("proprio1@test.com", "proprio2@test.com"));

        ReglePlateformeDTOResponse response = reglePlateformeService.creerRegle(request, "admin@test.com");

        assertNotNull(response);
        assertEquals("Respect des règles d'hygiène", response.getTitre());
        verify(notificationEmailService).queueNotificationEmail(eq("proprio1@test.com"), contains("Nouvelle règle"), any());
        verify(notificationEmailService).queueNotificationEmail(eq("proprio2@test.com"), contains("Nouvelle règle"), any());
        verify(auditLogService).logActionPlateforme(eq(TypeActionAudit.CREATION), eq("ReglePlateforme"), eq("10"), any(), any(), eq(admin), eq("ADMIN_SYSTEME"));
    }

    @Test
    void modifierRegle_succes() {
        ReglePlateformeUpdateDTORequest request = new ReglePlateformeUpdateDTORequest(
                "Nouveau Titre",
                "Nouvelle Description"
        );

        when(reglePlateformeRepository.findById(10L)).thenReturn(Optional.of(regle));
        when(reglePlateformeRepository.save(any(ReglePlateforme.class))).thenReturn(regle);
        when(compteRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(affectationSalonRepository.findDistinctEmailsByRole(TypeRoleSalon.PROPRIETAIRE))
                .thenReturn(List.of("proprio1@test.com"));

        ReglePlateformeDTOResponse response = reglePlateformeService.modifierRegle(10L, request, "admin@test.com");

        assertNotNull(response);
        verify(auditLogService).logActionPlateforme(eq(TypeActionAudit.MODIFICATION), eq("ReglePlateforme"), eq("10"), any(), any(), eq(admin), eq("ADMIN_SYSTEME"));
    }

    @Test
    void desactiverRegle_succes() {
        when(reglePlateformeRepository.findById(10L)).thenReturn(Optional.of(regle));
        when(reglePlateformeRepository.save(any(ReglePlateforme.class))).thenReturn(regle);
        when(compteRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(affectationSalonRepository.findDistinctEmailsByRole(TypeRoleSalon.PROPRIETAIRE))
                .thenReturn(List.of("proprio1@test.com"));

        ReglePlateformeDTOResponse response = reglePlateformeService.desactiverRegle(10L, "admin@test.com");

        assertFalse(response.getActif());
        verify(auditLogService).logActionPlateforme(eq(TypeActionAudit.DESACTIVATION), eq("ReglePlateforme"), eq("10"), any(), any(), eq(admin), eq("ADMIN_SYSTEME"));
    }

    @Test
    void activerRegle_succes() {
        regle.setActif(false);
        when(reglePlateformeRepository.findById(10L)).thenReturn(Optional.of(regle));
        when(reglePlateformeRepository.save(any(ReglePlateforme.class))).thenReturn(regle);
        when(compteRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(affectationSalonRepository.findDistinctEmailsByRole(TypeRoleSalon.PROPRIETAIRE))
                .thenReturn(List.of("proprio1@test.com"));

        ReglePlateformeDTOResponse response = reglePlateformeService.activerRegle(10L, "admin@test.com");

        assertTrue(response.getActif());
        verify(auditLogService).logActionPlateforme(eq(TypeActionAudit.REACTIVATION), eq("ReglePlateforme"), eq("10"), any(), any(), eq(admin), eq("ADMIN_SYSTEME"));
    }
}

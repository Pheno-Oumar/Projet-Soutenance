package com.kadi_aon.mon_salon.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.kadi_aon.mon_salon.audit.dto.AuditLogDTOResponse;
import com.kadi_aon.mon_salon.audit.entity.AuditLog;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.repository.AuditLogRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class AuditLogAdminServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private SalonRepository salonRepository;

    @InjectMocks
    private AuditLogAdminService auditLogAdminService;

    private AuditLog logPlateforme;
    private AuditLog logSalon;

    @BeforeEach
    void setUp() {
        Compte compteAdmin = Compte.builder()
                .id(1L)
                .nom("Traore")
                .prenom("Amadou")
                .email("admin@test.com")
                .build();

        logPlateforme = AuditLog.builder()
                .id(100L)
                .action(TypeActionAudit.CHANGEMENT_MDP)
                .entite("Compte")
                .entiteId("1")
                .compte(compteAdmin)
                .dateHeure(LocalDateTime.now())
                .roleUtilise("ADMIN_SYSTEME")
                .adresseIP("127.0.0.1")
                .build();

        Salon salon = Salon.builder()
                .id(10L)
                .nom("Salon Chic")
                .slug("salon-chic")
                .build();

        AffectationSalon affectation = AffectationSalon.builder()
                .id(5L)
                .compte(compteAdmin)
                .salon(salon)
                .build();

        logSalon = AuditLog.builder()
                .id(200L)
                .action(TypeActionAudit.CREATION)
                .entite("RegleSalon")
                .entiteId("3")
                .affectationSalon(affectation)
                .dateHeure(LocalDateTime.now())
                .roleUtilise("PROPRIETAIRE")
                .adresseIP("192.168.1.50")
                .build();
    }

    @Test
    void listerTousLesLogs_plateforme() {
        when(auditLogRepository.findAllByOrderByDateHeureDesc()).thenReturn(List.of(logPlateforme));

        List<AuditLogDTOResponse> result = auditLogAdminService.listerTousLesLogs();

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
        assertEquals("admin@test.com", result.get(0).getCompteEmail());
    }

    @Test
    void filtrerLogsPlateforme() {
        when(auditLogRepository.filtrerLogsPlateforme(eq(TypeActionAudit.CHANGEMENT_MDP), any(), any(), any()))
                .thenReturn(List.of(logPlateforme));

        List<AuditLogDTOResponse> result = auditLogAdminService.filtrerLogsPlateforme(
                TypeActionAudit.CHANGEMENT_MDP, null, null, null);

        assertEquals(1, result.size());
        assertEquals(TypeActionAudit.CHANGEMENT_MDP, result.get(0).getAction());
    }

    @Test
    void rechercherLogsPlateforme() {
        when(auditLogRepository.rechercherLogsPlateforme("MDP")).thenReturn(List.of(logPlateforme));

        List<AuditLogDTOResponse> result = auditLogAdminService.rechercherLogsPlateforme("MDP");

        assertEquals(1, result.size());
    }

    @Test
    void listerActionsSensiblesPlateforme() {
        when(auditLogRepository.findActionsSensibles(any(), any())).thenReturn(List.of(logPlateforme));

        List<AuditLogDTOResponse> result = auditLogAdminService.listerActionsSensiblesPlateforme();

        assertEquals(1, result.size());
    }

    @Test
    void obtenirLogPlateforme_succes() {
        when(auditLogRepository.findById(100L)).thenReturn(Optional.of(logPlateforme));

        AuditLogDTOResponse response = auditLogAdminService.obtenirLogPlateforme(100L);

        assertNotNull(response);
        assertEquals(100L, response.getId());
    }

    @Test
    void obtenirLogPlateforme_nonTrouve_lanceException() {
        when(auditLogRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> auditLogAdminService.obtenirLogPlateforme(999L));
    }

    @Test
    void listerLogsSalon_succes() {
        when(salonRepository.existsBySlug("salon-chic")).thenReturn(true);
        when(auditLogRepository.findByAffectationSalonSalonSlugOrderByDateHeureDesc("salon-chic")).thenReturn(List.of(logSalon));

        List<AuditLogDTOResponse> result = auditLogAdminService.listerLogsSalon("salon-chic");

        assertEquals(1, result.size());
        assertEquals(200L, result.get(0).getId());
        assertEquals("salon-chic", result.get(0).getSalonSlug());
    }

    @Test
    void listerLogsSalon_salonInexistant_lanceException() {
        when(salonRepository.existsBySlug("inconnu")).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> auditLogAdminService.listerLogsSalon("inconnu"));
    }

    @Test
    void obtenirLogSalon_succes() {
        when(salonRepository.existsBySlug("salon-chic")).thenReturn(true);
        when(auditLogRepository.findByIdAndAffectationSalonSalonSlug(200L, "salon-chic")).thenReturn(Optional.of(logSalon));

        AuditLogDTOResponse result = auditLogAdminService.obtenirLogSalon("salon-chic", 200L);

        assertNotNull(result);
        assertEquals(200L, result.getId());
    }
}

package com.kadi_aon.mon_salon.rgpd.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.common.service.CloudinaryService;
import com.kadi_aon.mon_salon.notification.service.NotificationEmailService;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeExportDTORequest;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeExportDTOResponse;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeSuppressionDTORequest;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeSuppressionDTOResponse;
import com.kadi_aon.mon_salon.rgpd.entity.DemandeExportDonnees;
import com.kadi_aon.mon_salon.rgpd.entity.DemandeSuppressionCompte;
import com.kadi_aon.mon_salon.rgpd.enums.FormatExportDonnees;
import com.kadi_aon.mon_salon.rgpd.enums.StatutDemandeSuppression;
import com.kadi_aon.mon_salon.rgpd.enums.StatutExportDonnees;
import com.kadi_aon.mon_salon.rgpd.repository.DemandeExportDonneesRepository;
import com.kadi_aon.mon_salon.rgpd.repository.DemandeSuppressionCompteRepository;

@ExtendWith(MockitoExtension.class)
class RgpdClientServiceTest {

    @Mock
    private DemandeSuppressionCompteRepository demandeSuppressionRepository;

    @Mock
    private DemandeExportDonneesRepository demandeExportRepository;

    @Mock
    private CompteRepository compteRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private NotificationEmailService notificationEmailService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private RgpdClientService rgpdClientService;

    private Compte compte;

    @BeforeEach
    void setUp() {
        compte = Compte.builder()
                .id(1L)
                .nom("Diallo")
                .prenom("Mamadou")
                .email("mamadou@test.com")
                .telephone("+22370000000")
                .statut(true)
                .dateCreation(LocalDateTime.now())
                .build();
    }

    @Test
    void demanderSuppressionCompte_Succes() {
        when(compteRepository.findByEmail("mamadou@test.com")).thenReturn(Optional.of(compte));
        when(demandeSuppressionRepository.existsByCompteEmailAndStatut("mamadou@test.com", StatutDemandeSuppression.EN_ATTENTE))
                .thenReturn(false);
        when(demandeSuppressionRepository.save(any(DemandeSuppressionCompte.class))).thenAnswer(inv -> {
            DemandeSuppressionCompte d = inv.getArgument(0);
            d.setId(10L);
            return d;
        });

        DemandeSuppressionDTORequest req = new DemandeSuppressionDTORequest("Je déménage");
        DemandeSuppressionDTOResponse resp = rgpdClientService.demanderSuppressionCompte("mamadou@test.com", req);

        assertNotNull(resp);
        assertEquals(10L, resp.id());
        assertEquals("EN_ATTENTE", resp.statut());
        assertEquals("Je déménage", resp.motif());
        verify(demandeSuppressionRepository).save(any(DemandeSuppressionCompte.class));
    }

    @Test
    void demanderSuppressionCompte_DejaEnAttente_LanceException() {
        when(compteRepository.findByEmail("mamadou@test.com")).thenReturn(Optional.of(compte));
        when(demandeSuppressionRepository.existsByCompteEmailAndStatut("mamadou@test.com", StatutDemandeSuppression.EN_ATTENTE))
                .thenReturn(true);

        DemandeSuppressionDTORequest req = new DemandeSuppressionDTORequest("Doublon");

        assertThrows(IllegalStateException.class, () ->
                rgpdClientService.demanderSuppressionCompte("mamadou@test.com", req));
    }

    @Test
    void demanderExportDonnees_JSON_Succes() throws Exception {
        when(compteRepository.findByEmail("mamadou@test.com")).thenReturn(Optional.of(compte));
        when(demandeExportRepository.save(any(DemandeExportDonnees.class))).thenAnswer(inv -> {
            DemandeExportDonnees d = inv.getArgument(0);
            if (d.getId() == null) d.setId(20L);
            return d;
        });

        when(cloudinaryService.uploadExportRgpd(any(byte[].class), any(String.class)))
                .thenReturn(Map.of("secure_url", "https://cloudinary.com/export.json", "public_id", "rgpd_export_123"));

        DemandeExportDTORequest req = new DemandeExportDTORequest(FormatExportDonnees.JSON);
        DemandeExportDTOResponse resp = rgpdClientService.demanderExportDonnees("mamadou@test.com", req);

        assertNotNull(resp);
        assertEquals("JSON", resp.format());
        assertEquals("DISPONIBLE", resp.statut());
        assertEquals("https://cloudinary.com/export.json", resp.urlTelechargement());
        assertFalse(resp.expire());
        verify(notificationEmailService).queueNotificationEmail(eq("mamadou@test.com"), any(), any());
    }

    @Test
    void demanderExportDonnees_CSV_Succes() throws Exception {
        when(compteRepository.findByEmail("mamadou@test.com")).thenReturn(Optional.of(compte));
        when(demandeExportRepository.save(any(DemandeExportDonnees.class))).thenAnswer(inv -> {
            DemandeExportDonnees d = inv.getArgument(0);
            if (d.getId() == null) d.setId(21L);
            return d;
        });

        when(cloudinaryService.uploadExportRgpd(any(byte[].class), any(String.class)))
                .thenReturn(Map.of("secure_url", "https://cloudinary.com/export.csv", "public_id", "rgpd_export_456"));

        DemandeExportDTORequest req = new DemandeExportDTORequest(FormatExportDonnees.CSV);
        DemandeExportDTOResponse resp = rgpdClientService.demanderExportDonnees("mamadou@test.com", req);

        assertNotNull(resp);
        assertEquals("CSV", resp.format());
        assertEquals("DISPONIBLE", resp.statut());
        assertEquals("https://cloudinary.com/export.csv", resp.urlTelechargement());
    }

    @Test
    void consulterMesExports_Succes() {
        DemandeExportDonnees exp = DemandeExportDonnees.builder()
                .id(1L)
                .compte(compte)
                .format(FormatExportDonnees.JSON)
                .statut(StatutExportDonnees.DISPONIBLE)
                .dateDemande(LocalDateTime.now().minusDays(3))
                .dateExpiration(LocalDateTime.now().minusDays(1)) // Expired
                .urlTelechargement("https://cloudinary.com/old.json")
                .build();

        when(demandeExportRepository.findByCompteEmailOrderByDateDemandeDesc("mamadou@test.com"))
                .thenReturn(List.of(exp));

        List<DemandeExportDTOResponse> list = rgpdClientService.consulterMesExports("mamadou@test.com");

        assertEquals(1, list.size());
        assertTrue(list.get(0).expire());
        assertEquals("EXPIRE", list.get(0).statut());
    }
}

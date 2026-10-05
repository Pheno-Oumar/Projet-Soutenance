package com.kadi_aon.mon_salon.rgpd.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.notification.service.NotificationEmailService;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeSuppressionDecisionDTORequest;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeSuppressionDTOResponse;
import com.kadi_aon.mon_salon.rgpd.entity.DemandeSuppressionCompte;
import com.kadi_aon.mon_salon.rgpd.enums.StatutDemandeSuppression;
import com.kadi_aon.mon_salon.rgpd.repository.DemandeSuppressionCompteRepository;

@ExtendWith(MockitoExtension.class)
class RgpdAdminServiceTest {

    @Mock
    private DemandeSuppressionCompteRepository demandeSuppressionRepository;

    @Mock
    private CompteRepository compteRepository;

    @Mock
    private NotificationEmailService notificationEmailService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private RgpdAdminService rgpdAdminService;

    private Compte adminCompte;
    private Compte clientCompte;
    private DemandeSuppressionCompte demande;

    @BeforeEach
    void setUp() {
        adminCompte = Compte.builder()
                .id(99L)
                .nom("Admin")
                .prenom("Super")
                .email("admin@test.com")
                .build();

        clientCompte = Compte.builder()
                .id(10L)
                .nom("Traore")
                .prenom("Aicha")
                .email("aicha@test.com")
                .telephone("+22371234567")
                .statut(true)
                .build();

        demande = DemandeSuppressionCompte.builder()
                .id(1L)
                .compte(clientCompte)
                .motif("Départ à l'étranger")
                .statut(StatutDemandeSuppression.EN_ATTENTE)
                .dateDemande(LocalDateTime.now())
                .build();
    }

    @Test
    void listerDemandesSuppression_Succes() {
        when(demandeSuppressionRepository.findAllByOrderByDateDemandeDesc()).thenReturn(List.of(demande));

        List<DemandeSuppressionDTOResponse> list = rgpdAdminService.listerDemandesSuppression(null);

        assertEquals(1, list.size());
        assertEquals("aicha@test.com", list.get(0).compteEmail());
    }

    @Test
    void traiterDemandeSuppression_Approbation_Succes() {
        when(compteRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminCompte));
        when(demandeSuppressionRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(demandeSuppressionRepository.save(any(DemandeSuppressionCompte.class))).thenAnswer(inv -> inv.getArgument(0));

        DemandeSuppressionDecisionDTORequest req = new DemandeSuppressionDecisionDTORequest(true, "Approuvé par conformité");
        DemandeSuppressionDTOResponse resp = rgpdAdminService.traiterDemandeSuppression(1L, "admin@test.com", req);

        assertNotNull(resp);
        assertEquals("APPROUVEE", resp.statut());
        assertEquals("admin@test.com", resp.traiteParAdminEmail());

        // Vérification de l'anonymisation du compte
        assertEquals("ANONYME", clientCompte.getNom());
        assertEquals("ANONYME", clientCompte.getPrenom());
        assertEquals("anonyme_10@rgpd.supprime", clientCompte.getEmail());
        assertEquals("0000000000", clientCompte.getTelephone());
        assertFalse(clientCompte.getStatut());

        verify(compteRepository).save(clientCompte);
        verify(notificationEmailService).queueNotificationEmail(eq("aicha@test.com"), any(), any());
    }

    @Test
    void traiterDemandeSuppression_Rejet_Succes() {
        when(compteRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminCompte));
        when(demandeSuppressionRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(demandeSuppressionRepository.save(any(DemandeSuppressionCompte.class))).thenAnswer(inv -> inv.getArgument(0));

        DemandeSuppressionDecisionDTORequest req = new DemandeSuppressionDecisionDTORequest(false, "Paiement en attente non soldé");
        DemandeSuppressionDTOResponse resp = rgpdAdminService.traiterDemandeSuppression(1L, "admin@test.com", req);

        assertNotNull(resp);
        assertEquals("REJETEE", resp.statut());
        assertEquals("Paiement en attente non soldé", resp.motifDecision());

        // Le compte ne doit pas être anonymisé
        assertEquals("Traore", clientCompte.getNom());
        verify(notificationEmailService).queueNotificationEmail(eq("aicha@test.com"), any(), any());
    }

    @Test
    void traiterDemandeSuppression_DejaTraitee_LanceException() {
        demande.setStatut(StatutDemandeSuppression.APPROUVEE);
        when(compteRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminCompte));
        when(demandeSuppressionRepository.findById(1L)).thenReturn(Optional.of(demande));

        DemandeSuppressionDecisionDTORequest req = new DemandeSuppressionDecisionDTORequest(true, "Test");

        assertThrows(IllegalStateException.class, () ->
                rgpdAdminService.traiterDemandeSuppression(1L, "admin@test.com", req));
    }
}

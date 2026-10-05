package com.kadi_aon.mon_salon.regle.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.notification.service.NotificationEmailService;
import com.kadi_aon.mon_salon.regle.dto.RegleSalonCreateDTORequest;
import com.kadi_aon.mon_salon.regle.dto.RegleSalonDTOResponse;
import com.kadi_aon.mon_salon.regle.dto.RegleSalonUpdateDTORequest;
import com.kadi_aon.mon_salon.regle.entity.RegleSalon;
import com.kadi_aon.mon_salon.regle.repository.RegleSalonRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

@ExtendWith(MockitoExtension.class)
class RegleSalonServiceTest {

    @Mock
    private RegleSalonRepository regleSalonRepository;

    @Mock
    private SalonRepository salonRepository;

    @Mock
    private AffectationSalonRepository affectationSalonRepository;

    @Mock
    private NotificationEmailService notificationEmailService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private RegleSalonService regleSalonService;

    private Salon salon;
    private RegleSalon regleSalon;
    private AffectationSalon affectation;

    @BeforeEach
    void setUp() {
        salon = Salon.builder()
                .id(1L)
                .nom("Salon Moderne")
                .slug("salon-moderne")
                .build();

        Compte proprio = Compte.builder()
                .id(2L)
                .email("proprio@test.com")
                .nom("Diop")
                .prenom("Moussa")
                .build();

        affectation = AffectationSalon.builder()
                .id(5L)
                .compte(proprio)
                .salon(salon)
                .build();

        regleSalon = RegleSalon.builder()
                .id(20L)
                .titre("Arriver 10 minutes avant le rendez-vous")
                .description("Pour garantir la ponctualité des prestations.")
                .salon(salon)
                .dateCreation(LocalDateTime.now())
                .build();
    }

    @Test
    void creerRegle_succes_notifieClientsDuSalonEtLogAudit() {
        RegleSalonCreateDTORequest request = new RegleSalonCreateDTORequest(
                "Arriver 10 minutes avant le rendez-vous",
                "Pour garantir la ponctualité des prestations."
        );

        when(salonRepository.findBySlug("salon-moderne")).thenReturn(Optional.of(salon));
        when(regleSalonRepository.save(any(RegleSalon.class))).thenReturn(regleSalon);
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-moderne"))
                .thenReturn(Optional.of(affectation));
        when(affectationSalonRepository.findDistinctEmailsBySalonSlugAndRole("salon-moderne", TypeRoleSalon.CLIENT))
                .thenReturn(List.of("client1@test.com", "client2@test.com"));

        RegleSalonDTOResponse response = regleSalonService.creerRegle("salon-moderne", request, "proprio@test.com");

        assertNotNull(response);
        assertEquals("Arriver 10 minutes avant le rendez-vous", response.getTitre());
        verify(notificationEmailService).queueNotificationEmail(eq("client1@test.com"), contains("Nouvelle règle"), any());
        verify(notificationEmailService).queueNotificationEmail(eq("client2@test.com"), contains("Nouvelle règle"), any());
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.CREATION), eq("RegleSalon"), eq("20"), any(), any(), eq(affectation), eq("PROPRIETAIRE"));
    }

    @Test
    void modifierRegle_succes() {
        RegleSalonUpdateDTORequest request = new RegleSalonUpdateDTORequest(
                "Nouveau Titre",
                "Nouvelle Description"
        );

        when(regleSalonRepository.findByIdAndSalonSlug(20L, "salon-moderne")).thenReturn(Optional.of(regleSalon));
        when(regleSalonRepository.save(any(RegleSalon.class))).thenReturn(regleSalon);
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-moderne"))
                .thenReturn(Optional.of(affectation));
        when(affectationSalonRepository.findDistinctEmailsBySalonSlugAndRole("salon-moderne", TypeRoleSalon.CLIENT))
                .thenReturn(List.of("client1@test.com"));

        RegleSalonDTOResponse response = regleSalonService.modifierRegle("salon-moderne", 20L, request, "proprio@test.com");

        assertNotNull(response);
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.MODIFICATION), eq("RegleSalon"), eq("20"), any(), any(), eq(affectation), eq("PROPRIETAIRE"));
    }

    @Test
    void supprimerRegle_succes() {
        when(regleSalonRepository.findByIdAndSalonSlug(20L, "salon-moderne")).thenReturn(Optional.of(regleSalon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-moderne"))
                .thenReturn(Optional.of(affectation));

        regleSalonService.supprimerRegle("salon-moderne", 20L, "proprio@test.com");

        verify(regleSalonRepository).delete(regleSalon);
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.SUPPRESSION), eq("RegleSalon"), eq("20"), any(), any(), eq(affectation), eq("PROPRIETAIRE"));
    }
}

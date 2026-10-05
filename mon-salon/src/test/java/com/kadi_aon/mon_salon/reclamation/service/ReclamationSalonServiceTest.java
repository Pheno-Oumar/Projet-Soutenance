package com.kadi_aon.mon_salon.reclamation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.notification.service.NotificationEmailService;
import com.kadi_aon.mon_salon.reclamation.dto.ReclamationCreateDTORequest;
import com.kadi_aon.mon_salon.reclamation.dto.ReclamationDTOResponse;
import com.kadi_aon.mon_salon.reclamation.dto.ReclamationTraiterDTORequest;
import com.kadi_aon.mon_salon.reclamation.entity.Reclamation;
import com.kadi_aon.mon_salon.reclamation.enums.StatutReclamation;
import com.kadi_aon.mon_salon.reclamation.repository.ReclamationRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

@ExtendWith(MockitoExtension.class)
class ReclamationSalonServiceTest {

    @Mock
    private ReclamationRepository reclamationRepository;

    @Mock
    private AffectationSalonRepository affectationSalonRepository;

    @Mock
    private SalonRepository salonRepository;

    @Mock
    private NotificationEmailService notificationEmailService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ReclamationSalonService reclamationSalonService;

    private Salon salon;
    private Compte clientCompte;
    private AffectationSalon clientAffectation;
    private Compte managerCompte;
    private AffectationSalon managerAffectation;
    private Reclamation reclamation;

    @BeforeEach
    void setUp() {
        salon = Salon.builder().id(1L).nom("Salon Chic").slug("salon-chic").build();

        clientCompte = Compte.builder().id(10L).nom("Kone").prenom("Awa").email("awa@test.com").build();
        RoleSalon roleClient = RoleSalon.builder().id(101L).role(TypeRoleSalon.CLIENT).build();
        Set<RoleSalon> rolesClient = new HashSet<>();
        rolesClient.add(roleClient);

        clientAffectation = AffectationSalon.builder()
                .id(20L)
                .salon(salon)
                .compte(clientCompte)
                .statut(true)
                .roles(rolesClient)
                .build();

        managerCompte = Compte.builder().id(11L).nom("Traore").prenom("Ousmane").email("manager@test.com").build();
        RoleSalon roleManager = RoleSalon.builder().id(102L).role(TypeRoleSalon.MANAGER).build();
        Set<RoleSalon> rolesManager = new HashSet<>();
        rolesManager.add(roleManager);

        managerAffectation = AffectationSalon.builder()
                .id(21L)
                .salon(salon)
                .compte(managerCompte)
                .statut(true)
                .roles(rolesManager)
                .build();

        reclamation = Reclamation.builder()
                .id(50L)
                .affectationClient(clientAffectation)
                .salon(salon)
                .objet("Retard coiffeur")
                .description("Le coiffeur avait 30 minutes de retard.")
                .statut(StatutReclamation.EN_ATTENTE)
                .dateCreation(LocalDateTime.now())
                .build();
    }

    @Test
    void deposerReclamation_Succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("awa@test.com", "salon-chic"))
                .thenReturn(Optional.of(clientAffectation));
        when(reclamationRepository.save(any(Reclamation.class))).thenAnswer(inv -> {
            Reclamation r = inv.getArgument(0);
            r.setId(51L);
            return r;
        });

        ReclamationCreateDTORequest req = new ReclamationCreateDTORequest("Attente longue", "Plus d'une heure d'attente");
        ReclamationDTOResponse resp = reclamationSalonService.deposerReclamation("salon-chic", "awa@test.com", req);

        assertNotNull(resp);
        assertEquals("Attente longue", resp.objet());
        assertEquals("EN_ATTENTE", resp.statut());
        assertEquals("Awa Kone", resp.clientNomComplet());
        verify(reclamationRepository).save(any(Reclamation.class));
    }

    @Test
    void listerReclamationsClient_Succes() {
        when(salonRepository.existsBySlug("salon-chic")).thenReturn(true);
        when(reclamationRepository.findByAffectationClientCompteEmailAndSalonSlugOrderByDateCreationDesc("awa@test.com", "salon-chic"))
                .thenReturn(List.of(reclamation));

        List<ReclamationDTOResponse> list = reclamationSalonService.listerReclamationsClient("salon-chic", "awa@test.com");

        assertEquals(1, list.size());
        assertEquals("Retard coiffeur", list.get(0).objet());
    }

    @Test
    void traiterReclamation_Resolution_Succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("manager@test.com", "salon-chic"))
                .thenReturn(Optional.of(managerAffectation));
        when(reclamationRepository.findByIdAndSalonSlug(50L, "salon-chic")).thenReturn(Optional.of(reclamation));
        when(reclamationRepository.save(any(Reclamation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReclamationTraiterDTORequest req = new ReclamationTraiterDTORequest(StatutReclamation.RESOLUE, "Nous vous offrons un soin offert.");
        ReclamationDTOResponse resp = reclamationSalonService.traiterReclamation("salon-chic", 50L, "manager@test.com", req);

        assertNotNull(resp);
        assertEquals("RESOLUE", resp.statut());
        assertEquals("Nous vous offrons un soin offert.", resp.reponseTraitement());
        assertEquals("Ousmane Traore", resp.traiteParNomComplet());

        verify(notificationEmailService).queueNotificationEmail(
                eq("awa@test.com"),
                eq("Mise à jour de votre réclamation - Salon Chic"),
                any()
        );
    }

    @Test
    void traiterReclamation_Rejet_Succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("manager@test.com", "salon-chic"))
                .thenReturn(Optional.of(managerAffectation));
        when(reclamationRepository.findByIdAndSalonSlug(50L, "salon-chic")).thenReturn(Optional.of(reclamation));
        when(reclamationRepository.save(any(Reclamation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReclamationTraiterDTORequest req = new ReclamationTraiterDTORequest(StatutReclamation.REJETEE, "Le coiffeur était dans les délais convenus.");
        ReclamationDTOResponse resp = reclamationSalonService.traiterReclamation("salon-chic", 50L, "manager@test.com", req);

        assertNotNull(resp);
        assertEquals("REJETEE", resp.statut());
        verify(notificationEmailService).queueNotificationEmail(eq("awa@test.com"), any(), any());
    }

    @Test
    void traiterReclamation_DejaTraitee_LanceException() {
        reclamation.setStatut(StatutReclamation.RESOLUE);
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("manager@test.com", "salon-chic"))
                .thenReturn(Optional.of(managerAffectation));
        when(reclamationRepository.findByIdAndSalonSlug(50L, "salon-chic")).thenReturn(Optional.of(reclamation));

        ReclamationTraiterDTORequest req = new ReclamationTraiterDTORequest(StatutReclamation.RESOLUE, "Réponse");

        assertThrows(IllegalStateException.class, () ->
                reclamationSalonService.traiterReclamation("salon-chic", 50L, "manager@test.com", req));
    }
}

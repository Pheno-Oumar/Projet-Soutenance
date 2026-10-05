package com.kadi_aon.mon_salon.avis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashSet;
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
import com.kadi_aon.mon_salon.avis.dto.AvisModerationDTORequest;
import com.kadi_aon.mon_salon.avis.dto.AvisPrestationCreateDTORequest;
import com.kadi_aon.mon_salon.avis.dto.AvisPrestationDTOResponse;
import com.kadi_aon.mon_salon.avis.dto.AvisPrestationUpdateDTORequest;
import com.kadi_aon.mon_salon.avis.dto.AvisSalonCreateDTORequest;
import com.kadi_aon.mon_salon.avis.dto.AvisSalonDTOResponse;
import com.kadi_aon.mon_salon.avis.dto.AvisSalonUpdateDTORequest;
import com.kadi_aon.mon_salon.avis.entity.AvisPrestation;
import com.kadi_aon.mon_salon.avis.entity.AvisSalon;
import com.kadi_aon.mon_salon.avis.repository.AvisPrestationRepository;
import com.kadi_aon.mon_salon.avis.repository.AvisSalonRepository;
import com.kadi_aon.mon_salon.prestation.entity.LignePrestation;
import com.kadi_aon.mon_salon.prestation.entity.Prestation;
import com.kadi_aon.mon_salon.prestation.enums.StatutPrestation;
import com.kadi_aon.mon_salon.prestation.repository.LignePrestationRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

@ExtendWith(MockitoExtension.class)
class AvisSalonServiceTest {

    @Mock
    private AvisPrestationRepository avisPrestationRepository;
    @Mock
    private AvisSalonRepository avisSalonRepository;
    @Mock
    private LignePrestationRepository lignePrestationRepository;
    @Mock
    private AffectationSalonRepository affectationSalonRepository;
    @Mock
    private SalonRepository salonRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AvisSalonService avisSalonService;

    private Salon salon;
    private Compte client;
    private Compte manager;
    private AffectationSalon affectationClient;
    private AffectationSalon affectationManager;
    private Prestation prestation;
    private LignePrestation lignePrestation;

    @BeforeEach
    void setUp() {
        salon = Salon.builder().id(1L).slug("salon-chic").nom("Salon Chic").build();
        client = Compte.builder().id(10L).nom("Dupont").prenom("Alice").email("alice@test.com").build();
        manager = Compte.builder().id(20L).nom("Boss").prenom("Marc").email("manager@test.com").build();

        RoleSalon roleClient = RoleSalon.builder().id(1L).role(TypeRoleSalon.CLIENT).build();
        Set<RoleSalon> rolesClient = new HashSet<>();
        rolesClient.add(roleClient);

        affectationClient = AffectationSalon.builder()
                .id(100L)
                .salon(salon)
                .compte(client)
                .roles(rolesClient)
                .statut(true)
                .build();

        RoleSalon roleManager = RoleSalon.builder().id(2L).role(TypeRoleSalon.MANAGER).build();
        Set<RoleSalon> rolesManager = new HashSet<>();
        rolesManager.add(roleManager);

        affectationManager = AffectationSalon.builder()
                .id(200L)
                .salon(salon)
                .compte(manager)
                .roles(rolesManager)
                .statut(true)
                .build();

        prestation = Prestation.builder()
                .id(50L)
                .salon(salon)
                .client(client)
                .statut(StatutPrestation.TERMINEE)
                .build();

        lignePrestation = LignePrestation.builder()
                .id(501L)
                .prestation(prestation)
                .build();
    }

    @Test
    void creerAvisPrestation_succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("alice@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationClient));
        when(lignePrestationRepository.findById(501L)).thenReturn(Optional.of(lignePrestation));
        when(avisPrestationRepository.existsByClientIdAndLignePrestationId(10L, 501L)).thenReturn(false);

        when(avisPrestationRepository.save(any(AvisPrestation.class))).thenAnswer(invocation -> {
            AvisPrestation a = invocation.getArgument(0);
            a.setId(1L);
            return a;
        });

        AvisPrestationCreateDTORequest request = new AvisPrestationCreateDTORequest(501L, 5, "Prestation impeccable !");
        AvisPrestationDTOResponse response = avisSalonService.creerAvisPrestation("salon-chic", "alice@test.com", request);

        assertNotNull(response);
        assertEquals(5, response.note());
        assertEquals("Prestation impeccable !", response.commentaire());
        assertFalse(response.statut()); // 🔥 Par défaut false en attente modération
    }

    @Test
    void creerAvisPrestation_nonTerminee_lanceIllegalStateException() {
        prestation.setStatut(StatutPrestation.EN_COURS);
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("alice@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationClient));
        when(lignePrestationRepository.findById(501L)).thenReturn(Optional.of(lignePrestation));

        AvisPrestationCreateDTORequest request = new AvisPrestationCreateDTORequest(501L, 5, "Super");

        assertThrows(IllegalStateException.class, () ->
                avisSalonService.creerAvisPrestation("salon-chic", "alice@test.com", request));
    }

    @Test
    void modifierAvisPrestation_succes_remetStatutFalse() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("alice@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationClient));

        AvisPrestation existant = AvisPrestation.builder()
                .id(1L)
                .client(client)
                .lignePrestation(lignePrestation)
                .note(4)
                .commentaire("Bien")
                .statut(true) // Était validé
                .dateCreation(LocalDateTime.now().minusDays(1))
                .build();

        when(avisPrestationRepository.findByIdAndClientEmailAndLignePrestationPrestationSalonSlug(1L, "alice@test.com", "salon-chic"))
                .thenReturn(Optional.of(existant));
        when(avisPrestationRepository.save(any(AvisPrestation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AvisPrestationUpdateDTORequest request = new AvisPrestationUpdateDTORequest(5, "En fait exceptionnel !");
        AvisPrestationDTOResponse response = avisSalonService.modifierAvisPrestation("salon-chic", 1L, "alice@test.com", request);

        assertNotNull(response);
        assertEquals(5, response.note());
        assertEquals("En fait exceptionnel !", response.commentaire());
        assertFalse(response.statut()); // 🔥 Repasse bien à false après modif client
    }

    @Test
    void modererAvisPrestation_parManager_succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("manager@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationManager));

        AvisPrestation existant = AvisPrestation.builder()
                .id(1L)
                .client(client)
                .lignePrestation(lignePrestation)
                .note(5)
                .commentaire("Très bien")
                .statut(false)
                .build();

        when(avisPrestationRepository.findByIdAndLignePrestationPrestationSalonSlug(1L, "salon-chic"))
                .thenReturn(Optional.of(existant));
        when(avisPrestationRepository.save(any(AvisPrestation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AvisModerationDTORequest request = new AvisModerationDTORequest(true);
        AvisPrestationDTOResponse response = avisSalonService.modererAvisPrestation("salon-chic", 1L, "manager@test.com", request);

        assertNotNull(response);
        assertTrue(response.statut());
        assertEquals(5, response.note()); // Note et commentaire non altérés
        assertEquals("Très bien", response.commentaire());
    }

    @Test
    void creerAvisSalon_succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("alice@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationClient));
        when(avisSalonRepository.existsByClientIdAndSalonId(10L, 1L)).thenReturn(false);

        when(avisSalonRepository.save(any(AvisSalon.class))).thenAnswer(invocation -> {
            AvisSalon a = invocation.getArgument(0);
            a.setId(2L);
            return a;
        });

        AvisSalonCreateDTORequest request = new AvisSalonCreateDTORequest(5, "Superbe salon !");
        AvisSalonDTOResponse response = avisSalonService.creerAvisSalon("salon-chic", "alice@test.com", request);

        assertNotNull(response);
        assertEquals(5, response.note());
        assertFalse(response.statut());
    }

    @Test
    void modifierAvisSalon_succes_remetStatutFalse() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("alice@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationClient));

        AvisSalon existant = AvisSalon.builder()
                .id(2L)
                .client(client)
                .salon(salon)
                .note(3)
                .commentaire("Moyen")
                .statut(true)
                .build();

        when(avisSalonRepository.findByClientEmailAndSalonSlug("alice@test.com", "salon-chic"))
                .thenReturn(Optional.of(existant));
        when(avisSalonRepository.save(any(AvisSalon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AvisSalonUpdateDTORequest request = new AvisSalonUpdateDTORequest(5, "Beaucoup mieux maintenant");
        AvisSalonDTOResponse response = avisSalonService.modifierAvisSalon("salon-chic", "alice@test.com", request);

        assertNotNull(response);
        assertEquals(5, response.note());
        assertFalse(response.statut()); // 🔥 Repasse à false
    }
}

package com.kadi_aon.mon_salon.salon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.salon.dto.FermetureExceptionnelleDTORequest;
import com.kadi_aon.mon_salon.salon.dto.FermetureExceptionnelleDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.HoraireOuvertureDTORequest;
import com.kadi_aon.mon_salon.salon.dto.HoraireOuvertureDTOResponse;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.FermetureExceptionnelle;
import com.kadi_aon.mon_salon.salon.entity.HoraireOuverture;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.JourSemaine;
import com.kadi_aon.mon_salon.salon.mapper.FermetureExceptionnelleDTOResponseMapper;
import com.kadi_aon.mon_salon.salon.mapper.HoraireOuvertureDTOResponseMapper;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.FermetureExceptionnelleRepository;
import com.kadi_aon.mon_salon.salon.repository.HoraireOuvertureRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

@ExtendWith(MockitoExtension.class)
class HoraireSalonServiceTest {

    @Mock
    private SalonRepository salonRepository;
    @Mock
    private HoraireOuvertureRepository horaireOuvertureRepository;
    @Mock
    private FermetureExceptionnelleRepository fermetureExceptionnelleRepository;
    @Mock
    private AffectationSalonRepository affectationSalonRepository;
    @Mock
    private HoraireOuvertureDTOResponseMapper horaireMapper;
    @Mock
    private FermetureExceptionnelleDTOResponseMapper fermetureMapper;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private HoraireSalonService horaireSalonService;

    private Salon salon;
    private AffectationSalon affectation;

    @BeforeEach
    void setUp() {
        salon = Salon.builder()
                .id(1L)
                .nom("Salon Belle Époque")
                .slug("salon-belle-epoque")
                .statut(true)
                .build();

        affectation = AffectationSalon.builder()
                .id(10L)
                .salon(salon)
                .statut(true)
                .build();
    }

    @Test
    void testDefinirHoraireSuccesAvecPause() {
        HoraireOuvertureDTORequest request = new HoraireOuvertureDTORequest(
                JourSemaine.LUNDI,
                LocalTime.of(9, 0),
                LocalTime.of(19, 0),
                LocalTime.of(12, 30),
                LocalTime.of(13, 30),
                true
        );

        when(salonRepository.findBySlug("salon-belle-epoque")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-belle-epoque"))
                .thenReturn(Optional.of(affectation));
        when(horaireOuvertureRepository.findBySalonIdAndJourSemaine(1L, JourSemaine.LUNDI))
                .thenReturn(Optional.empty());
        when(horaireOuvertureRepository.save(any(HoraireOuverture.class))).thenAnswer(i -> {
            HoraireOuverture h = i.getArgument(0);
            h.setId(100L);
            return h;
        });
        when(horaireMapper.apply(any(HoraireOuverture.class))).thenAnswer(i -> {
            HoraireOuverture h = i.getArgument(0);
            return new HoraireOuvertureDTOResponse(h.getId(), h.getJourSemaine(), h.getHeureOuverture(),
                    h.getHeureFermeture(), h.getPauseDebut(), h.getPauseFin(), h.getActif());
        });

        HoraireOuvertureDTOResponse response = horaireSalonService.definirHoraire("salon-belle-epoque", request, "proprio@test.com");

        assertNotNull(response);
        assertEquals(JourSemaine.LUNDI, response.jourSemaine());
        assertEquals(LocalTime.of(9, 0), response.heureOuverture());
        assertEquals(LocalTime.of(19, 0), response.heureFermeture());
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.CREATION), eq("HoraireOuverture"), eq("100"), any(), any(), eq(affectation), eq("PROPRIETAIRE"));
    }

    @Test
    void testDefinirHoraireRejetHeureOuvertureApresFermeture() {
        HoraireOuvertureDTORequest request = new HoraireOuvertureDTORequest(
                JourSemaine.LUNDI,
                LocalTime.of(20, 0),
                LocalTime.of(8, 0),
                null, null, true
        );

        when(salonRepository.findBySlug("salon-belle-epoque")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-belle-epoque"))
                .thenReturn(Optional.of(affectation));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> horaireSalonService.definirHoraire("salon-belle-epoque", request, "proprio@test.com"));

        assertTrue(ex.getMessage().contains("précéder"));
    }

    @Test
    void testDefinirHoraireRejetPauseHorsPlage() {
        HoraireOuvertureDTORequest request = new HoraireOuvertureDTORequest(
                JourSemaine.LUNDI,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(8, 0),
                LocalTime.of(9, 30),
                true
        );

        when(salonRepository.findBySlug("salon-belle-epoque")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-belle-epoque"))
                .thenReturn(Optional.of(affectation));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> horaireSalonService.definirHoraire("salon-belle-epoque", request, "proprio@test.com"));

        assertTrue(ex.getMessage().contains("strictement comprise"));
    }

    @Test
    void testDefinirHoraireRejetPauseIncomplete() {
        HoraireOuvertureDTORequest request = new HoraireOuvertureDTORequest(
                JourSemaine.LUNDI,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(12, 0),
                null,
                true
        );

        when(salonRepository.findBySlug("salon-belle-epoque")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-belle-epoque"))
                .thenReturn(Optional.of(affectation));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> horaireSalonService.definirHoraire("salon-belle-epoque", request, "proprio@test.com"));

        assertTrue(ex.getMessage().contains("définis conjointement"));
    }

    @Test
    void testAjouterFermetureSucces() {
        FermetureExceptionnelleDTORequest request = new FermetureExceptionnelleDTORequest(
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(12),
                "Fêtes de fin d'année"
        );

        when(salonRepository.findBySlug("salon-belle-epoque")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-belle-epoque"))
                .thenReturn(Optional.of(affectation));
        when(fermetureExceptionnelleRepository.findChevauchements(1L, request.dateDebut(), request.dateFin()))
                .thenReturn(List.of());
        when(fermetureExceptionnelleRepository.save(any(FermetureExceptionnelle.class))).thenAnswer(i -> {
            FermetureExceptionnelle f = i.getArgument(0);
            f.setId(50L);
            return f;
        });
        when(fermetureMapper.apply(any(FermetureExceptionnelle.class))).thenAnswer(i -> {
            FermetureExceptionnelle f = i.getArgument(0);
            return new FermetureExceptionnelleDTOResponse(f.getId(), f.getDateDebut(), f.getDateFin(), f.getMotif(), null);
        });

        FermetureExceptionnelleDTOResponse response = horaireSalonService.ajouterFermeture("salon-belle-epoque", request, "proprio@test.com");

        assertNotNull(response);
        assertEquals("Fêtes de fin d'année", response.motif());
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.CREATION), eq("FermetureExceptionnelle"), eq("50"), any(), any(), eq(affectation), eq("PROPRIETAIRE"));
    }

    @Test
    void testAjouterFermetureRejetDateDebutDansLePasse() {
        FermetureExceptionnelleDTORequest request = new FermetureExceptionnelleDTORequest(
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(2),
                "Fermeture passée"
        );

        when(salonRepository.findBySlug("salon-belle-epoque")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-belle-epoque"))
                .thenReturn(Optional.of(affectation));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> horaireSalonService.ajouterFermeture("salon-belle-epoque", request, "proprio@test.com"));

        assertTrue(ex.getMessage().contains("passé"));
    }

    @Test
    void testAjouterFermetureRejetDateDebutApresDateFin() {
        FermetureExceptionnelleDTORequest request = new FermetureExceptionnelleDTORequest(
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(5),
                "Erreur de dates"
        );

        when(salonRepository.findBySlug("salon-belle-epoque")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-belle-epoque"))
                .thenReturn(Optional.of(affectation));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> horaireSalonService.ajouterFermeture("salon-belle-epoque", request, "proprio@test.com"));

        assertTrue(ex.getMessage().contains("postérieure"));
    }

    @Test
    void testAjouterFermetureRejetChevauchement() {
        FermetureExceptionnelleDTORequest request = new FermetureExceptionnelleDTORequest(
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(12),
                "Noël"
        );

        when(salonRepository.findBySlug("salon-belle-epoque")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-belle-epoque"))
                .thenReturn(Optional.of(affectation));
        when(fermetureExceptionnelleRepository.findChevauchements(1L, request.dateDebut(), request.dateFin()))
                .thenReturn(List.of(FermetureExceptionnelle.builder().id(99L).build()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> horaireSalonService.ajouterFermeture("salon-belle-epoque", request, "proprio@test.com"));

        assertTrue(ex.getMessage().contains("chevauche déjà"));
    }

    @Test
    void testSupprimerFermetureSucces() {
        FermetureExceptionnelle fermeture = FermetureExceptionnelle.builder()
                .id(50L)
                .salon(salon)
                .dateDebut(LocalDate.now().plusDays(5))
                .dateFin(LocalDate.now().plusDays(15))
                .motif("Congés d'été")
                .build();

        when(salonRepository.findBySlug("salon-belle-epoque")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-belle-epoque"))
                .thenReturn(Optional.of(affectation));
        when(fermetureExceptionnelleRepository.findById(50L)).thenReturn(Optional.of(fermeture));

        horaireSalonService.supprimerFermeture("salon-belle-epoque", 50L, "proprio@test.com");

        verify(fermetureExceptionnelleRepository).delete(fermeture);
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.SUPPRESSION), eq("FermetureExceptionnelle"), eq("50"), any(), any(), eq(affectation), eq("PROPRIETAIRE"));
    }

    @Test
    void testSupprimerFermetureRejetDateDebutPasseeOuCommencee() {
        FermetureExceptionnelle fermeture = FermetureExceptionnelle.builder()
                .id(50L)
                .salon(salon)
                .dateDebut(LocalDate.now().minusDays(1))
                .dateFin(LocalDate.now().plusDays(3))
                .motif("Fermeture en cours")
                .build();

        when(salonRepository.findBySlug("salon-belle-epoque")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-belle-epoque"))
                .thenReturn(Optional.of(affectation));
        when(fermetureExceptionnelleRepository.findById(50L)).thenReturn(Optional.of(fermeture));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> horaireSalonService.supprimerFermeture("salon-belle-epoque", 50L, "proprio@test.com"));

        assertTrue(ex.getMessage().contains("avant sa date de début"));
    }

    @Test
    void testModifierFermetureFutureSucces() {
        FermetureExceptionnelle fermeture = FermetureExceptionnelle.builder()
                .id(50L)
                .salon(salon)
                .dateDebut(LocalDate.now().plusDays(3))
                .dateFin(LocalDate.now().plusDays(5))
                .motif("Ancien motif")
                .build();

        FermetureExceptionnelleDTORequest request = new FermetureExceptionnelleDTORequest(
                LocalDate.now().plusDays(4),
                LocalDate.now().plusDays(7),
                "Nouveau motif"
        );

        when(salonRepository.findBySlug("salon-belle-epoque")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-belle-epoque"))
                .thenReturn(Optional.of(affectation));
        when(fermetureExceptionnelleRepository.findById(50L)).thenReturn(Optional.of(fermeture));
        when(fermetureExceptionnelleRepository.findChevauchementsExcluant(eq(1L), eq(50L), eq(request.dateDebut()), eq(request.dateFin())))
                .thenReturn(List.of());
        when(fermetureExceptionnelleRepository.save(any(FermetureExceptionnelle.class))).thenAnswer(i -> i.getArgument(0));
        when(fermetureMapper.apply(any())).thenReturn(new FermetureExceptionnelleDTOResponse(
                50L, request.dateDebut(), request.dateFin(), request.motif(), java.time.LocalDateTime.now()
        ));

        FermetureExceptionnelleDTOResponse response = horaireSalonService.modifierFermeture(
                "salon-belle-epoque", 50L, request, "proprio@test.com");

        assertNotNull(response);
        assertEquals("Nouveau motif", response.motif());
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.MODIFICATION), eq("FermetureExceptionnelle"), eq("50"), any(), any(), eq(affectation), eq("PROPRIETAIRE"));
    }

    @Test
    void testMettreFinFermetureSucces() {
        FermetureExceptionnelle fermeture = FermetureExceptionnelle.builder()
                .id(50L)
                .salon(salon)
                .dateDebut(LocalDate.now().minusDays(2))
                .dateFin(LocalDate.now().plusDays(3))
                .motif("Fermeture en cours")
                .build();

        when(salonRepository.findBySlug("salon-belle-epoque")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-belle-epoque"))
                .thenReturn(Optional.of(affectation));
        when(fermetureExceptionnelleRepository.findById(50L)).thenReturn(Optional.of(fermeture));
        when(fermetureExceptionnelleRepository.save(any(FermetureExceptionnelle.class))).thenAnswer(i -> i.getArgument(0));
        when(fermetureMapper.apply(any())).thenReturn(new FermetureExceptionnelleDTOResponse(
                50L, fermeture.getDateDebut(), LocalDate.now(), fermeture.getMotif(), java.time.LocalDateTime.now()
        ));

        FermetureExceptionnelleDTOResponse response = horaireSalonService.mettreFinFermeture(
                "salon-belle-epoque", 50L, "proprio@test.com");

        assertNotNull(response);
        assertEquals(LocalDate.now(), response.dateFin());
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.MODIFICATION), eq("FermetureExceptionnelle"), eq("50"), any(), any(), eq(affectation), eq("PROPRIETAIRE"));
    }
}

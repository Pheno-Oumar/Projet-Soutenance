package com.kadi_aon.mon_salon.rendezvous.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
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
import com.kadi_aon.mon_salon.coiffeur.repository.IndisponibiliteCoiffeurRepository;
import com.kadi_aon.mon_salon.rendezvous.dto.CreneauDisponibleDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.DisponibiliteSearchDTORequest;
import com.kadi_aon.mon_salon.rendezvous.enums.StatutRendezVous;
import com.kadi_aon.mon_salon.rendezvous.repository.LigneRendezVousRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.FermetureExceptionnelle;
import com.kadi_aon.mon_salon.salon.entity.HoraireOuverture;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.entity.ServiceSalon;
import com.kadi_aon.mon_salon.salon.entity.VarianteService;
import com.kadi_aon.mon_salon.salon.enums.JourSemaine;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.FermetureExceptionnelleRepository;
import com.kadi_aon.mon_salon.salon.repository.HoraireOuvertureRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;
import com.kadi_aon.mon_salon.salon.repository.VarianteServiceRepository;

@ExtendWith(MockitoExtension.class)
class DisponibiliteServiceTest {

        @Mock
        private SalonRepository salonRepository;
        @Mock
        private VarianteServiceRepository varianteServiceRepository;
        @Mock
        private FermetureExceptionnelleRepository fermetureExceptionnelleRepository;
        @Mock
        private HoraireOuvertureRepository horaireOuvertureRepository;
        @Mock
        private AffectationSalonRepository affectationSalonRepository;
        @Mock
        private IndisponibiliteCoiffeurRepository indisponibiliteCoiffeurRepository;
        @Mock
        private LigneRendezVousRepository ligneRendezVousRepository;

        @InjectMocks
        private DisponibiliteService disponibiliteService;

        private Salon salon;
        private ServiceSalon serviceSalon;
        private VarianteService variante1;
        private VarianteService variante2;
        private AffectationSalon coiffeur1;
        private AffectationSalon coiffeur2;
        private LocalDate futureDate;

        @BeforeEach
        void setUp() {
                salon = Salon.builder().id(1L).slug("salon-star").statut(true).build();
                serviceSalon = ServiceSalon.builder().id(10L).salon(salon).nom("Coiffure Homme").statut(true).build();

                variante1 = VarianteService.builder()
                                .id(101L)
                                .serviceSalon(serviceSalon)
                                .nom("Coupe Dégradé")
                                .dureeMinutes(30)
                                .prix(new BigDecimal("20.00"))
                                .statut(true)
                                .build();

                variante2 = VarianteService.builder()
                                .id(102L)
                                .serviceSalon(serviceSalon)
                                .nom("Taille de Barbe")
                                .dureeMinutes(15)
                                .prix(new BigDecimal("10.00"))
                                .statut(true)
                                .build();

                Compte c1 = Compte.builder().id(1L).nom("Dupont").prenom("Jean").email("jean@test.com").build();
                Compte c2 = Compte.builder().id(2L).nom("Martin").prenom("Paul").email("paul@test.com").build();

                Set<RoleSalon> roles = new HashSet<>();
                roles.add(RoleSalon.builder().id(1L).role(TypeRoleSalon.COIFFEUR).build());

                coiffeur1 = AffectationSalon.builder().id(21L).salon(salon).compte(c1).roles(roles).statut(true)
                                .build();
                coiffeur2 = AffectationSalon.builder().id(22L).salon(salon).compte(c2).roles(roles).statut(true)
                                .build();

                futureDate = LocalDate.now().plusDays(5);
        }

        @Test
        void calculerDisponibilites_serviceInactif_lanceException() {
                serviceSalon.setStatut(false); // Service inactif mais variante active
                variante1.setStatut(true);

                when(salonRepository.findBySlug("salon-star")).thenReturn(Optional.of(salon));
                when(varianteServiceRepository.findById(101L)).thenReturn(Optional.of(variante1));

                DisponibiliteSearchDTORequest request = new DisponibiliteSearchDTORequest(
                                futureDate, List.of(101L), null, null);

                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                () -> disponibiliteService.calculerDisponibilites("salon-star", request));

                assertTrue(ex.getMessage().contains("inactif"));
        }

        @Test
        void calculerDisponibilites_salonFermeExceptionnellement_retourneVide() {
                when(salonRepository.findBySlug("salon-star")).thenReturn(Optional.of(salon));
                when(varianteServiceRepository.findById(101L)).thenReturn(Optional.of(variante1));

                when(fermetureExceptionnelleRepository.findChevauchements(1L, futureDate, futureDate))
                                .thenReturn(List.of(FermetureExceptionnelle.builder().id(1L).build()));

                DisponibiliteSearchDTORequest request = new DisponibiliteSearchDTORequest(
                                futureDate, List.of(101L), null, null);

                List<CreneauDisponibleDTOResponse> resultats = disponibiliteService.calculerDisponibilites("salon-star",
                                request);

                assertTrue(resultats.isEmpty());
        }

        @Test
        void calculerDisponibilites_monoCoiffeur_succes() {
                when(salonRepository.findBySlug("salon-star")).thenReturn(Optional.of(salon));
                when(varianteServiceRepository.findById(101L)).thenReturn(Optional.of(variante1));
                when(fermetureExceptionnelleRepository.findChevauchements(1L, futureDate, futureDate))
                                .thenReturn(Collections.emptyList());

                JourSemaine jour = JourSemaine.from(futureDate.getDayOfWeek());
                HoraireOuverture horaire = HoraireOuverture.builder()
                                .salon(salon)
                                .jourSemaine(jour)
                                .heureOuverture(LocalTime.of(9, 0))
                                .heureFermeture(LocalTime.of(10, 0)) // 1h d'ouverture: créneaux de 30 min possibles à
                                                                     // 09h00, 09h15, 09h30
                                .build();

                when(horaireOuvertureRepository.findBySalonIdAndJourSemaine(1L, jour)).thenReturn(Optional.of(horaire));
                when(affectationSalonRepository.findBySalonIdAndStatutTrue(1L)).thenReturn(List.of(coiffeur1));
                when(indisponibiliteCoiffeurRepository.findBySalonIdAndPeriode(eq(1L), any(), any()))
                                .thenReturn(Collections.emptyList());
                when(ligneRendezVousRepository.findLignesActivesBySalonAndPeriode(eq(1L), any(), any(),
                                eq(StatutRendezVous.ANNULE))).thenReturn(Collections.emptyList());

                DisponibiliteSearchDTORequest request = new DisponibiliteSearchDTORequest(
                                futureDate, List.of(101L), null, null);

                List<CreneauDisponibleDTOResponse> resultats = disponibiliteService.calculerDisponibilites("salon-star",
                                request);

                assertFalse(resultats.isEmpty());
                // Pour 09h00-10h00 avec prestation de 30 min par pas de 15 min : 09h00-09h30,
                // 09h15-09h45, 09h30-10h00
                assertEquals(3, resultats.size());
                assertEquals(LocalTime.of(9, 0), resultats.get(0).heureDebut());
                assertEquals(LocalTime.of(9, 30), resultats.get(0).heureFin());
                assertEquals(1, resultats.get(0).lignesProposees().size());
                assertEquals("Coupe Dégradé", resultats.get(0).lignesProposees().get(0).varianteNom());
        }

        @Test
        void calculerDisponibilites_avecHeureMinimale_filtreCreneauxAnterieurs() {
                when(salonRepository.findBySlug("salon-star")).thenReturn(Optional.of(salon));
                when(varianteServiceRepository.findById(101L)).thenReturn(Optional.of(variante1));
                when(fermetureExceptionnelleRepository.findChevauchements(1L, futureDate, futureDate))
                                .thenReturn(Collections.emptyList());

                JourSemaine jour = JourSemaine.from(futureDate.getDayOfWeek());
                HoraireOuverture horaire = HoraireOuverture.builder()
                                .salon(salon)
                                .jourSemaine(jour)
                                .heureOuverture(LocalTime.of(9, 0))
                                .heureFermeture(LocalTime.of(12, 0))
                                .build();

                when(horaireOuvertureRepository.findBySalonIdAndJourSemaine(1L, jour)).thenReturn(Optional.of(horaire));
                when(affectationSalonRepository.findBySalonIdAndStatutTrue(1L)).thenReturn(List.of(coiffeur1));
                when(indisponibiliteCoiffeurRepository.findBySalonIdAndPeriode(eq(1L), any(), any()))
                                .thenReturn(Collections.emptyList());
                when(ligneRendezVousRepository.findLignesActivesBySalonAndPeriode(eq(1L), any(), any(),
                                eq(StatutRendezVous.ANNULE))).thenReturn(Collections.emptyList());

                // Heure minimale fixée à 11h00
                DisponibiliteSearchDTORequest request = new DisponibiliteSearchDTORequest(
                                futureDate, List.of(101L), null, LocalTime.of(11, 0));

                List<CreneauDisponibleDTOResponse> resultats = disponibiliteService.calculerDisponibilites("salon-star",
                                request);

                assertFalse(resultats.isEmpty());
                // Les créneaux doivent tous commencer >= 11h00
                assertTrue(resultats.stream().allMatch(c -> !c.heureDebut().isBefore(LocalTime.of(11, 0))));
                assertEquals(LocalTime.of(11, 0), resultats.get(0).heureDebut());
        }

        @Test
        void calculerDisponibilites_plusieursVariantes_succes() {
                when(salonRepository.findBySlug("salon-star")).thenReturn(Optional.of(salon));
                when(varianteServiceRepository.findById(101L)).thenReturn(Optional.of(variante1)); // 30 min
                when(varianteServiceRepository.findById(102L)).thenReturn(Optional.of(variante2)); // 15 min
                when(fermetureExceptionnelleRepository.findChevauchements(1L, futureDate, futureDate))
                                .thenReturn(Collections.emptyList());

                JourSemaine jour = JourSemaine.from(futureDate.getDayOfWeek());
                HoraireOuverture horaire = HoraireOuverture.builder()
                                .salon(salon)
                                .jourSemaine(jour)
                                .heureOuverture(LocalTime.of(14, 0))
                                .heureFermeture(LocalTime.of(15, 0))
                                .build();

                when(horaireOuvertureRepository.findBySalonIdAndJourSemaine(1L, jour)).thenReturn(Optional.of(horaire));
                when(affectationSalonRepository.findBySalonIdAndStatutTrue(1L))
                                .thenReturn(List.of(coiffeur1, coiffeur2));
                when(indisponibiliteCoiffeurRepository.findBySalonIdAndPeriode(eq(1L), any(), any()))
                                .thenReturn(Collections.emptyList());
                when(ligneRendezVousRepository.findLignesActivesBySalonAndPeriode(eq(1L), any(), any(),
                                eq(StatutRendezVous.ANNULE))).thenReturn(Collections.emptyList());

                DisponibiliteSearchDTORequest request = new DisponibiliteSearchDTORequest(
                                futureDate, List.of(101L, 102L), null, null);

                List<CreneauDisponibleDTOResponse> resultats = disponibiliteService.calculerDisponibilites("salon-star",
                                request);

                assertFalse(resultats.isEmpty());
                assertEquals(45, resultats.get(0).dureeTotale()); // 30 + 15 min
                assertEquals(new BigDecimal("30.00"), resultats.get(0).montantTotal()); // 20 + 10
                assertEquals(2, resultats.get(0).lignesProposees().size());
        }
}

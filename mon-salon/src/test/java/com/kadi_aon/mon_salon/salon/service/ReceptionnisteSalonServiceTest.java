package com.kadi_aon.mon_salon.salon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.rendezvous.dto.PlanningRendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.RetardRendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.RetardTraitementDTORequest;
import com.kadi_aon.mon_salon.rendezvous.entity.LigneRendezVous;
import com.kadi_aon.mon_salon.rendezvous.entity.RendezVous;
import com.kadi_aon.mon_salon.rendezvous.enums.ActionTraitementRetard;
import com.kadi_aon.mon_salon.rendezvous.enums.StatutRendezVous;
import com.kadi_aon.mon_salon.rendezvous.mapper.RendezVousDTOResponseMapper;
import com.kadi_aon.mon_salon.rendezvous.repository.RendezVousRepository;
import com.kadi_aon.mon_salon.salon.dto.ClientRapideDTORequest;
import com.kadi_aon.mon_salon.salon.dto.ClientRapideDTOResponse;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.entity.ServiceSalon;
import com.kadi_aon.mon_salon.salon.entity.VarianteService;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.RoleSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

@ExtendWith(MockitoExtension.class)
class ReceptionnisteSalonServiceTest {

        @Mock
        private SalonRepository salonRepository;
        @Mock
        private CompteRepository compteRepository;
        @Mock
        private AffectationSalonRepository affectationSalonRepository;
        @Mock
        private RoleSalonRepository roleSalonRepository;
        @Mock
        private RendezVousRepository rendezVousRepository;
        @Mock
        private RendezVousDTOResponseMapper rendezVousDTOResponseMapper;
        @Mock
        private PasswordEncoder passwordEncoder;
        @Mock
        private AuditLogService auditLogService;
        @Mock
        private com.kadi_aon.mon_salon.prestation.repository.PrestationRepository prestationRepository;
        @Mock
        private com.kadi_aon.mon_salon.prestation.service.PrestationSalonService prestationSalonService;
        @Mock
        private com.kadi_aon.mon_salon.caisse.repository.SessionCaisseRepository sessionCaisseRepository;
        @Mock
        private com.kadi_aon.mon_salon.caisse.repository.OperationCaisseRepository operationCaisseRepository;

        @InjectMocks
        private ReceptionnisteSalonService receptionnisteSalonService;

        private Salon salon;
        private AffectationSalon affectationRecep;
        private AffectationSalon affectationCoiffeur;
        private Compte coiffeurCompte;
        private VarianteService variante;
        private LocalDateTime maintenant;

        @BeforeEach
        void setUp() {
                salon = Salon.builder().id(1L).slug("salon-royal").statut(true).nom("Royal").build();

                Compte recepCompte = Compte.builder().id(10L).email("recep@test.com").nom("Camara").prenom("Awa")
                                .build();
                coiffeurCompte = Compte.builder().id(20L).email("coiffeur@test.com").nom("Toure").prenom("Ali").build();

                Set<RoleSalon> rolesRecep = new HashSet<>(
                                List.of(RoleSalon.builder().id(1L).role(TypeRoleSalon.RECEPTIONNISTE).build()));
                Set<RoleSalon> rolesCoiffeur = new HashSet<>(
                                List.of(RoleSalon.builder().id(2L).role(TypeRoleSalon.COIFFEUR).build()));

                affectationRecep = AffectationSalon.builder().id(100L).salon(salon).compte(recepCompte)
                                .roles(rolesRecep).statut(true).build();
                affectationCoiffeur = AffectationSalon.builder().id(200L).salon(salon).compte(coiffeurCompte)
                                .roles(rolesCoiffeur).statut(true).build();

                ServiceSalon service = ServiceSalon.builder().id(300L).salon(salon).nom("Tresses").statut(true).build();
                variante = VarianteService.builder().id(400L).serviceSalon(service).nom("Tresses simples")
                                .dureeMinutes(45).prix(new BigDecimal("30.00")).statut(true).build();

                maintenant = LocalDateTime.now().withHour(11).withMinute(0);
        }

        @Test
        void rechercherClients_succes() {
                when(salonRepository.findBySlug("salon-royal")).thenReturn(Optional.of(salon));
                when(compteRepository.searchClients("diallo")).thenReturn(List.of(
                                Compte.builder().id(50L).nom("Diallo").prenom("Samba").telephone("0601020304")
                                                .email("samba@test.com").build()));

                List<ClientRapideDTOResponse> result = receptionnisteSalonService.rechercherClients("salon-royal",
                                "diallo");

                assertEquals(1, result.size());
                assertEquals("Diallo", result.get(0).nom());
        }

        @Test
        void creerClientRapide_succes() {
                when(salonRepository.findBySlug("salon-royal")).thenReturn(Optional.of(salon));
                when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("recep@test.com",
                                "salon-royal"))
                                .thenReturn(Optional.of(affectationRecep));

                when(compteRepository.findByEmail(anyString())).thenReturn(Optional.empty());
                when(passwordEncoder.encode(anyString())).thenReturn("hashedTempPassword");
                when(compteRepository.save(any(Compte.class))).thenAnswer(i -> {
                        Compte c = i.getArgument(0);
                        c.setId(88L);
                        return c;
                });

                RoleSalon roleClient = RoleSalon.builder().id(7L).role(TypeRoleSalon.CLIENT).build();
                when(roleSalonRepository.findByRole(TypeRoleSalon.CLIENT)).thenReturn(Optional.of(roleClient));
                when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue(
                                "oumar.barry@test.com", "salon-royal"))
                                .thenReturn(Optional.empty());
                when(affectationSalonRepository.save(any(AffectationSalon.class))).thenAnswer(i -> i.getArgument(0));

                ClientRapideDTORequest request = new ClientRapideDTORequest("Barry", "Oumar", "0708091011",
                                "oumar.barry@test.com", LocalDate.of(1995, 5, 20));

                ClientRapideDTOResponse response = receptionnisteSalonService.creerClientRapide("salon-royal", request,
                                "recep@test.com");

                assertNotNull(response);
                assertEquals(88L, response.id());
                assertEquals("Barry", response.nom());
        }

        @Test
        void consulterPlanning_succes() {
                when(salonRepository.findBySlug("salon-royal")).thenReturn(Optional.of(salon));

                Compte client = Compte.builder().id(50L).nom("Kone").prenom("Ibrahim").telephone("01020304").build();

                RendezVous rdv = RendezVous.builder()
                                .id(1L)
                                .salon(salon)
                                .client(client)
                                .dateHeurePrevue(maintenant)
                                .dateHeureFin(maintenant.plusMinutes(45))
                                .coiffeur(affectationCoiffeur)
                                .statut(StatutRendezVous.CONFIRME)
                                .montantEstime(new BigDecimal("30.00"))
                                .build();

                LigneRendezVous ligne = LigneRendezVous.builder()
                                .id(10L)
                                .varianteService(variante)
                                .dateHeureDebut(maintenant)
                                .dateHeureFin(maintenant.plusMinutes(45))
                                .duree(45)
                                .prix(new BigDecimal("30.00"))
                                .ordre(1)
                                .build();
                rdv.addLigne(ligne);

                when(rendezVousRepository.findPlanningBySalonAndJour(eq(1L), any(), any())).thenReturn(List.of(rdv));

                List<PlanningRendezVousDTOResponse> planning = receptionnisteSalonService
                                .consulterPlanning("salon-royal", maintenant.toLocalDate(), null);

                assertNotNull(planning);
                assertEquals(1, planning.size());
                assertEquals("Kone", planning.get(0).nomClient());
                assertEquals(1, planning.get(0).prestations().size());
        }

        @Test
        void traiterRetard_noShow_libereLeCreneau() {
                Compte client = Compte.builder().id(50L).nom("Client").prenom("Test").telephone("010203").build();
                RendezVous rdv = RendezVous.builder()
                                .id(1L)
                                .salon(salon)
                                .coiffeur(affectationCoiffeur)
                                .client(client)
                                .statut(StatutRendezVous.CONFIRME)
                                .dateHeurePrevue(LocalDateTime.now().minusMinutes(20))
                                .dateHeureFin(LocalDateTime.now().plusMinutes(25))
                                .build();

                when(rendezVousRepository.findByIdAndSalonSlug(1L, "salon-royal")).thenReturn(Optional.of(rdv));
                when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("recep@test.com",
                                "salon-royal"))
                                .thenReturn(Optional.of(affectationRecep));
                when(rendezVousRepository.save(any(RendezVous.class))).thenAnswer(i -> i.getArgument(0));

                RendezVousDTOResponse dto = new RendezVousDTOResponse(
                                1L, "salon-royal", "Royal", 101L, "Coiff", "Bob", "Client", "Test", "010203",
                                rdv.getDateHeurePrevue(), rdv.getDateHeureFin(), StatutRendezVous.NO_SHOW.name(),
                                new BigDecimal("30.00"), LocalDateTime.now(),
                                Collections.emptyList());
                when(rendezVousDTOResponseMapper.apply(any())).thenReturn(dto);

                RetardTraitementDTORequest request = new RetardTraitementDTORequest(ActionTraitementRetard.NO_SHOW,
                                null, "Ne répond pas aux appels");
                RendezVousDTOResponse response = receptionnisteSalonService.traiterRetard("salon-royal", 1L, request,
                                "recep@test.com");

                assertNotNull(response);
                assertEquals(StatutRendezVous.NO_SHOW, rdv.getStatut());
                assertNotNull(rdv.getDateAnnulation());
        }

        @Test
        void listerRendezVousEnRetard_succes() {
                when(salonRepository.findBySlug("salon-royal")).thenReturn(Optional.of(salon));

                Compte client = Compte.builder().id(50L).nom("Diallo").prenom("Amadou").telephone("0102030405").build();
                RendezVous rdv = RendezVous.builder()
                                .id(1L)
                                .salon(salon)
                                .coiffeur(affectationCoiffeur)
                                .client(client)
                                .statut(StatutRendezVous.CONFIRME)
                                .dateHeurePrevue(LocalDateTime.now().minusMinutes(25))
                                .dateHeureFin(LocalDateTime.now().plusMinutes(20))
                                .build();

                LigneRendezVous ligne = LigneRendezVous.builder()
                                .id(10L)
                                .build();
                rdv.addLigne(ligne);

                when(rendezVousRepository.findRendezVousEnRetard(eq(1L), any(), any(), eq(StatutRendezVous.CONFIRME)))
                                .thenReturn(List.of(rdv));

                List<RetardRendezVousDTOResponse> retards = receptionnisteSalonService
                                .listerRendezVousEnRetard("salon-royal");

                assertEquals(1, retards.size());
                assertEquals("Diallo", retards.get(0).nomClient());
        }

        @Test
        void getDashboard_succes() {
                when(salonRepository.findBySlug("salon-royal")).thenReturn(Optional.of(salon));
                when(rendezVousRepository.findPlanningBySalonAndJour(eq(1L), any(), any()))
                                .thenReturn(List.of());
                when(prestationSalonService.listerPrestations("salon-royal", com.kadi_aon.mon_salon.prestation.enums.StatutPrestation.EN_COURS))
                                .thenReturn(List.of());
                when(prestationRepository.findBySalonSlugOrderByDateHeureDebutDesc("salon-royal"))
                                .thenReturn(List.of());
                when(rendezVousRepository.findRendezVousEnRetard(eq(1L), any(), any(), eq(StatutRendezVous.CONFIRME)))
                                .thenReturn(List.of());
                when(sessionCaisseRepository.findByAffectationSalonSlugAndStatut("salon-royal", com.kadi_aon.mon_salon.caisse.enums.StatutSessionCaisse.EN_COURS))
                                .thenReturn(Optional.empty());

                com.kadi_aon.mon_salon.salon.dto.ReceptionnisteDashboardDTOResponse dashboard =
                                receptionnisteSalonService.getDashboard("salon-royal");

                assertNotNull(dashboard);
                assertNotNull(dashboard.stats());
                assertNotNull(dashboard.caisse());
                assertEquals(0, dashboard.stats().totalRendezVousJour());
                assertEquals(false, dashboard.caisse().caisseOuverte());
        }

        @Test
        void annulerRendezVous_succes() {
                RendezVous rdv = RendezVous.builder()
                                .id(1L)
                                .salon(salon)
                                .statut(StatutRendezVous.CONFIRME)
                                .build();

                when(rendezVousRepository.findByIdAndSalonSlug(1L, "salon-royal")).thenReturn(Optional.of(rdv));
                when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("recep@test.com", "salon-royal"))
                                .thenReturn(Optional.of(affectationRecep));
                when(rendezVousRepository.save(any(RendezVous.class))).thenAnswer(inv -> inv.getArgument(0));

                RendezVousDTOResponse dummyDto = new RendezVousDTOResponse(
                                1L, "salon-royal", "Royal", null, null, null, null, null, null, null, null, "ANNULE", null, null, null
                );
                when(rendezVousDTOResponseMapper.apply(any(RendezVous.class))).thenReturn(dummyDto);

                com.kadi_aon.mon_salon.rendezvous.dto.RendezVousAnnulationDTORequest req =
                                new com.kadi_aon.mon_salon.rendezvous.dto.RendezVousAnnulationDTORequest("Annulé par téléphone");

                RendezVousDTOResponse res = receptionnisteSalonService.annulerRendezVous("salon-royal", 1L, req, "recep@test.com");

                assertNotNull(res);
                assertEquals("ANNULE", res.statut());
                assertEquals(StatutRendezVous.ANNULE, rdv.getStatut());
                assertEquals("Annulé par téléphone", rdv.getMotifAnnulation());
        }
}

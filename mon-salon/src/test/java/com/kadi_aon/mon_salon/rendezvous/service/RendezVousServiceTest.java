package com.kadi_aon.mon_salon.rendezvous.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.coiffeur.repository.IndisponibiliteCoiffeurRepository;
import com.kadi_aon.mon_salon.prestation.repository.PrestationRepository;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousCreateDTORequest;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.entity.RendezVous;
import com.kadi_aon.mon_salon.rendezvous.enums.StatutRendezVous;
import com.kadi_aon.mon_salon.rendezvous.mapper.RendezVousDTOResponseMapper;
import com.kadi_aon.mon_salon.rendezvous.repository.LigneRendezVousRepository;
import com.kadi_aon.mon_salon.rendezvous.repository.RendezVousRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
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
import com.kadi_aon.mon_salon.salon.repository.RoleSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;
import com.kadi_aon.mon_salon.salon.repository.VarianteServiceRepository;

@ExtendWith(MockitoExtension.class)
class RendezVousServiceTest {

    @Mock
    private SalonRepository salonRepository;
    @Mock
    private CompteRepository compteRepository;
    @Mock
    private VarianteServiceRepository varianteServiceRepository;
    @Mock
    private AffectationSalonRepository affectationSalonRepository;
    @Mock
    private RoleSalonRepository roleSalonRepository;
    @Mock
    private FermetureExceptionnelleRepository fermetureExceptionnelleRepository;
    @Mock
    private HoraireOuvertureRepository horaireOuvertureRepository;
    @Mock
    private IndisponibiliteCoiffeurRepository indisponibiliteCoiffeurRepository;
    @Mock
    private LigneRendezVousRepository ligneRendezVousRepository;
    @Mock
    private RendezVousRepository rendezVousRepository;
    @Mock
    private PrestationRepository prestationRepository;
    @Mock
    private DisponibiliteService disponibiliteService;
    @Mock
    private RendezVousDTOResponseMapper rendezVousDTOResponseMapper;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private RendezVousService rendezVousService;

    private Salon salon;
    private Compte clientCompte;
    private Compte coiffeurCompte;
    private AffectationSalon clientAffectation;
    private AffectationSalon coiffeurAffectation;
    private ServiceSalon service;
    private VarianteService variante;
    private LocalDateTime debut;

    @BeforeEach
    void setUp() {
        salon = Salon.builder().id(1L).slug("mon-salon-chic").statut(true).nom("Chic").build();
        clientCompte = Compte.builder().id(10L).nom("Diallo").prenom("Amadou").email("client@test.com").telephone("0102030405").build();
        coiffeurCompte = Compte.builder().id(11L).nom("Konate").prenom("Moussa").email("coiffeur@test.com").build();

        Set<RoleSalon> rolesClient = new HashSet<>();
        rolesClient.add(RoleSalon.builder().id(1L).role(TypeRoleSalon.CLIENT).build());

        Set<RoleSalon> rolesCoiffeur = new HashSet<>();
        rolesCoiffeur.add(RoleSalon.builder().id(2L).role(TypeRoleSalon.COIFFEUR).build());

        clientAffectation = AffectationSalon.builder().id(30L).salon(salon).compte(clientCompte).roles(rolesClient).statut(true).build();
        coiffeurAffectation = AffectationSalon.builder().id(31L).salon(salon).compte(coiffeurCompte).roles(rolesCoiffeur).statut(true).build();

        service = ServiceSalon.builder().id(100L).salon(salon).nom("Coiffure").statut(true).build();
        variante = VarianteService.builder().id(200L).serviceSalon(service).nom("Coupe Classique").dureeMinutes(30).prix(new BigDecimal("25.00")).statut(true).build();

        debut = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    void creerRendezVousClient_succes() {
        when(salonRepository.findBySlug("mon-salon-chic")).thenReturn(Optional.of(salon));
        when(compteRepository.findByEmail("client@test.com")).thenReturn(Optional.of(clientCompte));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("client@test.com", "mon-salon-chic"))
                .thenReturn(Optional.of(clientAffectation));

        when(varianteServiceRepository.findById(200L)).thenReturn(Optional.of(variante));
        when(fermetureExceptionnelleRepository.findChevauchements(eq(1L), any(), any())).thenReturn(Collections.emptyList());

        JourSemaine jour = JourSemaine.from(debut.toLocalDate().getDayOfWeek());
        HoraireOuverture horaire = HoraireOuverture.builder()
                .salon(salon)
                .jourSemaine(jour)
                .heureOuverture(LocalTime.of(9, 0))
                .heureFermeture(LocalTime.of(19, 0))
                .build();
        when(horaireOuvertureRepository.findBySalonIdAndJourSemaine(1L, jour)).thenReturn(Optional.of(horaire));

        when(rendezVousRepository.findRendezVousActifsByClientAndSalonAndPeriode(eq(10L), eq(1L), eq(debut), eq(debut.plusMinutes(30)), eq(StatutRendezVous.ANNULE)))
                .thenReturn(Collections.emptyList());

        when(affectationSalonRepository.findBySalonIdAndStatutTrue(1L)).thenReturn(List.of(coiffeurAffectation));
        when(indisponibiliteCoiffeurRepository.findBySalonIdAndPeriode(eq(1L), any(), any())).thenReturn(Collections.emptyList());
        when(ligneRendezVousRepository.findLignesActivesBySalonAndPeriode(eq(1L), any(), any(), eq(StatutRendezVous.ANNULE))).thenReturn(Collections.emptyList());

        when(disponibiliteService.isCoiffeurLibre(eq(31L), eq(debut), eq(debut.plusMinutes(30)), any(), any())).thenReturn(true);

        when(rendezVousRepository.save(any(RendezVous.class))).thenAnswer(i -> {
            RendezVous r = i.getArgument(0);
            r.setId(500L);
            return r;
        });

        RendezVousDTOResponse expected = new RendezVousDTOResponse(
                500L, "mon-salon-chic", "Chic", 31L, "Konate", "Moussa", "Diallo", "Amadou", "0102030405",
                debut, debut.plusMinutes(30), StatutRendezVous.CONFIRME.name(),
                new BigDecimal("25.00"), LocalDateTime.now(), Collections.emptyList()
        );
        when(rendezVousDTOResponseMapper.apply(any())).thenReturn(expected);

        RendezVousCreateDTORequest request = new RendezVousCreateDTORequest(
                debut, List.of(200L), null
        );

        RendezVousDTOResponse response = rendezVousService.creerRendezVousClient("mon-salon-chic", request, "client@test.com");

        assertNotNull(response);
        assertEquals(500L, response.id());
        assertEquals("Diallo", response.nomClient());
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.CREATION), eq("RendezVous"), eq("500"), any(), anyString(), eq(clientAffectation), eq("CLIENT"));
    }

    @Test
    void creerRendezVousClient_serviceInactif_lanceException() {
        service.setStatut(false); // Service parent inactif

        when(salonRepository.findBySlug("mon-salon-chic")).thenReturn(Optional.of(salon));
        when(compteRepository.findByEmail("client@test.com")).thenReturn(Optional.of(clientCompte));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("client@test.com", "mon-salon-chic"))
                .thenReturn(Optional.of(clientAffectation));
        when(varianteServiceRepository.findById(200L)).thenReturn(Optional.of(variante));

        RendezVousCreateDTORequest request = new RendezVousCreateDTORequest(
                debut, List.of(200L), null
        );

        assertThrows(IllegalArgumentException.class, () ->
                rendezVousService.creerRendezVousClient("mon-salon-chic", request, "client@test.com"));
    }

    @Test
    void annulerRendezVousClient_succes() {
        RendezVous rdv = RendezVous.builder()
                .id(500L)
                .salon(salon)
                .coiffeur(coiffeurAffectation)
                .client(clientCompte)
                .dateHeurePrevue(debut)
                .statut(StatutRendezVous.CONFIRME)
                .build();

        when(rendezVousRepository.findByIdAndSalonSlug(500L, "mon-salon-chic")).thenReturn(Optional.of(rdv));
        when(rendezVousRepository.save(any(RendezVous.class))).thenAnswer(i -> i.getArgument(0));

        RendezVousDTOResponse expected = new RendezVousDTOResponse(
                500L, "mon-salon-chic", "Chic", 31L, "Konate", "Moussa", "Diallo", "Amadou", "0102030405",
                debut, debut.plusMinutes(30), StatutRendezVous.ANNULE.name(),
                new BigDecimal("25.00"), LocalDateTime.now(), Collections.emptyList()
        );
        when(rendezVousDTOResponseMapper.apply(any())).thenReturn(expected);

        com.kadi_aon.mon_salon.rendezvous.dto.RendezVousAnnulationDTORequest request =
                new com.kadi_aon.mon_salon.rendezvous.dto.RendezVousAnnulationDTORequest("Empêchement imprévu");

        RendezVousDTOResponse response = rendezVousService.annulerRendezVousClient("mon-salon-chic", 500L, request, "client@test.com");

        assertNotNull(response);
        assertEquals(StatutRendezVous.ANNULE.name(), response.statut());
        assertEquals(StatutRendezVous.ANNULE, rdv.getStatut());
        assertEquals("Empêchement imprévu", rdv.getMotifAnnulation());
        assertNotNull(rdv.getDateAnnulation());
    }

    @Test
    void annulerRendezVousClient_nonProprietaire_lanceException() {
        RendezVous rdv = RendezVous.builder()
                .id(500L)
                .salon(salon)
                .client(Compte.builder().id(99L).email("autre@test.com").build())
                .statut(StatutRendezVous.CONFIRME)
                .build();

        when(rendezVousRepository.findByIdAndSalonSlug(500L, "mon-salon-chic")).thenReturn(Optional.of(rdv));

        com.kadi_aon.mon_salon.rendezvous.dto.RendezVousAnnulationDTORequest request =
                new com.kadi_aon.mon_salon.rendezvous.dto.RendezVousAnnulationDTORequest("Motif");

        assertThrows(IllegalArgumentException.class, () ->
                rendezVousService.annulerRendezVousClient("mon-salon-chic", 500L, request, "client@test.com"));
    }

    @Test
    void annulerRendezVousClient_dejaAnnule_lanceException() {
        RendezVous rdv = RendezVous.builder()
                .id(500L)
                .salon(salon)
                .client(clientCompte)
                .statut(StatutRendezVous.ANNULE)
                .build();

        when(rendezVousRepository.findByIdAndSalonSlug(500L, "mon-salon-chic")).thenReturn(Optional.of(rdv));

        com.kadi_aon.mon_salon.rendezvous.dto.RendezVousAnnulationDTORequest request =
                new com.kadi_aon.mon_salon.rendezvous.dto.RendezVousAnnulationDTORequest("Motif");

        assertThrows(IllegalStateException.class, () ->
                rendezVousService.annulerRendezVousClient("mon-salon-chic", 500L, request, "client@test.com"));
    }

    @Test
    void creerRendezVousClient_conflitClientExistant_lanceException() {
        when(salonRepository.findBySlug("mon-salon-chic")).thenReturn(Optional.of(salon));
        when(compteRepository.findByEmail("client@test.com")).thenReturn(Optional.of(clientCompte));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("client@test.com", "mon-salon-chic"))
                .thenReturn(Optional.of(clientAffectation));
        when(varianteServiceRepository.findById(200L)).thenReturn(Optional.of(variante));
        when(fermetureExceptionnelleRepository.findChevauchements(eq(1L), any(), any())).thenReturn(Collections.emptyList());

        JourSemaine jour = JourSemaine.from(debut.toLocalDate().getDayOfWeek());
        HoraireOuverture horaire = HoraireOuverture.builder()
                .salon(salon)
                .jourSemaine(jour)
                .heureOuverture(LocalTime.of(9, 0))
                .heureFermeture(LocalTime.of(19, 0))
                .build();
        when(horaireOuvertureRepository.findBySalonIdAndJourSemaine(1L, jour)).thenReturn(Optional.of(horaire));

        // Client a déjà un RDV
        when(rendezVousRepository.findRendezVousActifsByClientAndSalonAndPeriode(eq(10L), eq(1L), eq(debut), eq(debut.plusMinutes(30)), eq(StatutRendezVous.ANNULE)))
                .thenReturn(List.of(RendezVous.builder().id(999L).build()));

        RendezVousCreateDTORequest request = new RendezVousCreateDTORequest(
                debut, List.of(200L), null
        );

        assertThrows(IllegalArgumentException.class, () ->
                rendezVousService.creerRendezVousClient("mon-salon-chic", request, "client@test.com"));
    }

    @Test
    void annulerRendezVousClient_delai15MinDepasse_lanceIllegalStateException() {
        RendezVous rdv = RendezVous.builder()
                .id(500L)
                .salon(salon)
                .client(clientCompte)
                .dateHeurePrevue(LocalDateTime.now().minusMinutes(20))
                .statut(StatutRendezVous.CONFIRME)
                .build();

        when(rendezVousRepository.findByIdAndSalonSlug(500L, "mon-salon-chic")).thenReturn(Optional.of(rdv));

        com.kadi_aon.mon_salon.rendezvous.dto.RendezVousAnnulationDTORequest request =
                new com.kadi_aon.mon_salon.rendezvous.dto.RendezVousAnnulationDTORequest("Retard trop grand");

        assertThrows(IllegalStateException.class, () ->
                rendezVousService.annulerRendezVousClient("mon-salon-chic", 500L, request, "client@test.com"));
    }
}

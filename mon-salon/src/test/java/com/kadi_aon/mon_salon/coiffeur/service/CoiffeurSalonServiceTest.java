package com.kadi_aon.mon_salon.coiffeur.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.coiffeur.dto.IndisponibiliteDTORequest;
import com.kadi_aon.mon_salon.coiffeur.dto.IndisponibiliteDTOResponse;
import com.kadi_aon.mon_salon.coiffeur.dto.ProfilCoiffeurDTORequest;
import com.kadi_aon.mon_salon.coiffeur.dto.ProfilCoiffeurDTOResponse;
import com.kadi_aon.mon_salon.coiffeur.entity.IndisponibiliteCoiffeur;
import com.kadi_aon.mon_salon.coiffeur.entity.ProfilCoiffeur;
import com.kadi_aon.mon_salon.coiffeur.enums.MotifIndisponibilite;
import com.kadi_aon.mon_salon.coiffeur.mapper.IndisponibiliteDTOResponseMapper;
import com.kadi_aon.mon_salon.coiffeur.mapper.ProfilCoiffeurDTOResponseMapper;
import com.kadi_aon.mon_salon.coiffeur.repository.IndisponibiliteCoiffeurRepository;
import com.kadi_aon.mon_salon.coiffeur.repository.ProfilCoiffeurRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;

@ExtendWith(MockitoExtension.class)
class CoiffeurSalonServiceTest {

    @Mock
    private AffectationSalonRepository affectationSalonRepository;
    @Mock
    private ProfilCoiffeurRepository profilCoiffeurRepository;
    @Mock
    private IndisponibiliteCoiffeurRepository indisponibiliteCoiffeurRepository;
    @Mock
    private ProfilCoiffeurDTOResponseMapper profilCoiffeurDTOResponseMapper;
    @Mock
    private IndisponibiliteDTOResponseMapper indisponibiliteDTOResponseMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private com.kadi_aon.mon_salon.common.service.CloudinaryService cloudinaryService;
    @Mock
    private com.kadi_aon.mon_salon.salon.service.ReceptionnisteSalonService receptionnisteSalonService;
    @Mock
    private com.kadi_aon.mon_salon.prestation.repository.PrestationRepository prestationRepository;
    @Mock
    private com.kadi_aon.mon_salon.avis.repository.AvisPrestationRepository avisPrestationRepository;
    @Mock
    private com.kadi_aon.mon_salon.profilcapillaire.repository.ProfilCapillaireRepository profilCapillaireRepository;
    @Mock
    private com.kadi_aon.mon_salon.salon.repository.SalonRepository salonRepository;

    @InjectMocks
    private CoiffeurSalonService coiffeurSalonService;

    private Salon salon;
    private Compte compte;
    private AffectationSalon affectation;

    @BeforeEach
    void setUp() {
        salon = Salon.builder().id(1L).slug("salon-elegance").nom("Salon Elegance").statut(true).build();
        compte = Compte.builder().id(10L).email("coiffeur@salon.com").nom("Traore").prenom("Ali").build();

        Set<RoleSalon> roles = new HashSet<>();
        roles.add(RoleSalon.builder().id(1L).role(TypeRoleSalon.COIFFEUR).build());

        affectation = AffectationSalon.builder()
                .id(20L)
                .salon(salon)
                .compte(compte)
                .roles(roles)
                .statut(true)
                .build();
    }

    @Test
    void getProfil_creeProfilParDefaut_siInexistant() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("coiffeur@salon.com", "salon-elegance"))
                .thenReturn(Optional.of(affectation));
        when(profilCoiffeurRepository.findByAffectationId(20L)).thenReturn(Optional.empty());

        ProfilCoiffeurDTOResponse expected = new ProfilCoiffeurDTOResponse(
                null, 20L, "Traore", "Ali", "Ali Traore", null, null, null, null
        );
        when(profilCoiffeurDTOResponseMapper.apply(any())).thenReturn(expected);

        ProfilCoiffeurDTOResponse response = coiffeurSalonService.getProfil("salon-elegance", "coiffeur@salon.com");

        assertNotNull(response);
        assertEquals("Ali Traore", response.nomAffichage());
    }

    @Test
    void updateProfil_metAJourProfilEtAudit() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("coiffeur@salon.com", "salon-elegance"))
                .thenReturn(Optional.of(affectation));

        ProfilCoiffeur profilExistant = ProfilCoiffeur.builder()
                .id(5L)
                .affectation(affectation)
                .nomAffichage("Ali")
                .anneeExperience(3)
                .build();

        when(profilCoiffeurRepository.findByAffectationId(20L)).thenReturn(Optional.of(profilExistant));
        when(profilCoiffeurRepository.save(any(ProfilCoiffeur.class))).thenAnswer(i -> i.getArgument(0));

        ProfilCoiffeurDTORequest request = new ProfilCoiffeurDTORequest(
                "Master Ali", "Expert dégradé", 5, "http://img.webp", "Description pro"
        );

        ProfilCoiffeurDTOResponse expected = new ProfilCoiffeurDTOResponse(
                5L, 20L, "Traore", "Ali", "Master Ali", "Expert dégradé", 5, "http://img.webp", "Description pro"
        );
        when(profilCoiffeurDTOResponseMapper.apply(any())).thenReturn(expected);

        ProfilCoiffeurDTOResponse response = coiffeurSalonService.updateProfil("salon-elegance", request, "coiffeur@salon.com");

        assertNotNull(response);
        assertEquals("Master Ali", response.nomAffichage());
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.MODIFICATION), eq("ProfilCoiffeur"), anyString(), anyString(), anyString(), eq(affectation), eq("COIFFEUR"));
    }

    @Test
    void ajouterIndisponibilite_succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("coiffeur@salon.com", "salon-elegance"))
                .thenReturn(Optional.of(affectation));

        LocalDateTime debut = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        LocalDateTime fin = debut.plusHours(4);

        when(indisponibiliteCoiffeurRepository.findByCoiffeurIdAndPeriode(20L, debut, fin))
                .thenReturn(Collections.emptyList());

        when(indisponibiliteCoiffeurRepository.save(any(IndisponibiliteCoiffeur.class))).thenAnswer(i -> {
            IndisponibiliteCoiffeur ind = i.getArgument(0);
            ind.setId(100L);
            return ind;
        });

        IndisponibiliteDTORequest request = new IndisponibiliteDTORequest(debut, fin, MotifIndisponibilite.CONGE, "Congé prévu");
        IndisponibiliteDTOResponse expected = new IndisponibiliteDTOResponse(100L, 20L, debut, fin, "CONGE", "Congé prévu", LocalDateTime.now());
        when(indisponibiliteDTOResponseMapper.apply(any())).thenReturn(expected);

        IndisponibiliteDTOResponse response = coiffeurSalonService.ajouterIndisponibilite("salon-elegance", request, "coiffeur@salon.com");

        assertNotNull(response);
        assertEquals("CONGE", response.motif());
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.CREATION), eq("IndisponibiliteCoiffeur"), eq("100"), any(), anyString(), eq(affectation), eq("COIFFEUR"));
    }

    @Test
    void ajouterIndisponibilite_dateDebutApresDateFin_lanceException() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("coiffeur@salon.com", "salon-elegance"))
                .thenReturn(Optional.of(affectation));

        LocalDateTime debut = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
        LocalDateTime fin = debut.minusHours(2);

        IndisponibiliteDTORequest request = new IndisponibiliteDTORequest(debut, fin, MotifIndisponibilite.MALADIE, "Erreur dates");

        assertThrows(IllegalArgumentException.class, () ->
                coiffeurSalonService.ajouterIndisponibilite("salon-elegance", request, "coiffeur@salon.com"));
    }

    @Test
    void ajouterIndisponibilite_chevauchement_lanceException() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("coiffeur@salon.com", "salon-elegance"))
                .thenReturn(Optional.of(affectation));

        LocalDateTime debut = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        LocalDateTime fin = debut.plusHours(4);

        IndisponibiliteCoiffeur indExistante = IndisponibiliteCoiffeur.builder().id(99L).build();
        when(indisponibiliteCoiffeurRepository.findByCoiffeurIdAndPeriode(20L, debut, fin))
                .thenReturn(List.of(indExistante));

        IndisponibiliteDTORequest request = new IndisponibiliteDTORequest(debut, fin, MotifIndisponibilite.CONGE, "Déjà pris");

        assertThrows(IllegalArgumentException.class, () ->
                coiffeurSalonService.ajouterIndisponibilite("salon-elegance", request, "coiffeur@salon.com"));
    }

    @Test
    void supprimerIndisponibilite_succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("coiffeur@salon.com", "salon-elegance"))
                .thenReturn(Optional.of(affectation));

        IndisponibiliteCoiffeur ind = IndisponibiliteCoiffeur.builder()
                .id(100L)
                .coiffeur(affectation)
                .dateDebut(LocalDateTime.now().plusDays(2))
                .dateFin(LocalDateTime.now().plusDays(2).plusHours(2))
                .build();

        when(indisponibiliteCoiffeurRepository.findById(100L)).thenReturn(Optional.of(ind));

        coiffeurSalonService.supprimerIndisponibilite("salon-elegance", 100L, "coiffeur@salon.com");

        verify(indisponibiliteCoiffeurRepository).delete(ind);
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.SUPPRESSION), eq("IndisponibiliteCoiffeur"), eq("100"), anyString(), eq("SUPPRIMEE"), eq(affectation), eq("COIFFEUR"));
    }

    @Test
    void supprimerIndisponibilite_dateDebutPassee_lanceIllegalStateException() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("coiffeur@salon.com", "salon-elegance"))
                .thenReturn(Optional.of(affectation));

        IndisponibiliteCoiffeur ind = IndisponibiliteCoiffeur.builder()
                .id(100L)
                .coiffeur(affectation)
                .dateDebut(LocalDateTime.now().minusHours(1))
                .dateFin(LocalDateTime.now().plusHours(2))
                .build();

        when(indisponibiliteCoiffeurRepository.findById(100L)).thenReturn(Optional.of(ind));

        assertThrows(IllegalStateException.class, () ->
                coiffeurSalonService.supprimerIndisponibilite("salon-elegance", 100L, "coiffeur@salon.com"));
    }

    @Test
    void ajouterIndisponibiliteParManager_succes() {
        Compte compteManager = Compte.builder().id(2L).email("manager@salon.com").build();
        AffectationSalon affectationManager = AffectationSalon.builder()
                .id(3L)
                .salon(salon)
                .compte(compteManager)
                .roles(Set.of(RoleSalon.builder().role(TypeRoleSalon.MANAGER).build()))
                .statut(true)
                .build();

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("manager@salon.com", "salon-elegance"))
                .thenReturn(Optional.of(affectationManager));
        when(affectationSalonRepository.findById(20L)).thenReturn(Optional.of(affectation));

        LocalDateTime debut = LocalDateTime.now().plusDays(2);
        LocalDateTime fin = debut.plusHours(3);

        when(indisponibiliteCoiffeurRepository.findByCoiffeurIdAndPeriode(20L, debut, fin))
                .thenReturn(Collections.emptyList());
        when(indisponibiliteCoiffeurRepository.save(any(IndisponibiliteCoiffeur.class))).thenAnswer(i -> {
            IndisponibiliteCoiffeur ind = i.getArgument(0);
            ind.setId(200L);
            return ind;
        });

        IndisponibiliteDTORequest request = new IndisponibiliteDTORequest(20L, debut, fin, MotifIndisponibilite.ABSENCE, "Absence");
        IndisponibiliteDTOResponse expected = new IndisponibiliteDTOResponse(200L, 20L, debut, fin, "ABSENCE", "Absence", LocalDateTime.now());
        when(indisponibiliteDTOResponseMapper.apply(any())).thenReturn(expected);

        IndisponibiliteDTOResponse response = coiffeurSalonService.ajouterIndisponibiliteParManagerOuProprio(
                "salon-elegance", request, "manager@salon.com");

        assertNotNull(response);
        assertEquals("ABSENCE", response.motif());
    }

    @Test
    void modifierIndisponibilite_dateDebutPassee_lanceIllegalStateException() {
        Compte compteManager = Compte.builder().id(2L).email("manager@salon.com").build();
        AffectationSalon affectationManager = AffectationSalon.builder()
                .id(3L)
                .salon(salon)
                .compte(compteManager)
                .roles(Set.of(RoleSalon.builder().role(TypeRoleSalon.MANAGER).build()))
                .statut(true)
                .build();

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("manager@salon.com", "salon-elegance"))
                .thenReturn(Optional.of(affectationManager));

        IndisponibiliteCoiffeur ind = IndisponibiliteCoiffeur.builder()
                .id(100L)
                .coiffeur(affectation)
                .dateDebut(LocalDateTime.now().minusMinutes(5))
                .dateFin(LocalDateTime.now().plusHours(2))
                .build();

        when(indisponibiliteCoiffeurRepository.findByIdAndCoiffeurSalonSlug(100L, "salon-elegance"))
                .thenReturn(Optional.of(ind));

        IndisponibiliteDTORequest request = new IndisponibiliteDTORequest(
                20L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2),
                MotifIndisponibilite.CONGE, "Décalage");

        assertThrows(IllegalStateException.class, () ->
                coiffeurSalonService.modifierIndisponibiliteParManagerOuProprio("salon-elegance", 100L, request, "manager@salon.com"));
    }

    @Test
    void obtenirDashboardCoiffeur_succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("coiffeur@salon.com", "salon-elegance"))
                .thenReturn(Optional.of(affectation));
        when(profilCoiffeurRepository.findByAffectationId(20L))
                .thenReturn(Optional.of(ProfilCoiffeur.builder().affectation(affectation).nomAffichage("Ali Master").anneeExperience(5).build()));

        com.kadi_aon.mon_salon.rendezvous.dto.PlanningRendezVousDTOResponse rdv1 = new com.kadi_aon.mon_salon.rendezvous.dto.PlanningRendezVousDTOResponse(
                1L, 20L, "Traore", "Ali", LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                "Client", "Un", "771111111", 50L, "CONFIRME", new java.math.BigDecimal("5000"), List.of()
        );
        com.kadi_aon.mon_salon.rendezvous.dto.PlanningRendezVousDTOResponse rdv2 = new com.kadi_aon.mon_salon.rendezvous.dto.PlanningRendezVousDTOResponse(
                2L, 20L, "Traore", "Ali", LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(3),
                "Client", "Deux", "772222222", 51L, "TERMINE", new java.math.BigDecimal("7000"), List.of()
        );

        when(receptionnisteSalonService.consulterPlanning(eq("salon-elegance"), any(java.time.LocalDate.class), eq(20L)))
                .thenReturn(List.of(rdv1, rdv2));

        when(prestationRepository.countPrestationsMoisCoiffeur(eq(20L), eq("salon-elegance"), eq(com.kadi_aon.mon_salon.prestation.enums.StatutPrestation.TERMINEE), any(LocalDateTime.class)))
                .thenReturn(18L);

        com.kadi_aon.mon_salon.avis.entity.AvisPrestation avis = com.kadi_aon.mon_salon.avis.entity.AvisPrestation.builder()
                .id(1L)
                .note(5)
                .statut(true)
                .build();
        when(avisPrestationRepository.findByCoiffeurEmailAndSalonSlugAndStatut("coiffeur@salon.com", "salon-elegance", true))
                .thenReturn(List.of(avis));

        when(indisponibiliteCoiffeurRepository.findByCoiffeurIdOrderByDateDebutAsc(20L))
                .thenReturn(List.of());

        com.kadi_aon.mon_salon.coiffeur.dto.CoiffeurDashboardDTOResponse dashboard = coiffeurSalonService.obtenirDashboardCoiffeur("salon-elegance", "coiffeur@salon.com");

        assertNotNull(dashboard);
        assertEquals("Ali Master", dashboard.getNomAffichage());
        assertEquals(2, dashboard.getRdvAujourdhuiTotal());
        assertEquals(1, dashboard.getRdvAujourdhuiTermines());
        assertEquals(1, dashboard.getRdvAujourdhuiEnAttente());
        assertEquals(18L, dashboard.getPrestationsMoisTerminees());
        assertEquals(5.0, dashboard.getNoteMoyenne());
        assertEquals(1L, dashboard.getTotalAvis());
    }

    @Test
    void listerClientsPourCoiffeur_succes_avecFiltre() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("coiffeur@salon.com", "salon-elegance"))
                .thenReturn(Optional.of(affectation));

        Compte c1 = Compte.builder().id(101L).nom("Diop").prenom("Aminata").telephone("771234567").email("ami@test.com").build();
        Compte c2 = Compte.builder().id(102L).nom("Sow").prenom("Moussa").telephone("779876543").email("moussa@test.com").build();

        when(affectationSalonRepository.findComptesBySalonSlugAndRole("salon-elegance", TypeRoleSalon.CLIENT))
                .thenReturn(List.of(c1, c2));

        when(profilCapillaireRepository.existsByCompteId(101L)).thenReturn(true);

        List<com.kadi_aon.mon_salon.salon.dto.pilotage.ClientSalonResumeDTOResponse> result =
                coiffeurSalonService.listerClientsPourCoiffeur("salon-elegance", "ami", "coiffeur@salon.com");

        assertEquals(1, result.size());
        assertEquals("Aminata", result.get(0).getPrenom());
        assertEquals(true, result.get(0).getHasProfilCapillaire());
    }
}

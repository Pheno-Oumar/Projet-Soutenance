package com.kadi_aon.mon_salon.controller.coiffeur;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.kadi_aon.mon_salon.account.dto.ChangementMotDePasseDTORequest;
import com.kadi_aon.mon_salon.account.service.CompteService;
import com.kadi_aon.mon_salon.coiffeur.service.CoiffeurSalonService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.profilcapillaire.dto.CodeProfilVerificationDTORequest;
import com.kadi_aon.mon_salon.profilcapillaire.dto.ProfilCapillaireDTOResponse;
import com.kadi_aon.mon_salon.profilcapillaire.service.ProfilCapillaireService;

@ExtendWith(MockitoExtension.class)
class CoiffeurControllerTest {

        @Mock
        private CoiffeurSalonService coiffeurSalonService;
        @Mock
        private CompteService compteService;
        @Mock
        private ProfilCapillaireService profilCapillaireService;
        @Mock
        private com.kadi_aon.mon_salon.avis.service.AvisSalonService avisSalonService;

        @InjectMocks
        private CoiffeurController coiffeurController;

        private Principal principal;

        @BeforeEach
        void setUp() {
                principal = mock(Principal.class);
                when(principal.getName()).thenReturn("coiffeur@test.com");
        }

        @Test
        void changerMotDePasse_appelleCompteService() {
                ChangementMotDePasseDTORequest request = new ChangementMotDePasseDTORequest("ancien", "nouveau");

                ResponseEntity<APIResponse<Void>> response = coiffeurController.changerMotDePasse("mon-salon", request,
                                principal);

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
                verify(compteService).changerMotDePasse("coiffeur@test.com", request, "mon-salon", "COIFFEUR");
        }

        @Test
        void consulterProfilCapillaireClient_succes() {
                CodeProfilVerificationDTORequest request = new CodeProfilVerificationDTORequest("123456");
                ProfilCapillaireDTOResponse dto = new ProfilCapillaireDTOResponse(
                                1L, 10L, "Nom", "Prenom", "Lisse", "Fin", null, null, null, null, null, null, null,
                                true, null, null);
                when(profilCapillaireService.consulterProfilClientParCoiffeur("mon-salon", 10L, request,
                                "coiffeur@test.com"))
                                .thenReturn(dto);

                ResponseEntity<APIResponse<ProfilCapillaireDTOResponse>> response = coiffeurController
                                .consulterProfilCapillaireClient("mon-salon", 10L, request, principal);

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
                assertEquals("Lisse", response.getBody().getData().typeCheveux());
        }

        @Test
        void getPlanning_succes() {
                java.time.LocalDate date = java.time.LocalDate.now();
                com.kadi_aon.mon_salon.rendezvous.dto.PlanningRendezVousDTOResponse p = new com.kadi_aon.mon_salon.rendezvous.dto.PlanningRendezVousDTOResponse(
                                1L, 10L, "Coiff", "Bob", java.time.LocalDateTime.now(),
                                java.time.LocalDateTime.now().plusMinutes(60),
                                "Client", "Test", "771234567", 10L, "CONFIRME",
                                new java.math.BigDecimal("5000.00"), List.of());

                when(coiffeurSalonService.obtenirPlanningCoiffeur("mon-salon", "coiffeur@test.com", date))
                                .thenReturn(List.of(p));

                ResponseEntity<APIResponse<List<com.kadi_aon.mon_salon.rendezvous.dto.PlanningRendezVousDTOResponse>>> response = coiffeurController
                                .getPlanning("mon-salon", date, principal);

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
                assertEquals(1, response.getBody().getData().size());
                verify(coiffeurSalonService).obtenirPlanningCoiffeur("mon-salon", "coiffeur@test.com", date);
        }

        @Test
        void getAvis_succes() {
                com.kadi_aon.mon_salon.avis.dto.AvisPrestationDTOResponse avis = new com.kadi_aon.mon_salon.avis.dto.AvisPrestationDTOResponse(
                                1L, 10L, 20L, "Coupe", "Dégradé", 5L, "Coiffeur Test", 8L, "Client Test",
                                5, "Super prestation", true, java.time.LocalDateTime.now(), null);

                when(avisSalonService.listerAvisPourCoiffeur("mon-salon", "coiffeur@test.com", true))
                                .thenReturn(List.of(avis));

                ResponseEntity<APIResponse<List<com.kadi_aon.mon_salon.avis.dto.AvisPrestationDTOResponse>>> response = coiffeurController
                                .getAvis("mon-salon", true, principal);

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
                assertEquals(1, response.getBody().getData().size());
                assertEquals(5, response.getBody().getData().get(0).note());
                verify(avisSalonService).listerAvisPourCoiffeur("mon-salon", "coiffeur@test.com", true);
        }

        @Test
        void getDashboard_succes() {
                com.kadi_aon.mon_salon.coiffeur.dto.CoiffeurDashboardDTOResponse dashboard = com.kadi_aon.mon_salon.coiffeur.dto.CoiffeurDashboardDTOResponse.builder()
                                .affectationId(10L)
                                .nomAffichage("Ali Coiffeur")
                                .rdvAujourdhuiTotal(5)
                                .rdvAujourdhuiTermines(2)
                                .rdvAujourdhuiEnAttente(3)
                                .prestationsMoisTerminees(28)
                                .noteMoyenne(4.8)
                                .totalAvis(15)
                                .planningAujourdhui(List.of())
                                .prochainesIndisponibilites(List.of())
                                .build();

                when(coiffeurSalonService.obtenirDashboardCoiffeur("mon-salon", "coiffeur@test.com"))
                                .thenReturn(dashboard);

                ResponseEntity<APIResponse<com.kadi_aon.mon_salon.coiffeur.dto.CoiffeurDashboardDTOResponse>> response = coiffeurController
                                .getDashboard("mon-salon", principal);

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
                assertEquals("Ali Coiffeur", response.getBody().getData().getNomAffichage());
                assertEquals(5, response.getBody().getData().getRdvAujourdhuiTotal());
                assertEquals(4.8, response.getBody().getData().getNoteMoyenne());
                verify(coiffeurSalonService).obtenirDashboardCoiffeur("mon-salon", "coiffeur@test.com");
        }

        @Test
        void getClients_succes() {
                com.kadi_aon.mon_salon.salon.dto.pilotage.ClientSalonResumeDTOResponse client = com.kadi_aon.mon_salon.salon.dto.pilotage.ClientSalonResumeDTOResponse.builder()
                                .clientId(101L)
                                .nom("Diop")
                                .prenom("Aminata")
                                .telephone("771234567")
                                .email("ami@test.com")
                                .hasProfilCapillaire(true)
                                .build();

                when(coiffeurSalonService.listerClientsPourCoiffeur("mon-salon", "Aminata", "coiffeur@test.com"))
                                .thenReturn(List.of(client));

                ResponseEntity<APIResponse<List<com.kadi_aon.mon_salon.salon.dto.pilotage.ClientSalonResumeDTOResponse>>> response = coiffeurController
                                .getClients("mon-salon", "Aminata", principal);

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
                assertEquals(1, response.getBody().getData().size());
                assertEquals("Aminata", response.getBody().getData().get(0).getPrenom());
                org.junit.jupiter.api.Assertions.assertTrue(response.getBody().getData().get(0).getHasProfilCapillaire());
                verify(coiffeurSalonService).listerClientsPourCoiffeur("mon-salon", "Aminata", "coiffeur@test.com");
        }

        @Test
        void ajouterIndisponibilite_succes() {
                com.kadi_aon.mon_salon.coiffeur.dto.IndisponibiliteDTORequest req = new com.kadi_aon.mon_salon.coiffeur.dto.IndisponibiliteDTORequest(
                                null,
                                java.time.LocalDateTime.now().plusDays(1),
                                java.time.LocalDateTime.now().plusDays(2),
                                com.kadi_aon.mon_salon.coiffeur.enums.MotifIndisponibilite.CONGE,
                                "Vacances"
                );
                com.kadi_aon.mon_salon.coiffeur.dto.IndisponibiliteDTOResponse res = new com.kadi_aon.mon_salon.coiffeur.dto.IndisponibiliteDTOResponse(
                                1L, 10L,
                                req.dateDebut(), req.dateFin(), req.motif().name(), req.commentaire(),
                                java.time.LocalDateTime.now()
                );

                when(coiffeurSalonService.ajouterIndisponibilite("mon-salon", req, "coiffeur@test.com"))
                                .thenReturn(res);

                ResponseEntity<APIResponse<com.kadi_aon.mon_salon.coiffeur.dto.IndisponibiliteDTOResponse>> response = coiffeurController
                                .ajouterIndisponibilite("mon-salon", req, principal);

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
                assertEquals(1L, response.getBody().getData().id());
                verify(coiffeurSalonService).ajouterIndisponibilite("mon-salon", req, "coiffeur@test.com");
        }

        @Test
        void supprimerIndisponibilite_succes() {
                ResponseEntity<APIResponse<Void>> response = coiffeurController
                                .supprimerIndisponibilite("mon-salon", 42L, principal);

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
                verify(coiffeurSalonService).supprimerIndisponibilite("mon-salon", 42L, "coiffeur@test.com");
        }
}

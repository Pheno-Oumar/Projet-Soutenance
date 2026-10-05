package com.kadi_aon.mon_salon.facturation.service;

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
import java.util.ArrayList;
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
import com.kadi_aon.mon_salon.caisse.entity.OperationCaisse;
import com.kadi_aon.mon_salon.caisse.entity.SessionCaisse;
import com.kadi_aon.mon_salon.caisse.enums.StatutSessionCaisse;
import com.kadi_aon.mon_salon.caisse.exception.SessionCaisseFermeeException;
import com.kadi_aon.mon_salon.caisse.service.CaisseSalonService;
import com.kadi_aon.mon_salon.facturation.dto.PaiementDTORequest;
import com.kadi_aon.mon_salon.facturation.dto.PaiementDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.RemboursementDTORequest;
import com.kadi_aon.mon_salon.facturation.entity.Facture;
import com.kadi_aon.mon_salon.facturation.entity.Paiement;
import com.kadi_aon.mon_salon.facturation.enums.StatutPaiement;
import com.kadi_aon.mon_salon.facturation.enums.TypePaiement;
import com.kadi_aon.mon_salon.facturation.repository.FactureRepository;
import com.kadi_aon.mon_salon.facturation.repository.PaiementRepository;
import com.kadi_aon.mon_salon.prestation.entity.Prestation;
import com.kadi_aon.mon_salon.prestation.enums.StatutPrestation;
import com.kadi_aon.mon_salon.prestation.repository.PrestationRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;

@ExtendWith(MockitoExtension.class)
class FacturationSalonServiceTest {

    @Mock
    private FactureRepository factureRepository;
    @Mock
    private PaiementRepository paiementRepository;
    @Mock
    private PrestationRepository prestationRepository;
    @Mock
    private CaisseSalonService caisseSalonService;
    @Mock
    private AffectationSalonRepository affectationSalonRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private FacturationSalonService facturationSalonService;

    private Salon salon;
    private Compte employe;
    private AffectationSalon affectation;
    private Prestation prestation;
    private Facture facture;
    private SessionCaisse sessionCaisse;

    @BeforeEach
    void setUp() {
        salon = Salon.builder().id(1L).slug("salon-chic").nom("Salon Chic").statut(true).build();
        employe = Compte.builder().id(10L).nom("Recep").prenom("Alice").email("recep@test.com").build();

        RoleSalon roleRecep = RoleSalon.builder().id(1L).role(TypeRoleSalon.RECEPTIONNISTE).build();
        Set<RoleSalon> roles = new HashSet<>();
        roles.add(roleRecep);

        affectation = AffectationSalon.builder().id(100L).salon(salon).compte(employe).roles(roles).statut(true).build();

        prestation = Prestation.builder()
                .id(50L)
                .salon(salon)
                .nomClient("Client")
                .prenomClient("Test")
                .montantTotal(new BigDecimal("50.00"))
                .statut(StatutPrestation.EN_COURS)
                .build();

        facture = Facture.builder()
                .id(200L)
                .numeroFacture("FAC-2026-0001")
                .montantTotal(new BigDecimal("50.00"))
                .prestation(prestation)
                .paiements(new ArrayList<>())
                .build();

        sessionCaisse = SessionCaisse.builder()
                .id(1L)
                .affectation(affectation)
                .statut(StatutSessionCaisse.EN_COURS)
                .soldeOuverture(BigDecimal.ZERO)
                .build();
    }

    @Test
    void creerFactureInitiale_succes() {
        when(factureRepository.save(any(Facture.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Facture f = facturationSalonService.creerFactureInitiale(prestation);

        assertNotNull(f);
        assertNotNull(f.getNumeroFacture());
        assertEquals(new BigDecimal("50.00"), f.getMontantTotal());
        assertEquals(prestation, f.getPrestation());
        verify(factureRepository).save(any(Facture.class));
    }

    @Test
    void encaisserPaiement_acompte_succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("recep@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectation));
        when(caisseSalonService.obtenirSessionActive("salon-chic")).thenReturn(sessionCaisse);
        when(factureRepository.findByIdAndPrestationSalonSlug(200L, "salon-chic")).thenReturn(Optional.of(facture));

        when(caisseSalonService.enregistrerOperationEntree(eq(sessionCaisse), eq(new BigDecimal("20.00")), anyString(), any()))
                .thenReturn(OperationCaisse.builder().id(999L).build());

        when(paiementRepository.save(any(Paiement.class))).thenAnswer(invocation -> {
            Paiement p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        PaiementDTORequest request = new PaiementDTORequest(new BigDecimal("20.00"), "ACOMPTE", "Acompte réservation");
        PaiementDTOResponse response = facturationSalonService.encaisserPaiement("salon-chic", 200L, "recep@test.com", request);

        assertNotNull(response);
        assertEquals(new BigDecimal("20.00"), response.montant());
        assertEquals("ACOMPTE", response.type());
        assertEquals("PAYE", response.statut());
        assertEquals(StatutPrestation.EN_COURS, prestation.getStatut()); // Pas encore soldée
    }

    @Test
    void encaisserPaiement_soldeComplet_terminePrestation() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("recep@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectation));
        when(caisseSalonService.obtenirSessionActive("salon-chic")).thenReturn(sessionCaisse);
        when(factureRepository.findByIdAndPrestationSalonSlug(200L, "salon-chic")).thenReturn(Optional.of(facture));

        when(caisseSalonService.enregistrerOperationEntree(eq(sessionCaisse), eq(new BigDecimal("50.00")), anyString(), any()))
                .thenReturn(OperationCaisse.builder().id(999L).build());

        when(paiementRepository.save(any(Paiement.class))).thenAnswer(invocation -> {
            Paiement p = invocation.getArgument(0);
            p.setId(2L);
            return p;
        });

        PaiementDTORequest request = new PaiementDTORequest(new BigDecimal("50.00"), "SOLDE", "Paiement intégral");
        PaiementDTOResponse response = facturationSalonService.encaisserPaiement("salon-chic", 200L, "recep@test.com", request);

        assertNotNull(response);
        assertEquals(new BigDecimal("50.00"), response.montant());
        assertEquals(StatutPrestation.TERMINEE, prestation.getStatut()); // Soldée donc TERMINEE
        verify(prestationRepository).save(prestation);
    }

    @Test
    void encaisserPaiement_sessionCaisseFermee_lanceSessionCaisseFermeeException() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("recep@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectation));
        when(caisseSalonService.obtenirSessionActive("salon-chic"))
                .thenThrow(new SessionCaisseFermeeException("Caisse fermée"));

        PaiementDTORequest request = new PaiementDTORequest(new BigDecimal("50.00"), "SOLDE", null);

        assertThrows(SessionCaisseFermeeException.class, () ->
                facturationSalonService.encaisserPaiement("salon-chic", 200L, "recep@test.com", request));
    }

    @Test
    void effectuerRemboursement_succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("recep@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectation));
        when(caisseSalonService.obtenirSessionActive("salon-chic")).thenReturn(sessionCaisse);

        Paiement paiementInitial = Paiement.builder()
                .id(1L)
                .numeroPaiement("PAI-001")
                .montant(new BigDecimal("50.00"))
                .statut(StatutPaiement.PAYE)
                .type(TypePaiement.SOLDE)
                .facture(facture)
                .salon(salon)
                .build();

        when(paiementRepository.findByIdAndSalonSlug(1L, "salon-chic")).thenReturn(Optional.of(paiementInitial));

        when(caisseSalonService.enregistrerOperationSortie(eq(sessionCaisse), eq(new BigDecimal("50.00")), anyString(), any()))
                .thenReturn(OperationCaisse.builder().id(998L).build());

        when(paiementRepository.save(any(Paiement.class))).thenAnswer(invocation -> {
            Paiement p = invocation.getArgument(0);
            p.setId(3L);
            return p;
        });

        RemboursementDTORequest request = new RemboursementDTORequest(new BigDecimal("50.00"), "Client insatisfait");
        PaiementDTOResponse response = facturationSalonService.effectuerRemboursement("salon-chic", 1L, "recep@test.com", request);

        assertNotNull(response);
        assertEquals(StatutPaiement.REMBOURSE.name(), response.statut());
        assertEquals(new BigDecimal("50.00"), response.montant());
        verify(caisseSalonService).enregistrerOperationSortie(eq(sessionCaisse), eq(new BigDecimal("50.00")), anyString(), any());
    }

    @Test
    void encaisserPaiement_nonReceptionniste_lanceAccessDeniedException() {
        RoleSalon roleCoiffeur = RoleSalon.builder().id(2L).role(TypeRoleSalon.COIFFEUR).build();
        AffectationSalon affectationCoiffeur = AffectationSalon.builder()
                .id(101L).salon(salon).compte(employe).roles(Set.of(roleCoiffeur)).statut(true).build();

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("recep@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationCoiffeur));

        PaiementDTORequest request = new PaiementDTORequest(new BigDecimal("20.00"), "SOLDE", "Paiement");

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
                facturationSalonService.encaisserPaiement("salon-chic", 200L, "recep@test.com", request));
    }
}

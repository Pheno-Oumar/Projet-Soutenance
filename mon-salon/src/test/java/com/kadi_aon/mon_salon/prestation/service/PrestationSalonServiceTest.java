package com.kadi_aon.mon_salon.prestation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.facturation.entity.Facture;
import com.kadi_aon.mon_salon.facturation.service.FacturationSalonService;
import com.kadi_aon.mon_salon.prestation.dto.LignePrestationDTORequest;
import com.kadi_aon.mon_salon.prestation.dto.PrestationCreateDTORequest;
import com.kadi_aon.mon_salon.prestation.dto.PrestationDTOResponse;
import com.kadi_aon.mon_salon.prestation.entity.Prestation;
import com.kadi_aon.mon_salon.prestation.enums.StatutPrestation;
import com.kadi_aon.mon_salon.prestation.repository.PrestationRepository;
import com.kadi_aon.mon_salon.rendezvous.entity.RendezVous;
import com.kadi_aon.mon_salon.rendezvous.enums.StatutRendezVous;
import com.kadi_aon.mon_salon.rendezvous.repository.RendezVousRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.entity.ServiceSalon;
import com.kadi_aon.mon_salon.salon.entity.VarianteService;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;
import com.kadi_aon.mon_salon.salon.repository.VarianteServiceRepository;

@ExtendWith(MockitoExtension.class)
class PrestationSalonServiceTest {

    @Mock
    private PrestationRepository prestationRepository;
    @Mock
    private RendezVousRepository rendezVousRepository;
    @Mock
    private SalonRepository salonRepository;
    @Mock
    private CompteRepository compteRepository;
    @Mock
    private VarianteServiceRepository varianteServiceRepository;
    @Mock
    private AffectationSalonRepository affectationSalonRepository;
    @Mock
    private FacturationSalonService facturationSalonService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private PrestationSalonService prestationSalonService;

    private Salon salon;
    private Compte receptionniste;
    private Compte coiffeur;
    private AffectationSalon affectationRecep;
    private AffectationSalon affectationCoiffeur;
    private VarianteService variante;

    @BeforeEach
    void setUp() {
        salon = Salon.builder().id(1L).slug("salon-chic").nom("Salon Chic").statut(true).build();
        receptionniste = Compte.builder().id(10L).nom("Recep").prenom("Alice").email("recep@test.com").build();
        coiffeur = Compte.builder().id(20L).nom("Coiff").prenom("Bob").email("coiffeur@test.com").build();

        RoleSalon roleRecep = RoleSalon.builder().id(1L).role(TypeRoleSalon.RECEPTIONNISTE).build();
        Set<RoleSalon> rolesR = new HashSet<>();
        rolesR.add(roleRecep);

        affectationRecep = AffectationSalon.builder()
                .id(100L).salon(salon).compte(receptionniste).roles(rolesR).statut(true).build();

        RoleSalon roleCoiff = RoleSalon.builder().id(2L).role(TypeRoleSalon.COIFFEUR).build();
        Set<RoleSalon> rolesC = new HashSet<>();
        rolesC.add(roleCoiff);

        affectationCoiffeur = AffectationSalon.builder()
                .id(101L).salon(salon).compte(coiffeur).roles(rolesC).statut(true).build();

        ServiceSalon service = ServiceSalon.builder().id(5L).nom("Coupe").salon(salon).build();
        variante = VarianteService.builder().id(50L).nom("Dégradé").serviceSalon(service).build();
    }

    @Test
    void creerPrestation_depuisRendezVous_succes() {
        Compte client = Compte.builder().id(30L).nom("Client").prenom("David").telephone("0123456789").email("client@test.com").build();

        RendezVous rdv = RendezVous.builder()
                .id(200L)
                .salon(salon)
                .coiffeur(affectationCoiffeur)
                .client(client)
                .statut(StatutRendezVous.CONFIRME)
                .build();

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("recep@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationRecep));
        when(rendezVousRepository.findById(200L)).thenReturn(Optional.of(rdv));
        when(prestationRepository.findByRendezVousId(200L)).thenReturn(Optional.empty());

        when(varianteServiceRepository.findById(50L)).thenReturn(Optional.of(variante));

        when(prestationRepository.save(any(Prestation.class))).thenAnswer(invocation -> {
            Prestation p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        LignePrestationDTORequest ligneReq = new LignePrestationDTORequest(50L, new BigDecimal("25.00"));
        PrestationCreateDTORequest request = new PrestationCreateDTORequest(200L, null, null, null, null, null, List.of(ligneReq));

        PrestationDTOResponse response = prestationSalonService.creerPrestation("salon-chic", "recep@test.com", request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(new BigDecimal("25.00"), response.montantTotal());
        assertEquals("EN_COURS", response.statut());
        assertEquals(StatutRendezVous.EN_COURS, rdv.getStatut());
    }

    @Test
    void creerPrestation_walkIn_succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("recep@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationRecep));

        when(affectationSalonRepository.findById(101L)).thenReturn(Optional.of(affectationCoiffeur));
        when(varianteServiceRepository.findById(50L)).thenReturn(Optional.of(variante));

        when(prestationRepository.save(any(Prestation.class))).thenAnswer(invocation -> {
            Prestation p = invocation.getArgument(0);
            p.setId(2L);
            return p;
        });

        LignePrestationDTORequest ligneReq = new LignePrestationDTORequest(50L, new BigDecimal("30.00"));
        PrestationCreateDTORequest request = new PrestationCreateDTORequest(null, 101L, null, "Passant", "Paul", "0600000000", List.of(ligneReq));

        PrestationDTOResponse response = prestationSalonService.creerPrestation("salon-chic", "recep@test.com", request);

        assertNotNull(response);
        assertEquals(2L, response.id());
        assertEquals("Passant", response.nomClient());
        assertEquals(new BigDecimal("30.00"), response.montantTotal());
    }

    @Test
    void terminerPrestation_succes() {
        RendezVous rdv = RendezVous.builder()
                .id(200L)
                .salon(salon)
                .coiffeur(affectationCoiffeur)
                .statut(StatutRendezVous.EN_COURS)
                .build();

        Facture facture = Facture.builder().id(300L).numeroFacture("FAC-2026-0001").montantTotal(new BigDecimal("25.00")).build();

        Prestation prestation = Prestation.builder()
                .id(1L)
                .salon(salon)
                .coiffeur(affectationCoiffeur)
                .rendezVous(rdv)
                .nomClient("Client")
                .prenomClient("David")
                .telephoneClient("0123456789")
                .montantTotal(new BigDecimal("25.00"))
                .statut(StatutPrestation.EN_COURS)
                .facture(facture)
                .build();

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("recep@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationRecep));
        when(prestationRepository.findByIdAndSalonSlug(1L, "salon-chic"))
                .thenReturn(Optional.of(prestation));
        when(prestationRepository.save(any(Prestation.class))).thenAnswer(i -> i.getArgument(0));

        PrestationDTOResponse response = prestationSalonService.terminerPrestation("salon-chic", 1L, "recep@test.com");

        assertNotNull(response);
        assertEquals("TERMINEE", response.statut());
        assertEquals(StatutRendezVous.TERMINE, rdv.getStatut());
        assertNotNull(prestation.getDateHeureFin());
    }
}

package com.kadi_aon.mon_salon.salon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.depense.dto.DepenseDTOResponse;
import com.kadi_aon.mon_salon.depense.repository.DepenseRepository;
import com.kadi_aon.mon_salon.depense.service.DepenseSalonService;
import com.kadi_aon.mon_salon.facturation.dto.FactureDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.PaiementDTOResponse;
import com.kadi_aon.mon_salon.facturation.entity.Facture;
import com.kadi_aon.mon_salon.facturation.entity.Paiement;
import com.kadi_aon.mon_salon.facturation.enums.StatutPaiement;
import com.kadi_aon.mon_salon.facturation.enums.TypePaiement;
import com.kadi_aon.mon_salon.facturation.repository.FactureRepository;
import com.kadi_aon.mon_salon.facturation.repository.PaiementRepository;
import com.kadi_aon.mon_salon.facturation.service.FacturationSalonService;
import com.kadi_aon.mon_salon.prestation.dto.PrestationDTOResponse;
import com.kadi_aon.mon_salon.prestation.entity.LignePrestation;
import com.kadi_aon.mon_salon.prestation.entity.Prestation;
import com.kadi_aon.mon_salon.prestation.enums.StatutPrestation;
import com.kadi_aon.mon_salon.prestation.repository.LignePrestationRepository;
import com.kadi_aon.mon_salon.prestation.repository.PrestationRepository;
import com.kadi_aon.mon_salon.prestation.service.PrestationSalonService;
import com.kadi_aon.mon_salon.profilcapillaire.dto.ProfilCapillaireDTOResponse;
import com.kadi_aon.mon_salon.profilcapillaire.entity.ProfilCapillaire;
import com.kadi_aon.mon_salon.profilcapillaire.mapper.ProfilCapillaireDTOResponseMapper;
import com.kadi_aon.mon_salon.profilcapillaire.repository.ProfilCapillaireRepository;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.entity.RendezVous;
import com.kadi_aon.mon_salon.rendezvous.enums.StatutRendezVous;
import com.kadi_aon.mon_salon.rendezvous.mapper.RendezVousDTOResponseMapper;
import com.kadi_aon.mon_salon.rendezvous.repository.RendezVousRepository;
import com.kadi_aon.mon_salon.salon.dto.pilotage.ClientSalonResumeDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.pilotage.FicheClientCompleteDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.pilotage.KpiSalonDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.pilotage.PerformanceCoiffeurDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.pilotage.StockSyntheseDTOResponse;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;
import com.kadi_aon.mon_salon.stock.entity.CategorieProduit;
import com.kadi_aon.mon_salon.stock.entity.Produit;
import com.kadi_aon.mon_salon.stock.entity.StockProduit;
import com.kadi_aon.mon_salon.stock.repository.ProduitRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProprietairePilotageServiceTest {

    @Mock
    private SalonRepository salonRepository;
    @Mock
    private AffectationSalonRepository affectationSalonRepository;
    @Mock
    private CompteRepository compteRepository;
    @Mock
    private LignePrestationRepository lignePrestationRepository;
    @Mock
    private RendezVousRepository rendezVousRepository;
    @Mock
    private PrestationRepository prestationRepository;
    @Mock
    private PaiementRepository paiementRepository;
    @Mock
    private DepenseRepository depenseRepository;
    @Mock
    private DepenseSalonService depenseSalonService;
    @Mock
    private ProduitRepository produitRepository;
    @Mock
    private ProfilCapillaireRepository profilCapillaireRepository;
    @Mock
    private FactureRepository factureRepository;
    @Mock
    private FacturationSalonService facturationSalonService;
    @Mock
    private PrestationSalonService prestationSalonService;
    @Mock
    private RendezVousDTOResponseMapper rendezVousMapper;
    @Mock
    private ProfilCapillaireDTOResponseMapper profilCapillaireMapper;

    @InjectMocks
    private ProprietairePilotageService proprietairePilotageService;

    private Salon salon;
    private Compte coiffeur;
    private Compte client;

    @BeforeEach
    void setUp() {
        salon = Salon.builder()
                .id(1L)
                .nom("Salon Royal")
                .slug("salon-royal")
                .build();

        coiffeur = Compte.builder()
                .id(10L)
                .nom("Kone")
                .prenom("Bakary")
                .email("bakary@test.com")
                .build();

        client = Compte.builder()
                .id(20L)
                .nom("Traore")
                .prenom("Fatou")
                .email("fatou@test.com")
                .telephone("77123456")
                .dateNaissance(LocalDate.of(1995, 5, 10))
                .dateCreation(LocalDateTime.now())
                .build();
    }

    @Test
    void obtenirPerformancesCoiffeurs_succes() {
        when(salonRepository.existsBySlug("salon-royal")).thenReturn(true);
        when(affectationSalonRepository.findComptesBySalonSlugAndRole("salon-royal", TypeRoleSalon.COIFFEUR))
                .thenReturn(List.of(coiffeur));

        LignePrestation lp1 = LignePrestation.builder().prixReel(new BigDecimal("5000.00")).build();
        LignePrestation lp2 = LignePrestation.builder().prixReel(new BigDecimal("7000.00")).build();

        when(lignePrestationRepository.findLignesTermineesByCoiffeurAndSalon(10L, "salon-royal"))
                .thenReturn(List.of(lp1, lp2));

        List<PerformanceCoiffeurDTOResponse> performances = proprietairePilotageService.obtenirPerformancesCoiffeurs("salon-royal");

        assertEquals(1, performances.size());
        assertEquals("Bakary", performances.get(0).getPrenom());
        assertEquals(2L, performances.get(0).getNombrePrestations());
        assertEquals(new BigDecimal("12000.00"), performances.get(0).getChiffreAffaires());
    }

    @Test
    void obtenirKpiSalon_succes() {
        when(salonRepository.findBySlug("salon-royal")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.countBySalonSlugAndRole("salon-royal", TypeRoleSalon.CLIENT)).thenReturn(30L);
        when(affectationSalonRepository.countBySalonSlugAndRole("salon-royal", TypeRoleSalon.COIFFEUR)).thenReturn(4L);

        when(rendezVousRepository.countBySalonSlug("salon-royal")).thenReturn(50L);
        when(rendezVousRepository.countBySalonSlugAndStatut("salon-royal", StatutRendezVous.TERMINE)).thenReturn(40L);
        when(rendezVousRepository.countBySalonSlugAndStatut("salon-royal", StatutRendezVous.ANNULE)).thenReturn(5L);

        when(prestationRepository.countBySalonSlug("salon-royal")).thenReturn(45L);
        when(prestationRepository.countBySalonSlugAndStatut("salon-royal", StatutPrestation.TERMINEE)).thenReturn(42L);

        when(paiementRepository.totalPaiementsSalon("salon-royal")).thenReturn(new BigDecimal("200000.00"));
        when(depenseRepository.totalDepensesSalon("salon-royal")).thenReturn(new BigDecimal("50000.00"));

        KpiSalonDTOResponse kpis = proprietairePilotageService.obtenirKpiSalon("salon-royal");

        assertNotNull(kpis);
        assertEquals("salon-royal", kpis.getSlugSalon());
        assertEquals(30L, kpis.getNombreClients());
        assertEquals(4L, kpis.getNombreCoiffeurs());
        assertEquals(new BigDecimal("200000.00"), kpis.getTotalRevenus());
        assertEquals(new BigDecimal("50000.00"), kpis.getTotalDepenses());
        assertEquals(new BigDecimal("150000.00"), kpis.getBeneficeNet());
    }

    @Test
    void obtenirSyntheseStock_detecteAlerteStockBas() {
        when(salonRepository.existsBySlug("salon-royal")).thenReturn(true);

        CategorieProduit cat = CategorieProduit.builder().nom("Shampoings").build();
        Produit p = Produit.builder()
                .id(1L)
                .nom("Shampoing Bio")
                .prixVente(new BigDecimal("3500.00"))
                .categorie(cat)
                .build();
        StockProduit stock = StockProduit.builder()
                .quantiteDisponible(2)
                .seuilMinimum(5)
                .seuilMaximum(50)
                .build();
        p.setStock(stock);

        when(produitRepository.findByCategorieSalonSlug("salon-royal")).thenReturn(List.of(p));

        List<StockSyntheseDTOResponse> stockList = proprietairePilotageService.obtenirSyntheseStock("salon-royal");

        assertEquals(1, stockList.size());
        assertEquals("Shampoing Bio", stockList.get(0).getProduitNom());
        assertEquals(2, stockList.get(0).getQuantiteDisponible());
        assertTrue(stockList.get(0).isAlerteStockBas());
    }

    @Test
    void listerClientsDuSalon_succes() {
        when(salonRepository.existsBySlug("salon-royal")).thenReturn(true);
        when(affectationSalonRepository.findComptesBySalonSlugAndRole("salon-royal", TypeRoleSalon.CLIENT))
                .thenReturn(List.of(client));

        List<ClientSalonResumeDTOResponse> clients = proprietairePilotageService.listerClientsDuSalon("salon-royal");

        assertEquals(1, clients.size());
        assertEquals("Fatou", clients.get(0).getPrenom());
        assertEquals("77123456", clients.get(0).getTelephone());
    }

    @Test
    void obtenirFicheClientComplete_succes() {
        when(salonRepository.existsBySlug("salon-royal")).thenReturn(true);
        when(affectationSalonRepository.isCompteRoleDuSalon("salon-royal", 20L, TypeRoleSalon.CLIENT)).thenReturn(true);
        when(compteRepository.findById(20L)).thenReturn(Optional.of(client));

        ProfilCapillaire profil = ProfilCapillaire.builder().compte(client).build();
        when(profilCapillaireRepository.findByCompteId(20L)).thenReturn(Optional.of(profil));

        ProfilCapillaireDTOResponse profilDTO = new ProfilCapillaireDTOResponse(
                1L, 20L, "Traore", "Fatou", "CREPUS", "SECS", "NORMALE", "NORMALE",
                null, null, null, null, null, false, LocalDateTime.now(), LocalDateTime.now()
        );
        when(profilCapillaireMapper.apply(profil)).thenReturn(profilDTO);

        RendezVous rdv = RendezVous.builder().id(101L).build();
        when(rendezVousRepository.findByClientIdAndSalonSlugOrderByDateHeurePrevueDesc(20L, "salon-royal"))
                .thenReturn(List.of(rdv));
        RendezVousDTOResponse rdvDTO = new RendezVousDTOResponse(
                101L, "salon-royal", "Salon Royal", 15L, "Diop", "Ali", "Traore", "Fatou", "77123456",
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), "CONFIRME", BigDecimal.valueOf(5000), LocalDateTime.now(), List.of()
        );
        when(rendezVousMapper.apply(rdv)).thenReturn(rdvDTO);

        Prestation prest = Prestation.builder().id(201L).build();
        when(prestationRepository.findByClientIdAndSalonSlugOrderByDateHeureDebutDesc(20L, "salon-royal"))
                .thenReturn(List.of(prest));
        PrestationDTOResponse prestDTO = new PrestationDTOResponse(
                201L, "salon-royal", 15L, "Diop", "Ali", 20L, "Traore", "Fatou", "77123456",
                101L, LocalDateTime.now(), LocalDateTime.now().plusHours(1), new BigDecimal("10000.00"), "TERMINEE", null, List.of()
        );
        when(prestationSalonService.mapToResponse(prest)).thenReturn(prestDTO);

        Facture fac = Facture.builder().id(301L).build();
        when(factureRepository.findByPrestationClientIdAndPrestationSalonSlugOrderByDateEmissionDesc(20L, "salon-royal"))
                .thenReturn(List.of(fac));
        FactureDTOResponse facDTO = new FactureDTOResponse(
                301L, "FAC-001", LocalDateTime.now(), new BigDecimal("10000.00"), new BigDecimal("10000.00"), BigDecimal.ZERO, 201L, List.of()
        );
        when(facturationSalonService.mapFactureToResponse(fac)).thenReturn(facDTO);

        Paiement pai = Paiement.builder().id(401L).build();
        when(paiementRepository.findByClientIdAndSalonSlugOrderByDatePaiementDesc(20L, "salon-royal"))
                .thenReturn(List.of(pai));
        PaiementDTOResponse paiDTO = new PaiementDTOResponse(
                401L, "PAI-001", new BigDecimal("10000.00"), LocalDateTime.now(), "ESPECES", "PAYE", 301L, "FAC-001", 201L, "salon-royal", "Traore", "Fatou"
        );
        when(facturationSalonService.mapPaiementToResponse(pai)).thenReturn(paiDTO);

        FicheClientCompleteDTOResponse fiche = proprietairePilotageService.obtenirFicheClientComplete("salon-royal", 20L);

        assertNotNull(fiche);
        assertEquals(20L, fiche.getClientId());
        assertEquals("fatou@test.com", fiche.getEmail());
        assertNotNull(fiche.getProfilCapillaire());
        assertEquals(1, fiche.getRendezVous().size());
        assertEquals(1, fiche.getPrestations().size());
        assertEquals(1, fiche.getFactures().size());
        assertEquals(1, fiche.getPaiements().size());
    }

    @Test
    void obtenirFicheClientComplete_clientNonAssocieAuSalon_lanceException() {
        when(salonRepository.existsBySlug("salon-royal")).thenReturn(true);
        when(affectationSalonRepository.isCompteRoleDuSalon("salon-royal", 99L, TypeRoleSalon.CLIENT)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> proprietairePilotageService.obtenirFicheClientComplete("salon-royal", 99L));
    }
}

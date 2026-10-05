package com.kadi_aon.mon_salon.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.facturation.entity.Facture;
import com.kadi_aon.mon_salon.facturation.repository.FactureRepository;
import com.kadi_aon.mon_salon.notification.service.NotificationEmailService;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.stock.dto.CommandeDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.KpiStockDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.ProduitAlerteStockDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.RejetCommandeDTORequest;
import com.kadi_aon.mon_salon.stock.dto.RetraitCommandeDTORequest;
import com.kadi_aon.mon_salon.stock.entity.CategorieProduit;
import com.kadi_aon.mon_salon.stock.entity.Commande;
import com.kadi_aon.mon_salon.stock.entity.LigneCommande;
import com.kadi_aon.mon_salon.stock.entity.LignePanier;
import com.kadi_aon.mon_salon.stock.entity.MouvementStock;
import com.kadi_aon.mon_salon.stock.entity.Panier;
import com.kadi_aon.mon_salon.stock.entity.Produit;
import com.kadi_aon.mon_salon.stock.entity.StockProduit;
import com.kadi_aon.mon_salon.stock.enums.StatutCommande;
import com.kadi_aon.mon_salon.stock.exception.StockInsuffisantException;
import com.kadi_aon.mon_salon.stock.repository.CommandeRepository;
import com.kadi_aon.mon_salon.stock.repository.MouvementStockRepository;
import com.kadi_aon.mon_salon.stock.repository.ProduitRepository;
import com.kadi_aon.mon_salon.stock.repository.StockProduitRepository;

@ExtendWith(MockitoExtension.class)
class CommandeSalonServiceTest {

    @Mock
    private CommandeRepository commandeRepository;

    @Mock
    private PanierService panierService;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private StockProduitRepository stockProduitRepository;

    @Mock
    private MouvementStockRepository mouvementStockRepository;

    @Mock
    private FactureRepository factureRepository;

    @Mock
    private AffectationSalonRepository affectationSalonRepository;

    @Mock
    private NotificationEmailService notificationEmailService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private com.kadi_aon.mon_salon.salon.repository.SalonRepository salonRepository;

    @Mock
    private com.kadi_aon.mon_salon.account.repository.CompteRepository compteRepository;

    @Mock
    private com.kadi_aon.mon_salon.salon.repository.RoleSalonRepository roleSalonRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks
    private CommandeSalonService commandeSalonService;

    private Salon salon;
    private Compte clientCompte;
    private Compte responsableCompte;
    private AffectationSalon affectationClient;
    private AffectationSalon affectationResponsable;
    private CategorieProduit categorie;
    private Produit produit;
    private StockProduit stock;
    private Panier panier;

    @BeforeEach
    void setUp() {
        salon = Salon.builder().id(1L).nom("Salon Chic").slug("salon-chic").build();
        clientCompte = Compte.builder().id(10L).email("client@test.com").prenom("Awa").nom("Ba").build();
        responsableCompte = Compte.builder().id(11L).email("stock@test.com").prenom("Moussa").nom("Traore").build();

        RoleSalon roleClient = RoleSalon.builder().id(1L).role(TypeRoleSalon.CLIENT).build();
        affectationClient = AffectationSalon.builder()
                .id(100L)
                .salon(salon)
                .compte(clientCompte)
                .roles(Set.of(roleClient))
                .statut(true)
                .build();

        RoleSalon roleStock = RoleSalon.builder().id(2L).role(TypeRoleSalon.RESPONSABLE_STOCK).build();
        affectationResponsable = AffectationSalon.builder()
                .id(101L)
                .salon(salon)
                .compte(responsableCompte)
                .roles(Set.of(roleStock))
                .statut(true)
                .build();

        categorie = CategorieProduit.builder().id(5L).nom("Soins").salon(salon).build();

        produit = Produit.builder()
                .id(20L)
                .nom("Huile de Ricin")
                .prixVente(new BigDecimal("4000.00"))
                .statut(true)
                .categorie(categorie)
                .build();

        stock = StockProduit.builder()
                .id(30L)
                .produit(produit)
                .quantiteDisponible(10)
                .seuilMinimum(3)
                .build();
        produit.setStock(stock);

        panier = Panier.builder()
                .id(50L)
                .clientAffectation(affectationClient)
                .lignes(new ArrayList<>())
                .build();
    }

    @Test
    void passerCommande_succes_creeCommandeEtEnvoieEmail() {
        LignePanier lp = LignePanier.builder().id(1L).panier(panier).produit(produit).quantite(2).build();
        panier.getLignes().add(lp);

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("client@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationClient));
        when(panierService.getOrCreatePanier(affectationClient)).thenReturn(panier);
        when(commandeRepository.save(any(Commande.class))).thenAnswer(i -> {
            Commande c = i.getArgument(0);
            c.setId(200L);
            return c;
        });

        CommandeDTOResponse response = commandeSalonService.passerCommande("salon-chic", "client@test.com");

        assertNotNull(response);
        assertEquals("EN_ATTENTE", response.statut());
        assertEquals(new BigDecimal("8000.00"), response.montantTotal());
        assertEquals(1, response.lignes().size());
        verify(panierService).viderPanier("salon-chic", "client@test.com");
        verify(notificationEmailService).queueNotificationEmail(eq("client@test.com"), contains("Confirmation de votre commande"), any());
    }

    @Test
    void passerCommande_panierVide_leveException() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("client@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationClient));
        when(panierService.getOrCreatePanier(affectationClient)).thenReturn(panier);

        assertThrows(IllegalStateException.class, () ->
                commandeSalonService.passerCommande("salon-chic", "client@test.com"));
    }

    @Test
    void passerCommande_stockInsuffisant_leveException() {
        LignePanier lp = LignePanier.builder().id(1L).panier(panier).produit(produit).quantite(15).build();
        panier.getLignes().add(lp);

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("client@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationClient));
        when(panierService.getOrCreatePanier(affectationClient)).thenReturn(panier);

        assertThrows(StockInsuffisantException.class, () ->
                commandeSalonService.passerCommande("salon-chic", "client@test.com"));
    }

    @Test
    void validerCommande_succes_decrementeStock_genereFactureEtCodeRetrait() {
        Commande commande = Commande.builder()
                .id(200L)
                .numeroCommande("CMD-20260922-001")
                .statut(StatutCommande.EN_ATTENTE)
                .montantTotal(new BigDecimal("8000.00"))
                .clientAffectation(affectationClient)
                .lignes(new ArrayList<>())
                .build();

        LigneCommande lc = LigneCommande.builder()
                .id(10L)
                .commande(commande)
                .produit(produit)
                .quantite(2)
                .prixUnitaire(new BigDecimal("4000.00"))
                .sousTotal(new BigDecimal("8000.00"))
                .build();
        commande.getLignes().add(lc);

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationResponsable));
        when(commandeRepository.findByIdAndClientAffectationSalonSlug(200L, "salon-chic"))
                .thenReturn(Optional.of(commande));
        when(factureRepository.save(any(Facture.class))).thenAnswer(i -> {
            Facture f = i.getArgument(0);
            f.setId(300L);
            return f;
        });
        when(commandeRepository.save(any(Commande.class))).thenAnswer(i -> i.getArgument(0));

        CommandeDTOResponse response = commandeSalonService.validerCommande("salon-chic", "stock@test.com", 200L);

        assertNotNull(response);
        assertEquals("VALIDEE", response.statut());
        assertNotNull(response.codeRetrait());
        assertNotNull(response.numeroFacture());
        assertEquals(8, stock.getQuantiteDisponible()); // 10 - 2
        verify(stockProduitRepository).save(stock);
        verify(mouvementStockRepository).save(any(MouvementStock.class));
        verify(factureRepository).save(any(Facture.class));
        verify(notificationEmailService).queueNotificationEmail(eq("client@test.com"), contains("est prête"), any());
    }

    @Test
    void validerCommande_dejaTraitee_leveException() {
        Commande commande = Commande.builder()
                .id(200L)
                .statut(StatutCommande.VALIDEE)
                .clientAffectation(affectationClient)
                .build();

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationResponsable));
        when(commandeRepository.findByIdAndClientAffectationSalonSlug(200L, "salon-chic"))
                .thenReturn(Optional.of(commande));

        assertThrows(IllegalStateException.class, () ->
                commandeSalonService.validerCommande("salon-chic", "stock@test.com", 200L));
    }

    @Test
    void validerCommande_siRejetee_leveException() {
        Commande commande = Commande.builder()
                .id(200L)
                .statut(StatutCommande.REJETEE)
                .clientAffectation(affectationClient)
                .build();

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationResponsable));
        when(commandeRepository.findByIdAndClientAffectationSalonSlug(200L, "salon-chic"))
                .thenReturn(Optional.of(commande));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                commandeSalonService.validerCommande("salon-chic", "stock@test.com", 200L));
        assertEquals("Une commande rejetée ne peut plus être validée.", ex.getMessage());
    }

    @Test
    void rejeterCommande_siValidee_leveException() {
        Commande commande = Commande.builder()
                .id(200L)
                .statut(StatutCommande.VALIDEE)
                .clientAffectation(affectationClient)
                .build();

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationResponsable));
        when(commandeRepository.findByIdAndClientAffectationSalonSlug(200L, "salon-chic"))
                .thenReturn(Optional.of(commande));

        RejetCommandeDTORequest request = new RejetCommandeDTORequest("Erreur de stock");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                commandeSalonService.rejeterCommande("salon-chic", "stock@test.com", 200L, request));
        assertEquals("Une commande validée ne peut pas être rejetée.", ex.getMessage());
    }

    @Test
    void rejeterCommande_succes_enregistreMotifEtEnvoieEmail() {
        Commande commande = Commande.builder()
                .id(200L)
                .numeroCommande("CMD-20260922-001")
                .statut(StatutCommande.EN_ATTENTE)
                .montantTotal(new BigDecimal("8000.00"))
                .clientAffectation(affectationClient)
                .build();

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationResponsable));
        when(commandeRepository.findByIdAndClientAffectationSalonSlug(200L, "salon-chic"))
                .thenReturn(Optional.of(commande));
        when(commandeRepository.save(any(Commande.class))).thenAnswer(i -> i.getArgument(0));

        RejetCommandeDTORequest request = new RejetCommandeDTORequest("Avarie sur le flacon lors de la préparation");
        CommandeDTOResponse response = commandeSalonService.rejeterCommande("salon-chic", "stock@test.com", 200L, request);

        assertNotNull(response);
        assertEquals("REJETEE", response.statut());
        assertEquals("Avarie sur le flacon lors de la préparation", response.motifRejet());
        verify(notificationEmailService).queueNotificationEmail(eq("client@test.com"), contains("Information concernant votre commande"), any());
    }

    @Test
    void confirmerRetrait_succes() {
        Commande commande = Commande.builder()
                .id(200L)
                .numeroCommande("CMD-20260922-001")
                .statut(StatutCommande.VALIDEE)
                .codeRetrait("RET-123456")
                .clientAffectation(affectationClient)
                .build();

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationResponsable));
        when(commandeRepository.findByIdAndClientAffectationSalonSlug(200L, "salon-chic"))
                .thenReturn(Optional.of(commande));
        when(commandeRepository.save(any(Commande.class))).thenAnswer(i -> i.getArgument(0));

        RetraitCommandeDTORequest request = new RetraitCommandeDTORequest("RET-123456");
        CommandeDTOResponse response = commandeSalonService.confirmerRetrait("salon-chic", "stock@test.com", 200L, request);

        assertNotNull(response);
        assertEquals("RECUPEREE", response.statut());
    }

    @Test
    void confirmerRetrait_codeInvalide_leveException() {
        Commande commande = Commande.builder()
                .id(200L)
                .statut(StatutCommande.VALIDEE)
                .codeRetrait("RET-123456")
                .clientAffectation(affectationClient)
                .build();

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationResponsable));
        when(commandeRepository.findByIdAndClientAffectationSalonSlug(200L, "salon-chic"))
                .thenReturn(Optional.of(commande));

        RetraitCommandeDTORequest request = new RetraitCommandeDTORequest("RET-999999");
        assertThrows(IllegalArgumentException.class, () ->
                commandeSalonService.confirmerRetrait("salon-chic", "stock@test.com", 200L, request));
    }

    @Test
    void obtenirProduitsBientotEnRupture_succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationResponsable));
        when(produitRepository.findProduitsEnAlerteStock("salon-chic")).thenReturn(List.of(produit));

        List<ProduitAlerteStockDTOResponse> alertes = commandeSalonService.obtenirProduitsBientotEnRupture("salon-chic", "stock@test.com");

        assertNotNull(alertes);
        assertEquals(1, alertes.size());
        assertEquals("Huile de Ricin", alertes.get(0).produitNom());
        assertEquals(10, alertes.get(0).quantiteDisponible());
    }

    @Test
    void obtenirKpiStock_succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationResponsable));
        when(produitRepository.countByCategorieSalonSlugAndStatut("salon-chic", true)).thenReturn(15L);
        when(produitRepository.countProduitsEnAlerte("salon-chic")).thenReturn(3L);
        when(produitRepository.countProduitsEnRupture("salon-chic")).thenReturn(1L);
        when(produitRepository.calculateValeurTotaleStock("salon-chic")).thenReturn(new BigDecimal("150000.00"));
        when(commandeRepository.countByClientAffectationSalonSlug("salon-chic")).thenReturn(10L);
        when(commandeRepository.countByClientAffectationSalonSlugAndStatut("salon-chic", StatutCommande.EN_ATTENTE)).thenReturn(2L);
        when(commandeRepository.countByClientAffectationSalonSlugAndStatut("salon-chic", StatutCommande.VALIDEE)).thenReturn(5L);
        when(commandeRepository.countByClientAffectationSalonSlugAndStatut("salon-chic", StatutCommande.REJETEE)).thenReturn(1L);
        when(commandeRepository.countByClientAffectationSalonSlugAndStatut("salon-chic", StatutCommande.RECUPEREE)).thenReturn(2L);
        when(commandeRepository.totalChiffreAffairesCommandesValidees("salon-chic")).thenReturn(new BigDecimal("75000.00"));

        KpiStockDTOResponse kpi = commandeSalonService.obtenirKpiStock("salon-chic", "stock@test.com");

        assertNotNull(kpi);
        assertEquals(15L, kpi.totalProduitsActifs());
        assertEquals(3L, kpi.totalProduitsEnAlerte());
        assertEquals(1L, kpi.totalProduitsEnRupture());
        assertEquals(new BigDecimal("150000.00"), kpi.valeurTotaleStock());
        assertEquals(10L, kpi.totalCommandes());
        assertEquals(new BigDecimal("75000.00"), kpi.chiffreAffairesVentes());
    }

    @Test
    void passerCommandeDepuisVitrine_succes() {
        when(salonRepository.findBySlug("salon-chic")).thenReturn(Optional.of(salon));
        when(compteRepository.findByEmail("client@test.com")).thenReturn(Optional.of(clientCompte));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("client@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationClient));
        when(produitRepository.findByIdAndCategorieSalonSlug(20L, "salon-chic"))
                .thenReturn(Optional.of(produit));
        when(commandeRepository.save(any(Commande.class))).thenAnswer(invocation -> {
            Commande c = invocation.getArgument(0);
            c.setId(99L);
            return c;
        });

        var req = new com.kadi_aon.mon_salon.stock.dto.VitrineCommandeDTORequest(
                "Client Nom",
                "Client Prenom",
                "+223 70 00 00 00",
                "client@test.com",
                null,
                List.of(new com.kadi_aon.mon_salon.stock.dto.VitrineCommandeDTORequest.LigneVitrineCommandeDTORequest(20L, 2))
        );

        CommandeDTOResponse res = commandeSalonService.passerCommandeDepuisVitrine("salon-chic", req);

        assertNotNull(res);
        assertEquals(99L, res.id());
        assertEquals("EN_ATTENTE", res.statut());
        assertEquals(0, res.montantTotal().compareTo(new BigDecimal("8000.00")));
        verify(commandeRepository).save(any(Commande.class));
    }
}

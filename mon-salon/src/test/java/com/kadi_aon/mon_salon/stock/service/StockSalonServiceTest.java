package com.kadi_aon.mon_salon.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.caisse.entity.SessionCaisse;
import com.kadi_aon.mon_salon.caisse.service.CaisseSalonService;
import com.kadi_aon.mon_salon.depense.service.DepenseSalonService;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;
import com.kadi_aon.mon_salon.stock.dto.CategorieProduitCreateDTORequest;
import com.kadi_aon.mon_salon.stock.dto.CategorieProduitDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.CategorieProduitUpdateDTORequest;
import com.kadi_aon.mon_salon.stock.dto.MouvementStockCreateDTORequest;
import com.kadi_aon.mon_salon.stock.dto.MouvementStockDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.ProduitCreateDTORequest;
import com.kadi_aon.mon_salon.stock.dto.ProduitDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.ProduitInitialDTORequest;
import com.kadi_aon.mon_salon.stock.dto.ProduitUpdateDTORequest;
import com.kadi_aon.mon_salon.stock.entity.CategorieProduit;
import com.kadi_aon.mon_salon.stock.entity.MouvementStock;
import com.kadi_aon.mon_salon.stock.entity.Produit;
import com.kadi_aon.mon_salon.stock.entity.StockProduit;
import com.kadi_aon.mon_salon.stock.enums.TypeMouvementStock;
import com.kadi_aon.mon_salon.stock.exception.StockInsuffisantException;
import com.kadi_aon.mon_salon.stock.repository.CategorieProduitRepository;
import com.kadi_aon.mon_salon.stock.repository.MouvementStockRepository;
import com.kadi_aon.mon_salon.stock.repository.ProduitRepository;
import com.kadi_aon.mon_salon.stock.repository.StockProduitRepository;

import java.io.IOException;
import static org.mockito.Mockito.mock;
import org.springframework.web.multipart.MultipartFile;
import com.kadi_aon.mon_salon.common.service.CloudinaryService;

@ExtendWith(MockitoExtension.class)
class StockSalonServiceTest {

    @Mock
    private CategorieProduitRepository categorieProduitRepository;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private StockProduitRepository stockProduitRepository;

    @Mock
    private MouvementStockRepository mouvementStockRepository;

    @Mock
    private AffectationSalonRepository affectationSalonRepository;

    @Mock
    private SalonRepository salonRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private CaisseSalonService caisseSalonService;

    @Mock
    private DepenseSalonService depenseSalonService;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private StockSalonService stockSalonService;

    private Salon salon;
    private Compte compte;
    private AffectationSalon affectation;
    private CategorieProduit categorie;
    private Produit produit;
    private StockProduit stockProduit;

    @BeforeEach
    void setUp() {
        salon = Salon.builder().id(1L).nom("Salon Test").slug("salon-test").build();

        compte = Compte.builder().id(10L).email("stock@test.com").nom("Traore").prenom("Fatou").build();

        RoleSalon role = RoleSalon.builder().id(100L).role(TypeRoleSalon.RESPONSABLE_STOCK).build();
        Set<RoleSalon> roles = new HashSet<>();
        roles.add(role);

        affectation = AffectationSalon.builder()
                .id(20L)
                .salon(salon)
                .compte(compte)
                .statut(true)
                .roles(roles)
                .build();

        categorie = CategorieProduit.builder()
                .id(100L)
                .nom("Shampoings")
                .description("Produits capillaires nettoyants")
                .statut(true)
                .salon(salon)
                .produits(new ArrayList<>())
                .build();

        produit = Produit.builder()
                .id(200L)
                .nom("Shampoing Karité")
                .description("Pour cheveux secs")
                .prixVente(BigDecimal.valueOf(5000))
                .statut(true)
                .dateCreation(LocalDateTime.now())
                .categorie(categorie)
                .build();

        stockProduit = StockProduit.builder()
                .id(300L)
                .produit(produit)
                .quantiteDisponible(20)
                .seuilMinimum(5)
                .seuilMaximum(50)
                .dateDerniereMiseAJour(LocalDateTime.now())
                .build();

        produit.setStock(stockProduit);
    }

    @Test
    void creerCategorie_Succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-test"))
                .thenReturn(Optional.of(affectation));
        when(categorieProduitRepository.existsBySalonIdAndNomIgnoreCase(1L, "Soins")).thenReturn(false);

        CategorieProduit savedCat = CategorieProduit.builder()
                .id(101L)
                .nom("Soins")
                .description("Soins profonds")
                .statut(true)
                .salon(salon)
                .build();

        when(categorieProduitRepository.save(any(CategorieProduit.class))).thenReturn(savedCat);
        when(produitRepository.save(any(Produit.class))).thenAnswer(invocation -> {
            Produit p = invocation.getArgument(0);
            p.setId(201L);
            return p;
        });
        when(stockProduitRepository.save(any(StockProduit.class))).thenAnswer(invocation -> {
            StockProduit s = invocation.getArgument(0);
            s.setId(301L);
            return s;
        });

        when(categorieProduitRepository.findByIdAndSalonSlug(101L, "salon-test")).thenReturn(Optional.of(savedCat));
        when(produitRepository.findByCategorieId(101L)).thenReturn(List.of(produit));

        ProduitInitialDTORequest prodReq = new ProduitInitialDTORequest(
                "Masque Hydratant",
                "Masque avocat",
                BigDecimal.valueOf(6000),
                5,
                30,
                10,
                BigDecimal.valueOf(3000)
        );

        CategorieProduitCreateDTORequest req = new CategorieProduitCreateDTORequest("Soins", "Soins profonds", List.of(prodReq));

        CategorieProduitDTOResponse response = stockSalonService.creerCategorie("salon-test", "stock@test.com", req);

        assertNotNull(response);
        assertEquals("Soins", response.nom());
        verify(categorieProduitRepository).save(any(CategorieProduit.class));
        verify(produitRepository).save(any(Produit.class));
        verify(stockProduitRepository).save(any(StockProduit.class));
        verify(mouvementStockRepository).save(any(MouvementStock.class));
    }

    @Test
    void creerCategorie_NomExisteDeja_LanceException() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-test"))
                .thenReturn(Optional.of(affectation));
        when(categorieProduitRepository.existsBySalonIdAndNomIgnoreCase(1L, "Shampoings")).thenReturn(true);

        ProduitInitialDTORequest prodReq = new ProduitInitialDTORequest("Prod A", "", BigDecimal.valueOf(1000), 1, 10, 5, null);
        CategorieProduitCreateDTORequest req = new CategorieProduitCreateDTORequest("Shampoings", "desc", List.of(prodReq));

        assertThrows(IllegalArgumentException.class, () ->
                stockSalonService.creerCategorie("salon-test", "stock@test.com", req));
    }

    @Test
    void creerCategorie_SansProduitInitial_Succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-test"))
                .thenReturn(Optional.of(affectation));
        when(categorieProduitRepository.existsBySalonIdAndNomIgnoreCase(1L, "Nouvelle Cat")).thenReturn(false);

        CategorieProduit savedCat = CategorieProduit.builder()
                .id(102L)
                .nom("Nouvelle Cat")
                .description("desc")
                .statut(true)
                .salon(salon)
                .build();

        when(categorieProduitRepository.save(any(CategorieProduit.class))).thenReturn(savedCat);
        when(categorieProduitRepository.findByIdAndSalonSlug(102L, "salon-test")).thenReturn(Optional.of(savedCat));
        when(produitRepository.findByCategorieId(102L)).thenReturn(List.of());

        CategorieProduitCreateDTORequest req = new CategorieProduitCreateDTORequest("Nouvelle Cat", "desc", List.of());

        CategorieProduitDTOResponse response = stockSalonService.creerCategorie("salon-test", "stock@test.com", req);

        assertNotNull(response);
        assertEquals("Nouvelle Cat", response.nom());
        assertEquals(0, response.nombreProduits());
        verify(categorieProduitRepository).save(any(CategorieProduit.class));
    }

    @Test
    void obtenirCategorie_Succes() {
        when(categorieProduitRepository.findByIdAndSalonSlug(100L, "salon-test")).thenReturn(Optional.of(categorie));
        when(produitRepository.findByCategorieId(100L)).thenReturn(List.of(produit));

        CategorieProduitDTOResponse resp = stockSalonService.obtenirCategorie("salon-test", 100L);

        assertNotNull(resp);
        assertEquals(100L, resp.id());
        assertEquals("Shampoings", resp.nom());
        assertEquals(1, resp.nombreProduits());
        assertEquals("Shampoing Karité", resp.produits().get(0).nom());
    }

    @Test
    void basculerStatutCategorie_Succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-test"))
                .thenReturn(Optional.of(affectation));
        when(categorieProduitRepository.findByIdAndSalonSlug(100L, "salon-test")).thenReturn(Optional.of(categorie));
        when(categorieProduitRepository.save(any(CategorieProduit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(produitRepository.findByCategorieId(100L)).thenReturn(List.of());

        CategorieProduitDTOResponse resp = stockSalonService.basculerStatutCategorie("salon-test", 100L, "stock@test.com");

        assertFalse(resp.statut());
        verify(categorieProduitRepository).save(any(CategorieProduit.class));
    }

    @Test
    void basculerStatutProduit_Succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-test"))
                .thenReturn(Optional.of(affectation));
        when(produitRepository.findByIdAndCategorieSalonSlug(200L, "salon-test")).thenReturn(Optional.of(produit));
        when(produitRepository.save(any(Produit.class))).thenAnswer(inv -> inv.getArgument(0));

        ProduitDTOResponse resp = stockSalonService.basculerStatutProduit("salon-test", 200L, "stock@test.com");

        assertFalse(resp.statut());
        verify(produitRepository).save(any(Produit.class));
    }

    @Test
    void ajouterProduit_Succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-test"))
                .thenReturn(Optional.of(affectation));
        when(categorieProduitRepository.findByIdAndSalonSlug(100L, "salon-test")).thenReturn(Optional.of(categorie));
        when(produitRepository.existsByCategorieIdAndNomIgnoreCase(100L, "Après-shampoing")).thenReturn(false);

        when(produitRepository.save(any(Produit.class))).thenAnswer(inv -> {
            Produit p = inv.getArgument(0);
            p.setId(205L);
            return p;
        });
        when(stockProduitRepository.save(any(StockProduit.class))).thenAnswer(inv -> {
            StockProduit s = inv.getArgument(0);
            s.setId(305L);
            return s;
        });

        ProduitCreateDTORequest req = new ProduitCreateDTORequest(
                "Après-shampoing", "Démêlant doux", BigDecimal.valueOf(4500), 5, 25, 0, null
        );

        ProduitDTOResponse resp = stockSalonService.ajouterProduit("salon-test", 100L, "stock@test.com", req);

        assertNotNull(resp);
        assertEquals("Après-shampoing", resp.nom());
        verify(produitRepository).save(any(Produit.class));
    }

    @Test
    void enregistrerMouvementStock_Entree_AvecAchat_Succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-test"))
                .thenReturn(Optional.of(affectation));
        when(produitRepository.findByIdAndCategorieSalonSlug(200L, "salon-test")).thenReturn(Optional.of(produit));
        when(stockProduitRepository.findByProduitId(200L)).thenReturn(Optional.of(stockProduit));
        when(stockProduitRepository.save(any(StockProduit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mouvementStockRepository.save(any(MouvementStock.class))).thenAnswer(inv -> {
            MouvementStock m = inv.getArgument(0);
            m.setId(500L);
            return m;
        });

        SessionCaisse sessionCaisse = SessionCaisse.builder().id(999L).build();
        when(caisseSalonService.obtenirSessionActive("salon-test")).thenReturn(sessionCaisse);

        MouvementStockCreateDTORequest req = new MouvementStockCreateDTORequest(
                200L,
                10,
                TypeMouvementStock.ENTREE,
                BigDecimal.valueOf(2500),
                "Livraison fournisseur"
        );

        MouvementStockDTOResponse resp = stockSalonService.enregistrerMouvementStock("salon-test", "stock@test.com", req);

        assertNotNull(resp);
        assertEquals(30, stockProduit.getQuantiteDisponible()); // 20 + 10 = 30
        assertEquals(30, resp.quantiteRestante());
        verify(depenseSalonService).creerDepenseAchatStock(
                eq(affectation),
                eq(sessionCaisse),
                eq(BigDecimal.valueOf(25000)), // 2500 * 10
                any(),
                any(MouvementStock.class)
        );
    }

    @Test
    void enregistrerMouvementStock_Sortie_Vente_Succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-test"))
                .thenReturn(Optional.of(affectation));
        when(produitRepository.findByIdAndCategorieSalonSlug(200L, "salon-test")).thenReturn(Optional.of(produit));
        when(stockProduitRepository.findByProduitId(200L)).thenReturn(Optional.of(stockProduit));
        when(stockProduitRepository.save(any(StockProduit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mouvementStockRepository.save(any(MouvementStock.class))).thenAnswer(inv -> {
            MouvementStock m = inv.getArgument(0);
            m.setId(501L);
            return m;
        });

        MouvementStockCreateDTORequest req = new MouvementStockCreateDTORequest(
                200L,
                5,
                TypeMouvementStock.VENTE,
                null,
                "Vente client"
        );

        MouvementStockDTOResponse resp = stockSalonService.enregistrerMouvementStock("salon-test", "stock@test.com", req);

        assertNotNull(resp);
        assertEquals(15, stockProduit.getQuantiteDisponible()); // 20 - 5 = 15
        assertEquals(15, resp.quantiteRestante());
    }

    @Test
    void enregistrerMouvementStock_StockInsuffisant_LanceException() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-test"))
                .thenReturn(Optional.of(affectation));
        when(produitRepository.findByIdAndCategorieSalonSlug(200L, "salon-test")).thenReturn(Optional.of(produit));
        when(stockProduitRepository.findByProduitId(200L)).thenReturn(Optional.of(stockProduit));

        MouvementStockCreateDTORequest req = new MouvementStockCreateDTORequest(
                200L,
                50, // Stock disponible is only 20
                TypeMouvementStock.VENTE,
                null,
                "Vente trop importante"
        );

        assertThrows(StockInsuffisantException.class, () ->
                stockSalonService.enregistrerMouvementStock("salon-test", "stock@test.com", req));
    }

    @Test
    void enregistrerMouvementStock_Ajustement_Succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-test"))
                .thenReturn(Optional.of(affectation));
        when(produitRepository.findByIdAndCategorieSalonSlug(200L, "salon-test")).thenReturn(Optional.of(produit));
        when(stockProduitRepository.findByProduitId(200L)).thenReturn(Optional.of(stockProduit));
        when(stockProduitRepository.save(any(StockProduit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mouvementStockRepository.save(any(MouvementStock.class))).thenAnswer(inv -> {
            MouvementStock m = inv.getArgument(0);
            m.setId(502L);
            return m;
        });

        MouvementStockCreateDTORequest req = new MouvementStockCreateDTORequest(
                200L,
                18, // Nouvel inventaire constaté
                TypeMouvementStock.AJUSTEMENT,
                null,
                "Inventaire de fin de mois"
        );

        MouvementStockDTOResponse resp = stockSalonService.enregistrerMouvementStock("salon-test", "stock@test.com", req);

        assertNotNull(resp);
        assertEquals(18, stockProduit.getQuantiteDisponible());
        assertEquals(18, resp.quantiteRestante());
    }

    @Test
    void creerCategorie_avecImage_succes() throws IOException {
        MultipartFile imageFile = mock(MultipartFile.class);
        when(imageFile.isEmpty()).thenReturn(false);
        String uploadedUrl = "https://res.cloudinary.com/test/image/upload/v1/categories/cat1.webp";
        when(cloudinaryService.uploadImageCategorieProduit(imageFile, "salon-test")).thenReturn(uploadedUrl);

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-test"))
                .thenReturn(Optional.of(affectation));
        when(categorieProduitRepository.existsBySalonIdAndNomIgnoreCase(1L, "Soins")).thenReturn(false);

        CategorieProduit savedCat = CategorieProduit.builder()
                .id(101L)
                .nom("Soins")
                .description("Soins profonds")
                .imageUrl(uploadedUrl)
                .statut(true)
                .salon(salon)
                .build();

        when(categorieProduitRepository.save(any(CategorieProduit.class))).thenReturn(savedCat);
        when(produitRepository.save(any(Produit.class))).thenAnswer(invocation -> {
            Produit p = invocation.getArgument(0);
            p.setId(201L);
            return p;
        });
        when(stockProduitRepository.save(any(StockProduit.class))).thenAnswer(invocation -> {
            StockProduit s = invocation.getArgument(0);
            s.setId(301L);
            return s;
        });

        when(categorieProduitRepository.findByIdAndSalonSlug(101L, "salon-test")).thenReturn(Optional.of(savedCat));
        when(produitRepository.findByCategorieId(101L)).thenReturn(List.of(produit));

        ProduitInitialDTORequest prodReq = new ProduitInitialDTORequest(
                "Masque Hydratant", "Masque avocat", BigDecimal.valueOf(6000), 5, 30, 10, BigDecimal.valueOf(3000)
        );
        CategorieProduitCreateDTORequest req = new CategorieProduitCreateDTORequest("Soins", "Soins profonds", List.of(prodReq));

        CategorieProduitDTOResponse response = stockSalonService.creerCategorie("salon-test", "stock@test.com", req, imageFile);

        assertNotNull(response);
        assertEquals("Soins", response.nom());
        assertEquals(uploadedUrl, response.imageUrl());
        verify(cloudinaryService).uploadImageCategorieProduit(imageFile, "salon-test");
    }

    @Test
    void modifierCategorie_attributs_succes() {
        CategorieProduit cat = CategorieProduit.builder()
                .id(100L)
                .nom("Shampoings")
                .description("Produits nettoyants")
                .imageUrl("https://res.cloudinary.com/test/image.webp")
                .statut(true)
                .salon(salon)
                .build();

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-test"))
                .thenReturn(Optional.of(affectation));
        when(categorieProduitRepository.findByIdAndSalonSlug(100L, "salon-test")).thenReturn(Optional.of(cat));
        when(categorieProduitRepository.existsBySalonIdAndNomIgnoreCaseAndIdNot(1L, "Shampoings Premium", 100L)).thenReturn(false);
        when(categorieProduitRepository.save(any(CategorieProduit.class))).thenAnswer(i -> i.getArgument(0));
        when(produitRepository.findByCategorieId(100L)).thenReturn(List.of());

        CategorieProduitUpdateDTORequest req = new CategorieProduitUpdateDTORequest("Shampoings Premium", "Nouvelle description");

        CategorieProduitDTOResponse response = stockSalonService.modifierCategorie(
                "salon-test", 100L, "stock@test.com", req);

        assertNotNull(response);
        assertEquals("Shampoings Premium", response.nom());
        assertEquals("Nouvelle description", response.description());
    }

    @Test
    void uploadImageCategorie_supprimeAncienneEtUploadeNouvelle() throws IOException {
        String ancienneUrl = "https://res.cloudinary.com/test/image/upload/v1/categories/ancienne.webp";
        String nouvelleUrl = "https://res.cloudinary.com/test/image/upload/v1/categories/remplacement.webp";

        CategorieProduit cat = CategorieProduit.builder()
                .id(100L)
                .nom("Shampoings")
                .description("Produits")
                .imageUrl(ancienneUrl)
                .statut(true)
                .salon(salon)
                .build();

        MultipartFile file = mock(MultipartFile.class);
        when(cloudinaryService.uploadImageCategorieProduit(file, "salon-test")).thenReturn(nouvelleUrl);

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-test"))
                .thenReturn(Optional.of(affectation));
        when(categorieProduitRepository.findByIdAndSalonSlug(100L, "salon-test")).thenReturn(Optional.of(cat));
        when(categorieProduitRepository.save(any(CategorieProduit.class))).thenAnswer(i -> i.getArgument(0));
        when(produitRepository.findByCategorieId(100L)).thenReturn(List.of());

        CategorieProduitDTOResponse response = stockSalonService.uploadImageCategorie(
                "salon-test", 100L, "stock@test.com", file);

        assertNotNull(response);
        assertEquals(nouvelleUrl, response.imageUrl());
        verify(cloudinaryService).deleteMediaByUrl(ancienneUrl, "image");
        verify(cloudinaryService).uploadImageCategorieProduit(file, "salon-test");
    }

    @Test
    void ajouterProduit_avecImage_succes() throws IOException {
        MultipartFile imageFile = mock(MultipartFile.class);
        when(imageFile.isEmpty()).thenReturn(false);
        String uploadedUrl = "https://res.cloudinary.com/test/image/upload/v1/produits/prod1.webp";
        when(cloudinaryService.uploadImageProduit(imageFile, "salon-test")).thenReturn(uploadedUrl);

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-test"))
                .thenReturn(Optional.of(affectation));
        when(categorieProduitRepository.findByIdAndSalonSlug(100L, "salon-test")).thenReturn(Optional.of(categorie));
        when(produitRepository.existsByCategorieIdAndNomIgnoreCase(100L, "Après-shampoing Bio")).thenReturn(false);

        when(produitRepository.save(any(Produit.class))).thenAnswer(inv -> {
            Produit p = inv.getArgument(0);
            p.setId(205L);
            return p;
        });
        when(stockProduitRepository.save(any(StockProduit.class))).thenAnswer(inv -> {
            StockProduit s = inv.getArgument(0);
            s.setId(305L);
            return s;
        });

        ProduitCreateDTORequest req = new ProduitCreateDTORequest(
                "Après-shampoing Bio", "Démêlant doux", BigDecimal.valueOf(4500), 5, 25, 0, null
        );

        ProduitDTOResponse resp = stockSalonService.ajouterProduit("salon-test", 100L, "stock@test.com", req, imageFile);

        assertNotNull(resp);
        assertEquals("Après-shampoing Bio", resp.nom());
        assertEquals(uploadedUrl, resp.imageUrl());
        verify(cloudinaryService).uploadImageProduit(imageFile, "salon-test");
    }

    @Test
    void modifierProduit_attributs_succes() {
        Produit existingProduit = Produit.builder()
                .id(200L)
                .nom("Shampoing Karité")
                .description("Pour cheveux secs")
                .prixVente(BigDecimal.valueOf(5000))
                .imageUrl("https://res.cloudinary.com/test/image.webp")
                .statut(true)
                .categorie(categorie)
                .build();

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-test"))
                .thenReturn(Optional.of(affectation));
        when(produitRepository.findByIdAndCategorieSalonSlug(200L, "salon-test")).thenReturn(Optional.of(existingProduit));
        when(produitRepository.existsByCategorieIdAndNomIgnoreCaseAndIdNot(100L, "Shampoing Karité Pur", 200L)).thenReturn(false);
        when(produitRepository.save(any(Produit.class))).thenAnswer(inv -> inv.getArgument(0));

        ProduitUpdateDTORequest req = new ProduitUpdateDTORequest("Shampoing Karité Pur", "Nouvelle desc", BigDecimal.valueOf(5500), 5, 50);

        ProduitDTOResponse resp = stockSalonService.modifierProduit("salon-test", 200L, "stock@test.com", req);

        assertNotNull(resp);
        assertEquals("Shampoing Karité Pur", resp.nom());
        assertEquals("Nouvelle desc", resp.description());
        assertEquals(BigDecimal.valueOf(5500), resp.prixVente());
    }

    @Test
    void uploadImageProduit_supprimeAncienneEtUploadeNouvelle() throws IOException {
        String ancienneUrl = "https://res.cloudinary.com/test/image/upload/v1/produits/ancienne.webp";
        String nouvelleUrl = "https://res.cloudinary.com/test/image/upload/v1/produits/remplacement.webp";

        Produit existingProduit = Produit.builder()
                .id(200L)
                .nom("Shampoing Karité")
                .description("Pour cheveux secs")
                .prixVente(BigDecimal.valueOf(5000))
                .imageUrl(ancienneUrl)
                .statut(true)
                .categorie(categorie)
                .build();

        MultipartFile file = mock(MultipartFile.class);
        when(cloudinaryService.uploadImageProduit(file, "salon-test")).thenReturn(nouvelleUrl);

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("stock@test.com", "salon-test"))
                .thenReturn(Optional.of(affectation));
        when(produitRepository.findByIdAndCategorieSalonSlug(200L, "salon-test")).thenReturn(Optional.of(existingProduit));
        when(produitRepository.save(any(Produit.class))).thenAnswer(inv -> inv.getArgument(0));

        ProduitDTOResponse resp = stockSalonService.uploadImageProduit("salon-test", 200L, "stock@test.com", file);

        assertNotNull(resp);
        assertEquals(nouvelleUrl, resp.imageUrl());
        verify(cloudinaryService).deleteMediaByUrl(ancienneUrl, "image");
        verify(cloudinaryService).uploadImageProduit(file, "salon-test");
    }
}

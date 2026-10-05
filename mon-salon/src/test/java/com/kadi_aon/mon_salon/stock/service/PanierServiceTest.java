package com.kadi_aon.mon_salon.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.stock.dto.AjoutPanierDTORequest;
import com.kadi_aon.mon_salon.stock.dto.ModificationQuantiteDTORequest;
import com.kadi_aon.mon_salon.stock.dto.PanierDTOResponse;
import com.kadi_aon.mon_salon.stock.entity.LignePanier;
import com.kadi_aon.mon_salon.stock.entity.Panier;
import com.kadi_aon.mon_salon.stock.entity.Produit;
import com.kadi_aon.mon_salon.stock.entity.StockProduit;
import com.kadi_aon.mon_salon.stock.exception.StockInsuffisantException;
import com.kadi_aon.mon_salon.stock.repository.LignePanierRepository;
import com.kadi_aon.mon_salon.stock.repository.PanierRepository;
import com.kadi_aon.mon_salon.stock.repository.ProduitRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class PanierServiceTest {

    @Mock
    private PanierRepository panierRepository;

    @Mock
    private LignePanierRepository lignePanierRepository;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private AffectationSalonRepository affectationSalonRepository;

    @InjectMocks
    private PanierService panierService;

    private Salon salon;
    private Compte clientCompte;
    private AffectationSalon affectationClient;
    private Produit produit;
    private StockProduit stock;
    private Panier panier;

    @BeforeEach
    void setUp() {
        salon = Salon.builder().id(1L).nom("Salon Chic").slug("salon-chic").build();
        clientCompte = Compte.builder().id(10L).email("client@test.com").prenom("Fatou").nom("Diop").build();

        RoleSalon roleClient = RoleSalon.builder().id(1L).role(TypeRoleSalon.CLIENT).build();
        affectationClient = AffectationSalon.builder()
                .id(100L)
                .salon(salon)
                .compte(clientCompte)
                .roles(Set.of(roleClient))
                .statut(true)
                .build();

        produit = Produit.builder()
                .id(20L)
                .nom("Shampoing Argan")
                .prixVente(new BigDecimal("5000.00"))
                .statut(true)
                .build();

        stock = StockProduit.builder()
                .id(30L)
                .produit(produit)
                .quantiteDisponible(10)
                .seuilMinimum(2)
                .build();
        produit.setStock(stock);

        panier = Panier.builder()
                .id(50L)
                .clientAffectation(affectationClient)
                .lignes(new ArrayList<>())
                .build();
    }

    @Test
    void obtenirPanier_creeNouveauPanierSiInexistant() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("client@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationClient));
        when(panierRepository.findByClientAffectationId(100L)).thenReturn(Optional.empty());
        when(panierRepository.save(any(Panier.class))).thenReturn(panier);

        PanierDTOResponse response = panierService.obtenirPanier("salon-chic", "client@test.com");

        assertNotNull(response);
        assertEquals("client@test.com", response.clientEmail());
        assertEquals("salon-chic", response.slugSalon());
        assertEquals(0, response.nombreArticles());
    }

    @Test
    void ajouterArticle_succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("client@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationClient));
        when(panierRepository.findByClientAffectationId(100L)).thenReturn(Optional.of(panier));
        when(produitRepository.findByIdAndCategorieSalonSlug(20L, "salon-chic")).thenReturn(Optional.of(produit));
        when(panierRepository.save(any(Panier.class))).thenAnswer(i -> i.getArgument(0));

        AjoutPanierDTORequest request = new AjoutPanierDTORequest(20L, 2);
        PanierDTOResponse response = panierService.ajouterArticle("salon-chic", "client@test.com", request);

        assertNotNull(response);
        assertEquals(1, response.lignes().size());
        assertEquals(2, response.nombreArticles());
        assertEquals(new BigDecimal("10000.00"), response.montantTotal());
    }

    @Test
    void ajouterArticle_stockInsuffisant_leveException() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("client@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationClient));
        when(panierRepository.findByClientAffectationId(100L)).thenReturn(Optional.of(panier));
        when(produitRepository.findByIdAndCategorieSalonSlug(20L, "salon-chic")).thenReturn(Optional.of(produit));

        AjoutPanierDTORequest request = new AjoutPanierDTORequest(20L, 15); // disponible = 10

        assertThrows(StockInsuffisantException.class, () ->
                panierService.ajouterArticle("salon-chic", "client@test.com", request));
    }

    @Test
    void modifierQuantite_succes() {
        LignePanier ligne = LignePanier.builder().id(1L).panier(panier).produit(produit).quantite(2).build();
        panier.getLignes().add(ligne);

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("client@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationClient));
        when(panierRepository.findByClientAffectationId(100L)).thenReturn(Optional.of(panier));
        when(panierRepository.save(any(Panier.class))).thenAnswer(i -> i.getArgument(0));

        ModificationQuantiteDTORequest request = new ModificationQuantiteDTORequest(5);
        PanierDTOResponse response = panierService.modifierQuantite("salon-chic", "client@test.com", 20L, request);

        assertNotNull(response);
        assertEquals(5, response.nombreArticles());
        assertEquals(new BigDecimal("25000.00"), response.montantTotal());
    }

    @Test
    void supprimerArticle_succes() {
        LignePanier ligne = LignePanier.builder().id(1L).panier(panier).produit(produit).quantite(2).build();
        panier.getLignes().add(ligne);

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("client@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationClient));
        when(panierRepository.findByClientAffectationId(100L)).thenReturn(Optional.of(panier));
        when(panierRepository.save(any(Panier.class))).thenAnswer(i -> i.getArgument(0));

        PanierDTOResponse response = panierService.supprimerArticle("salon-chic", "client@test.com", 20L);

        assertNotNull(response);
        assertEquals(0, response.lignes().size());
        assertEquals(0, response.nombreArticles());
    }

    @Test
    void viderPanier_succes() {
        LignePanier ligne = LignePanier.builder().id(1L).panier(panier).produit(produit).quantite(2).build();
        panier.getLignes().add(ligne);

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("client@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationClient));
        when(panierRepository.findByClientAffectationId(100L)).thenReturn(Optional.of(panier));

        panierService.viderPanier("salon-chic", "client@test.com");

        assertEquals(0, panier.getLignes().size());
        verify(panierRepository).save(panier);
    }
}

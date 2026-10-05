package com.kadi_aon.mon_salon.depense.service;

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
import com.kadi_aon.mon_salon.caisse.entity.OperationCaisse;
import com.kadi_aon.mon_salon.caisse.entity.SessionCaisse;
import com.kadi_aon.mon_salon.caisse.service.CaisseSalonService;
import com.kadi_aon.mon_salon.depense.dto.DepenseCreateDTORequest;
import com.kadi_aon.mon_salon.depense.dto.DepenseDTOResponse;
import com.kadi_aon.mon_salon.depense.entity.Depense;
import com.kadi_aon.mon_salon.depense.enums.CategorieDepense;
import com.kadi_aon.mon_salon.depense.repository.DepenseRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;
import com.kadi_aon.mon_salon.stock.entity.MouvementStock;
import com.kadi_aon.mon_salon.stock.entity.Produit;
import com.kadi_aon.mon_salon.stock.entity.StockProduit;
import com.kadi_aon.mon_salon.stock.enums.TypeMouvementStock;
import com.kadi_aon.mon_salon.stock.repository.MouvementStockRepository;
import com.kadi_aon.mon_salon.stock.repository.StockProduitRepository;

@ExtendWith(MockitoExtension.class)
class DepenseSalonServiceTest {

    @Mock
    private DepenseRepository depenseRepository;

    @Mock
    private CaisseSalonService caisseSalonService;

    @Mock
    private AffectationSalonRepository affectationSalonRepository;

    @Mock
    private SalonRepository salonRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private StockProduitRepository stockProduitRepository;

    @Mock
    private MouvementStockRepository mouvementStockRepository;

    @InjectMocks
    private DepenseSalonService depenseSalonService;

    private Salon salon;
    private Compte compte;
    private AffectationSalon affectation;
    private SessionCaisse sessionCaisse;
    private OperationCaisse opCaisse;
    private Depense depense;

    @BeforeEach
    void setUp() {
        salon = Salon.builder().id(1L).nom("Salon Test").slug("salon-test").build();

        compte = Compte.builder().id(10L).email("comptable@test.com").nom("Diallo").prenom("Mamadou").build();

        RoleSalon role = RoleSalon.builder().id(100L).role(TypeRoleSalon.COMPTABLE).build();
        Set<RoleSalon> roles = new HashSet<>();
        roles.add(role);

        affectation = AffectationSalon.builder()
                .id(20L)
                .salon(salon)
                .compte(compte)
                .statut(true)
                .roles(roles)
                .build();

        sessionCaisse = SessionCaisse.builder()
                .id(50L)
                .affectation(affectation)
                .soldeOuverture(BigDecimal.valueOf(100000))
                .build();

        opCaisse = OperationCaisse.builder()
                .id(70L)
                .sessionCaisse(sessionCaisse)
                .montant(BigDecimal.valueOf(15000))
                .statut(true)
                .build();

        depense = Depense.builder()
                .id(1L)
                .montant(BigDecimal.valueOf(15000))
                .categorie(CategorieDepense.ELECTRICITE)
                .description("Facture EDM")
                .dateDepense(LocalDateTime.now())
                .statut(true)
                .salon(salon)
                .comptable(affectation)
                .operationCaisse(opCaisse)
                .build();
    }

    @Test
    void creerDepense_Succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("comptable@test.com", "salon-test"))
                .thenReturn(Optional.of(affectation));
        when(caisseSalonService.obtenirSessionActive("salon-test")).thenReturn(sessionCaisse);
        when(caisseSalonService.enregistrerOperationSortie(eq(sessionCaisse), eq(BigDecimal.valueOf(15000)), any(), eq(null)))
                .thenReturn(opCaisse);
        when(depenseRepository.save(any(Depense.class))).thenReturn(depense);

        DepenseCreateDTORequest req = new DepenseCreateDTORequest(
                BigDecimal.valueOf(15000),
                CategorieDepense.ELECTRICITE,
                "Facture EDM"
        );

        DepenseDTOResponse resp = depenseSalonService.creerDepense("salon-test", "comptable@test.com", req);

        assertNotNull(resp);
        assertEquals(BigDecimal.valueOf(15000), resp.montant());
        assertEquals("ELECTRICITE", resp.categorie());
        assertTrue(resp.statut());
        verify(depenseRepository).save(any(Depense.class));
        verify(auditLogService).logActionSalon(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void annulerDepense_Succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("comptable@test.com", "salon-test"))
                .thenReturn(Optional.of(affectation));
        when(depenseRepository.findByIdAndSalonSlug(1L, "salon-test")).thenReturn(Optional.of(depense));
        when(depenseRepository.save(any(Depense.class))).thenAnswer(inv -> inv.getArgument(0));

        DepenseDTOResponse resp = depenseSalonService.annulerDepense("salon-test", 1L, "comptable@test.com");

        assertNotNull(resp);
        assertFalse(resp.statut());
        assertFalse(opCaisse.getStatut());
        verify(depenseRepository).save(depense);
    }

    @Test
    void annulerDepense_DejaAnnulee_LanceException() {
        depense.setStatut(false);
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("comptable@test.com", "salon-test"))
                .thenReturn(Optional.of(affectation));
        when(depenseRepository.findByIdAndSalonSlug(1L, "salon-test")).thenReturn(Optional.of(depense));

        assertThrows(IllegalStateException.class, () ->
                depenseSalonService.annulerDepense("salon-test", 1L, "comptable@test.com"));
    }

    @Test
    void listerDepenses_Succes() {
        when(salonRepository.existsBySlug("salon-test")).thenReturn(true);
        when(depenseRepository.findBySalonSlugOrderByDateDepenseDesc("salon-test")).thenReturn(List.of(depense));

        List<DepenseDTOResponse> list = depenseSalonService.listerDepenses("salon-test", null, null);

        assertEquals(1, list.size());
        assertEquals("ELECTRICITE", list.get(0).categorie());
    }

    @Test
    void obtenirDepense_Succes() {
        when(depenseRepository.findByIdAndSalonSlug(1L, "salon-test")).thenReturn(Optional.of(depense));

        DepenseDTOResponse resp = depenseSalonService.obtenirDepense("salon-test", 1L);

        assertNotNull(resp);
        assertEquals(1L, resp.id());
    }

    @Test
    void annulerDepense_achatStock_annuleStockProduit() {
        Produit produit = Produit.builder().id(5L).nom("Shampoing Bio").build();
        StockProduit stock = StockProduit.builder().id(50L).produit(produit).quantiteDisponible(15).build();

        MouvementStock mvt = MouvementStock.builder()
                .id(100L)
                .produit(produit)
                .type(TypeMouvementStock.ENTREE)
                .quantite(10)
                .motif("Réapprovisionnement")
                .build();

        Depense depenseAchat = Depense.builder()
                .id(2L)
                .montant(BigDecimal.valueOf(50000))
                .categorie(CategorieDepense.ACHAT_STOCK)
                .description("Achat shampoing")
                .statut(true)
                .salon(salon)
                .comptable(affectation)
                .operationCaisse(opCaisse)
                .mouvementStock(mvt)
                .build();

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("comptable@test.com", "salon-test"))
                .thenReturn(Optional.of(affectation));
        when(depenseRepository.findByIdAndSalonSlug(2L, "salon-test")).thenReturn(Optional.of(depenseAchat));
        when(depenseRepository.save(any(Depense.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockProduitRepository.findByProduitId(5L)).thenReturn(Optional.of(stock));

        DepenseDTOResponse resp = depenseSalonService.annulerDepense("salon-test", 2L, "comptable@test.com");

        assertNotNull(resp);
        assertFalse(resp.statut());
        assertEquals(5, stock.getQuantiteDisponible()); // 15 - 10 = 5
        verify(stockProduitRepository).save(stock);
        verify(mouvementStockRepository).save(mvt);
        assertTrue(mvt.getMotif().contains("[ANNULE PAR DEPENSE #2]"));
    }
}

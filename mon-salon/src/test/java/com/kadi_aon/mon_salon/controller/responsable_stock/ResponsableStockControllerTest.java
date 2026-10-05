package com.kadi_aon.mon_salon.controller.responsable_stock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.stock.dto.CategorieProduitCreateDTORequest;
import com.kadi_aon.mon_salon.stock.dto.CategorieProduitDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.CategorieProduitUpdateDTORequest;
import com.kadi_aon.mon_salon.stock.dto.MouvementStockCreateDTORequest;
import com.kadi_aon.mon_salon.stock.dto.MouvementStockDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.ProduitCreateDTORequest;
import com.kadi_aon.mon_salon.stock.dto.ProduitDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.ProduitInitialDTORequest;
import com.kadi_aon.mon_salon.stock.dto.ProduitUpdateDTORequest;
import com.kadi_aon.mon_salon.stock.enums.TypeMouvementStock;
import com.kadi_aon.mon_salon.stock.service.StockSalonService;

@ExtendWith(MockitoExtension.class)
class ResponsableStockControllerTest {

    @Mock
    private StockSalonService stockSalonService;

    @InjectMocks
    private ResponsableStockController responsableStockController;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        lenient().when(principal.getName()).thenReturn("stock@test.com");
    }

    @Test
    void creerCategorie_multipart_succes() throws IOException {
        MultipartFile image = mock(MultipartFile.class);
        ProduitInitialDTORequest prod = new ProduitInitialDTORequest("Shampoing Bio", "Bio", BigDecimal.valueOf(3000), 2, 20, 5, null);
        CategorieProduitCreateDTORequest req = new CategorieProduitCreateDTORequest("Shampoings", "desc", List.of(prod));

        CategorieProduitDTOResponse dto = new CategorieProduitDTOResponse(
                1L, "Shampoings", "desc", "https://img.url", true, "mon-salon", 1, List.of()
        );

        when(stockSalonService.creerCategorie("mon-salon", "stock@test.com", req, image)).thenReturn(dto);

        ResponseEntity<APIResponse<CategorieProduitDTOResponse>> response =
                responsableStockController.creerCategorie("mon-salon", req, image, principal);

        assertNotNull(response);
        assertEquals(201, response.getStatusCode().value());
        assertEquals("Shampoings", response.getBody().getData().nom());
        verify(stockSalonService).creerCategorie("mon-salon", "stock@test.com", req, image);
    }

    @Test
    void obtenirCategorie_succes() {
        CategorieProduitDTOResponse dto = new CategorieProduitDTOResponse(
                1L, "Shampoings", "desc", true, "mon-salon", 0, List.of()
        );

        when(stockSalonService.obtenirCategorie("mon-salon", 1L)).thenReturn(dto);

        ResponseEntity<APIResponse<CategorieProduitDTOResponse>> response =
                responsableStockController.obtenirCategorie("mon-salon", 1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Shampoings", response.getBody().getData().nom());
    }

    @Test
    void modifierCategorie_json_succes() {
        CategorieProduitUpdateDTORequest req = new CategorieProduitUpdateDTORequest("Soins VIP", "Nouvelle desc");
        CategorieProduitDTOResponse dto = new CategorieProduitDTOResponse(
                1L, "Soins VIP", "Nouvelle desc", true, "mon-salon", 0, List.of()
        );

        when(stockSalonService.modifierCategorie("mon-salon", 1L, "stock@test.com", req)).thenReturn(dto);

        ResponseEntity<APIResponse<CategorieProduitDTOResponse>> response =
                responsableStockController.modifierCategorie("mon-salon", 1L, req, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Soins VIP", response.getBody().getData().nom());
        verify(stockSalonService).modifierCategorie("mon-salon", 1L, "stock@test.com", req);
    }

    @Test
    void uploadImageCategorie_succes() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        CategorieProduitDTOResponse dto = new CategorieProduitDTOResponse(
                1L, "Soins", "desc", "https://patch.img.url", true, "mon-salon", 0, List.of()
        );

        when(stockSalonService.uploadImageCategorie("mon-salon", 1L, "stock@test.com", file)).thenReturn(dto);

        ResponseEntity<APIResponse<CategorieProduitDTOResponse>> response =
                responsableStockController.uploadImageCategorie("mon-salon", 1L, file, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("https://patch.img.url", response.getBody().getData().imageUrl());
        verify(stockSalonService).uploadImageCategorie("mon-salon", 1L, "stock@test.com", file);
    }

    @Test
    void basculerStatutCategorie_succes() {
        CategorieProduitDTOResponse dto = new CategorieProduitDTOResponse(
                1L, "Shampoings", "desc", false, "mon-salon", 0, List.of()
        );

        when(stockSalonService.basculerStatutCategorie("mon-salon", 1L, "stock@test.com")).thenReturn(dto);

        ResponseEntity<APIResponse<CategorieProduitDTOResponse>> response =
                responsableStockController.basculerStatutCategorie("mon-salon", 1L, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(false, response.getBody().getData().statut());
    }

    @Test
    void ajouterProduit_multipart_succes() throws IOException {
        MultipartFile image = mock(MultipartFile.class);
        ProduitCreateDTORequest req = new ProduitCreateDTORequest("Produit X", "desc", BigDecimal.valueOf(3000), 2, 20, 5, null);
        ProduitDTOResponse dto = new ProduitDTOResponse(
                10L, "Produit X", "desc", BigDecimal.valueOf(3000), "https://prod.img.url", true, 1L, "Soins", null, LocalDateTime.now(), null
        );

        when(stockSalonService.ajouterProduit("mon-salon", 1L, "stock@test.com", req, image)).thenReturn(dto);

        ResponseEntity<APIResponse<ProduitDTOResponse>> response =
                responsableStockController.ajouterProduit("mon-salon", 1L, req, image, principal);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("https://prod.img.url", response.getBody().getData().imageUrl());
        verify(stockSalonService).ajouterProduit("mon-salon", 1L, "stock@test.com", req, image);
    }

    @Test
    void modifierProduit_json_succes() {
        ProduitUpdateDTORequest req = new ProduitUpdateDTORequest("Produit X+", "desc", BigDecimal.valueOf(3500), 2, 20);
        ProduitDTOResponse dto = new ProduitDTOResponse(
                10L, "Produit X+", "desc", BigDecimal.valueOf(3500), true, 1L, "Soins", null, LocalDateTime.now(), null
        );

        when(stockSalonService.modifierProduit("mon-salon", 10L, "stock@test.com", req)).thenReturn(dto);

        ResponseEntity<APIResponse<ProduitDTOResponse>> response =
                responsableStockController.modifierProduit("mon-salon", 10L, req, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Produit X+", response.getBody().getData().nom());
        verify(stockSalonService).modifierProduit("mon-salon", 10L, "stock@test.com", req);
    }

    @Test
    void uploadImageProduit_succes() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        ProduitDTOResponse dto = new ProduitDTOResponse(
                10L, "Produit X", "desc", BigDecimal.valueOf(3000), "https://patchprod.img.url", true, 1L, "Soins", null, LocalDateTime.now(), null
        );

        when(stockSalonService.uploadImageProduit("mon-salon", 10L, "stock@test.com", file)).thenReturn(dto);

        ResponseEntity<APIResponse<ProduitDTOResponse>> response =
                responsableStockController.uploadImageProduit("mon-salon", 10L, file, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("https://patchprod.img.url", response.getBody().getData().imageUrl());
        verify(stockSalonService).uploadImageProduit("mon-salon", 10L, "stock@test.com", file);
    }

    @Test
    void basculerStatutProduit_succes() {
        ProduitDTOResponse dto = new ProduitDTOResponse(
                2L, "Après-shampoing", "desc", BigDecimal.valueOf(3500), false, 1L, "Shampoings", null, LocalDateTime.now(), null
        );

        when(stockSalonService.basculerStatutProduit("mon-salon", 2L, "stock@test.com")).thenReturn(dto);

        ResponseEntity<APIResponse<ProduitDTOResponse>> response =
                responsableStockController.basculerStatutProduit("mon-salon", 2L, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(false, response.getBody().getData().statut());
    }

    @Test
    void enregistrerMouvement_succes() {
        MouvementStockCreateDTORequest req = new MouvementStockCreateDTORequest(2L, 5, TypeMouvementStock.ENTREE, BigDecimal.valueOf(1500), "Livraison");
        MouvementStockDTOResponse dto = new MouvementStockDTOResponse(
                10L, 2L, "Après-shampoing", 5, "ENTREE", BigDecimal.valueOf(1500), LocalDateTime.now(), "Livraison", "Fatou Traore", 15
        );

        when(stockSalonService.enregistrerMouvementStock("mon-salon", "stock@test.com", req)).thenReturn(dto);

        ResponseEntity<APIResponse<MouvementStockDTOResponse>> response =
                responsableStockController.enregistrerMouvement("mon-salon", req, principal);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(15, response.getBody().getData().quantiteRestante());
    }
}

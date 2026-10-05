package com.kadi_aon.mon_salon.controller.comptable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.depense.dto.DepenseCreateDTORequest;
import com.kadi_aon.mon_salon.depense.dto.DepenseDTOResponse;
import com.kadi_aon.mon_salon.depense.enums.CategorieDepense;
import com.kadi_aon.mon_salon.depense.service.DepenseSalonService;

@ExtendWith(MockitoExtension.class)
class ComptableDepenseControllerTest {

    @Mock
    private DepenseSalonService depenseSalonService;

    @InjectMocks
    private ComptableDepenseController comptableDepenseController;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        lenient().when(principal.getName()).thenReturn("comptable@test.com");
    }

    @Test
    void creerDepense_succes() {
        DepenseCreateDTORequest req = new DepenseCreateDTORequest(
                BigDecimal.valueOf(25000), CategorieDepense.LOYER, "Loyer local"
        );
        DepenseDTOResponse dto = new DepenseDTOResponse(
                1L, BigDecimal.valueOf(25000), LocalDateTime.now(), "Loyer local", "LOYER", true, "mon-salon", "Mamadou Diallo", 10L, null
        );

        when(depenseSalonService.creerDepense("mon-salon", "comptable@test.com", req)).thenReturn(dto);

        ResponseEntity<APIResponse<DepenseDTOResponse>> resp =
                comptableDepenseController.creerDepense("mon-salon", req, principal);

        assertNotNull(resp);
        assertEquals(201, resp.getStatusCode().value());
        assertEquals("LOYER", resp.getBody().getData().categorie());
        verify(depenseSalonService).creerDepense("mon-salon", "comptable@test.com", req);
    }

    @Test
    void listerDepenses_succes() {
        DepenseDTOResponse dto = new DepenseDTOResponse(
                1L, BigDecimal.valueOf(25000), LocalDateTime.now(), "Loyer local", "LOYER", true, "mon-salon", "Mamadou Diallo", 10L, null
        );

        when(depenseSalonService.listerDepenses("mon-salon", null, null)).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<DepenseDTOResponse>>> resp =
                comptableDepenseController.listerDepenses("mon-salon", null, null);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
    }

    @Test
    void obtenirDepense_succes() {
        DepenseDTOResponse dto = new DepenseDTOResponse(
                1L, BigDecimal.valueOf(25000), LocalDateTime.now(), "Loyer local", "LOYER", true, "mon-salon", "Mamadou Diallo", 10L, null
        );

        when(depenseSalonService.obtenirDepense("mon-salon", 1L)).thenReturn(dto);

        ResponseEntity<APIResponse<DepenseDTOResponse>> resp =
                comptableDepenseController.obtenirDepense("mon-salon", 1L);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1L, resp.getBody().getData().id());
    }

    @Test
    void annulerDepense_succes() {
        DepenseDTOResponse dto = new DepenseDTOResponse(
                1L, BigDecimal.valueOf(25000), LocalDateTime.now(), "Loyer local", "LOYER", false, "mon-salon", "Mamadou Diallo", 10L, null
        );

        when(depenseSalonService.annulerDepense("mon-salon", 1L, "comptable@test.com")).thenReturn(dto);

        ResponseEntity<APIResponse<DepenseDTOResponse>> resp =
                comptableDepenseController.annulerDepense("mon-salon", 1L, principal);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(false, resp.getBody().getData().statut());
    }
}

package com.kadi_aon.mon_salon.controller.admin_systeme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.regle.dto.ReglePlateformeCreateDTORequest;
import com.kadi_aon.mon_salon.regle.dto.ReglePlateformeDTOResponse;
import com.kadi_aon.mon_salon.regle.dto.ReglePlateformeUpdateDTORequest;
import com.kadi_aon.mon_salon.regle.service.ReglePlateformeService;

@ExtendWith(MockitoExtension.class)
class AdminSystemeRegleControllerTest {

    @Mock
    private ReglePlateformeService reglePlateformeService;

    @InjectMocks
    private AdminSystemeRegleController adminSystemeRegleController;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        lenient().when(principal.getName()).thenReturn("admin@test.com");
    }

    @Test
    void creerRegle() {
        ReglePlateformeCreateDTORequest req = new ReglePlateformeCreateDTORequest("Titre", "Description");
        ReglePlateformeDTOResponse dto = ReglePlateformeDTOResponse.builder().id(1L).titre("Titre").build();

        when(reglePlateformeService.creerRegle(req, "admin@test.com")).thenReturn(dto);

        ResponseEntity<APIResponse<ReglePlateformeDTOResponse>> response =
                adminSystemeRegleController.creerRegle(req, principal);

        assertNotNull(response);
        assertEquals(201, response.getStatusCode().value());
        assertEquals("Titre", response.getBody().getData().getTitre());
    }

    @Test
    void listerRegles() {
        ReglePlateformeDTOResponse dto = ReglePlateformeDTOResponse.builder().id(1L).build();
        when(reglePlateformeService.listerRegles()).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<ReglePlateformeDTOResponse>>> response =
                adminSystemeRegleController.listerRegles();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void modifierRegle() {
        ReglePlateformeUpdateDTORequest req = new ReglePlateformeUpdateDTORequest("Titre Modifie", "Desc");
        ReglePlateformeDTOResponse dto = ReglePlateformeDTOResponse.builder().id(1L).titre("Titre Modifie").build();

        when(reglePlateformeService.modifierRegle(1L, req, "admin@test.com")).thenReturn(dto);

        ResponseEntity<APIResponse<ReglePlateformeDTOResponse>> response =
                adminSystemeRegleController.modifierRegle(1L, req, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Titre Modifie", response.getBody().getData().getTitre());
    }
}

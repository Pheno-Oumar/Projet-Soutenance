package com.kadi_aon.mon_salon.controller.admin_systeme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.kadi_aon.mon_salon.audit.dto.AuditLogDTOResponse;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogAdminService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;

@ExtendWith(MockitoExtension.class)
class AdminSystemeAuditControllerTest {

    @Mock
    private AuditLogAdminService auditLogAdminService;

    @InjectMocks
    private AdminSystemeAuditController adminSystemeAuditController;

    @Test
    void listerLogs_tous() {
        AuditLogDTOResponse dto = AuditLogDTOResponse.builder()
                .id(1L)
                .action(TypeActionAudit.CONNEXION)
                .entite("Compte")
                .dateHeure(LocalDateTime.now())
                .build();

        when(auditLogAdminService.listerTousLesLogs()).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<AuditLogDTOResponse>>> response =
                adminSystemeAuditController.listerLogs(null, null, null, null);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void rechercherLogs() {
        AuditLogDTOResponse dto = AuditLogDTOResponse.builder().id(2L).build();
        when(auditLogAdminService.rechercherLogsPlateforme("test")).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<AuditLogDTOResponse>>> response =
                adminSystemeAuditController.rechercherLogs("test");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void listerActionsSensibles() {
        AuditLogDTOResponse dto = AuditLogDTOResponse.builder().id(3L).build();
        when(auditLogAdminService.listerActionsSensiblesPlateforme()).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<AuditLogDTOResponse>>> response =
                adminSystemeAuditController.listerActionsSensibles();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void getLogById() {
        AuditLogDTOResponse dto = AuditLogDTOResponse.builder().id(5L).build();
        when(auditLogAdminService.obtenirLogPlateforme(5L)).thenReturn(dto);

        ResponseEntity<APIResponse<AuditLogDTOResponse>> response =
                adminSystemeAuditController.getLogById(5L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(5L, response.getBody().getData().getId());
    }
}

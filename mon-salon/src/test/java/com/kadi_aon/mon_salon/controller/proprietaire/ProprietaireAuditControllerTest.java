package com.kadi_aon.mon_salon.controller.proprietaire;

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
class ProprietaireAuditControllerTest {

    @Mock
    private AuditLogAdminService auditLogAdminService;

    @InjectMocks
    private ProprietaireAuditController proprietaireAuditController;

    @Test
    void listerLogs() {
        AuditLogDTOResponse dto = AuditLogDTOResponse.builder()
                .id(1L)
                .action(TypeActionAudit.CREATION)
                .salonSlug("salon-chic")
                .dateHeure(LocalDateTime.now())
                .build();

        when(auditLogAdminService.listerLogsSalon("salon-chic")).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<AuditLogDTOResponse>>> response =
                proprietaireAuditController.listerLogs("salon-chic", null, null, null, null);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void rechercherLogs() {
        AuditLogDTOResponse dto = AuditLogDTOResponse.builder().id(2L).build();
        when(auditLogAdminService.rechercherLogsSalon("salon-chic", "coiffeur")).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<AuditLogDTOResponse>>> response =
                proprietaireAuditController.rechercherLogs("salon-chic", "coiffeur");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void listerActionsSensibles() {
        AuditLogDTOResponse dto = AuditLogDTOResponse.builder().id(3L).build();
        when(auditLogAdminService.listerActionsSensiblesSalon("salon-chic")).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<AuditLogDTOResponse>>> response =
                proprietaireAuditController.listerActionsSensibles("salon-chic");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void getLogById() {
        AuditLogDTOResponse dto = AuditLogDTOResponse.builder().id(4L).build();
        when(auditLogAdminService.obtenirLogSalon("salon-chic", 4L)).thenReturn(dto);

        ResponseEntity<APIResponse<AuditLogDTOResponse>> response =
                proprietaireAuditController.getLogById("salon-chic", 4L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(4L, response.getBody().getData().getId());
    }
}

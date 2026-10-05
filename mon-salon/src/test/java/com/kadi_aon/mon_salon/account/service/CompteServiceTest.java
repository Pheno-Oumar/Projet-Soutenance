package com.kadi_aon.mon_salon.account.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kadi_aon.mon_salon.account.dto.ChangementMotDePasseDTORequest;
import com.kadi_aon.mon_salon.account.dto.CompteDTOResponse;
import com.kadi_aon.mon_salon.account.dto.CompteUpdateDTORequest;
import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.mapper.CompteDTOResponseMapper;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;

@ExtendWith(MockitoExtension.class)
class CompteServiceTest {

    @Mock
    private CompteRepository compteRepository;
    @Mock
    private CompteDTOResponseMapper compteDTOResponseMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private AffectationSalonRepository affectationSalonRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private CompteService compteService;

    private Compte compte;

    @BeforeEach
    void setUp() {
        compte = Compte.builder()
                .id(1L)
                .email("proprio@test.com")
                .nom("AncienNom")
                .prenom("AncienPrenom")
                .telephone("70000000")
                .password("hashedPassword")
                .statut(true)
                .build();
    }

    @Test
    void testUpdateProfilSuccess() {
        CompteUpdateDTORequest request = new CompteUpdateDTORequest("NouveauNom", "NouveauPrenom", LocalDate.of(1995, 5, 20), "71111111");

        when(compteRepository.findByEmail("proprio@test.com")).thenReturn(Optional.of(compte));
        when(compteRepository.findByTelephone("71111111")).thenReturn(Optional.empty());
        when(compteRepository.save(any(Compte.class))).thenAnswer(i -> i.getArgument(0));
        when(compteDTOResponseMapper.apply(any(Compte.class))).thenAnswer(i -> {
            Compte c = i.getArgument(0);
            return new CompteDTOResponse(c.getId(), c.getNom(), c.getPrenom(), c.getDateNaissance(), c.getEmail(), c.getTelephone(), c.getStatut(), null, null);
        });

        CompteDTOResponse response = compteService.updateProfil("proprio@test.com", request, null, "PROPRIETAIRE");

        assertNotNull(response);
        assertEquals("NouveauNom", response.nom());
        assertEquals("71111111", response.telephone());
        verify(auditLogService).logActionPlateforme(eq(TypeActionAudit.MODIFICATION), eq("Compte"), eq("1"), any(), any(), any(), eq("PROPRIETAIRE"));
    }

    @Test
    void testChangerMotDePasseSuccess() {
        ChangementMotDePasseDTORequest request = new ChangementMotDePasseDTORequest("ancienMdp123", "nouveauMdp456");

        when(compteRepository.findByEmail("proprio@test.com")).thenReturn(Optional.of(compte));
        when(passwordEncoder.matches("ancienMdp123", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.matches("nouveauMdp456", "hashedPassword")).thenReturn(false);
        when(passwordEncoder.encode("nouveauMdp456")).thenReturn("hashedNouveauPassword");

        assertDoesNotThrow(() -> compteService.changerMotDePasse("proprio@test.com", request, null, "PROPRIETAIRE"));

        verify(refreshTokenService).revokeAllByCompte(compte);
        verify(compteRepository).save(compte);
    }

    @Test
    void testChangerMotDePasseWrongOldPasswordThrows() {
        ChangementMotDePasseDTORequest request = new ChangementMotDePasseDTORequest("mauvaisMdp", "nouveauMdp456");

        when(compteRepository.findByEmail("proprio@test.com")).thenReturn(Optional.of(compte));
        when(passwordEncoder.matches("mauvaisMdp", "hashedPassword")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
                compteService.changerMotDePasse("proprio@test.com", request, null, "PROPRIETAIRE"));
    }

    @Test
    void testChangerMotDePasseSamePasswordThrows() {
        ChangementMotDePasseDTORequest request = new ChangementMotDePasseDTORequest("memeMdp", "memeMdp");

        when(compteRepository.findByEmail("proprio@test.com")).thenReturn(Optional.of(compte));
        when(passwordEncoder.matches("memeMdp", "hashedPassword")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                compteService.changerMotDePasse("proprio@test.com", request, null, "PROPRIETAIRE"));
    }
}

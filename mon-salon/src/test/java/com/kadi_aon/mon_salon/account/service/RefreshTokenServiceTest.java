package com.kadi_aon.mon_salon.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.entity.RefreshToken;
import com.kadi_aon.mon_salon.account.repository.RefreshTokenRepository;
import com.kadi_aon.mon_salon.common.exception.UnauthorizedException;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private Compte testCompte;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 604800000L);

        testCompte = Compte.builder()
                .id(1L)
                .email("test@example.com")
                .nom("Doe")
                .prenom("John")
                .statut(true)
                .build();
    }

    @Test
    void testCreateRefreshToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken token = refreshTokenService.createRefreshToken(testCompte);

        assertNotNull(token);
        assertNotNull(token.getToken());
        assertEquals(testCompte, token.getCompte());
        assertTrue(token.getDateExpiration().isAfter(Instant.now()));
    }

    @Test
    void testRotateRefreshTokenSuccess() {
        String oldTokenStr = UUID.randomUUID().toString();
        RefreshToken existingToken = RefreshToken.builder()
                .id(1L)
                .token(oldTokenStr)
                .compte(testCompte)
                .dateExpiration(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken(oldTokenStr)).thenReturn(Optional.of(existingToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken newToken = refreshTokenService.rotateRefreshToken(oldTokenStr);

        assertNotNull(newToken);
        assertTrue(existingToken.getRevoked());
        assertEquals(testCompte, newToken.getCompte());
    }

    @Test
    void testRotateRefreshTokenReplayAttackRevokesAll() {
        String tokenStr = UUID.randomUUID().toString();
        RefreshToken revokedToken = RefreshToken.builder()
                .id(1L)
                .token(tokenStr)
                .compte(testCompte)
                .dateExpiration(Instant.now().plusSeconds(3600))
                .revoked(true) // Déjà révoqué !
                .build();

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(revokedToken));

        assertThrows(UnauthorizedException.class, () -> refreshTokenService.rotateRefreshToken(tokenStr));
        verify(refreshTokenRepository).revokeAllByCompte(testCompte);
    }

    @Test
    void testRotateRefreshTokenExpired() {
        String tokenStr = UUID.randomUUID().toString();
        RefreshToken expiredToken = RefreshToken.builder()
                .id(1L)
                .token(tokenStr)
                .compte(testCompte)
                .dateExpiration(Instant.now().minusSeconds(100)) // Expiré
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(expiredToken));

        assertThrows(UnauthorizedException.class, () -> refreshTokenService.rotateRefreshToken(tokenStr));
        assertTrue(expiredToken.getRevoked());
    }

    @Test
    void testRotateRefreshTokenInactiveAccount() {
        testCompte.setStatut(false); // Compte inactif
        String tokenStr = UUID.randomUUID().toString();
        RefreshToken token = RefreshToken.builder()
                .id(1L)
                .token(tokenStr)
                .compte(testCompte)
                .dateExpiration(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(token));

        assertThrows(UnauthorizedException.class, () -> refreshTokenService.rotateRefreshToken(tokenStr));
    }
}

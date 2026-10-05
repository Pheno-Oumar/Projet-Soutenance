package com.kadi_aon.mon_salon.account.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.entity.RefreshToken;
import com.kadi_aon.mon_salon.account.repository.RefreshTokenRepository;
import com.kadi_aon.mon_salon.common.exception.UnauthorizedException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Transactional
    public RefreshToken createRefreshToken(Compte compte) {
        RefreshToken refreshToken = RefreshToken.builder()
                .compte(compte)
                .token(UUID.randomUUID().toString())
                .dateExpiration(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken rotateRefreshToken(String tokenStr) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new UnauthorizedException("Session introuvable ou invalide."));

        // Détection de tentative de rejeu / vol de token
        if (Boolean.TRUE.equals(token.getRevoked())) {
            log.warn("Tentative d'utilisation d'un Refresh Token déjà révoqué pour le compte {} ! Révocation de toutes les sessions.",
                    token.getCompte().getEmail());
            refreshTokenRepository.revokeAllByCompte(token.getCompte());
            throw new UnauthorizedException("Alerte de sécurité : Cette session a déjà été utilisée. Veuillez vous reconnecter.");
        }

        // Vérification de l'expiration
        if (token.isExpired()) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw new UnauthorizedException("Session expirée. Veuillez vous reconnecter.");
        }

        // Vérification de l'état du compte
        if (!Boolean.TRUE.equals(token.getCompte().getStatut())) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw new UnauthorizedException("Le compte utilisateur associé est désactivé.");
        }

        // Invalidation de l'ancien token (Rotation)
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        // Création du nouveau token
        return createRefreshToken(token.getCompte());
    }

    @Transactional
    public void revokeToken(String tokenStr) {
        refreshTokenRepository.findByToken(tokenStr).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void revokeAllByCompte(Compte compte) {
        refreshTokenRepository.revokeAllByCompte(compte);
    }
}

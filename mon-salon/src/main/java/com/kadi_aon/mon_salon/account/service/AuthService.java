package com.kadi_aon.mon_salon.account.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.account.dto.CompteSummaryDTO;
import com.kadi_aon.mon_salon.account.dto.LoginRequestDTO;
import com.kadi_aon.mon_salon.account.dto.LoginResponseDTO;
import com.kadi_aon.mon_salon.account.dto.TokenRefreshResponseDTO;
import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.entity.RefreshToken;
import com.kadi_aon.mon_salon.account.enums.TypeRolePlateforme;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.common.exception.ForbiddenException;
import com.kadi_aon.mon_salon.common.exception.UnauthorizedException;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.RoleSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;
import com.kadi_aon.mon_salon.security.jwt.JwtUtils;
import com.kadi_aon.mon_salon.security.service.UserDetailsImpl;

import com.kadi_aon.mon_salon.account.repository.RefreshTokenRepository;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;

import jakarta.persistence.EntityNotFoundException;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CompteRepository compteRepository;
    private final SalonRepository salonRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final RoleSalonRepository roleSalonRepository;
    private final AuditLogService auditLogService;

    @Getter
    @Builder
    public static class LoginResult {
        private LoginResponseDTO responseDto;
        private String refreshToken;
    }

    @Getter
    @Builder
    public static class RefreshResult {
        private TokenRefreshResponseDTO responseDto;
        private String newRefreshToken;
    }

    @Transactional
    public LoginResult loginAdminSysteme(LoginRequestDTO request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Compte compte = userDetails.getCompte();

        if (compte.getRolePlateforme() == null ||
                compte.getRolePlateforme().getRole() != TypeRolePlateforme.ADMIN_SYSTEME) {
            throw new ForbiddenException("Ce compte n'a pas les droits d'administrateur système.");
        }

        if (!Boolean.TRUE.equals(compte.getStatut())) {
            throw new UnauthorizedException("Ce compte est désactivé.");
        }

        String accessToken = jwtUtils.generateAccessToken(
                compte.getId(),
                compte.getEmail(),
                compte.getRolePlateforme().getRole().name()
        );

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(compte);

        auditLogService.logActionPlateforme(
                TypeActionAudit.CONNEXION,
                "Compte",
                String.valueOf(compte.getId()),
                null,
                "Connexion réussie Administrateur Système",
                compte,
                "ADMIN_SYSTEME"
        );

        LoginResponseDTO responseDTO = LoginResponseDTO.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpirationMs() / 1000)
                .compte(mapToSummary(compte))
                .build();

        return LoginResult.builder()
                .responseDto(responseDTO)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    @Transactional
    public LoginResult loginSalon(String slugSalon, LoginRequestDTO request) {
        // Pipeline Étape 3 : Vérifier que le salon existe et est actif
        Salon salon = salonRepository.findBySlug(slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Salon introuvable avec le slug : " + slugSalon));

        if (!Boolean.TRUE.equals(salon.getStatut())) {
            throw new ForbiddenException("Le salon '" + salon.getNom() + "' est inactif.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Compte compte = userDetails.getCompte();

        if (!Boolean.TRUE.equals(compte.getStatut())) {
            throw new UnauthorizedException("Votre compte est actuellement désactivé.");
        }

        // Pipeline Étape 4 & 5 : Vérifier l'affectation active ou auto-affectation Client
        AffectationSalon affectation = affectationSalonRepository
                .findByCompteAndSalon(compte, salon)
                .orElse(null);

        if (affectation != null) {
            if (Boolean.FALSE.equals(affectation.getStatut())) {
                throw new ForbiddenException("Votre compte client a été désactivé par ce salon. Pour réactiver votre accès, veuillez vous rendre directement au salon.");
            }
            // Garantir que tout employé/membre actif possède également le rôle CLIENT dans son affectation
            boolean hasClientRole = affectation.getRoles().stream()
                    .anyMatch(r -> r.getRole() == TypeRoleSalon.CLIENT);
            if (!hasClientRole) {
                RoleSalon roleClient = roleSalonRepository.findByRole(TypeRoleSalon.CLIENT).orElse(null);
                if (roleClient != null) {
                    affectation.getRoles().add(roleClient);
                    affectationSalonRepository.save(affectation);
                }
            }
        } else {
            // Auto-affectation Client : rattachement transparent en tant que CLIENT actif
            RoleSalon roleClient = roleSalonRepository.findByRole(TypeRoleSalon.CLIENT)
                    .orElseThrow(() -> new IllegalStateException("Rôle CLIENT non initialisé"));

            Set<RoleSalon> roles = new HashSet<>();
            roles.add(roleClient);

            AffectationSalon nouvelleAffectation = AffectationSalon.builder()
                    .compte(compte)
                    .salon(salon)
                    .roles(roles)
                    .statut(true)
                    .dateDebut(LocalDate.now())
                    .build();

            affectation = affectationSalonRepository.save(nouvelleAffectation);

            auditLogService.logActionSalon(
                    TypeActionAudit.CREATION,
                    "AffectationSalon (Client)",
                    String.valueOf(affectation.getId()),
                    null,
                    "Rattachement automatique du client '" + compte.getEmail() + "' au salon '" + salon.getNom() + "' lors de la connexion",
                    affectation,
                    "CLIENT"
            );
        }

        List<String> rolesList = affectation.getRoles().stream()
                .map(r -> r.getRole().name())
                .toList();

        String rolePlateforme = (compte.getRolePlateforme() != null) ? compte.getRolePlateforme().getRole().name() : null;
        String accessToken = jwtUtils.generateAccessToken(compte.getId(), compte.getEmail(), rolePlateforme);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(compte);

        auditLogService.logActionSalon(
                TypeActionAudit.CONNEXION,
                "Compte",
                String.valueOf(compte.getId()),
                null,
                "Connexion réussie au salon '" + salon.getNom() + "'",
                affectation,
                rolesList.isEmpty() ? "EMPLOYE" : rolesList.get(0)
        );

        LoginResponseDTO responseDTO = LoginResponseDTO.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpirationMs() / 1000)
                .compte(mapToSummary(compte))
                .slugSalon(slugSalon)
                .nomSalon(salon.getNom())
                .logoUrl(salon.getLogoUrl())
                .rolesSalon(rolesList)
                .build();

        return LoginResult.builder()
                .responseDto(responseDTO)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    @Transactional
    public RefreshResult refreshToken(String refreshTokenStr) {
        if (refreshTokenStr == null || refreshTokenStr.isBlank()) {
            throw new UnauthorizedException("Aucun Refresh Token fourni.");
        }

        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(refreshTokenStr);
        Compte compte = newRefreshToken.getCompte();

        String rolePlateforme = (compte.getRolePlateforme() != null) ? compte.getRolePlateforme().getRole().name() : null;
        String newAccessToken = jwtUtils.generateAccessToken(compte.getId(), compte.getEmail(), rolePlateforme);

        TokenRefreshResponseDTO responseDTO = TokenRefreshResponseDTO.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpirationMs() / 1000)
                .build();

        return RefreshResult.builder()
                .responseDto(responseDTO)
                .newRefreshToken(newRefreshToken.getToken())
                .build();
    }

    @Transactional
    public void logout(String refreshTokenStr) {
        if (refreshTokenStr != null && !refreshTokenStr.isBlank()) {
            refreshTokenRepository.findByToken(refreshTokenStr).ifPresent(token -> {
                auditLogService.logActionPlateforme(
                        TypeActionAudit.DECONNEXION,
                        "Compte",
                        String.valueOf(token.getCompte().getId()),
                        "session_active",
                        "session_revoquee",
                        token.getCompte(),
                        token.getCompte().getRolePlateforme() != null ? token.getCompte().getRolePlateforme().getRole().name() : "UTILISATEUR"
                );
            });
            refreshTokenService.revokeToken(refreshTokenStr);
        }
    }

    private CompteSummaryDTO mapToSummary(Compte compte) {
        return CompteSummaryDTO.builder()
                .id(compte.getId())
                .nom(compte.getNom())
                .prenom(compte.getPrenom())
                .email(compte.getEmail())
                .telephone(compte.getTelephone())
                .rolePlateforme(compte.getRolePlateforme() != null ? compte.getRolePlateforme().getRole().name() : null)
                .build();
    }
}

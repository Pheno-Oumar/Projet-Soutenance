package com.kadi_aon.mon_salon.controller.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import com.kadi_aon.mon_salon.salon.dto.SalonCheckDTO;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

import com.kadi_aon.mon_salon.account.dto.LoginRequestDTO;
import com.kadi_aon.mon_salon.account.dto.LoginResponseDTO;
import com.kadi_aon.mon_salon.account.dto.TokenRefreshResponseDTO;
import com.kadi_aon.mon_salon.account.service.AuthService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Gestion des connexions, déconnexions et rafraîchissement de session")
public class AuthController {

        private final AuthService authService;
        private final SalonRepository salonRepository;

        @Value("${app.jwt.cookie-name:refreshToken}")
        private String cookieName;

        @Value("${app.jwt.cookie-secure:false}")
        private boolean cookieSecure;

        @Value("${app.jwt.refresh-expiration-ms:604800000}")
        private long refreshExpirationMs;

        @PostMapping("/auth/login")
        @Operation(summary = "Connexion globale Administrateur Système")
        public ResponseEntity<APIResponse<LoginResponseDTO>> loginAdminSysteme(
                        @Valid @RequestBody LoginRequestDTO request) {
                AuthService.LoginResult result = authService.loginAdminSysteme(request);
                ResponseCookie cookie = createRefreshTokenCookie(result.getRefreshToken(), refreshExpirationMs / 1000);

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                                .body(new APIResponse<>(true, "Connexion réussie", result.getResponseDto()));
        }

        @PostMapping("/{slugSalon}/auth/login")
        @Operation(summary = "Connexion ciblée dans un salon spécifique")
        public ResponseEntity<APIResponse<LoginResponseDTO>> loginSalon(
                        @PathVariable String slugSalon,
                        @Valid @RequestBody LoginRequestDTO request) {

                AuthService.LoginResult result = authService.loginSalon(slugSalon, request);
                ResponseCookie cookie = createRefreshTokenCookie(result.getRefreshToken(), refreshExpirationMs / 1000);

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                                .body(new APIResponse<>(true, "Connexion au salon '" + slugSalon + "' réussie",
                                                result.getResponseDto()));
        }

        @GetMapping("/{slugSalon}/check")
        @Operation(summary = "Vérifier l'existence d'un salon par son slug (actif ou inactif)")
        public ResponseEntity<APIResponse<SalonCheckDTO>> checkSalon(@PathVariable String slugSalon) {
                Optional<Salon> salonOpt = salonRepository.findBySlug(slugSalon);
                if (salonOpt.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(new APIResponse<>(false, "Salon introuvable avec le slug '" + slugSalon + "'", null));
                }
                Salon s = salonOpt.get();
                SalonCheckDTO dto = new SalonCheckDTO(
                                true,
                                s.getId(),
                                s.getNom(),
                                s.getSlug(),
                                Boolean.TRUE.equals(s.getStatut()),
                                s.getLogoUrl(),
                                s.getTelephone(),
                                s.getAdresse()
                );
                return ResponseEntity.ok(new APIResponse<>(true, "Salon trouvé", dto));
        }

        @PostMapping("/auth/refresh-token")
        @Operation(summary = "Rafraîchir l'Access Token via le Refresh Token du Cookie HttpOnly")
        public ResponseEntity<APIResponse<TokenRefreshResponseDTO>> refreshToken(
                        @CookieValue(name = "${app.jwt.cookie-name:refreshToken}", required = false) String refreshTokenCookie) {

                AuthService.RefreshResult result = authService.refreshToken(refreshTokenCookie);
                ResponseCookie cookie = createRefreshTokenCookie(result.getNewRefreshToken(),
                                refreshExpirationMs / 1000);

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                                .body(new APIResponse<>(true, "Session rafraîchie avec succès",
                                                result.getResponseDto()));
        }

        @PostMapping("/auth/logout")
        @Operation(summary = "Déconnexion et révocation de la session")
        public ResponseEntity<APIResponse<Void>> logout(
                        @CookieValue(name = "${app.jwt.cookie-name:refreshToken}", required = false) String refreshTokenCookie) {

                authService.logout(refreshTokenCookie);
                ResponseCookie cleanCookie = createRefreshTokenCookie("", 0);

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                                .body(new APIResponse<>(true, "Déconnexion réussie", null));
        }

        private ResponseCookie createRefreshTokenCookie(String token, long maxAgeSeconds) {
                return ResponseCookie.from(cookieName, token)
                                .httpOnly(true)
                                .secure(cookieSecure)
                                .path("/api/v1/mon-salon/auth")
                                .maxAge(maxAgeSeconds)
                                .sameSite("Strict")
                                .build();
        }
}

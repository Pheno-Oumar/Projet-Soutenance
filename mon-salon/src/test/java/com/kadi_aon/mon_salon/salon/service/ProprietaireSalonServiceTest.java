package com.kadi_aon.mon_salon.salon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.account.service.RefreshTokenService;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.common.service.CloudinaryService;
import com.kadi_aon.mon_salon.salon.dto.EmployeDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.EmployeUpdateRolesDTORequest;
import com.kadi_aon.mon_salon.salon.dto.SalonDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.SalonUpdateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.TransfertProprieteDTORequest;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.mapper.EmployeDTOResponseMapper;
import com.kadi_aon.mon_salon.salon.mapper.SalonDTOResponseMapper;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.RoleSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

@ExtendWith(MockitoExtension.class)
class ProprietaireSalonServiceTest {

    @Mock
    private SalonRepository salonRepository;
    @Mock
    private AffectationSalonRepository affectationSalonRepository;
    @Mock
    private SalonDTOResponseMapper salonDTOResponseMapper;
    @Mock
    private AdminSalonService adminSalonService;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private EmployeSalonService employeSalonService;
    @Mock
    private EmployeDTOResponseMapper employeDTOResponseMapper;
    @Mock
    private CompteRepository compteRepository;
    @Mock
    private RoleSalonRepository roleSalonRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private ProprietaireSalonService proprietaireSalonService;

    private Salon salon;
    private AffectationSalon affectation;
    private Compte proprioCompte;
    private RoleSalon roleProprio;
    private RoleSalon roleClient;

    @BeforeEach
    void setUp() {
        salon = Salon.builder()
                .id(1L)
                .nom("Salon Original")
                .slug("salon-original")
                .statut(true)
                .build();

        proprioCompte = Compte.builder()
                .id(100L)
                .email("proprio@test.com")
                .nom("Dupont")
                .prenom("Jean")
                .password("encoded_pass")
                .statut(true)
                .build();

        roleProprio = RoleSalon.builder().id(1L).role(TypeRoleSalon.PROPRIETAIRE).build();
        roleClient = RoleSalon.builder().id(2L).role(TypeRoleSalon.CLIENT).build();

        affectation = AffectationSalon.builder()
                .id(10L)
                .salon(salon)
                .compte(proprioCompte)
                .roles(new HashSet<>(Set.of(roleProprio, roleClient)))
                .statut(true)
                .build();
    }

    @Test
    void testUpdateSalonWithNewNameGeneratesNewSlug() {
        SalonUpdateDTORequest request = new SalonUpdateDTORequest(
                "Salon Nouveau Nom", "Nouvelle description", "123 Rue de la Paix", "0102030405", "contact@nouveau.com", 48.8566, 2.3522
        );

        when(salonRepository.findBySlug("salon-original")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-original"))
                .thenReturn(Optional.of(affectation));
        when(adminSalonService.generateUniqueSlug("Salon Nouveau Nom")).thenReturn("salon-nouveau-nom");
        when(salonRepository.save(any(Salon.class))).thenAnswer(i -> i.getArgument(0));
        when(salonDTOResponseMapper.apply(any(Salon.class))).thenAnswer(i -> {
            Salon s = i.getArgument(0);
            return new SalonDTOResponse(s.getId(), s.getNom(), s.getSlug(), s.getLogoUrl(), s.getDescription(), s.getAdresse(), s.getTelephone(), s.getEmail(), s.getLatitude(), s.getLongitude(), s.getStatut(), null, "proprio@test.com");
        });

        SalonDTOResponse response = proprietaireSalonService.updateSalon("salon-original", request, "proprio@test.com");

        assertNotNull(response);
        assertEquals("Salon Nouveau Nom", response.nom());
        assertEquals("salon-nouveau-nom", response.slug());
        assertEquals(48.8566, response.latitude());
        assertEquals(2.3522, response.longitude());

        verify(auditLogService).logActionSalon(eq(TypeActionAudit.MODIFICATION), eq("Salon"), eq("1"), any(), any(), eq(affectation), eq("PROPRIETAIRE"));
    }

    @Test
    void testUploadLogoWebPSuccess() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);

        when(salonRepository.findBySlug("salon-original")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-original"))
                .thenReturn(Optional.of(affectation));
        when(cloudinaryService.uploadLogo(mockFile, "salon-original")).thenReturn("https://res.cloudinary.com/test/image/upload/v1/logo.webp");
        when(salonRepository.save(any(Salon.class))).thenAnswer(i -> i.getArgument(0));
        when(salonDTOResponseMapper.apply(any(Salon.class))).thenAnswer(i -> {
            Salon s = i.getArgument(0);
            return new SalonDTOResponse(s.getId(), s.getNom(), s.getSlug(), s.getLogoUrl(), null, null, null, null, null, null, true, null, "proprio@test.com");
        });

        SalonDTOResponse response = proprietaireSalonService.uploadLogo("salon-original", mockFile, "proprio@test.com");

        assertNotNull(response);
        assertEquals("https://res.cloudinary.com/test/image/upload/v1/logo.webp", response.logoUrl());
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.MODIFICATION), eq("Salon (Logo)"), eq("1"), any(), eq("https://res.cloudinary.com/test/image/upload/v1/logo.webp"), eq(affectation), eq("PROPRIETAIRE"));
    }

    @Test
    void testGetMesRolesSuccess() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-original"))
                .thenReturn(Optional.of(affectation));
        when(employeDTOResponseMapper.apply(affectation))
                .thenReturn(new EmployeDTOResponse(10L, 100L, "Dupont", "Jean", "proprio@test.com", "0102030405", Set.of("PROPRIETAIRE", "CLIENT"), true, LocalDate.now(), null));

        EmployeDTOResponse response = proprietaireSalonService.getMesRoles("salon-original", "proprio@test.com");

        assertNotNull(response);
        assertTrue(response.roles().contains("PROPRIETAIRE"));
        assertTrue(response.roles().contains("CLIENT"));
    }

    @Test
    void testUpdateMesRolesDelegatesToEmployeSalonService() {
        EmployeUpdateRolesDTORequest request = new EmployeUpdateRolesDTORequest(Set.of(TypeRoleSalon.COIFFEUR));

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-original"))
                .thenReturn(Optional.of(affectation));
        when(employeSalonService.updateRoles("salon-original", 10L, request, "proprio@test.com"))
                .thenReturn(new EmployeDTOResponse(10L, 100L, "Dupont", "Jean", "proprio@test.com", "0102030405", Set.of("PROPRIETAIRE", "CLIENT", "COIFFEUR"), true, LocalDate.now(), null));

        EmployeDTOResponse response = proprietaireSalonService.updateMesRoles("salon-original", request, "proprio@test.com");

        assertNotNull(response);
        verify(employeSalonService).updateRoles("salon-original", 10L, request, "proprio@test.com");
    }

    @Test
    void testTransfererProprieteSuccess() {
        TransfertProprieteDTORequest request = new TransfertProprieteDTORequest("nouveau@test.com", "secret123");

        Compte nouveauCompte = Compte.builder().id(200L).email("nouveau@test.com").statut(true).build();

        when(salonRepository.findBySlug("salon-original")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-original"))
                .thenReturn(Optional.of(affectation));
        when(passwordEncoder.matches("secret123", "encoded_pass")).thenReturn(true);
        when(compteRepository.findByEmail("nouveau@test.com")).thenReturn(Optional.of(nouveauCompte));
        when(roleSalonRepository.findByRole(TypeRoleSalon.PROPRIETAIRE)).thenReturn(Optional.of(roleProprio));
        when(roleSalonRepository.findByRole(TypeRoleSalon.CLIENT)).thenReturn(Optional.of(roleClient));
        when(affectationSalonRepository.findByCompteAndSalon(nouveauCompte, salon)).thenReturn(Optional.empty());
        when(salonDTOResponseMapper.apply(salon)).thenReturn(new SalonDTOResponse(1L, "Salon Original", "salon-original", null, null, null, null, null, null, null, true, null, "nouveau@test.com"));

        SalonDTOResponse response = proprietaireSalonService.transfererPropriete("salon-original", request, "proprio@test.com");

        assertNotNull(response);
        // Ancien propriétaire n'a plus PROPRIETAIRE
        assertFalse(affectation.getRoles().stream().anyMatch(r -> r.getRole() == TypeRoleSalon.PROPRIETAIRE));
        // Sessions révoquées
        verify(refreshTokenService).revokeAllByCompte(proprioCompte);
        verify(refreshTokenService).revokeAllByCompte(nouveauCompte);
        // Audit log
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.MODIFICATION), eq("Salon (Transfert Propriétaire)"), eq("1"), eq("proprio@test.com"), eq("nouveau@test.com"), any(), eq("PROPRIETAIRE"));
    }

    @Test
    void testTransfererProprieteMotDePasseInvalide() {
        TransfertProprieteDTORequest request = new TransfertProprieteDTORequest("nouveau@test.com", "mauvais_pass");

        when(salonRepository.findBySlug("salon-original")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-original"))
                .thenReturn(Optional.of(affectation));
        when(passwordEncoder.matches("mauvais_pass", "encoded_pass")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> proprietaireSalonService.transfererPropriete("salon-original", request, "proprio@test.com"));

        assertTrue(ex.getMessage().contains("Mot de passe"));
    }
}

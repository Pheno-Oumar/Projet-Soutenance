package com.kadi_aon.mon_salon.salon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.account.service.RefreshTokenService;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.notification.service.NotificationEmailService;
import com.kadi_aon.mon_salon.salon.dto.EmployeCreateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.EmployeDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.EmployeUpdateRolesDTORequest;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.mapper.EmployeDTOResponseMapper;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.RoleSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

@ExtendWith(MockitoExtension.class)
class EmployeSalonServiceTest {

    @Mock
    private SalonRepository salonRepository;
    @Mock
    private CompteRepository compteRepository;
    @Mock
    private RoleSalonRepository roleSalonRepository;
    @Mock
    private AffectationSalonRepository affectationSalonRepository;
    @Mock
    private EmployeDTOResponseMapper employeDTOResponseMapper;
    @Mock
    private NotificationEmailService notificationEmailService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private EmployeSalonService employeSalonService;

    private Salon salon;
    private AffectationSalon proprioAffectation;
    private Compte proprioCompte;
    private RoleSalon roleCoiffeur;
    private RoleSalon roleManager;
    private RoleSalon roleClient;
    private RoleSalon roleProprio;

    @BeforeEach
    void setUp() {
        salon = Salon.builder()
                .id(1L)
                .nom("Salon Chic")
                .slug("salon-chic")
                .statut(true)
                .build();

        proprioCompte = Compte.builder()
                .id(100L)
                .email("proprio@test.com")
                .nom("Dupont")
                .prenom("Jean")
                .statut(true)
                .build();

        roleCoiffeur = RoleSalon.builder().id(1L).role(TypeRoleSalon.COIFFEUR).build();
        roleManager = RoleSalon.builder().id(2L).role(TypeRoleSalon.MANAGER).build();
        roleClient = RoleSalon.builder().id(3L).role(TypeRoleSalon.CLIENT).build();
        roleProprio = RoleSalon.builder().id(4L).role(TypeRoleSalon.PROPRIETAIRE).build();

        proprioAffectation = AffectationSalon.builder()
                .id(10L)
                .salon(salon)
                .compte(proprioCompte)
                .roles(new HashSet<>(Set.of(roleProprio, roleClient)))
                .statut(true)
                .build();

        lenient().when(roleSalonRepository.findByRole(TypeRoleSalon.COIFFEUR)).thenReturn(Optional.of(roleCoiffeur));
        lenient().when(roleSalonRepository.findByRole(TypeRoleSalon.MANAGER)).thenReturn(Optional.of(roleManager));
        lenient().when(roleSalonRepository.findByRole(TypeRoleSalon.CLIENT)).thenReturn(Optional.of(roleClient));
        lenient().when(roleSalonRepository.findByRole(TypeRoleSalon.PROPRIETAIRE)).thenReturn(Optional.of(roleProprio));
    }

    @Test
    void testCreerEmployeSucces() {
        EmployeCreateDTORequest request = new EmployeCreateDTORequest(
                "Martin", "Paul", "coiffeur@test.com", "0611223344",
                Set.of(TypeRoleSalon.COIFFEUR)
        );

        when(salonRepository.findBySlug("salon-chic")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-chic"))
                .thenReturn(Optional.of(proprioAffectation));
        when(compteRepository.findByEmail("coiffeur@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedTempPassword");
        when(compteRepository.save(any(Compte.class))).thenAnswer(i -> {
            Compte c = i.getArgument(0);
            c.setId(200L);
            return c;
        });
        when(roleSalonRepository.findByRole(TypeRoleSalon.COIFFEUR)).thenReturn(Optional.of(roleCoiffeur));
        when(roleSalonRepository.findByRole(TypeRoleSalon.CLIENT)).thenReturn(Optional.of(roleClient));
        when(affectationSalonRepository.save(any(AffectationSalon.class))).thenAnswer(i -> {
            AffectationSalon a = i.getArgument(0);
            a.setId(20L);
            return a;
        });
        when(employeDTOResponseMapper.apply(any(AffectationSalon.class))).thenAnswer(i -> {
            AffectationSalon a = i.getArgument(0);
            return new EmployeDTOResponse(a.getId(), a.getCompte().getId(), a.getCompte().getNom(),
                    a.getCompte().getPrenom(), a.getCompte().getEmail(), a.getCompte().getTelephone(),
                    Set.of("COIFFEUR"), a.getStatut(), LocalDate.now(), null);
        });

        EmployeDTOResponse response = employeSalonService.creerEmploye("salon-chic", request, "proprio@test.com");

        assertNotNull(response);
        assertEquals("coiffeur@test.com", response.email());
        assertTrue(response.roles().contains("COIFFEUR"));
        verify(notificationEmailService).queueNouveauProprietaireEmail(eq("coiffeur@test.com"), eq("Salon Chic"), anyString());
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.CREATION), eq("AffectationSalon"), eq("20"), any(), any(), eq(proprioAffectation), eq("PROPRIETAIRE"));
    }

    @Test
    void testCreerEmployeRejetRoleProprietaire() {
        EmployeCreateDTORequest request = new EmployeCreateDTORequest(
                "Hacker", "Joe", "hacker@test.com", "0600000000",
                Set.of(TypeRoleSalon.PROPRIETAIRE)
        );

        when(salonRepository.findBySlug("salon-chic")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-chic"))
                .thenReturn(Optional.of(proprioAffectation));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> employeSalonService.creerEmploye("salon-chic", request, "proprio@test.com"));

        assertTrue(ex.getMessage().contains("PROPRIETAIRE"));
    }

    @Test
    void testCreerEmployeRejetDejaAffecte() {
        EmployeCreateDTORequest request = new EmployeCreateDTORequest(
                "Existant", "Bob", "existant@test.com", "0600000000",
                Set.of(TypeRoleSalon.COIFFEUR)
        );

        when(salonRepository.findBySlug("salon-chic")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-chic"))
                .thenReturn(Optional.of(proprioAffectation));
        Compte existantCompte = Compte.builder().id(300L).email("existant@test.com").build();
        when(compteRepository.findByEmail("existant@test.com")).thenReturn(Optional.of(existantCompte));
        when(affectationSalonRepository.findByCompteAndSalonOrderByStatutDescIdDesc(existantCompte, salon))
                .thenReturn(List.of(AffectationSalon.builder().id(30L).statut(true).build()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> employeSalonService.creerEmploye("salon-chic", request, "proprio@test.com"));

        assertTrue(ex.getMessage().contains("possède déjà une affectation active"));
    }

    @Test
    void testUpdateRolesSucces() {
        Compte employeCompte = Compte.builder().id(200L).email("employe@test.com").build();
        AffectationSalon employeAffectation = AffectationSalon.builder()
                .id(20L)
                .salon(salon)
                .compte(employeCompte)
                .roles(new HashSet<>(Set.of(roleCoiffeur)))
                .statut(true)
                .build();

        EmployeUpdateRolesDTORequest request = new EmployeUpdateRolesDTORequest(Set.of(TypeRoleSalon.MANAGER));

        when(affectationSalonRepository.findById(20L)).thenReturn(Optional.of(employeAffectation));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-chic"))
                .thenReturn(Optional.of(proprioAffectation));
        when(roleSalonRepository.findByRole(TypeRoleSalon.MANAGER)).thenReturn(Optional.of(roleManager));
        when(roleSalonRepository.findByRole(TypeRoleSalon.CLIENT)).thenReturn(Optional.of(roleClient));
        when(affectationSalonRepository.save(any(AffectationSalon.class))).thenAnswer(i -> i.getArgument(0));
        when(employeDTOResponseMapper.apply(any(AffectationSalon.class))).thenAnswer(i -> {
            AffectationSalon a = i.getArgument(0);
            return new EmployeDTOResponse(a.getId(), a.getCompte().getId(), a.getCompte().getNom(),
                    a.getCompte().getPrenom(), a.getCompte().getEmail(), a.getCompte().getTelephone(),
                    Set.of("MANAGER"), true, LocalDate.now(), null);
        });

        EmployeDTOResponse response = employeSalonService.updateRoles("salon-chic", 20L, request, "proprio@test.com");

        assertNotNull(response);
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.MODIFICATION), eq("AffectationSalon (Roles)"), eq("20"), any(), any(), eq(proprioAffectation), eq("PROPRIETAIRE"));
    }

    @Test
    void testUpdateRolesProprietairePeutSattribuerDesRolesEtConserveToujoursProprietaireEtClient() {
        when(affectationSalonRepository.findById(10L)).thenReturn(Optional.of(proprioAffectation));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-chic"))
                .thenReturn(Optional.of(proprioAffectation));
        when(affectationSalonRepository.save(any(AffectationSalon.class))).thenAnswer(i -> i.getArgument(0));
        when(employeDTOResponseMapper.apply(any(AffectationSalon.class))).thenAnswer(i -> {
            AffectationSalon a = i.getArgument(0);
            return new EmployeDTOResponse(a.getId(), a.getCompte().getId(), a.getCompte().getNom(),
                    a.getCompte().getPrenom(), a.getCompte().getEmail(), a.getCompte().getTelephone(),
                    Set.of("PROPRIETAIRE", "CLIENT", "COIFFEUR"), true, LocalDate.now(), null);
        });

        EmployeUpdateRolesDTORequest request = new EmployeUpdateRolesDTORequest(Set.of(TypeRoleSalon.COIFFEUR));

        EmployeDTOResponse response = employeSalonService.updateRoles("salon-chic", 10L, request, "proprio@test.com");

        assertNotNull(response);
        // Vérifie que l'affectation enregistrée contient bien PROPRIETAIRE, CLIENT et COIFFEUR
        assertTrue(proprioAffectation.getRoles().stream().anyMatch(r -> r.getRole() == TypeRoleSalon.PROPRIETAIRE));
        assertTrue(proprioAffectation.getRoles().stream().anyMatch(r -> r.getRole() == TypeRoleSalon.CLIENT));
        assertTrue(proprioAffectation.getRoles().stream().anyMatch(r -> r.getRole() == TypeRoleSalon.COIFFEUR));
    }

    @Test
    void testUpdateRolesEmployeNePeutPasRecevoirProprietaire() {
        Compte employeCompte = Compte.builder().id(200L).email("employe@test.com").build();
        AffectationSalon employeAffectation = AffectationSalon.builder()
                .id(20L)
                .salon(salon)
                .compte(employeCompte)
                .roles(new HashSet<>(Set.of(roleCoiffeur)))
                .statut(true)
                .build();

        when(affectationSalonRepository.findById(20L)).thenReturn(Optional.of(employeAffectation));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-chic"))
                .thenReturn(Optional.of(proprioAffectation));

        EmployeUpdateRolesDTORequest request = new EmployeUpdateRolesDTORequest(Set.of(TypeRoleSalon.PROPRIETAIRE));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> employeSalonService.updateRoles("salon-chic", 20L, request, "proprio@test.com"));

        assertTrue(ex.getMessage().contains("ne peut pas être attribué"));
    }

    @Test
    void testDesactiverEmployeSuccesEtRevocationTokens() {
        Compte employeCompte = Compte.builder().id(200L).email("employe@test.com").build();
        AffectationSalon employeAffectation = AffectationSalon.builder()
                .id(20L)
                .salon(salon)
                .compte(employeCompte)
                .statut(true)
                .build();

        when(affectationSalonRepository.findById(20L)).thenReturn(Optional.of(employeAffectation));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-chic"))
                .thenReturn(Optional.of(proprioAffectation));
        when(affectationSalonRepository.save(any(AffectationSalon.class))).thenAnswer(i -> i.getArgument(0));
        when(employeDTOResponseMapper.apply(any(AffectationSalon.class))).thenAnswer(i -> {
            AffectationSalon a = i.getArgument(0);
            return new EmployeDTOResponse(a.getId(), a.getCompte().getId(), a.getCompte().getNom(),
                    a.getCompte().getPrenom(), a.getCompte().getEmail(), a.getCompte().getTelephone(),
                    Set.of(), false, LocalDate.now(), LocalDate.now());
        });

        EmployeDTOResponse response = employeSalonService.desactiverEmploye("salon-chic", 20L, "proprio@test.com");

        assertNotNull(response);
        assertFalse(response.statut());
        verify(refreshTokenService).revokeAllByCompte(employeCompte);
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.DESACTIVATION), eq("AffectationSalon"), eq("20"), eq("statut=true"), eq("statut=false"), eq(proprioAffectation), eq("PROPRIETAIRE"));
    }

    @Test
    void testDesactiverEmployeInterdictionAutoDesactivation() {
        when(affectationSalonRepository.findById(10L)).thenReturn(Optional.of(proprioAffectation));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-chic"))
                .thenReturn(Optional.of(proprioAffectation));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> employeSalonService.desactiverEmploye("salon-chic", 10L, "proprio@test.com"));

        assertTrue(ex.getMessage().contains("désactiver lui-même"));
    }

    @Test
    void testReactiverEmployeSucces() {
        Compte employeCompte = Compte.builder().id(200L).email("employe@test.com").build();
        AffectationSalon employeAffectation = AffectationSalon.builder()
                .id(20L)
                .salon(salon)
                .compte(employeCompte)
                .statut(false)
                .build();

        when(affectationSalonRepository.findById(20L)).thenReturn(Optional.of(employeAffectation));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-chic"))
                .thenReturn(Optional.of(proprioAffectation));
        when(affectationSalonRepository.save(any(AffectationSalon.class))).thenAnswer(i -> i.getArgument(0));
        when(employeDTOResponseMapper.apply(any(AffectationSalon.class))).thenAnswer(i -> {
            AffectationSalon a = i.getArgument(0);
            return new EmployeDTOResponse(a.getId(), a.getCompte().getId(), a.getCompte().getNom(),
                    a.getCompte().getPrenom(), a.getCompte().getEmail(), a.getCompte().getTelephone(),
                    Set.of(), true, LocalDate.now(), null);
        });

        EmployeDTOResponse response = employeSalonService.reactiverEmploye("salon-chic", 20L, "proprio@test.com");

        assertNotNull(response);
        assertTrue(response.statut());
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.REACTIVATION), eq("AffectationSalon"), eq("20"), eq("statut=false"), eq("statut=true"), eq(proprioAffectation), eq("PROPRIETAIRE"));
    }
}

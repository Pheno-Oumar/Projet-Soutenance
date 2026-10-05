package com.kadi_aon.mon_salon.salon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import com.kadi_aon.mon_salon.account.dto.CompteDTOResponse;
import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.mapper.CompteDTOResponseMapper;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.service.RendezVousService;
import com.kadi_aon.mon_salon.salon.dto.ClientRegisterDTORequest;
import com.kadi_aon.mon_salon.salon.dto.VitrineRendezVousDTORequest;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.RoleSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

@ExtendWith(MockitoExtension.class)
class ClientSalonServiceTest {

    @Mock
    private SalonRepository salonRepository;
    @Mock
    private CompteRepository compteRepository;
    @Mock
    private RoleSalonRepository roleSalonRepository;
    @Mock
    private AffectationSalonRepository affectationSalonRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CompteDTOResponseMapper compteDTOResponseMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private RendezVousService rendezVousService;

    @InjectMocks
    private ClientSalonService clientSalonService;

    private Salon salon;

    @BeforeEach
    void setUp() {
        salon = Salon.builder().id(1L).slug("mon-salon").statut(true).nom("Mon Salon").build();
    }

    @Test
    void enregistrerClientDansSalon_nouveauCompte_succes() {
        when(salonRepository.findBySlug("mon-salon")).thenReturn(Optional.of(salon));
        when(compteRepository.findByEmail("nouveau.client@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("encodedPassword");

        when(compteRepository.save(any(Compte.class))).thenAnswer(i -> {
            Compte c = i.getArgument(0);
            c.setId(10L);
            return c;
        });

        RoleSalon roleClient = RoleSalon.builder().id(1L).role(TypeRoleSalon.CLIENT).build();
        when(roleSalonRepository.findByRole(TypeRoleSalon.CLIENT)).thenReturn(Optional.of(roleClient));

        when(affectationSalonRepository.save(any(AffectationSalon.class))).thenAnswer(i -> {
            AffectationSalon a = i.getArgument(0);
            a.setId(50L);
            return a;
        });

        CompteDTOResponse expected = new CompteDTOResponse(
                10L, "Kante", "Fatou", LocalDate.of(1995, 5, 12),
                "nouveau.client@test.com", "0601020304", true, null, null
        );
        when(compteDTOResponseMapper.apply(any())).thenReturn(expected);

        ClientRegisterDTORequest request = new ClientRegisterDTORequest(
                "Kante", "Fatou", "nouveau.client@test.com", "0601020304", "secret123", LocalDate.of(1995, 5, 12)
        );

        CompteDTOResponse response = clientSalonService.enregistrerClientDansSalon("mon-salon", request);

        assertNotNull(response);
        assertEquals("Fatou", response.prenom());
        assertEquals("nouveau.client@test.com", response.email());
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.CREATION), eq("AffectationSalon (Client)"), eq("50"), any(), anyString(), any(), eq("CLIENT"));
    }

    @Test
    void enregistrerClientDansSalon_salonInactif_lanceException() {
        salon.setStatut(false);
        when(salonRepository.findBySlug("mon-salon")).thenReturn(Optional.of(salon));

        ClientRegisterDTORequest request = new ClientRegisterDTORequest(
                "Kante", "Fatou", "client@test.com", "0601020304", "secret123", null
        );

        assertThrows(IllegalArgumentException.class, () ->
                clientSalonService.enregistrerClientDansSalon("mon-salon", request));
    }

    @Test
    void enregistrerClientDansSalon_dejaInscrit_lanceException() {
        when(salonRepository.findBySlug("mon-salon")).thenReturn(Optional.of(salon));

        Compte existant = Compte.builder().id(10L).email("client@test.com").build();
        when(compteRepository.findByEmail("client@test.com")).thenReturn(Optional.of(existant));

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("client@test.com", "mon-salon"))
                .thenReturn(Optional.of(AffectationSalon.builder().id(1L).build()));

        ClientRegisterDTORequest request = new ClientRegisterDTORequest(
                "Kante", "Fatou", "client@test.com", "0601020304", "secret123", null
        );

        assertThrows(IllegalArgumentException.class, () ->
                clientSalonService.enregistrerClientDansSalon("mon-salon", request));
    }

    @Test
    void reserverRendezVousDepuisVitrine_succes() {
        when(salonRepository.findBySlug("mon-salon")).thenReturn(Optional.of(salon));
        when(compteRepository.findByEmail("guest@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        when(compteRepository.save(any(Compte.class))).thenAnswer(i -> {
            Compte c = i.getArgument(0);
            c.setId(22L);
            return c;
        });

        RoleSalon roleClient = RoleSalon.builder().id(1L).role(TypeRoleSalon.CLIENT).build();
        when(roleSalonRepository.findByRole(TypeRoleSalon.CLIENT)).thenReturn(Optional.of(roleClient));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("guest@test.com", "mon-salon"))
                .thenReturn(Optional.empty());

        when(affectationSalonRepository.save(any(AffectationSalon.class))).thenAnswer(i -> {
            AffectationSalon a = i.getArgument(0);
            a.setId(60L);
            return a;
        });

        RendezVousDTOResponse expectedRdv = new RendezVousDTOResponse(
                100L, "mon-salon", "Mon Salon", 1L, "Stylist", "Bob",
                "Guest", "User", "0600000000",
                java.time.LocalDateTime.now().plusDays(2),
                java.time.LocalDateTime.now().plusDays(2).plusMinutes(45),
                "CONFIRME", java.math.BigDecimal.valueOf(5000),
                java.time.LocalDateTime.now(), java.util.Collections.emptyList()
        );
        when(rendezVousService.creerRendezVousClient(eq("mon-salon"), any(), eq("guest@test.com")))
                .thenReturn(expectedRdv);

        VitrineRendezVousDTORequest request = new VitrineRendezVousDTORequest(
                java.time.LocalDateTime.now().plusDays(2),
                java.util.List.of(1L, 2L),
                null,
                "Guest",
                "User",
                "guest@test.com",
                "0600000000",
                "pass123",
                "Demande spéciale"
        );

        RendezVousDTOResponse response = clientSalonService.reserverRendezVousDepuisVitrine("mon-salon", request);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("CONFIRME", response.statut());
        verify(rendezVousService).creerRendezVousClient(eq("mon-salon"), any(), eq("guest@test.com"));
    }
}

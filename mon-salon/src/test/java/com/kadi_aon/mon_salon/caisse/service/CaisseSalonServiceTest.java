package com.kadi_aon.mon_salon.caisse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.caisse.dto.SessionCaisseClotureDTORequest;
import com.kadi_aon.mon_salon.caisse.dto.SessionCaisseDTOResponse;
import com.kadi_aon.mon_salon.caisse.dto.SessionCaisseOuvertureDTORequest;
//import com.kadi_aon.mon_salon.caisse.entity.OperationCaisse;
import com.kadi_aon.mon_salon.caisse.entity.SessionCaisse;
import com.kadi_aon.mon_salon.caisse.enums.StatutSessionCaisse;
//import com.kadi_aon.mon_salon.caisse.enums.TypeOperationCaisse;
import com.kadi_aon.mon_salon.caisse.exception.SessionCaisseFermeeException;
import com.kadi_aon.mon_salon.caisse.repository.OperationCaisseRepository;
import com.kadi_aon.mon_salon.caisse.repository.SessionCaisseRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

@ExtendWith(MockitoExtension.class)
class CaisseSalonServiceTest {

    @Mock
    private SessionCaisseRepository sessionCaisseRepository;

    @Mock
    private OperationCaisseRepository operationCaisseRepository;

    @Mock
    private AffectationSalonRepository affectationSalonRepository;

    @Mock
    private SalonRepository salonRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private CaisseSalonService caisseSalonService;

    private Salon salon;
    private Compte comptable;
    private AffectationSalon affectation;

    @BeforeEach
    void setUp() {
        salon = Salon.builder().id(1L).slug("salon-chic").nom("Salon Chic").statut(true).build();
        comptable = Compte.builder().id(10L).nom("Dupont").prenom("Jean").email("comptable@test.com").build();

        RoleSalon roleComptable = RoleSalon.builder().id(1L).role(TypeRoleSalon.COMPTABLE).build();
        Set<RoleSalon> roles = new HashSet<>();
        roles.add(roleComptable);

        affectation = AffectationSalon.builder()
                .id(100L)
                .salon(salon)
                .compte(comptable)
                .roles(roles)
                .statut(true)
                .build();
    }

    @Test
    void ouvrirSessionCaisse_succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("comptable@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectation));
        when(sessionCaisseRepository.existsByAffectationSalonSlugAndStatut("salon-chic", StatutSessionCaisse.EN_COURS))
                .thenReturn(false);

        SessionCaisse sessionMock = SessionCaisse.builder()
                .id(1L)
                .affectation(affectation)
                .soldeOuverture(new BigDecimal("100.00"))
                .statut(StatutSessionCaisse.EN_COURS)
                .dateOuverture(LocalDateTime.now())
                .operations(new ArrayList<>())
                .build();

        when(sessionCaisseRepository.save(any(SessionCaisse.class))).thenReturn(sessionMock);

        SessionCaisseOuvertureDTORequest request = new SessionCaisseOuvertureDTORequest(new BigDecimal("100.00"));
        SessionCaisseDTOResponse response = caisseSalonService.ouvrirSessionCaisse("salon-chic", "comptable@test.com", request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(new BigDecimal("100.00"), response.soldeOuverture());
        assertEquals("EN_COURS", response.statut());
        verify(sessionCaisseRepository).save(any(SessionCaisse.class));
    }

    @Test
    void ouvrirSessionCaisse_dejaEnCours_lanceIllegalStateException() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("comptable@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectation));
        when(sessionCaisseRepository.existsByAffectationSalonSlugAndStatut("salon-chic", StatutSessionCaisse.EN_COURS))
                .thenReturn(true);

        SessionCaisseOuvertureDTORequest request = new SessionCaisseOuvertureDTORequest(new BigDecimal("100.00"));

        assertThrows(IllegalStateException.class, () ->
                caisseSalonService.ouvrirSessionCaisse("salon-chic", "comptable@test.com", request));
    }

    @Test
    void cloturerSessionCaisse_succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("comptable@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectation));

        SessionCaisse sessionActive = SessionCaisse.builder()
                .id(1L)
                .affectation(affectation)
                .soldeOuverture(new BigDecimal("100.00"))
                .statut(StatutSessionCaisse.EN_COURS)
                .dateOuverture(LocalDateTime.now())
                .operations(new ArrayList<>())
                .build();

        when(sessionCaisseRepository.findByAffectationSalonSlugAndStatut("salon-chic", StatutSessionCaisse.EN_COURS))
                .thenReturn(Optional.of(sessionActive));
        when(sessionCaisseRepository.save(any(SessionCaisse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SessionCaisseClotureDTORequest request = new SessionCaisseClotureDTORequest(new BigDecimal("250.00"));
        SessionCaisseDTOResponse response = caisseSalonService.cloturerSessionCaisse("salon-chic", "comptable@test.com", request);

        assertNotNull(response);
        assertEquals(StatutSessionCaisse.CLOTURE.name(), response.statut());
        assertEquals(new BigDecimal("250.00"), response.soldeFermeture());
    }

    @Test
    void obtenirSessionActive_fermee_lanceSessionCaisseFermeeException() {
        when(sessionCaisseRepository.findByAffectationSalonSlugAndStatut("salon-chic", StatutSessionCaisse.EN_COURS))
                .thenReturn(Optional.empty());

        assertThrows(SessionCaisseFermeeException.class, () ->
                caisseSalonService.obtenirSessionActive("salon-chic"));
    }
}

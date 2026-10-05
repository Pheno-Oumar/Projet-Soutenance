package com.kadi_aon.mon_salon.favori.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.coiffeur.repository.ProfilCoiffeurRepository;
import com.kadi_aon.mon_salon.favori.dto.FavoriCoiffeurDTOResponse;
import com.kadi_aon.mon_salon.favori.dto.FavoriSalonDTOResponse;
import com.kadi_aon.mon_salon.favori.entity.FavoriCoiffeur;
import com.kadi_aon.mon_salon.favori.entity.FavoriSalon;
import com.kadi_aon.mon_salon.favori.repository.FavoriCoiffeurRepository;
import com.kadi_aon.mon_salon.favori.repository.FavoriSalonRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;

@ExtendWith(MockitoExtension.class)
class FavoriSalonServiceTest {

    @Mock
    private FavoriSalonRepository favoriSalonRepository;
    @Mock
    private FavoriCoiffeurRepository favoriCoiffeurRepository;
    @Mock
    private AffectationSalonRepository affectationSalonRepository;
    @Mock
    private CompteRepository compteRepository;
    @Mock
    private ProfilCoiffeurRepository profilCoiffeurRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private FavoriSalonService favoriSalonService;

    private Salon salon;
    private Compte client;
    private Compte coiffeur;
    private AffectationSalon affectationClient;
    private AffectationSalon affectationCoiffeur;

    @BeforeEach
    void setUp() {
        salon = Salon.builder().id(1L).slug("salon-chic").nom("Salon Chic").build();
        client = Compte.builder().id(10L).nom("Dupont").prenom("Alice").email("alice@test.com").build();
        coiffeur = Compte.builder().id(30L).nom("Barber").prenom("Bob").email("bob@test.com").build();

        RoleSalon roleClient = RoleSalon.builder().id(1L).role(TypeRoleSalon.CLIENT).build();
        Set<RoleSalon> rolesClient = new HashSet<>();
        rolesClient.add(roleClient);

        affectationClient = AffectationSalon.builder()
                .id(100L)
                .salon(salon)
                .compte(client)
                .roles(rolesClient)
                .statut(true)
                .build();

        RoleSalon roleCoiffeur = RoleSalon.builder().id(2L).role(TypeRoleSalon.COIFFEUR).build();
        Set<RoleSalon> rolesCoiffeur = new HashSet<>();
        rolesCoiffeur.add(roleCoiffeur);

        affectationCoiffeur = AffectationSalon.builder()
                .id(200L)
                .salon(salon)
                .compte(coiffeur)
                .roles(rolesCoiffeur)
                .statut(true)
                .build();
    }

    @Test
    void ajouterSalonFavori_succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("alice@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationClient));
        when(favoriSalonRepository.findByClientIdAndSalonId(10L, 1L))
                .thenReturn(Optional.empty());

        when(favoriSalonRepository.save(any(FavoriSalon.class))).thenAnswer(invocation -> {
            FavoriSalon f = invocation.getArgument(0);
            f.setId(1L);
            return f;
        });

        FavoriSalonDTOResponse response = favoriSalonService.ajouterSalonFavori("salon-chic", "alice@test.com");

        assertNotNull(response);
        assertEquals("salon-chic", response.salonSlug());
        assertEquals("Salon Chic", response.salonNom());
    }

    @Test
    void retirerSalonFavori_succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("alice@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationClient));
        when(favoriSalonRepository.existsByClientIdAndSalonId(10L, 1L)).thenReturn(true);

        favoriSalonService.retirerSalonFavori("salon-chic", "alice@test.com");

        verify(favoriSalonRepository).deleteByClientIdAndSalonId(10L, 1L);
    }

    @Test
    void isSalonFavori_test() {
        when(favoriSalonRepository.findByClientEmailAndSalonSlug("alice@test.com", "salon-chic"))
                .thenReturn(Optional.of(FavoriSalon.builder().id(1L).salon(salon).client(client).dateAjout(LocalDateTime.now()).build()));

        assertTrue(favoriSalonService.isSalonFavori("salon-chic", "alice@test.com"));

        when(favoriSalonRepository.findByClientEmailAndSalonSlug("alice@test.com", "autre-salon"))
                .thenReturn(Optional.empty());

        assertFalse(favoriSalonService.isSalonFavori("autre-salon", "alice@test.com"));
    }

    @Test
    void ajouterCoiffeurFavori_succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("alice@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationClient));
        when(compteRepository.findById(30L)).thenReturn(Optional.of(coiffeur));

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("bob@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationCoiffeur));

        when(favoriCoiffeurRepository.findByClientIdAndCoiffeurIdAndSalonId(10L, 30L, 1L))
                .thenReturn(Optional.empty());

        when(favoriCoiffeurRepository.save(any(FavoriCoiffeur.class))).thenAnswer(invocation -> {
            FavoriCoiffeur f = invocation.getArgument(0);
            f.setId(101L);
            return f;
        });

        FavoriCoiffeurDTOResponse response = favoriSalonService.ajouterCoiffeurFavori("salon-chic", 30L, "alice@test.com");

        assertNotNull(response);
        assertEquals(30L, response.coiffeurId());
        assertEquals("Bob Barber", response.coiffeurNomComplet());
    }

    @Test
    void ajouterCoiffeurFavori_nonCoiffeur_lanceIllegalArgumentException() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("alice@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationClient));
        when(compteRepository.findById(30L)).thenReturn(Optional.of(coiffeur));

        // Affectation sans rôle coiffeur
        affectationCoiffeur.setRoles(Set.of());
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("bob@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationCoiffeur));

        assertThrows(IllegalArgumentException.class, () ->
                favoriSalonService.ajouterCoiffeurFavori("salon-chic", 30L, "alice@test.com"));
    }

    @Test
    void listerMesCoiffeursFavoris_succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("alice@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationClient));

        FavoriCoiffeur fc = FavoriCoiffeur.builder()
                .id(1L)
                .client(client)
                .coiffeur(coiffeur)
                .salon(salon)
                .dateAjout(LocalDateTime.now())
                .build();

        when(favoriCoiffeurRepository.findByClientEmailAndSalonSlugOrderByDateAjoutDesc("alice@test.com", "salon-chic"))
                .thenReturn(List.of(fc));

        List<FavoriCoiffeurDTOResponse> list = favoriSalonService.listerMesCoiffeursFavoris("salon-chic", "alice@test.com");

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Bob Barber", list.get(0).coiffeurNomComplet());
    }
}

package com.kadi_aon.mon_salon.realisation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.realisation.dto.CommentaireCreateDTORequest;
import com.kadi_aon.mon_salon.realisation.dto.CommentaireDTOResponse;
import com.kadi_aon.mon_salon.realisation.dto.KadysRealisationDTOResponse;
import com.kadi_aon.mon_salon.realisation.dto.LikeToggleDTOResponse;
import com.kadi_aon.mon_salon.realisation.entity.CommentaireRealisation;
import com.kadi_aon.mon_salon.realisation.entity.LikeRealisation;
import com.kadi_aon.mon_salon.realisation.entity.Realisation;
import com.kadi_aon.mon_salon.realisation.enums.StatutCommentaire;
import com.kadi_aon.mon_salon.realisation.repository.CommentaireRealisationRepository;
import com.kadi_aon.mon_salon.realisation.repository.LikeRealisationRepository;
import com.kadi_aon.mon_salon.realisation.repository.RealisationRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.RoleSalonRepository;

@ExtendWith(MockitoExtension.class)
class KadysInteractionServiceTest {

    @Mock
    private RealisationRepository realisationRepository;
    @Mock
    private LikeRealisationRepository likeRealisationRepository;
    @Mock
    private CommentaireRealisationRepository commentaireRealisationRepository;
    @Mock
    private AffectationSalonRepository affectationSalonRepository;
    @Mock
    private CompteRepository compteRepository;
    @Mock
    private RoleSalonRepository roleSalonRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private KadysInteractionService kadysInteractionService;

    private Salon salon;
    private Compte clientCompte;
    private Realisation realisation;
    private AffectationSalon affectationActive;

    @BeforeEach
    void setUp() {
        salon = Salon.builder()
                .id(1L)
                .nom("Salon Kady")
                .slug("salon-kady")
                .statut(true)
                .build();

        clientCompte = Compte.builder()
                .id(10L)
                .nom("Diallo")
                .prenom("Fatou")
                .email("fatou@client.com")
                .statut(true)
                .build();

        realisation = Realisation.builder()
                .id(100L)
                .titre("Tresses Braid tendance")
                .urlVideo("https://cloudinary.com/video.mp4")
                .salon(salon)
                .statutPublication(true)
                .totalLikes(5)
                .totalCommentaires(2)
                .totalVues(40L)
                .datePublication(LocalDateTime.now())
                .build();

        affectationActive = AffectationSalon.builder()
                .id(50L)
                .compte(clientCompte)
                .salon(salon)
                .statut(true)
                .dateDebut(LocalDate.now())
                .roles(new HashSet<>(Set.of(RoleSalon.builder().role(TypeRoleSalon.CLIENT).build())))
                .build();
    }

    // ==========================================
    // TESTS DU RATTACHEMENT & REGLE STRICTE STATUT FALSE
    // ==========================================

    @Test
    @DisplayName("Rattachement auto : crée une nouvelle affectation client si absente")
    void testValiderOuCreerAffectationClient_NouvelleAffectation() {
        when(compteRepository.findByEmail("fatou@client.com")).thenReturn(Optional.of(clientCompte));
        when(affectationSalonRepository.findByCompteAndSalon(clientCompte, salon)).thenReturn(Optional.empty());
        when(roleSalonRepository.findByRole(TypeRoleSalon.CLIENT))
                .thenReturn(Optional.of(RoleSalon.builder().role(TypeRoleSalon.CLIENT).build()));
        when(affectationSalonRepository.save(any(AffectationSalon.class))).thenReturn(affectationActive);

        AffectationSalon result = kadysInteractionService.validerOuCreerAffectationClient(salon, "fatou@client.com");

        assertNotNull(result);
        assertTrue(result.getStatut());
        verify(affectationSalonRepository).save(any(AffectationSalon.class));
        verify(auditLogService).logActionSalon(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Rattachement : retourne l'affectation existante si elle est active")
    void testValiderOuCreerAffectationClient_AffectationExistanteActive() {
        when(compteRepository.findByEmail("fatou@client.com")).thenReturn(Optional.of(clientCompte));
        when(affectationSalonRepository.findByCompteAndSalon(clientCompte, salon)).thenReturn(Optional.of(affectationActive));

        AffectationSalon result = kadysInteractionService.validerOuCreerAffectationClient(salon, "fatou@client.com");

        assertNotNull(result);
        assertEquals(50L, result.getId());
        verify(affectationSalonRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rattachement : INTERDICTION ABSOLUE de réactivation si statut = false -> exception avec message explicite")
    void testValiderOuCreerAffectationClient_AffectationDesactivee_DoitLancerException() {
        AffectationSalon affectationDesactivee = AffectationSalon.builder()
                .id(51L)
                .compte(clientCompte)
                .salon(salon)
                .statut(false)
                .build();

        when(compteRepository.findByEmail("fatou@client.com")).thenReturn(Optional.of(clientCompte));
        when(affectationSalonRepository.findByCompteAndSalon(clientCompte, salon)).thenReturn(Optional.of(affectationDesactivee));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                kadysInteractionService.validerOuCreerAffectationClient(salon, "fatou@client.com")
        );

        assertEquals("Votre compte client a été désactivé par ce salon. Pour réactiver votre accès, veuillez vous rendre directement au salon.", ex.getMessage());
        verify(affectationSalonRepository, never()).save(any());
    }

    // ==========================================
    // TESTS DU LIKE (TOGGLE)
    // ==========================================

    @Test
    @DisplayName("Toggle Like : nouveau like incrémente le compteur")
    void testToggleLike_NouveauLike() {
        when(realisationRepository.findByIdAndStatutPublicationTrue(100L)).thenReturn(Optional.of(realisation));
        when(compteRepository.findByEmail("fatou@client.com")).thenReturn(Optional.of(clientCompte));
        when(affectationSalonRepository.findByCompteAndSalon(clientCompte, salon)).thenReturn(Optional.of(affectationActive));
        when(likeRealisationRepository.findByRealisationIdAndAffectationSalonId(100L, 50L)).thenReturn(Optional.empty());

        LikeToggleDTOResponse response = kadysInteractionService.toggleLike(100L, "fatou@client.com");

        assertTrue(response.liked());
        assertEquals(6, response.totalLikes());
        verify(likeRealisationRepository).save(any(LikeRealisation.class));
        verify(realisationRepository).save(realisation);
    }

    @Test
    @DisplayName("Toggle Like : unlike décrémente le compteur")
    void testToggleLike_Unlike() {
        LikeRealisation existingLike = LikeRealisation.builder()
                .id(1L)
                .realisation(realisation)
                .affectationSalon(affectationActive)
                .build();

        when(realisationRepository.findByIdAndStatutPublicationTrue(100L)).thenReturn(Optional.of(realisation));
        when(compteRepository.findByEmail("fatou@client.com")).thenReturn(Optional.of(clientCompte));
        when(affectationSalonRepository.findByCompteAndSalon(clientCompte, salon)).thenReturn(Optional.of(affectationActive));
        when(likeRealisationRepository.findByRealisationIdAndAffectationSalonId(100L, 50L)).thenReturn(Optional.of(existingLike));

        LikeToggleDTOResponse response = kadysInteractionService.toggleLike(100L, "fatou@client.com");

        assertFalse(response.liked());
        assertEquals(4, response.totalLikes());
        verify(likeRealisationRepository).delete(existingLike);
        verify(realisationRepository).save(realisation);
    }

    // ==========================================
    // TESTS DES COMMENTAIRES
    // ==========================================

    @Test
    @DisplayName("Commentaire : ajouter commentaire incrémente le compteur")
    void testAjouterCommentaire() {
        CommentaireCreateDTORequest request = new CommentaireCreateDTORequest("Superbe coupe !");

        when(realisationRepository.findByIdAndStatutPublicationTrue(100L)).thenReturn(Optional.of(realisation));
        when(compteRepository.findByEmail("fatou@client.com")).thenReturn(Optional.of(clientCompte));
        when(affectationSalonRepository.findByCompteAndSalon(clientCompte, salon)).thenReturn(Optional.of(affectationActive));

        CommentaireRealisation savedComment = CommentaireRealisation.builder()
                .id(10L)
                .realisation(realisation)
                .affectationSalon(affectationActive)
                .contenu("Superbe coupe !")
                .statut(StatutCommentaire.ACTIF)
                .dateCreation(LocalDateTime.now())
                .build();

        when(commentaireRealisationRepository.save(any(CommentaireRealisation.class))).thenReturn(savedComment);

        CommentaireDTOResponse response = kadysInteractionService.ajouterCommentaire(100L, "fatou@client.com", request);

        assertNotNull(response);
        assertEquals("Superbe coupe !", response.contenu());
        assertEquals(3, realisation.getTotalCommentaires());
        verify(commentaireRealisationRepository).save(any(CommentaireRealisation.class));
        verify(realisationRepository).save(realisation);
    }

    @Test
    @DisplayName("Commentaire : supprimer son commentaire décrémente le compteur")
    void testSupprimerCommentaire() {
        CommentaireRealisation comment = CommentaireRealisation.builder()
                .id(10L)
                .realisation(realisation)
                .affectationSalon(affectationActive)
                .contenu("Mon avis")
                .build();

        when(commentaireRealisationRepository.findByIdAndCompteEmail(10L, "fatou@client.com"))
                .thenReturn(Optional.of(comment));

        kadysInteractionService.supprimerCommentaire(10L, "fatou@client.com");

        assertEquals(1, realisation.getTotalCommentaires());
        verify(commentaireRealisationRepository).delete(comment);
        verify(realisationRepository).save(realisation);
    }

    @Test
    @DisplayName("Visionnage : incrémente le nombre de vues")
    void testEnregistrerVue() {
        when(realisationRepository.findByIdAndStatutPublicationTrue(100L)).thenReturn(Optional.of(realisation));

        kadysInteractionService.enregistrerVue(100L);

        assertEquals(41L, realisation.getTotalVues());
        verify(realisationRepository).save(realisation);
    }

    @Test
    @DisplayName("Feed Kady's : renvoie la page des réalisations avec isLikedByCurrentUser")
    void testListerFeedKadys() {
        Page<Realisation> page = new PageImpl<>(List.of(realisation));
        when(realisationRepository.findByStatutPublicationTrueOrderByDatePublicationDesc(any()))
                .thenReturn(page);
        when(likeRealisationRepository.findRealisationIdsLikedByEmail("fatou@client.com"))
                .thenReturn(Set.of(100L));

        Page<KadysRealisationDTOResponse> result = kadysInteractionService.listerFeedKadys("fatou@client.com", PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).isLikedByCurrentUser());
    }
}

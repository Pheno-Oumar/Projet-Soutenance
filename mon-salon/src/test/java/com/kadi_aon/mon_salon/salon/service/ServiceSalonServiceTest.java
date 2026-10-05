package com.kadi_aon.mon_salon.salon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import static org.mockito.Mockito.mock;
import org.springframework.web.multipart.MultipartFile;

import com.kadi_aon.mon_salon.common.service.CloudinaryService;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.salon.dto.ServiceSalonCreateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.ServiceSalonDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.ServiceSalonUpdateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.VarianteCreateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.VariantePrixUpdateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.VarianteServiceDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.VarianteUpdateDTORequest;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.entity.ServiceSalon;
import com.kadi_aon.mon_salon.salon.entity.VarianteService;
import com.kadi_aon.mon_salon.salon.mapper.ServiceSalonDTOResponseMapper;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;
import com.kadi_aon.mon_salon.salon.repository.ServiceSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.VarianteServiceRepository;

@ExtendWith(MockitoExtension.class)
class ServiceSalonServiceTest {

    @Mock
    private SalonRepository salonRepository;
    @Mock
    private ServiceSalonRepository serviceSalonRepository;
    @Mock
    private VarianteServiceRepository varianteServiceRepository;
    @Mock
    private AffectationSalonRepository affectationSalonRepository;
    @Mock
    private ServiceSalonDTOResponseMapper serviceSalonMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private ServiceSalonService serviceSalonService;

    private Salon salon;
    private AffectationSalon affectationProprio;
    private AffectationSalon affectationManager;

    @BeforeEach
    void setUp() {
        salon = Salon.builder()
                .id(1L)
                .nom("Salon Prestige")
                .slug("salon-prestige")
                .statut(true)
                .build();

        affectationProprio = AffectationSalon.builder()
                .id(10L)
                .salon(salon)
                .statut(true)
                .build();

        affectationManager = AffectationSalon.builder()
                .id(20L)
                .salon(salon)
                .statut(true)
                .build();
    }

    @Test
    void testCreerServiceSucces() {
        ServiceSalonCreateDTORequest request = new ServiceSalonCreateDTORequest(
                "Coupe Homme",
                "Coupe classique et moderne",
                List.of(
                        new VarianteCreateDTORequest("Classique", 20, new BigDecimal("15.00")),
                        new VarianteCreateDTORequest("Avec Barbe", 35, new BigDecimal("25.00"))
                )
        );

        when(salonRepository.findBySlug("salon-prestige")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-prestige"))
                .thenReturn(Optional.of(affectationProprio));
        when(serviceSalonRepository.existsBySalonIdAndNomIgnoreCase(1L, "Coupe Homme")).thenReturn(false);
        when(serviceSalonRepository.save(any(ServiceSalon.class))).thenAnswer(i -> {
            ServiceSalon s = i.getArgument(0);
            s.setId(100L);
            return s;
        });
        when(serviceSalonMapper.apply(any(ServiceSalon.class))).thenAnswer(i -> {
            ServiceSalon s = i.getArgument(0);
            return new ServiceSalonDTOResponse(s.getId(), s.getNom(), s.getDescription(), s.getStatut(), List.of(), null);
        });

        ServiceSalonDTOResponse response = serviceSalonService.creerService(
                "salon-prestige", request, "proprio@test.com", "PROPRIETAIRE");

        assertNotNull(response);
        assertEquals("Coupe Homme", response.nom());
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.CREATION), eq("ServiceSalon"), eq("100"), any(), any(), eq(affectationProprio), eq("PROPRIETAIRE"));
    }

    @Test
    void testCreerServiceSansVariantesSucces() {
        ServiceSalonCreateDTORequest request = new ServiceSalonCreateDTORequest(
                "Soin Visage",
                "Soin relaxant",
                List.of()
        );

        when(salonRepository.findBySlug("salon-prestige")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-prestige"))
                .thenReturn(Optional.of(affectationProprio));
        when(serviceSalonRepository.existsBySalonIdAndNomIgnoreCase(1L, "Soin Visage")).thenReturn(false);
        when(serviceSalonRepository.save(any(ServiceSalon.class))).thenAnswer(i -> {
            ServiceSalon s = i.getArgument(0);
            s.setId(105L);
            return s;
        });
        when(serviceSalonMapper.apply(any(ServiceSalon.class))).thenAnswer(i -> {
            ServiceSalon s = i.getArgument(0);
            return new ServiceSalonDTOResponse(s.getId(), s.getNom(), s.getDescription(), s.getStatut(), List.of(), null);
        });

        ServiceSalonDTOResponse response = serviceSalonService.creerService(
                "salon-prestige", request, "proprio@test.com", "PROPRIETAIRE");

        assertNotNull(response);
        assertEquals("Soin Visage", response.nom());
        verify(serviceSalonRepository).save(any(ServiceSalon.class));
    }

    @Test
    void testCreerServiceRejetVariantesDoublonNom() {
        ServiceSalonCreateDTORequest request = new ServiceSalonCreateDTORequest(
                "Coloration",
                "Coloration complète",
                List.of(
                        new VarianteCreateDTORequest("Standard", 45, new BigDecimal("30.00")),
                        new VarianteCreateDTORequest("standard", 60, new BigDecimal("40.00"))
                )
        );

        when(salonRepository.findBySlug("salon-prestige")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-prestige"))
                .thenReturn(Optional.of(affectationProprio));
        when(serviceSalonRepository.existsBySalonIdAndNomIgnoreCase(1L, "Coloration")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> serviceSalonService.creerService("salon-prestige", request, "proprio@test.com", "PROPRIETAIRE"));

        assertTrue(ex.getMessage().contains("même nom"));
    }

    @Test
    void testCreerServiceRejetNomExistantDansSalon() {
        ServiceSalonCreateDTORequest request = new ServiceSalonCreateDTORequest(
                "Shampoing",
                null,
                List.of(new VarianteCreateDTORequest("Simple", 10, new BigDecimal("5.00")))
        );

        when(salonRepository.findBySlug("salon-prestige")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-prestige"))
                .thenReturn(Optional.of(affectationProprio));
        when(serviceSalonRepository.existsBySalonIdAndNomIgnoreCase(1L, "Shampoing")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> serviceSalonService.creerService("salon-prestige", request, "proprio@test.com", "PROPRIETAIRE"));

        assertTrue(ex.getMessage().contains("existe déjà dans ce salon"));
    }

    @Test
    void testModifierServiceSucces() {
        ServiceSalon existingService = ServiceSalon.builder()
                .id(100L)
                .salon(salon)
                .nom("Coupe")
                .description("Ancienne description")
                .statut(true)
                .build();

        ServiceSalonUpdateDTORequest request = new ServiceSalonUpdateDTORequest("Coupe Dégradé", "Nouvelle description");

        when(salonRepository.findBySlug("salon-prestige")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-prestige"))
                .thenReturn(Optional.of(affectationProprio));
        when(serviceSalonRepository.findByIdAndSalonSlug(100L, "salon-prestige")).thenReturn(Optional.of(existingService));
        when(serviceSalonRepository.existsBySalonIdAndNomIgnoreCase(1L, "Coupe Dégradé")).thenReturn(false);
        when(serviceSalonRepository.save(any(ServiceSalon.class))).thenAnswer(i -> i.getArgument(0));
        when(serviceSalonMapper.apply(any(ServiceSalon.class))).thenAnswer(i -> {
            ServiceSalon s = i.getArgument(0);
            return new ServiceSalonDTOResponse(s.getId(), s.getNom(), s.getDescription(), s.getStatut(), List.of(), null);
        });

        ServiceSalonDTOResponse response = serviceSalonService.modifierService(
                "salon-prestige", 100L, request, "proprio@test.com", "PROPRIETAIRE");

        assertNotNull(response);
        assertEquals("Coupe Dégradé", response.nom());
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.MODIFICATION), eq("ServiceSalon"), eq("100"), any(), any(), eq(affectationProprio), eq("PROPRIETAIRE"));
    }

    @Test
    void testAjouterVarianteSuccesParManager() {
        ServiceSalon service = ServiceSalon.builder()
                .id(100L)
                .salon(salon)
                .nom("Coiffure")
                .statut(true)
                .variantes(new ArrayList<>())
                .build();

        VarianteCreateDTORequest request = new VarianteCreateDTORequest("Chignon", 40, new BigDecimal("35.00"));

        when(salonRepository.findBySlug("salon-prestige")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("manager@test.com", "salon-prestige"))
                .thenReturn(Optional.of(affectationManager));
        when(serviceSalonRepository.findByIdAndSalonSlug(100L, "salon-prestige")).thenReturn(Optional.of(service));
        when(varianteServiceRepository.existsByServiceSalonIdAndNomIgnoreCase(100L, "Chignon")).thenReturn(false);
        when(varianteServiceRepository.save(any(VarianteService.class))).thenAnswer(i -> {
            VarianteService v = i.getArgument(0);
            v.setId(200L);
            return v;
        });
        when(serviceSalonMapper.mapVariante(any(VarianteService.class))).thenAnswer(i -> {
            VarianteService v = i.getArgument(0);
            return new VarianteServiceDTOResponse(v.getId(), v.getNom(), v.getDureeMinutes(), v.getPrix(), v.getStatut());
        });

        VarianteServiceDTOResponse response = serviceSalonService.ajouterVariante(
                "salon-prestige", 100L, request, "manager@test.com", "MANAGER");

        assertNotNull(response);
        assertEquals("Chignon", response.nom());
        assertEquals(new BigDecimal("35.00"), response.prix());
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.CREATION), eq("VarianteService"), eq("200"), any(), any(), eq(affectationManager), eq("MANAGER"));
    }

    @Test
    void testModifierPrixVarianteSucces() {
        ServiceSalon service = ServiceSalon.builder().id(100L).salon(salon).build();
        VarianteService variante = VarianteService.builder()
                .id(200L)
                .serviceSalon(service)
                .nom("Classique")
                .prix(new BigDecimal("15.00"))
                .dureeMinutes(20)
                .statut(true)
                .build();

        VariantePrixUpdateDTORequest request = new VariantePrixUpdateDTORequest(new BigDecimal("18.50"));

        when(salonRepository.findBySlug("salon-prestige")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("manager@test.com", "salon-prestige"))
                .thenReturn(Optional.of(affectationManager));
        when(serviceSalonRepository.findByIdAndSalonSlug(100L, "salon-prestige")).thenReturn(Optional.of(service));
        when(varianteServiceRepository.findByIdAndServiceSalonId(200L, 100L)).thenReturn(Optional.of(variante));
        when(varianteServiceRepository.save(any(VarianteService.class))).thenAnswer(i -> i.getArgument(0));
        when(serviceSalonMapper.mapVariante(any(VarianteService.class))).thenAnswer(i -> {
            VarianteService v = i.getArgument(0);
            return new VarianteServiceDTOResponse(v.getId(), v.getNom(), v.getDureeMinutes(), v.getPrix(), v.getStatut());
        });

        VarianteServiceDTOResponse response = serviceSalonService.modifierPrixVariante(
                "salon-prestige", 100L, 200L, request, "manager@test.com", "MANAGER");

        assertNotNull(response);
        assertEquals(new BigDecimal("18.50"), response.prix());
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.MODIFICATION), eq("VarianteService"), eq("200"),
                eq("prix: 15.00"), eq("prix: 18.50"), eq(affectationManager), eq("MANAGER"));
    }

    @Test
    void testBasculerStatutService() {
        ServiceSalon service = ServiceSalon.builder()
                .id(100L)
                .salon(salon)
                .nom("Service")
                .statut(true)
                .build();

        when(salonRepository.findBySlug("salon-prestige")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-prestige"))
                .thenReturn(Optional.of(affectationProprio));
        when(serviceSalonRepository.findByIdAndSalonSlug(100L, "salon-prestige")).thenReturn(Optional.of(service));
        when(serviceSalonRepository.save(any(ServiceSalon.class))).thenAnswer(i -> i.getArgument(0));
        when(serviceSalonMapper.apply(any(ServiceSalon.class))).thenAnswer(i -> {
            ServiceSalon s = i.getArgument(0);
            return new ServiceSalonDTOResponse(s.getId(), s.getNom(), s.getDescription(), s.getStatut(), List.of(), null);
        });

        ServiceSalonDTOResponse response = serviceSalonService.basculerStatutService(
                "salon-prestige", 100L, "proprio@test.com", "PROPRIETAIRE");

        assertNotNull(response);
        assertFalse(response.statut());
        verify(auditLogService).logActionSalon(eq(TypeActionAudit.MODIFICATION), eq("ServiceSalon"), eq("100"),
                eq("statut: true"), eq("statut: false"), eq(affectationProprio), eq("PROPRIETAIRE"));
    }

    @Test
    void testCreerServiceAvecImage_succes() throws IOException {
        MultipartFile imageFile = mock(MultipartFile.class);
        when(imageFile.isEmpty()).thenReturn(false);
        String uploadedUrl = "https://res.cloudinary.com/test/image/upload/v1/services/img123.webp";
        when(cloudinaryService.uploadImageService(imageFile, "salon-prestige")).thenReturn(uploadedUrl);

        ServiceSalonCreateDTORequest request = new ServiceSalonCreateDTORequest(
                "Tresse Africaine", "Tresses et vanilles", List.of()
        );

        when(salonRepository.findBySlug("salon-prestige")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-prestige"))
                .thenReturn(Optional.of(affectationProprio));
        when(serviceSalonRepository.existsBySalonIdAndNomIgnoreCase(1L, "Tresse Africaine")).thenReturn(false);
        when(serviceSalonRepository.save(any(ServiceSalon.class))).thenAnswer(i -> {
            ServiceSalon s = i.getArgument(0);
            s.setId(101L);
            return s;
        });
        when(serviceSalonMapper.apply(any(ServiceSalon.class))).thenAnswer(i -> {
            ServiceSalon s = i.getArgument(0);
            return new ServiceSalonDTOResponse(s.getId(), s.getNom(), s.getDescription(), s.getImageUrl(), s.getStatut(), List.of(), null);
        });

        ServiceSalonDTOResponse response = serviceSalonService.creerService(
                "salon-prestige", request, imageFile, "proprio@test.com", "PROPRIETAIRE");

        assertNotNull(response);
        assertEquals("Tresse Africaine", response.nom());
        assertEquals(uploadedUrl, response.imageUrl());
        verify(cloudinaryService).uploadImageService(imageFile, "salon-prestige");
    }

    @Test
    void testModifierServiceAvecNouvelleImage_supprimeAncienneSurCloudinary() throws IOException {
        String ancienneUrl = "https://res.cloudinary.com/test/image/upload/v1/services/ancienne.webp";
        String nouvelleUrl = "https://res.cloudinary.com/test/image/upload/v1/services/nouvelle.webp";

        ServiceSalon existingService = ServiceSalon.builder()
                .id(100L)
                .salon(salon)
                .nom("Coupe")
                .imageUrl(ancienneUrl)
                .statut(true)
                .build();

        MultipartFile newImageFile = mock(MultipartFile.class);
        when(newImageFile.isEmpty()).thenReturn(false);
        when(cloudinaryService.uploadImageService(newImageFile, "salon-prestige")).thenReturn(nouvelleUrl);

        ServiceSalonUpdateDTORequest request = new ServiceSalonUpdateDTORequest("Coupe VIP", "Description VIP");

        when(salonRepository.findBySlug("salon-prestige")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-prestige"))
                .thenReturn(Optional.of(affectationProprio));
        when(serviceSalonRepository.findByIdAndSalonSlug(100L, "salon-prestige")).thenReturn(Optional.of(existingService));
        when(serviceSalonRepository.existsBySalonIdAndNomIgnoreCase(1L, "Coupe VIP")).thenReturn(false);
        when(serviceSalonRepository.save(any(ServiceSalon.class))).thenAnswer(i -> i.getArgument(0));
        when(serviceSalonMapper.apply(any(ServiceSalon.class))).thenAnswer(i -> {
            ServiceSalon s = i.getArgument(0);
            return new ServiceSalonDTOResponse(s.getId(), s.getNom(), s.getDescription(), s.getImageUrl(), s.getStatut(), List.of(), null);
        });

        ServiceSalonDTOResponse response = serviceSalonService.modifierService(
                "salon-prestige", 100L, request, newImageFile, "proprio@test.com", "PROPRIETAIRE");

        assertNotNull(response);
        assertEquals(nouvelleUrl, response.imageUrl());
        verify(cloudinaryService).deleteMediaByUrl(ancienneUrl, "image");
        verify(cloudinaryService).uploadImageService(newImageFile, "salon-prestige");
    }

    @Test
    void testUploadImageService_supprimeAncienneEtUploadeNouvelle() throws IOException {
        String ancienneUrl = "https://res.cloudinary.com/test/image/upload/v1/services/ancienne.webp";
        String nouvelleUrl = "https://res.cloudinary.com/test/image/upload/v1/services/remplacement.webp";

        ServiceSalon existingService = ServiceSalon.builder()
                .id(100L)
                .salon(salon)
                .nom("Coupe")
                .imageUrl(ancienneUrl)
                .statut(true)
                .build();

        MultipartFile file = mock(MultipartFile.class);
        when(cloudinaryService.uploadImageService(file, "salon-prestige")).thenReturn(nouvelleUrl);

        when(salonRepository.findBySlug("salon-prestige")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("proprio@test.com", "salon-prestige"))
                .thenReturn(Optional.of(affectationProprio));
        when(serviceSalonRepository.findByIdAndSalonSlug(100L, "salon-prestige")).thenReturn(Optional.of(existingService));
        when(serviceSalonRepository.save(any(ServiceSalon.class))).thenAnswer(i -> i.getArgument(0));
        when(serviceSalonMapper.apply(any(ServiceSalon.class))).thenAnswer(i -> {
            ServiceSalon s = i.getArgument(0);
            return new ServiceSalonDTOResponse(s.getId(), s.getNom(), s.getDescription(), s.getImageUrl(), s.getStatut(), List.of(), null);
        });

        ServiceSalonDTOResponse response = serviceSalonService.uploadImageService(
                "salon-prestige", 100L, file, "proprio@test.com", "PROPRIETAIRE");

        assertNotNull(response);
        assertEquals(nouvelleUrl, response.imageUrl());
        verify(cloudinaryService).deleteMediaByUrl(ancienneUrl, "image");
        verify(cloudinaryService).uploadImageService(file, "salon-prestige");
    }

    @Test
    void testAjouterVarianteAvecImage_succes() throws IOException {
        MultipartFile imageFile = mock(MultipartFile.class);
        when(imageFile.isEmpty()).thenReturn(false);
        String uploadedUrl = "https://res.cloudinary.com/test/image/upload/v1/variantes/var123.webp";
        when(cloudinaryService.uploadImageVariante(imageFile, "salon-prestige")).thenReturn(uploadedUrl);

        ServiceSalon service = ServiceSalon.builder()
                .id(100L)
                .salon(salon)
                .nom("Coiffure")
                .statut(true)
                .variantes(new ArrayList<>())
                .build();

        VarianteCreateDTORequest request = new VarianteCreateDTORequest("Chignon Mariage", 60, new BigDecimal("50.00"));

        when(salonRepository.findBySlug("salon-prestige")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("manager@test.com", "salon-prestige"))
                .thenReturn(Optional.of(affectationManager));
        when(serviceSalonRepository.findByIdAndSalonSlug(100L, "salon-prestige")).thenReturn(Optional.of(service));
        when(varianteServiceRepository.existsByServiceSalonIdAndNomIgnoreCase(100L, "Chignon Mariage")).thenReturn(false);
        when(varianteServiceRepository.save(any(VarianteService.class))).thenAnswer(i -> {
            VarianteService v = i.getArgument(0);
            v.setId(205L);
            return v;
        });
        when(serviceSalonMapper.mapVariante(any(VarianteService.class))).thenAnswer(i -> {
            VarianteService v = i.getArgument(0);
            return new VarianteServiceDTOResponse(v.getId(), v.getNom(), v.getDureeMinutes(), v.getPrix(), v.getImageUrl(), v.getStatut());
        });

        VarianteServiceDTOResponse response = serviceSalonService.ajouterVariante(
                "salon-prestige", 100L, request, imageFile, "manager@test.com", "MANAGER");

        assertNotNull(response);
        assertEquals("Chignon Mariage", response.nom());
        assertEquals(uploadedUrl, response.imageUrl());
        verify(cloudinaryService).uploadImageVariante(imageFile, "salon-prestige");
    }

    @Test
    void testModifierVarianteAvecNouvelleImage_supprimeAncienneSurCloudinary() throws IOException {
        String ancienneUrl = "https://res.cloudinary.com/test/image/upload/v1/variantes/ancienne.webp";
        String nouvelleUrl = "https://res.cloudinary.com/test/image/upload/v1/variantes/nouvelle.webp";

        ServiceSalon service = ServiceSalon.builder().id(100L).salon(salon).build();
        VarianteService variante = VarianteService.builder()
                .id(200L)
                .serviceSalon(service)
                .nom("Classique")
                .prix(new BigDecimal("15.00"))
                .dureeMinutes(20)
                .imageUrl(ancienneUrl)
                .statut(true)
                .build();

        MultipartFile newImageFile = mock(MultipartFile.class);
        when(newImageFile.isEmpty()).thenReturn(false);
        when(cloudinaryService.uploadImageVariante(newImageFile, "salon-prestige")).thenReturn(nouvelleUrl);

        VarianteUpdateDTORequest request = new VarianteUpdateDTORequest("Classique Plus", 25, new BigDecimal("17.00"));

        when(salonRepository.findBySlug("salon-prestige")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("manager@test.com", "salon-prestige"))
                .thenReturn(Optional.of(affectationManager));
        when(serviceSalonRepository.findByIdAndSalonSlug(100L, "salon-prestige")).thenReturn(Optional.of(service));
        when(varianteServiceRepository.findByIdAndServiceSalonId(200L, 100L)).thenReturn(Optional.of(variante));
        when(varianteServiceRepository.existsByServiceSalonIdAndNomIgnoreCase(100L, "Classique Plus")).thenReturn(false);
        when(varianteServiceRepository.save(any(VarianteService.class))).thenAnswer(i -> i.getArgument(0));
        when(serviceSalonMapper.mapVariante(any(VarianteService.class))).thenAnswer(i -> {
            VarianteService v = i.getArgument(0);
            return new VarianteServiceDTOResponse(v.getId(), v.getNom(), v.getDureeMinutes(), v.getPrix(), v.getImageUrl(), v.getStatut());
        });

        VarianteServiceDTOResponse response = serviceSalonService.modifierVariante(
                "salon-prestige", 100L, 200L, request, newImageFile, "manager@test.com", "MANAGER");

        assertNotNull(response);
        assertEquals(nouvelleUrl, response.imageUrl());
        verify(cloudinaryService).deleteMediaByUrl(ancienneUrl, "image");
        verify(cloudinaryService).uploadImageVariante(newImageFile, "salon-prestige");
    }

    @Test
    void testUploadImageVariante_supprimeAncienneEtUploadeNouvelle() throws IOException {
        String ancienneUrl = "https://res.cloudinary.com/test/image/upload/v1/variantes/ancienne.webp";
        String nouvelleUrl = "https://res.cloudinary.com/test/image/upload/v1/variantes/upload.webp";

        ServiceSalon service = ServiceSalon.builder().id(100L).salon(salon).build();
        VarianteService variante = VarianteService.builder()
                .id(200L)
                .serviceSalon(service)
                .nom("Classique")
                .prix(new BigDecimal("15.00"))
                .dureeMinutes(20)
                .imageUrl(ancienneUrl)
                .statut(true)
                .build();

        MultipartFile file = mock(MultipartFile.class);
        when(cloudinaryService.uploadImageVariante(file, "salon-prestige")).thenReturn(nouvelleUrl);

        when(salonRepository.findBySlug("salon-prestige")).thenReturn(Optional.of(salon));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("manager@test.com", "salon-prestige"))
                .thenReturn(Optional.of(affectationManager));
        when(serviceSalonRepository.findByIdAndSalonSlug(100L, "salon-prestige")).thenReturn(Optional.of(service));
        when(varianteServiceRepository.findByIdAndServiceSalonId(200L, 100L)).thenReturn(Optional.of(variante));
        when(varianteServiceRepository.save(any(VarianteService.class))).thenAnswer(i -> i.getArgument(0));
        when(serviceSalonMapper.mapVariante(any(VarianteService.class))).thenAnswer(i -> {
            VarianteService v = i.getArgument(0);
            return new VarianteServiceDTOResponse(v.getId(), v.getNom(), v.getDureeMinutes(), v.getPrix(), v.getImageUrl(), v.getStatut());
        });

        VarianteServiceDTOResponse response = serviceSalonService.uploadImageVariante(
                "salon-prestige", 100L, 200L, file, "manager@test.com", "MANAGER");

        assertNotNull(response);
        assertEquals(nouvelleUrl, response.imageUrl());
        verify(cloudinaryService).deleteMediaByUrl(ancienneUrl, "image");
        verify(cloudinaryService).uploadImageVariante(file, "salon-prestige");
    }
}

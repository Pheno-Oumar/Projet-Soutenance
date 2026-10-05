package com.kadi_aon.mon_salon.salon.service;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.common.service.CloudinaryService;
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

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceSalonService {

    private final SalonRepository salonRepository;
    private final ServiceSalonRepository serviceSalonRepository;
    private final VarianteServiceRepository varianteServiceRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final ServiceSalonDTOResponseMapper serviceSalonMapper;
    private final AuditLogService auditLogService;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public ServiceSalonDTOResponse creerService(
            String slugSalon,
            ServiceSalonCreateDTORequest request,
            String userEmail,
            String roleUtilise) {
        try {
            return creerService(slugSalon, request, null, userEmail, roleUtilise);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'upload de l'image du service : " + e.getMessage(), e);
        }
    }

    @Transactional
    public ServiceSalonDTOResponse creerService(
            String slugSalon,
            ServiceSalonCreateDTORequest request,
            MultipartFile imageFile,
            String userEmail,
            String roleUtilise) throws IOException {

        Salon salon = getSalonBySlug(slugSalon);
        AffectationSalon affectation = getAffectationActive(userEmail, slugSalon);

        String nom = request.nom().trim();
        if (serviceSalonRepository.existsBySalonIdAndNomIgnoreCase(salon.getId(), nom)) {
            throw new IllegalArgumentException("Un service avec le nom '" + nom + "' existe déjà dans ce salon.");
        }

        // Vérifier l'unicité des noms de variantes fournies (si présentes)
        if (request.variantes() != null && !request.variantes().isEmpty()) {
            Set<String> varianteNoms = new HashSet<>();
            for (VarianteCreateDTORequest v : request.variantes()) {
                String vNom = v.nom().trim().toLowerCase();
                if (!varianteNoms.add(vNom)) {
                    throw new IllegalArgumentException("Deux variantes ne peuvent pas porter le même nom : " + v.nom());
                }
            }
        }

        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = cloudinaryService.uploadImageService(imageFile, slugSalon);
        }

        ServiceSalon service = ServiceSalon.builder()
                .salon(salon)
                .nom(nom)
                .description(request.description())
                .imageUrl(imageUrl)
                .statut(true)
                .build();

        if (request.variantes() != null) {
            for (VarianteCreateDTORequest v : request.variantes()) {
                VarianteService variante = VarianteService.builder()
                        .nom(v.nom().trim())
                        .dureeMinutes(v.dureeMinutes())
                        .prix(v.prix())
                        .statut(true)
                        .build();
                service.addVariante(variante);
            }
        }

        ServiceSalon saved = serviceSalonRepository.save(service);
        int nbVariantes = (saved.getVariantes() != null) ? saved.getVariantes().size() : 0;

        auditLogService.logActionSalon(
                TypeActionAudit.CREATION,
                "ServiceSalon",
                String.valueOf(saved.getId()),
                null,
                String.format("Création du service '%s' avec %d variante(s)", saved.getNom(), nbVariantes),
                affectation,
                roleUtilise
        );

        return serviceSalonMapper.apply(saved);
    }

    @Transactional
    public ServiceSalonDTOResponse modifierService(
            String slugSalon,
            Long serviceId,
            ServiceSalonUpdateDTORequest request,
            String userEmail,
            String roleUtilise) {
        try {
            return modifierService(slugSalon, serviceId, request, null, userEmail, roleUtilise);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la mise à jour de l'image du service : " + e.getMessage(), e);
        }
    }

    @Transactional
    public ServiceSalonDTOResponse modifierService(
            String slugSalon,
            Long serviceId,
            ServiceSalonUpdateDTORequest request,
            MultipartFile imageFile,
            String userEmail,
            String roleUtilise) throws IOException {

        getSalonBySlug(slugSalon);
        AffectationSalon affectation = getAffectationActive(userEmail, slugSalon);

        ServiceSalon service = serviceSalonRepository.findByIdAndSalonSlug(serviceId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Service introuvable avec l'identifiant : " + serviceId));

        String nouveauNom = request.nom().trim();
        if (!service.getNom().equalsIgnoreCase(nouveauNom)
                && serviceSalonRepository.existsBySalonIdAndNomIgnoreCase(service.getSalon().getId(), nouveauNom)) {
            throw new IllegalArgumentException("Un service avec le nom '" + nouveauNom + "' existe déjà dans ce salon.");
        }

        String ancienneValeur = String.format("nom: '%s', description: '%s', image: '%s'",
                service.getNom(), service.getDescription(), service.getImageUrl());
        service.setNom(nouveauNom);
        service.setDescription(request.description());

        // Si une nouvelle photo est fournie, supprimer l'ancienne de Cloudinary puis uploader la nouvelle
        if (imageFile != null && !imageFile.isEmpty()) {
            String ancienneImage = service.getImageUrl();
            String nouvelleUrl = cloudinaryService.uploadImageService(imageFile, slugSalon);
            service.setImageUrl(nouvelleUrl);

            if (ancienneImage != null && !ancienneImage.isBlank()) {
                cloudinaryService.deleteMediaByUrl(ancienneImage, "image");
            }
        }

        ServiceSalon saved = serviceSalonRepository.save(service);
        String nouvelleValeur = String.format("nom: '%s', description: '%s', image: '%s'",
                saved.getNom(), saved.getDescription(), saved.getImageUrl());

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "ServiceSalon",
                String.valueOf(saved.getId()),
                ancienneValeur,
                nouvelleValeur,
                affectation,
                roleUtilise
        );

        return serviceSalonMapper.apply(saved);
    }

    @Transactional
    public ServiceSalonDTOResponse uploadImageService(
            String slugSalon,
            Long serviceId,
            MultipartFile file,
            String userEmail,
            String roleUtilise) throws IOException {

        getSalonBySlug(slugSalon);
        AffectationSalon affectation = getAffectationActive(userEmail, slugSalon);

        ServiceSalon service = serviceSalonRepository.findByIdAndSalonSlug(serviceId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Service introuvable avec l'identifiant : " + serviceId));

        String ancienneImage = service.getImageUrl();
        String nouvelleUrl = cloudinaryService.uploadImageService(file, slugSalon);
        service.setImageUrl(nouvelleUrl);

        if (ancienneImage != null && !ancienneImage.isBlank()) {
            cloudinaryService.deleteMediaByUrl(ancienneImage, "image");
        }

        ServiceSalon saved = serviceSalonRepository.save(service);

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "ServiceSalon (Image)",
                String.valueOf(saved.getId()),
                ancienneImage,
                nouvelleUrl,
                affectation,
                roleUtilise
        );

        return serviceSalonMapper.apply(saved);
    }

    @Transactional
    public ServiceSalonDTOResponse basculerStatutService(
            String slugSalon,
            Long serviceId,
            String userEmail,
            String roleUtilise) {

        getSalonBySlug(slugSalon);
        AffectationSalon affectation = getAffectationActive(userEmail, slugSalon);

        ServiceSalon service = serviceSalonRepository.findByIdAndSalonSlug(serviceId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Service introuvable avec l'identifiant : " + serviceId));

        boolean ancienStatut = service.getStatut();
        service.setStatut(!ancienStatut);
        ServiceSalon saved = serviceSalonRepository.save(service);

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "ServiceSalon",
                String.valueOf(saved.getId()),
                "statut: " + ancienStatut,
                "statut: " + saved.getStatut(),
                affectation,
                roleUtilise
        );

        return serviceSalonMapper.apply(saved);
    }

    @Transactional
    public VarianteServiceDTOResponse ajouterVariante(
            String slugSalon,
            Long serviceId,
            VarianteCreateDTORequest request,
            String userEmail,
            String roleUtilise) {
        try {
            return ajouterVariante(slugSalon, serviceId, request, null, userEmail, roleUtilise);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'upload de l'image de la variante : " + e.getMessage(), e);
        }
    }

    @Transactional
    public VarianteServiceDTOResponse ajouterVariante(
            String slugSalon,
            Long serviceId,
            VarianteCreateDTORequest request,
            MultipartFile imageFile,
            String userEmail,
            String roleUtilise) throws IOException {

        getSalonBySlug(slugSalon);
        AffectationSalon affectation = getAffectationActive(userEmail, slugSalon);

        ServiceSalon service = serviceSalonRepository.findByIdAndSalonSlug(serviceId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Service introuvable avec l'identifiant : " + serviceId));

        String nom = request.nom().trim();
        if (varianteServiceRepository.existsByServiceSalonIdAndNomIgnoreCase(service.getId(), nom)) {
            throw new IllegalArgumentException("Une variante avec le nom '" + nom + "' existe déjà pour ce service.");
        }

        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = cloudinaryService.uploadImageVariante(imageFile, slugSalon);
        }

        VarianteService variante = VarianteService.builder()
                .serviceSalon(service)
                .nom(nom)
                .dureeMinutes(request.dureeMinutes())
                .prix(request.prix())
                .imageUrl(imageUrl)
                .statut(true)
                .build();

        VarianteService saved = varianteServiceRepository.save(variante);

        auditLogService.logActionSalon(
                TypeActionAudit.CREATION,
                "VarianteService",
                String.valueOf(saved.getId()),
                null,
                String.format("Ajout variante '%s' (durée: %d min, prix: %s)", saved.getNom(), saved.getDureeMinutes(), saved.getPrix()),
                affectation,
                roleUtilise
        );

        return serviceSalonMapper.mapVariante(saved);
    }

    @Transactional
    public VarianteServiceDTOResponse modifierVariante(
            String slugSalon,
            Long serviceId,
            Long varianteId,
            VarianteUpdateDTORequest request,
            String userEmail,
            String roleUtilise) {
        try {
            return modifierVariante(slugSalon, serviceId, varianteId, request, null, userEmail, roleUtilise);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la modification de l'image de la variante : " + e.getMessage(), e);
        }
    }

    @Transactional
    public VarianteServiceDTOResponse modifierVariante(
            String slugSalon,
            Long serviceId,
            Long varianteId,
            VarianteUpdateDTORequest request,
            MultipartFile imageFile,
            String userEmail,
            String roleUtilise) throws IOException {

        getSalonBySlug(slugSalon);
        AffectationSalon affectation = getAffectationActive(userEmail, slugSalon);

        ServiceSalon service = serviceSalonRepository.findByIdAndSalonSlug(serviceId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Service introuvable avec l'identifiant : " + serviceId));

        VarianteService variante = varianteServiceRepository.findByIdAndServiceSalonId(varianteId, service.getId())
                .orElseThrow(() -> new EntityNotFoundException("Variante introuvable avec l'identifiant : " + varianteId));

        String nouveauNom = request.nom().trim();
        if (!variante.getNom().equalsIgnoreCase(nouveauNom)
                && varianteServiceRepository.existsByServiceSalonIdAndNomIgnoreCase(service.getId(), nouveauNom)) {
            throw new IllegalArgumentException("Une variante avec le nom '" + nouveauNom + "' existe déjà pour ce service.");
        }

        String ancienneValeur = String.format("nom: '%s', duree: %d min, prix: %s, image: '%s'",
                variante.getNom(), variante.getDureeMinutes(), variante.getPrix(), variante.getImageUrl());

        variante.setNom(nouveauNom);
        variante.setDureeMinutes(request.dureeMinutes());
        variante.setPrix(request.prix());

        // Si une nouvelle image est fournie, supprimer l'ancienne de Cloudinary puis uploader la nouvelle
        if (imageFile != null && !imageFile.isEmpty()) {
            String ancienneImage = variante.getImageUrl();
            String nouvelleUrl = cloudinaryService.uploadImageVariante(imageFile, slugSalon);
            variante.setImageUrl(nouvelleUrl);

            if (ancienneImage != null && !ancienneImage.isBlank()) {
                cloudinaryService.deleteMediaByUrl(ancienneImage, "image");
            }
        }

        VarianteService saved = varianteServiceRepository.save(variante);

        String nouvelleValeur = String.format("nom: '%s', duree: %d min, prix: %s, image: '%s'",
                saved.getNom(), saved.getDureeMinutes(), saved.getPrix(), saved.getImageUrl());

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "VarianteService",
                String.valueOf(saved.getId()),
                ancienneValeur,
                nouvelleValeur,
                affectation,
                roleUtilise
        );

        return serviceSalonMapper.mapVariante(saved);
    }

    @Transactional
    public VarianteServiceDTOResponse uploadImageVariante(
            String slugSalon,
            Long serviceId,
            Long varianteId,
            MultipartFile file,
            String userEmail,
            String roleUtilise) throws IOException {

        getSalonBySlug(slugSalon);
        AffectationSalon affectation = getAffectationActive(userEmail, slugSalon);

        ServiceSalon service = serviceSalonRepository.findByIdAndSalonSlug(serviceId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Service introuvable avec l'identifiant : " + serviceId));

        VarianteService variante = varianteServiceRepository.findByIdAndServiceSalonId(varianteId, service.getId())
                .orElseThrow(() -> new EntityNotFoundException("Variante introuvable avec l'identifiant : " + varianteId));

        String ancienneImage = variante.getImageUrl();
        String nouvelleUrl = cloudinaryService.uploadImageVariante(file, slugSalon);
        variante.setImageUrl(nouvelleUrl);

        if (ancienneImage != null && !ancienneImage.isBlank()) {
            cloudinaryService.deleteMediaByUrl(ancienneImage, "image");
        }

        VarianteService saved = varianteServiceRepository.save(variante);

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "VarianteService (Image)",
                String.valueOf(saved.getId()),
                ancienneImage,
                nouvelleUrl,
                affectation,
                roleUtilise
        );

        return serviceSalonMapper.mapVariante(saved);
    }

    @Transactional
    public VarianteServiceDTOResponse modifierPrixVariante(
            String slugSalon,
            Long serviceId,
            Long varianteId,
            VariantePrixUpdateDTORequest request,
            String userEmail,
            String roleUtilise) {

        getSalonBySlug(slugSalon);
        AffectationSalon affectation = getAffectationActive(userEmail, slugSalon);

        ServiceSalon service = serviceSalonRepository.findByIdAndSalonSlug(serviceId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Service introuvable avec l'identifiant : " + serviceId));

        VarianteService variante = varianteServiceRepository.findByIdAndServiceSalonId(varianteId, service.getId())
                .orElseThrow(() -> new EntityNotFoundException("Variante introuvable avec l'identifiant : " + varianteId));

        String ancienneValeur = "prix: " + variante.getPrix();
        variante.setPrix(request.prix());
        VarianteService saved = varianteServiceRepository.save(variante);
        String nouvelleValeur = "prix: " + saved.getPrix();

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "VarianteService",
                String.valueOf(saved.getId()),
                ancienneValeur,
                nouvelleValeur,
                affectation,
                roleUtilise
        );

        return serviceSalonMapper.mapVariante(saved);
    }

    @Transactional
    public VarianteServiceDTOResponse basculerStatutVariante(
            String slugSalon,
            Long serviceId,
            Long varianteId,
            String userEmail,
            String roleUtilise) {

        getSalonBySlug(slugSalon);
        AffectationSalon affectation = getAffectationActive(userEmail, slugSalon);

        ServiceSalon service = serviceSalonRepository.findByIdAndSalonSlug(serviceId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Service introuvable avec l'identifiant : " + serviceId));

        VarianteService variante = varianteServiceRepository.findByIdAndServiceSalonId(varianteId, service.getId())
                .orElseThrow(() -> new EntityNotFoundException("Variante introuvable avec l'identifiant : " + varianteId));

        boolean ancienStatut = variante.getStatut();
        variante.setStatut(!ancienStatut);
        VarianteService saved = varianteServiceRepository.save(variante);

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "VarianteService",
                String.valueOf(saved.getId()),
                "statut: " + ancienStatut,
                "statut: " + saved.getStatut(),
                affectation,
                roleUtilise
        );

        return serviceSalonMapper.mapVariante(saved);
    }

    @Transactional(readOnly = true)
    public List<ServiceSalonDTOResponse> listerServices(String slugSalon, boolean seulementActifs) {
        getSalonBySlug(slugSalon);
        List<ServiceSalon> services = seulementActifs
                ? serviceSalonRepository.findBySalonSlugActiveWithVariantes(slugSalon)
                : serviceSalonRepository.findBySalonSlugWithVariantes(slugSalon);

        if (seulementActifs) {
            return services.stream()
                    .filter(s -> s.getVariantes() != null && s.getVariantes().stream().anyMatch(v -> Boolean.TRUE.equals(v.getStatut())))
                    .map(s -> {
                        ServiceSalonDTOResponse dto = serviceSalonMapper.apply(s);
                        List<VarianteServiceDTOResponse> variantesActives = dto.variantes().stream()
                                .filter(v -> Boolean.TRUE.equals(v.statut()))
                                .toList();
                        return new ServiceSalonDTOResponse(
                                dto.id(),
                                dto.nom(),
                                dto.description(),
                                dto.imageUrl(),
                                dto.statut(),
                                variantesActives,
                                dto.dateCreation()
                        );
                    })
                    .toList();
        }

        return services.stream()
                .map(serviceSalonMapper)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceSalonDTOResponse getService(String slugSalon, Long serviceId) {
        getSalonBySlug(slugSalon);
        ServiceSalon service = serviceSalonRepository.findByIdAndSalonSlug(serviceId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Service introuvable avec l'identifiant : " + serviceId));

        return serviceSalonMapper.apply(service);
    }

    private Salon getSalonBySlug(String slugSalon) {
        return salonRepository.findBySlug(slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Salon introuvable avec le slug : " + slugSalon));
    }

    private AffectationSalon getAffectationActive(String email, String slugSalon) {
        return affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon)
                .orElseThrow(() -> new IllegalArgumentException("Vous n'avez pas d'affectation active dans ce salon."));
    }
}

package com.kadi_aon.mon_salon.rgpd.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.common.service.CloudinaryService;
import com.kadi_aon.mon_salon.notification.service.NotificationEmailService;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeExportDTORequest;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeExportDTOResponse;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeSuppressionDTORequest;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeSuppressionDTOResponse;
import com.kadi_aon.mon_salon.rgpd.entity.DemandeExportDonnees;
import com.kadi_aon.mon_salon.rgpd.entity.DemandeSuppressionCompte;
import com.kadi_aon.mon_salon.rgpd.enums.FormatExportDonnees;
import com.kadi_aon.mon_salon.rgpd.enums.StatutDemandeSuppression;
import com.kadi_aon.mon_salon.rgpd.enums.StatutExportDonnees;
import com.kadi_aon.mon_salon.rgpd.repository.DemandeExportDonneesRepository;
import com.kadi_aon.mon_salon.rgpd.repository.DemandeSuppressionCompteRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RgpdClientService {

    private final DemandeSuppressionCompteRepository demandeSuppressionRepository;
    private final DemandeExportDonneesRepository demandeExportRepository;
    private final CompteRepository compteRepository;
    private final CloudinaryService cloudinaryService;
    private final NotificationEmailService notificationEmailService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public DemandeSuppressionDTOResponse demanderSuppressionCompte(String clientEmail, DemandeSuppressionDTORequest request) {
        Compte compte = compteRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new EntityNotFoundException("Compte introuvable pour l'email : " + clientEmail));

        if (demandeSuppressionRepository.existsByCompteEmailAndStatut(clientEmail, StatutDemandeSuppression.EN_ATTENTE)) {
            throw new IllegalStateException("Une demande de suppression de compte est déjà en cours de traitement pour cet utilisateur.");
        }

        DemandeSuppressionCompte demande = DemandeSuppressionCompte.builder()
                .compte(compte)
                .motif(request != null ? request.motif() : null)
                .statut(StatutDemandeSuppression.EN_ATTENTE)
                .dateDemande(LocalDateTime.now())
                .build();

        DemandeSuppressionCompte saved = demandeSuppressionRepository.save(demande);

        auditLogService.logActionPlateforme(
                TypeActionAudit.CREATION,
                "DemandeSuppressionCompte",
                String.valueOf(saved.getId()),
                null,
                "Demande de suppression soumise par " + clientEmail,
                compte,
                "CLIENT"
        );

        log.info("Demande de suppression de compte {} enregistrée pour {}", saved.getId(), clientEmail);
        return mapSuppressionToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DemandeSuppressionDTOResponse> consulterMesDemandesSuppression(String clientEmail) {
        return demandeSuppressionRepository.findByCompteEmailOrderByDateDemandeDesc(clientEmail).stream()
                .map(this::mapSuppressionToResponse)
                .toList();
    }

    @Transactional
    public DemandeExportDTOResponse demanderExportDonnees(String clientEmail, DemandeExportDTORequest request) {
        Compte compte = compteRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new EntityNotFoundException("Compte introuvable pour l'email : " + clientEmail));

        DemandeExportDonnees demande = DemandeExportDonnees.builder()
                .compte(compte)
                .format(request.format())
                .statut(StatutExportDonnees.EN_ATTENTE)
                .dateDemande(LocalDateTime.now())
                .build();

        DemandeExportDonnees saved = demandeExportRepository.save(demande);

        try {
            // Génération du contenu d'exportation
            byte[] contenu = genererContenuExport(compte, request.format());
            String extension = request.format() == FormatExportDonnees.JSON ? "json" : "csv";
            String filename = "export_client_" + compte.getId() + "_" + System.currentTimeMillis() + "." + extension;

            // Upload sur Cloudinary (dans mon-salon/rgpd_exports)
            Map<String, String> uploadResult = cloudinaryService.uploadExportRgpd(contenu, filename);
            String secureUrl = uploadResult.get("secure_url");
            String publicId = uploadResult.get("public_id");

            saved.setUrlTelechargement(secureUrl);
            saved.setCloudinaryPublicId(publicId);
            saved.setDateExpiration(LocalDateTime.now().plusHours(48));
            saved.setStatut(StatutExportDonnees.DISPONIBLE);

            DemandeExportDonnees updated = demandeExportRepository.save(saved);

            // Notification envoyée au client
            String sujet = "Votre archive de données personnelles est prête (valable 48h)";
            String corps = String.format(
                    "Bonjour %s,\n\n" +
                    "Conformément à votre demande d'export RGPD, vos données personnelles sont prêtes au format %s.\n\n" +
                    "Vous pouvez télécharger votre archive sécurisée à l'adresse suivante :\n%s\n\n" +
                    "Attention : Ce lien est valide pendant 48 heures. Passé ce délai, le fichier sera automatiquement supprimé de nos serveurs.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe Mon Salon.",
                    compte.getPrenom(),
                    request.format().name(),
                    secureUrl
            );

            notificationEmailService.queueNotificationEmail(clientEmail, sujet, corps);

            auditLogService.logActionPlateforme(
                    TypeActionAudit.CREATION,
                    "DemandeExportDonnees",
                    String.valueOf(updated.getId()),
                    null,
                    "Export RGPD mis à disposition pour " + clientEmail + " (valable 48h)",
                    compte,
                    "CLIENT"
            );

            log.info("Export RGPD généré avec succès pour {} (URL: {})", clientEmail, secureUrl);
            return mapExportToResponse(updated);

        } catch (Exception e) {
            log.error("Erreur lors de la génération de l'export RGPD pour {} : {}", clientEmail, e.getMessage(), e);
            saved.setStatut(StatutExportDonnees.EN_ATTENTE);
            demandeExportRepository.save(saved);
            throw new RuntimeException("Impossible de générer l'archive d'export : " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<DemandeExportDTOResponse> consulterMesExports(String clientEmail) {
        return demandeExportRepository.findByCompteEmailOrderByDateDemandeDesc(clientEmail).stream()
                .map(this::mapExportToResponse)
                .toList();
    }

    private byte[] genererContenuExport(Compte compte, FormatExportDonnees format) throws Exception {
        Map<String, Object> donnees = Map.of(
                "id", compte.getId(),
                "nom", compte.getNom(),
                "prenom", compte.getPrenom(),
                "email", compte.getEmail(),
                "telephone", compte.getTelephone() != null ? compte.getTelephone() : "",
                "dateCreation", compte.getDateCreation() != null ? compte.getDateCreation().toString() : "",
                "statut", compte.getStatut() != null ? compte.getStatut() : true
        );

        if (format == FormatExportDonnees.JSON) {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(donnees);
        } else {
            // Format CSV
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String csv = "Id,Nom,Prenom,Email,Telephone,DateCreation,Statut\n" +
                    String.format("%s,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s\n",
                            compte.getId(),
                            compte.getNom(),
                            compte.getPrenom(),
                            compte.getEmail(),
                            compte.getTelephone() != null ? compte.getTelephone() : "",
                            compte.getDateCreation() != null ? compte.getDateCreation().toString() : "",
                            compte.getStatut() != null ? compte.getStatut() : true
                    );
            out.write(csv.getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        }
    }

    private DemandeSuppressionDTOResponse mapSuppressionToResponse(DemandeSuppressionCompte d) {
        String adminEmail = d.getTraitePar() != null ? d.getTraitePar().getEmail() : null;
        return new DemandeSuppressionDTOResponse(
                d.getId(),
                d.getCompte().getId(),
                d.getCompte().getEmail(),
                d.getMotif(),
                d.getStatut().name(),
                d.getDateDemande(),
                d.getDateDecision(),
                d.getMotifDecision(),
                adminEmail
        );
    }

    private DemandeExportDTOResponse mapExportToResponse(DemandeExportDonnees d) {
        boolean expire = d.getStatut() == StatutExportDonnees.EXPIRE
                || (d.getDateExpiration() != null && d.getDateExpiration().isBefore(LocalDateTime.now()));

        return new DemandeExportDTOResponse(
                d.getId(),
                d.getCompte().getEmail(),
                d.getFormat().name(),
                expire ? StatutExportDonnees.EXPIRE.name() : d.getStatut().name(),
                d.getUrlTelechargement(),
                d.getDateDemande(),
                d.getDateExpiration(),
                expire
        );
    }
}

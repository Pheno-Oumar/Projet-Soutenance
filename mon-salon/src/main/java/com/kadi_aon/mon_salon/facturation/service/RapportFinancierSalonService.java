package com.kadi_aon.mon_salon.facturation.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.caisse.entity.OperationCaisse;
import com.kadi_aon.mon_salon.caisse.entity.SessionCaisse;
import com.kadi_aon.mon_salon.caisse.enums.StatutSessionCaisse;
import com.kadi_aon.mon_salon.caisse.enums.TypeOperationCaisse;
import com.kadi_aon.mon_salon.caisse.repository.SessionCaisseRepository;
import com.kadi_aon.mon_salon.common.service.CloudinaryService;
import com.kadi_aon.mon_salon.depense.entity.Depense;
//import com.kadi_aon.mon_salon.depense.enums.CategorieDepense;
import com.kadi_aon.mon_salon.depense.repository.DepenseRepository;
import com.kadi_aon.mon_salon.facturation.dto.ExportTelechargementDTO;
import com.kadi_aon.mon_salon.facturation.dto.KpiFinancierDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.LigneRapportEntreeDTO;
import com.kadi_aon.mon_salon.facturation.dto.LigneRapportSortieDTO;
import com.kadi_aon.mon_salon.facturation.dto.RapportExportDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.RapportFinancierDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.RapportFinancierFiltreDTORequest;
import com.kadi_aon.mon_salon.facturation.entity.Paiement;
import com.kadi_aon.mon_salon.facturation.enums.StatutPaiement;
import com.kadi_aon.mon_salon.facturation.enums.TypeRapportFinancier;
import com.kadi_aon.mon_salon.facturation.repository.PaiementRepository;
import com.kadi_aon.mon_salon.rgpd.entity.DemandeExportDonnees;
import com.kadi_aon.mon_salon.rgpd.enums.FormatExportDonnees;
import com.kadi_aon.mon_salon.rgpd.enums.StatutExportDonnees;
import com.kadi_aon.mon_salon.rgpd.repository.DemandeExportDonneesRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RapportFinancierSalonService {

    private final SalonRepository salonRepository;
    private final PaiementRepository paiementRepository;
    private final DepenseRepository depenseRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
    private final DemandeExportDonneesRepository demandeExportDonneesRepository;
    private final CompteRepository compteRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final CloudinaryService cloudinaryService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Transactional(readOnly = true)
    public RapportFinancierDTOResponse consulterRapportFinancier(String slugSalon, RapportFinancierFiltreDTORequest filtre) {
        Salon salon = verifierSalonActif(slugSalon);

        LocalDate dateDebut = (filtre != null && filtre.dateDebut() != null) ? filtre.dateDebut() : LocalDate.now().withDayOfMonth(1);
        LocalDate dateFin = (filtre != null && filtre.dateFin() != null) ? filtre.dateFin() : LocalDate.now();

        if (dateDebut.isAfter(dateFin)) {
            throw new IllegalArgumentException("La date de début ne peut pas être postérieure à la date de fin.");
        }

        LocalDateTime debut = dateDebut.atStartOfDay();
        LocalDateTime fin = dateFin.atTime(23, 59, 59);

        TypeRapportFinancier type = (filtre != null && filtre.typeRapport() != null)
                ? filtre.typeRapport() : TypeRapportFinancier.ENTREES_SORTIES;

        List<Paiement> paiements = Collections.emptyList();
        List<Depense> depenses = Collections.emptyList();

        switch (type) {
            case ENTREES_SORTIES -> {
                paiements = paiementRepository.findBySalonSlugAndStatutAndDatePaiementBetweenOrderByDatePaiementDesc(
                        slugSalon, StatutPaiement.PAYE, debut, fin);
                depenses = depenseRepository.findBySalonSlugAndStatutAndDateDepenseBetweenOrderByDateDepenseDesc(
                        slugSalon, true, debut, fin);
            }
            case TOUTES_ENTREES -> {
                paiements = paiementRepository.findBySalonSlugAndStatutAndDatePaiementBetweenOrderByDatePaiementDesc(
                        slugSalon, StatutPaiement.PAYE, debut, fin);
            }
            case ENTREE_SPECIFIQUE -> {
                if (filtre != null && filtre.typePaiement() != null) {
                    paiements = paiementRepository.findBySalonSlugAndStatutAndTypeAndDatePaiementBetweenOrderByDatePaiementDesc(
                            slugSalon, StatutPaiement.PAYE, filtre.typePaiement(), debut, fin);
                } else {
                    paiements = paiementRepository.findBySalonSlugAndStatutAndDatePaiementBetweenOrderByDatePaiementDesc(
                            slugSalon, StatutPaiement.PAYE, debut, fin);
                }
            }
            case TOUTES_DEPENSES -> {
                depenses = depenseRepository.findBySalonSlugAndStatutAndDateDepenseBetweenOrderByDateDepenseDesc(
                        slugSalon, true, debut, fin);
            }
            case DEPENSES_CATEGORIE -> {
                if (filtre == null || filtre.categorieDepense() == null) {
                    throw new IllegalArgumentException("La catégorie de dépense est obligatoire pour le type de rapport DEPENSES_CATEGORIE.");
                }
                depenses = depenseRepository.findBySalonSlugAndCategorieAndStatutAndDateDepenseBetweenOrderByDateDepenseDesc(
                        slugSalon, filtre.categorieDepense(), true, debut, fin);
            }
        }

        List<LigneRapportEntreeDTO> lignesEntrees = paiements.stream()
                .map(this::mapToLigneEntree)
                .toList();

        List<LigneRapportSortieDTO> lignesSorties = depenses.stream()
                .map(this::mapToLigneSortie)
                .toList();

        BigDecimal totalEntrees = lignesEntrees.stream()
                .map(LigneRapportEntreeDTO::montant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSorties = lignesSorties.stream()
                .map(LigneRapportSortieDTO::montant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal soldeNet = totalEntrees.subtract(totalSorties);

        return RapportFinancierDTOResponse.builder()
                .slugSalon(slugSalon)
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .typeRapport(type)
                .categorieDepense(filtre != null ? filtre.categorieDepense() : null)
                .totalEntrees(totalEntrees)
                .totalSorties(totalSorties)
                .soldeNet(soldeNet)
                .nombreEntrees(lignesEntrees.size())
                .nombreSorties(lignesSorties.size())
                .entrees(lignesEntrees)
                .sorties(lignesSorties)
                .build();
    }

    @Transactional
    public RapportExportDTOResponse exporterRapportFinancier(
            String slugSalon,
            String comptableEmail,
            RapportFinancierFiltreDTORequest filtre) {

        AffectationSalon affectation = validerComptable(slugSalon, comptableEmail);
        Compte compte = affectation.getCompte();

        RapportFinancierDTOResponse rapport = consulterRapportFinancier(slugSalon, filtre);
        FormatExportDonnees format = (filtre != null && filtre.format() != null)
                ? filtre.format() : FormatExportDonnees.PDF;

        try {
            byte[] content;
            String extension;

            if (format == FormatExportDonnees.PDF) {
                content = genererRapportPdf(rapport, affectation.getSalon().getNom());
                extension = "pdf";
            } else if (format == FormatExportDonnees.CSV) {
                content = genererRapportCsv(rapport);
                extension = "csv";
            } else {
                content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(rapport);
                extension = "json";
            }

            String filename = "rapport_financier_" + slugSalon + "_" + System.currentTimeMillis() + "." + extension;
            Map<String, String> uploadResult = cloudinaryService.uploadExportRgpd(content, filename);

            String secureUrl = uploadResult.get("secure_url");
            String publicId = uploadResult.get("public_id");

            DemandeExportDonnees export = DemandeExportDonnees.builder()
                    .compte(compte)
                    .format(format)
                    .statut(StatutExportDonnees.DISPONIBLE)
                    .urlTelechargement(secureUrl)
                    .cloudinaryPublicId(publicId)
                    .dateDemande(LocalDateTime.now())
                    .dateExpiration(LocalDateTime.now().plusHours(24)) // Expire sous 24h
                    .build();

            DemandeExportDonnees saved = demandeExportDonneesRepository.save(export);

            auditLogService.logActionSalon(
                    TypeActionAudit.CREATION,
                    "RapportFinancierExport",
                    String.valueOf(saved.getId()),
                    null,
                    "Génération rapport financier " + format + " (valable 24h) par " + comptableEmail,
                    affectation,
                    TypeRoleSalon.COMPTABLE.name()
            );

            log.info("Rapport financier {} généré et téléversé sur Cloudinary pour le salon {} (URL: {})",
                    format, slugSalon, secureUrl);

            return RapportExportDTOResponse.builder()
                    .exportId(saved.getId())
                    .slugSalon(slugSalon)
                    .format(format)
                    .urlTelechargement(secureUrl)
                    .dateDemande(saved.getDateDemande())
                    .dateExpiration(saved.getDateExpiration())
                    .statut(saved.getStatut().name())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la génération de l'export rapport financier pour le salon {} : {}",
                    slugSalon, e.getMessage(), e);
            throw new RuntimeException("Échec de la génération du rapport financier : " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public KpiFinancierDTOResponse obtenirKpiFinanciers(String slugSalon) {
        verifierSalonActif(slugSalon);

        LocalDate now = LocalDate.now();
        LocalDateTime debutMois = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMois = now.atTime(23, 59, 59);

        LocalDateTime debutJour = now.atStartOfDay();
        LocalDateTime finJour = now.atTime(23, 59, 59);

        BigDecimal caTotal = paiementRepository.totalPaiementsSalon(slugSalon);
        BigDecimal revenusMois = paiementRepository.totalPaiementsSalonEntreDates(slugSalon, StatutPaiement.PAYE, debutMois, finMois);
        BigDecimal depensesMois = depenseRepository.totalDepensesSalonEntreDates(slugSalon, debutMois, finMois);
        BigDecimal beneficeMois = revenusMois.subtract(depensesMois);

        BigDecimal revenusJour = paiementRepository.totalPaiementsSalonEntreDates(slugSalon, StatutPaiement.PAYE, debutJour, finJour);
        BigDecimal depensesJour = depenseRepository.totalDepensesSalonEntreDates(slugSalon, debutJour, finJour);
        BigDecimal beneficeJour = revenusJour.subtract(depensesJour);

        BigDecimal remboursements = paiementRepository.totalRemboursementsSalon(slugSalon);

        List<Object[]> rawDepensesCat = depenseRepository.totalDepensesParCategorieEntreDates(slugSalon, debutMois, finMois);
        Map<String, BigDecimal> depensesParCat = new HashMap<>();
        for (Object[] row : rawDepensesCat) {
            if (row[0] != null) {
                depensesParCat.put(row[0].toString(), (BigDecimal) row[1]);
            }
        }

        boolean caisseOuverte = false;
        BigDecimal soldeTheoriqueCaisse = BigDecimal.ZERO;

        var optSession = sessionCaisseRepository.findByAffectationSalonSlugAndStatut(slugSalon, StatutSessionCaisse.EN_COURS);
        if (optSession.isPresent()) {
            caisseOuverte = true;
            SessionCaisse s = optSession.get();
            BigDecimal entreesCaisse = s.getOperations().stream()
                    .filter(o -> o.getType() == TypeOperationCaisse.ENTREE && Boolean.TRUE.equals(o.getStatut()))
                    .map(OperationCaisse::getMontant)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal sortiesCaisse = s.getOperations().stream()
                    .filter(o -> o.getType() == TypeOperationCaisse.SORTIE && Boolean.TRUE.equals(o.getStatut()))
                    .map(OperationCaisse::getMontant)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            soldeTheoriqueCaisse = s.getSoldeOuverture().add(entreesCaisse).subtract(sortiesCaisse);
        }

        return KpiFinancierDTOResponse.builder()
                .slugSalon(slugSalon)
                .chiffreAffairesTotal(caTotal)
                .revenusMoisEnCours(revenusMois)
                .depensesMoisEnCours(depensesMois)
                .beneficeNetMoisEnCours(beneficeMois)
                .revenusAujourdhui(revenusJour)
                .depensesAujourdhui(depensesJour)
                .beneficeNetAujourdhui(beneficeJour)
                .totalRemboursements(remboursements)
                .depensesParCategorie(depensesParCat)
                .sessionCaisseOuverte(caisseOuverte)
                .soldeTheoriqueCaisseActive(soldeTheoriqueCaisse)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RapportExportDTOResponse> listerMesExportsFinanciers(String comptableEmail, String slugSalon) {
        validerComptable(slugSalon, comptableEmail);
        return demandeExportDonneesRepository.findByCompteEmailOrderByDateDemandeDesc(comptableEmail).stream()
                .map(e -> RapportExportDTOResponse.builder()
                        .exportId(e.getId())
                        .slugSalon(slugSalon)
                        .format(e.getFormat())
                        .urlTelechargement(e.getUrlTelechargement())
                        .dateDemande(e.getDateDemande())
                        .dateExpiration(e.getDateExpiration())
                        .statut(e.getStatut().name())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public ExportTelechargementDTO telechargerExport(String slugSalon, Long exportId, String userEmail) {
        verifierSalonActif(slugSalon);
        validerComptableOuProprietaire(slugSalon, userEmail);

        DemandeExportDonnees export = demandeExportDonneesRepository.findById(exportId)
                .orElseThrow(() -> new EntityNotFoundException("Export introuvable avec l'identifiant : " + exportId));

        if (export.getDateExpiration() != null && export.getDateExpiration().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Ce document a expiré (validité de 24 heures). Veuillez relancer une génération.");
        }

        if (export.getStatut() != StatutExportDonnees.DISPONIBLE) {
            throw new IllegalStateException("Ce document n'est pas encore disponible ou a été révoqué.");
        }

        byte[] data;
        try {
            data = cloudinaryService.downloadRawFile(export.getCloudinaryPublicId(), export.getUrlTelechargement());
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du fichier export {} pour le salon {} : {}",
                    exportId, slugSalon, e.getMessage(), e);
            throw new RuntimeException("Impossible de récupérer le fichier d'export : " + e.getMessage(), e);
        }

        String extension = export.getFormat() == FormatExportDonnees.PDF ? "pdf" :
                           (export.getFormat() == FormatExportDonnees.CSV ? "csv" : "json");
        String filename = "rapport_financier_" + slugSalon + "_" + export.getId() + "." + extension;

        String contentType = switch (export.getFormat()) {
            case PDF -> "application/pdf";
            case CSV -> "text/csv; charset=UTF-8";
            case JSON -> "application/json";
        };

        return new ExportTelechargementDTO(data, filename, contentType);
    }

    // ==========================================
    // GÉNÉRATION PDF & CSV
    // ==========================================

    private byte[] genererRapportPdf(RapportFinancierDTOResponse r, String nomSalon) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        PdfWriter.getInstance(document, baos);

        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(44, 62, 80));
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
        Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

        Paragraph titre = new Paragraph("RAPPORT FINANCIER", titleFont);
        titre.setAlignment(Element.ALIGN_CENTER);
        document.add(titre);

        Paragraph salonInfo = new Paragraph("Salon : " + nomSalon + " | Période du " + r.dateDebut() + " au " + r.dateFin(), subTitleFont);
        salonInfo.setAlignment(Element.ALIGN_CENTER);
        salonInfo.setSpacingAfter(15);
        document.add(salonInfo);

        // Tableau Synthèse
        PdfPTable tableSynthese = new PdfPTable(3);
        tableSynthese.setWidthPercentage(100);
        tableSynthese.setSpacingAfter(20);

        PdfPCell c1 = new PdfPCell(new Phrase("Total Entrées : " + r.totalEntrees() + " CFA", boldFont));
        c1.setBackgroundColor(new Color(235, 247, 238));
        c1.setPadding(8);
        PdfPCell c2 = new PdfPCell(new Phrase("Total Dépenses : " + r.totalSorties() + " CFA", boldFont));
        c2.setBackgroundColor(new Color(253, 237, 237));
        c2.setPadding(8);
        PdfPCell c3 = new PdfPCell(new Phrase("Solde Net : " + r.soldeNet() + " CFA", boldFont));
        c3.setBackgroundColor(new Color(238, 242, 246));
        c3.setPadding(8);

        tableSynthese.addCell(c1);
        tableSynthese.addCell(c2);
        tableSynthese.addCell(c3);
        document.add(tableSynthese);

        // Section Entrées
        if (!r.entrees().isEmpty()) {
            Paragraph pEntrees = new Paragraph("Détail des Entrées / Paiements (" + r.nombreEntrees() + ")", boldFont);
            pEntrees.setSpacingAfter(8);
            document.add(pEntrees);

            PdfPTable tEntrees = new PdfPTable(5);
            tEntrees.setWidthPercentage(100);
            tEntrees.setSpacingAfter(15);

            String[] colsEntrees = {"N° Paiement", "Date", "Client", "Mode", "Montant"};
            for (String col : colsEntrees) {
                PdfPCell th = new PdfPCell(new Phrase(col, headerFont));
                th.setBackgroundColor(new Color(41, 128, 185));
                th.setPadding(5);
                tEntrees.addCell(th);
            }

            for (LigneRapportEntreeDTO e : r.entrees()) {
                tEntrees.addCell(new Phrase(e.numeroPaiement(), bodyFont));
                tEntrees.addCell(new Phrase(e.datePaiement() != null ? e.datePaiement().format(DATE_FORMATTER) : "", bodyFont));
                tEntrees.addCell(new Phrase(e.clientNom() != null ? e.clientNom() : "-", bodyFont));
                tEntrees.addCell(new Phrase(e.modePaiement() != null ? e.modePaiement().name() : "", bodyFont));
                tEntrees.addCell(new Phrase(e.montant().toString() + " CFA", bodyFont));
            }
            document.add(tEntrees);
        }

        // Section Sorties
        if (!r.sorties().isEmpty()) {
            Paragraph pSorties = new Paragraph("Détail des Dépenses / Sorties (" + r.nombreSorties() + ")", boldFont);
            pSorties.setSpacingAfter(8);
            document.add(pSorties);

            PdfPTable tSorties = new PdfPTable(5);
            tSorties.setWidthPercentage(100);
            tSorties.setSpacingAfter(15);

            String[] colsSorties = {"ID", "Date", "Catégorie", "Motif / Description", "Montant"};
            for (String col : colsSorties) {
                PdfPCell th = new PdfPCell(new Phrase(col, headerFont));
                th.setBackgroundColor(new Color(192, 57, 43));
                th.setPadding(5);
                tSorties.addCell(th);
            }

            for (LigneRapportSortieDTO s : r.sorties()) {
                tSorties.addCell(new Phrase(String.valueOf(s.depenseId()), bodyFont));
                tSorties.addCell(new Phrase(s.dateDepense() != null ? s.dateDepense().format(DATE_FORMATTER) : "", bodyFont));
                tSorties.addCell(new Phrase(s.categorie() != null ? s.categorie().name() : "", bodyFont));
                tSorties.addCell(new Phrase(s.description() != null ? s.description() : "-", bodyFont));
                tSorties.addCell(new Phrase(s.montant().toString() + " CFA", bodyFont));
            }
            document.add(tSorties);
        }

        Paragraph footer = new Paragraph("Ce rapport est valable 24 heures et sera purgé automatiquement par le système.", subTitleFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(15);
        document.add(footer);

        document.close();
        return baos.toByteArray();
    }

    private byte[] genererRapportCsv(RapportFinancierDTOResponse r) {
        StringBuilder sb = new StringBuilder();
        sb.append("RAPPORT FINANCIER;Salon:;").append(r.slugSalon()).append("\n");
        sb.append("Periode:;").append(r.dateDebut()).append(";au;").append(r.dateFin()).append("\n");
        sb.append("Total Entrees:;").append(r.totalEntrees()).append(";Total Sorties:;").append(r.totalSorties()).append(";Solde Net:;").append(r.soldeNet()).append("\n\n");

        if (!r.entrees().isEmpty()) {
            sb.append("--- ENTREES ---\n");
            sb.append("Numero Paiement;Date;Client;Mode;Montant\n");
            for (LigneRapportEntreeDTO e : r.entrees()) {
                sb.append(e.numeroPaiement()).append(";")
                        .append(e.datePaiement()).append(";")
                        .append(e.clientNom() != null ? e.clientNom() : "").append(";")
                        .append(e.modePaiement()).append(";")
                        .append(e.montant()).append("\n");
            }
            sb.append("\n");
        }

        if (!r.sorties().isEmpty()) {
            sb.append("--- SORTIES ---\n");
            sb.append("ID Depense;Date;Categorie;Description;Montant\n");
            for (LigneRapportSortieDTO s : r.sorties()) {
                sb.append(s.depenseId()).append(";")
                        .append(s.dateDepense()).append(";")
                        .append(s.categorie()).append(";")
                        .append(s.description() != null ? s.description().replace(";", " ") : "").append(";")
                        .append(s.montant()).append("\n");
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private LigneRapportEntreeDTO mapToLigneEntree(Paiement p) {
        String clientNom = "-";
        if (p.getClient() != null) {
            clientNom = p.getClient().getPrenom() + " " + p.getClient().getNom();
        } else if (p.getFacture() != null && p.getFacture().getPrestation() != null && p.getFacture().getPrestation().getClient() != null) {
            clientNom = p.getFacture().getPrestation().getClient().getPrenom() + " " + p.getFacture().getPrestation().getClient().getNom();
        }
        return LigneRapportEntreeDTO.builder()
                .paiementId(p.getId())
                .numeroPaiement(p.getNumeroPaiement())
                .numeroFacture(p.getFacture() != null ? p.getFacture().getNumeroFacture() : null)
                .montant(p.getMontant())
                .modePaiement(p.getType())
                .datePaiement(p.getDatePaiement())
                .clientNom(clientNom)
                .statut(p.getStatut().name())
                .build();
    }

    private LigneRapportSortieDTO mapToLigneSortie(Depense d) {
        String comptableNom = "-";
        if (d.getComptable() != null && d.getComptable().getCompte() != null) {
            comptableNom = d.getComptable().getCompte().getPrenom() + " " + d.getComptable().getCompte().getNom();
        }
        return LigneRapportSortieDTO.builder()
                .depenseId(d.getId())
                .montant(d.getMontant())
                .categorie(d.getCategorie())
                .dateDepense(d.getDateDepense())
                .description(d.getDescription())
                .comptableNom(comptableNom)
                .statut(d.isStatut())
                .build();
    }

    private Salon verifierSalonActif(String slugSalon) {
        Salon salon = salonRepository.findBySlug(slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Salon introuvable pour le slug : " + slugSalon));
        if (!Boolean.TRUE.equals(salon.getStatut())) {
            throw new IllegalStateException("Le salon " + slugSalon + " est inactif.");
        }
        return salon;
    }

    private AffectationSalon validerComptable(String slugSalon, String email) {
        AffectationSalon affectation = affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Affectation active introuvable pour l'utilisateur " + email + " sur le salon " + slugSalon));

        boolean hasComptableRole = affectation.getRoles().stream()
                .anyMatch(r -> r.getRole() == TypeRoleSalon.COMPTABLE);

        if (!hasComptableRole) {
            throw new IllegalArgumentException("L'utilisateur " + email + " ne possède pas le rôle COMPTABLE sur le salon " + slugSalon);
        }
        return affectation;
    }

    private AffectationSalon validerComptableOuProprietaire(String slugSalon, String email) {
        AffectationSalon affectation = affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Affectation active introuvable pour l'utilisateur " + email + " sur le salon " + slugSalon));

        boolean hasRole = affectation.getRoles().stream()
                .anyMatch(r -> r.getRole() == TypeRoleSalon.COMPTABLE || r.getRole() == TypeRoleSalon.PROPRIETAIRE);

        if (!hasRole) {
            throw new IllegalArgumentException("L'utilisateur " + email + " ne possède pas les autorisations nécessaires sur le salon " + slugSalon);
        }
        return affectation;
    }
}

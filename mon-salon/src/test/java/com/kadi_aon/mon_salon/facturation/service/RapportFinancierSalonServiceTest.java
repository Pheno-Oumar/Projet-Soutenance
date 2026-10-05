package com.kadi_aon.mon_salon.facturation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
import com.kadi_aon.mon_salon.caisse.repository.SessionCaisseRepository;
import com.kadi_aon.mon_salon.common.service.CloudinaryService;
import com.kadi_aon.mon_salon.depense.entity.Depense;
import com.kadi_aon.mon_salon.depense.enums.CategorieDepense;
import com.kadi_aon.mon_salon.depense.repository.DepenseRepository;
import com.kadi_aon.mon_salon.facturation.dto.KpiFinancierDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.RapportExportDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.RapportFinancierDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.RapportFinancierFiltreDTORequest;
import com.kadi_aon.mon_salon.facturation.entity.Facture;
import com.kadi_aon.mon_salon.facturation.entity.Paiement;
import com.kadi_aon.mon_salon.facturation.enums.StatutPaiement;
import com.kadi_aon.mon_salon.facturation.enums.TypePaiement;
import com.kadi_aon.mon_salon.facturation.enums.TypeRapportFinancier;
import com.kadi_aon.mon_salon.facturation.repository.PaiementRepository;
import com.kadi_aon.mon_salon.rgpd.entity.DemandeExportDonnees;
import com.kadi_aon.mon_salon.rgpd.enums.FormatExportDonnees;
import com.kadi_aon.mon_salon.rgpd.repository.DemandeExportDonneesRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

@ExtendWith(MockitoExtension.class)
class RapportFinancierSalonServiceTest {

    @Mock
    private SalonRepository salonRepository;

    @Mock
    private PaiementRepository paiementRepository;

    @Mock
    private DepenseRepository depenseRepository;

    @Mock
    private SessionCaisseRepository sessionCaisseRepository;

    @Mock
    private DemandeExportDonneesRepository demandeExportDonneesRepository;

    @Mock
    private CompteRepository compteRepository;

    @Mock
    private AffectationSalonRepository affectationSalonRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private RapportFinancierSalonService rapportFinancierSalonService;

    private Salon salon;
    private Compte comptableCompte;
    private AffectationSalon affectationComptable;
    private Paiement paiement;
    private Depense depense;

    @BeforeEach
    void setUp() {
        salon = Salon.builder()
                .id(1L)
                .nom("Salon Prestige")
                .slug("salon-prestige")
                .statut(true)
                .build();

        comptableCompte = Compte.builder()
                .id(10L)
                .nom("Diallo")
                .prenom("Mamadou")
                .email("comptable@prestige.com")
                .build();

        RoleSalon roleComptable = RoleSalon.builder().id(3L).role(TypeRoleSalon.COMPTABLE).build();

        affectationComptable = AffectationSalon.builder()
                .id(100L)
                .compte(comptableCompte)
                .salon(salon)
                .roles(Set.of(roleComptable))
                .statut(true)
                .build();

        Facture facture = Facture.builder()
                .id(50L)
                .numeroFacture("FAC-2026-001")
                .build();

        paiement = Paiement.builder()
                .id(500L)
                .numeroPaiement("PAY-2026-001")
                .montant(new BigDecimal("15000.00"))
                .type(TypePaiement.SOLDE)
                .statut(StatutPaiement.PAYE)
                .datePaiement(LocalDateTime.now().minusDays(2))
                .salon(salon)
                .facture(facture)
                .client(Compte.builder().id(20L).nom("Sow").prenom("Awa").build())
                .build();


        depense = Depense.builder()
                .id(600L)
                .montant(new BigDecimal("5000.00"))
                .categorie(CategorieDepense.LOYER)
                .dateDepense(LocalDateTime.now().minusDays(1))
                .description("Paiement loyer")
                .statut(true)
                .salon(salon)
                .comptable(affectationComptable)
                .build();
    }

    @Test
    void consulterRapportFinancier_entreesEtSorties() {
        when(salonRepository.findBySlug("salon-prestige")).thenReturn(Optional.of(salon));
        when(paiementRepository.findBySalonSlugAndStatutAndDatePaiementBetweenOrderByDatePaiementDesc(
                eq("salon-prestige"), eq(StatutPaiement.PAYE), any(), any()))
                .thenReturn(List.of(paiement));
        when(depenseRepository.findBySalonSlugAndStatutAndDateDepenseBetweenOrderByDateDepenseDesc(
                eq("salon-prestige"), eq(true), any(), any()))
                .thenReturn(List.of(depense));

        RapportFinancierFiltreDTORequest filtre = new RapportFinancierFiltreDTORequest(
                LocalDate.now().minusDays(5), LocalDate.now(), TypeRapportFinancier.ENTREES_SORTIES,
                null, null, FormatExportDonnees.PDF
        );

        RapportFinancierDTOResponse rapport = rapportFinancierSalonService.consulterRapportFinancier("salon-prestige", filtre);

        assertNotNull(rapport);
        assertEquals(new BigDecimal("15000.00"), rapport.totalEntrees());
        assertEquals(new BigDecimal("5000.00"), rapport.totalSorties());
        assertEquals(new BigDecimal("10000.00"), rapport.soldeNet());
        assertEquals(1, rapport.nombreEntrees());
        assertEquals(1, rapport.nombreSorties());
    }

    @Test
    void consulterRapportFinancier_datesInvalides_lanceException() {
        when(salonRepository.findBySlug("salon-prestige")).thenReturn(Optional.of(salon));

        RapportFinancierFiltreDTORequest filtre = new RapportFinancierFiltreDTORequest(
                LocalDate.now(), LocalDate.now().minusDays(5), TypeRapportFinancier.ENTREES_SORTIES,
                null, null, FormatExportDonnees.PDF
        );

        assertThrows(IllegalArgumentException.class, () ->
                rapportFinancierSalonService.consulterRapportFinancier("salon-prestige", filtre));
    }

    @Test
    void consulterRapportFinancier_depensesCategorie_succes() {
        when(salonRepository.findBySlug("salon-prestige")).thenReturn(Optional.of(salon));
        when(depenseRepository.findBySalonSlugAndCategorieAndStatutAndDateDepenseBetweenOrderByDateDepenseDesc(
                eq("salon-prestige"), eq(CategorieDepense.LOYER), eq(true), any(), any()))
                .thenReturn(List.of(depense));

        RapportFinancierFiltreDTORequest filtre = new RapportFinancierFiltreDTORequest(
                LocalDate.now().minusDays(5), LocalDate.now(), TypeRapportFinancier.DEPENSES_CATEGORIE,
                CategorieDepense.LOYER, null, FormatExportDonnees.PDF
        );

        RapportFinancierDTOResponse rapport = rapportFinancierSalonService.consulterRapportFinancier("salon-prestige", filtre);

        assertNotNull(rapport);
        assertEquals(BigDecimal.ZERO, rapport.totalEntrees());
        assertEquals(new BigDecimal("5000.00"), rapport.totalSorties());
        assertEquals(1, rapport.nombreSorties());
    }

    @Test
    void exporterRapportFinancier_pdf_succes_expireDans24h() throws Exception {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("comptable@prestige.com", "salon-prestige"))
                .thenReturn(Optional.of(affectationComptable));
        when(salonRepository.findBySlug("salon-prestige")).thenReturn(Optional.of(salon));
        when(paiementRepository.findBySalonSlugAndStatutAndDatePaiementBetweenOrderByDatePaiementDesc(
                eq("salon-prestige"), eq(StatutPaiement.PAYE), any(), any()))
                .thenReturn(List.of(paiement));
        when(depenseRepository.findBySalonSlugAndStatutAndDateDepenseBetweenOrderByDateDepenseDesc(
                eq("salon-prestige"), eq(true), any(), any()))
                .thenReturn(List.of(depense));

        when(cloudinaryService.uploadExportRgpd(any(byte[].class), any(String.class)))
                .thenReturn(Map.of("secure_url", "https://cloudinary.com/rapport.pdf", "public_id", "pub_123"));

        when(demandeExportDonneesRepository.save(any(DemandeExportDonnees.class)))
                .thenAnswer(inv -> {
                    DemandeExportDonnees d = inv.getArgument(0);
                    d.setId(77L);
                    return d;
                });

        RapportFinancierFiltreDTORequest filtre = new RapportFinancierFiltreDTORequest(
                LocalDate.now().minusDays(5), LocalDate.now(), TypeRapportFinancier.ENTREES_SORTIES,
                null, null, FormatExportDonnees.PDF
        );

        RapportExportDTOResponse response = rapportFinancierSalonService.exporterRapportFinancier(
                "salon-prestige", "comptable@prestige.com", filtre);

        assertNotNull(response);
        assertEquals("https://cloudinary.com/rapport.pdf", response.urlTelechargement());
        assertNotNull(response.dateExpiration());
        assertTrue(response.dateExpiration().isAfter(LocalDateTime.now().plusHours(23)));
        verify(cloudinaryService).uploadExportRgpd(any(byte[].class), any(String.class));
        verify(demandeExportDonneesRepository).save(any(DemandeExportDonnees.class));
    }

    @Test
    void obtenirKpiFinanciers_succes() {
        when(salonRepository.findBySlug("salon-prestige")).thenReturn(Optional.of(salon));
        when(paiementRepository.totalPaiementsSalon("salon-prestige")).thenReturn(new BigDecimal("50000.00"));
        when(paiementRepository.totalPaiementsSalonEntreDates(eq("salon-prestige"), eq(StatutPaiement.PAYE), any(), any()))
                .thenReturn(new BigDecimal("20000.00"));
        when(depenseRepository.totalDepensesSalonEntreDates(eq("salon-prestige"), any(), any()))
                .thenReturn(new BigDecimal("8000.00"));
        when(paiementRepository.totalRemboursementsSalon("salon-prestige")).thenReturn(new BigDecimal("500.00"));
        List<Object[]> depList = new java.util.ArrayList<>();
        depList.add(new Object[]{CategorieDepense.LOYER, new BigDecimal("8000.00")});
        when(depenseRepository.totalDepensesParCategorieEntreDates(eq("salon-prestige"), any(), any()))
                .thenReturn(depList);
        when(sessionCaisseRepository.findByAffectationSalonSlugAndStatut("salon-prestige", com.kadi_aon.mon_salon.caisse.enums.StatutSessionCaisse.EN_COURS))
                .thenReturn(Optional.empty());


        KpiFinancierDTOResponse kpi = rapportFinancierSalonService.obtenirKpiFinanciers("salon-prestige");

        assertNotNull(kpi);
        assertEquals(new BigDecimal("50000.00"), kpi.chiffreAffairesTotal());
        assertEquals(new BigDecimal("20000.00"), kpi.revenusMoisEnCours());
        assertEquals(new BigDecimal("8000.00"), kpi.depensesMoisEnCours());
        assertEquals(new BigDecimal("12000.00"), kpi.beneficeNetMoisEnCours());
    }
}

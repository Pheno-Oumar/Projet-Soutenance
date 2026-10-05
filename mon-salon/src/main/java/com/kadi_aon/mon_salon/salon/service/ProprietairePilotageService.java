package com.kadi_aon.mon_salon.salon.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.depense.dto.DepenseDTOResponse;
import com.kadi_aon.mon_salon.depense.repository.DepenseRepository;
import com.kadi_aon.mon_salon.depense.service.DepenseSalonService;
import com.kadi_aon.mon_salon.facturation.dto.FactureDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.PaiementDTOResponse;
import com.kadi_aon.mon_salon.facturation.entity.Facture;
import com.kadi_aon.mon_salon.facturation.entity.Paiement;
import com.kadi_aon.mon_salon.facturation.repository.FactureRepository;
import com.kadi_aon.mon_salon.facturation.repository.PaiementRepository;
import com.kadi_aon.mon_salon.facturation.service.FacturationSalonService;
import com.kadi_aon.mon_salon.prestation.dto.PrestationDTOResponse;
import com.kadi_aon.mon_salon.prestation.entity.LignePrestation;
import com.kadi_aon.mon_salon.prestation.entity.Prestation;
import com.kadi_aon.mon_salon.prestation.enums.StatutPrestation;
import com.kadi_aon.mon_salon.prestation.repository.LignePrestationRepository;
import com.kadi_aon.mon_salon.prestation.repository.PrestationRepository;
import com.kadi_aon.mon_salon.prestation.service.PrestationSalonService;
import com.kadi_aon.mon_salon.profilcapillaire.dto.ProfilCapillaireDTOResponse;
import com.kadi_aon.mon_salon.profilcapillaire.mapper.ProfilCapillaireDTOResponseMapper;
import com.kadi_aon.mon_salon.profilcapillaire.repository.ProfilCapillaireRepository;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.entity.RendezVous;
import com.kadi_aon.mon_salon.rendezvous.enums.StatutRendezVous;
import com.kadi_aon.mon_salon.rendezvous.mapper.RendezVousDTOResponseMapper;
import com.kadi_aon.mon_salon.rendezvous.repository.RendezVousRepository;
import com.kadi_aon.mon_salon.salon.dto.pilotage.ClientSalonResumeDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.pilotage.FicheClientCompleteDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.pilotage.FicheClientManagerDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.pilotage.KpiSalonDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.pilotage.PerformanceCoiffeurDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.pilotage.StockSyntheseDTOResponse;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;
import com.kadi_aon.mon_salon.stock.entity.Produit;
import com.kadi_aon.mon_salon.stock.entity.StockProduit;
import com.kadi_aon.mon_salon.stock.repository.ProduitRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProprietairePilotageService {

    private final SalonRepository salonRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final CompteRepository compteRepository;
    private final LignePrestationRepository lignePrestationRepository;
    private final RendezVousRepository rendezVousRepository;
    private final PrestationRepository prestationRepository;
    private final PaiementRepository paiementRepository;
    private final DepenseRepository depenseRepository;
    private final DepenseSalonService depenseSalonService;
    private final ProduitRepository produitRepository;
    private final ProfilCapillaireRepository profilCapillaireRepository;
    private final FactureRepository factureRepository;

    private final FacturationSalonService facturationSalonService;
    private final PrestationSalonService prestationSalonService;
    private final RendezVousDTOResponseMapper rendezVousMapper;
    private final ProfilCapillaireDTOResponseMapper profilCapillaireMapper;

    public List<PerformanceCoiffeurDTOResponse> obtenirPerformancesCoiffeurs(String slugSalon) {
        validerExistenceSalon(slugSalon);

        List<Compte> coiffeurs = affectationSalonRepository.findComptesBySalonSlugAndRole(slugSalon, TypeRoleSalon.COIFFEUR);

        return coiffeurs.stream().map(c -> {
            List<LignePrestation> lignes = lignePrestationRepository.findLignesTermineesByCoiffeurAndSalon(c.getId(), slugSalon);
            long nbPrestations = lignes.size();
            BigDecimal totalCA = lignes.stream()
                    .map(LignePrestation::getPrixReel)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return PerformanceCoiffeurDTOResponse.builder()
                    .coiffeurId(c.getId())
                    .nom(c.getNom())
                    .prenom(c.getPrenom())
                    .email(c.getEmail())
                    .nombrePrestations(nbPrestations)
                    .chiffreAffaires(totalCA)
                    .build();
        }).toList();
    }

    public KpiSalonDTOResponse obtenirKpiSalon(String slugSalon) {
        Salon salon = salonRepository.findBySlug(slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Salon non trouvé avec le slug : " + slugSalon));

        long nbClients = affectationSalonRepository.countBySalonSlugAndRole(slugSalon, TypeRoleSalon.CLIENT);
        long nbCoiffeurs = affectationSalonRepository.countBySalonSlugAndRole(slugSalon, TypeRoleSalon.COIFFEUR);

        long nbRdvTotal = rendezVousRepository.countBySalonSlug(slugSalon);
        long nbRdvTermines = rendezVousRepository.countBySalonSlugAndStatut(slugSalon, StatutRendezVous.TERMINE);
        long nbRdvAnnules = rendezVousRepository.countBySalonSlugAndStatut(slugSalon, StatutRendezVous.ANNULE);

        long nbPrestationsTotal = prestationRepository.countBySalonSlug(slugSalon);
        long nbPrestationsTerminees = prestationRepository.countBySalonSlugAndStatut(slugSalon, StatutPrestation.TERMINEE);

        BigDecimal totalRevenus = paiementRepository.totalPaiementsSalon(slugSalon);
        if (totalRevenus == null) {
            totalRevenus = BigDecimal.ZERO;
        }

        BigDecimal totalDepenses = depenseRepository.totalDepensesSalon(slugSalon);
        if (totalDepenses == null) {
            totalDepenses = BigDecimal.ZERO;
        }

        BigDecimal beneficeNet = totalRevenus.subtract(totalDepenses);

        return KpiSalonDTOResponse.builder()
                .slugSalon(salon.getSlug())
                .nomSalon(salon.getNom())
                .nombreClients(nbClients)
                .nombreCoiffeurs(nbCoiffeurs)
                .nombreRendezVousTotal(nbRdvTotal)
                .nombreRendezVousTermines(nbRdvTermines)
                .nombreRendezVousAnnules(nbRdvAnnules)
                .nombrePrestationsTotal(nbPrestationsTotal)
                .nombrePrestationsTerminees(nbPrestationsTerminees)
                .totalRevenus(totalRevenus)
                .totalDepenses(totalDepenses)
                .beneficeNet(beneficeNet)
                .build();
    }

    public List<PaiementDTOResponse> obtenirRevenusDetailles(String slugSalon) {
        validerExistenceSalon(slugSalon);
        List<Paiement> paiements = paiementRepository.findBySalonSlugOrderByDatePaiementDesc(slugSalon);
        return paiements.stream()
                .map(facturationSalonService::mapPaiementToResponse)
                .toList();
    }

    public List<DepenseDTOResponse> obtenirDepensesDetailles(String slugSalon) {
        return depenseSalonService.listerDepenses(slugSalon, null, null);
    }

    public List<StockSyntheseDTOResponse> obtenirSyntheseStock(String slugSalon) {
        validerExistenceSalon(slugSalon);
        List<Produit> produits = produitRepository.findByCategorieSalonSlug(slugSalon);

        return produits.stream().map(p -> {
            StockProduit stock = p.getStock();
            int dispo = stock != null ? stock.getQuantiteDisponible() : 0;
            int min = stock != null ? stock.getSeuilMinimum() : 0;
            Integer max = stock != null ? stock.getSeuilMaximum() : null;
            boolean alerte = dispo <= min;

            return StockSyntheseDTOResponse.builder()
                    .produitId(p.getId())
                    .produitNom(p.getNom())
                    .categorieNom(p.getCategorie() != null ? p.getCategorie().getNom() : null)
                    .prixVente(p.getPrixVente())
                    .quantiteDisponible(dispo)
                    .seuilMinimum(min)
                    .seuilMaximum(max)
                    .alerteStockBas(alerte)
                    .build();
        }).toList();
    }

    public List<ClientSalonResumeDTOResponse> listerClientsDuSalon(String slugSalon) {
        validerExistenceSalon(slugSalon);
        List<Compte> clients = affectationSalonRepository.findComptesBySalonSlugAndRole(slugSalon, TypeRoleSalon.CLIENT);

        return clients.stream().map(c -> ClientSalonResumeDTOResponse.builder()
                .clientId(c.getId())
                .nom(c.getNom())
                .prenom(c.getPrenom())
                .email(c.getEmail())
                .telephone(c.getTelephone())
                .dateNaissance(c.getDateNaissance())
                .dateInscription(c.getDateCreation())
                .hasProfilCapillaire(profilCapillaireRepository.existsByCompteId(c.getId()))
                .build()
        ).toList();
    }

    public FicheClientCompleteDTOResponse obtenirFicheClientComplete(String slugSalon, Long clientId) {
        validerExistenceSalon(slugSalon);

        boolean isClient = affectationSalonRepository.isCompteRoleDuSalon(slugSalon, clientId, TypeRoleSalon.CLIENT);
        if (!isClient) {
            throw new EntityNotFoundException("Client non trouvé pour ce salon avec l'id : " + clientId);
        }

        Compte client = compteRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Compte client non trouvé avec l'id : " + clientId));

        ProfilCapillaireDTOResponse profilCapillaire = profilCapillaireRepository.findByCompteId(clientId)
                .map(profilCapillaireMapper::apply)
                .orElse(null);

        List<RendezVous> rdvList = rendezVousRepository.findByClientIdAndSalonSlugOrderByDateHeurePrevueDesc(clientId, slugSalon);
        List<RendezVousDTOResponse> rdvDTOs = rdvList.stream().map(rendezVousMapper::apply).toList();

        List<Prestation> prestationList = prestationRepository.findByClientIdAndSalonSlugOrderByDateHeureDebutDesc(clientId, slugSalon);
        List<PrestationDTOResponse> prestationDTOs = prestationList.stream().map(prestationSalonService::mapToResponse).toList();

        List<Facture> factureList = factureRepository.findByPrestationClientIdAndPrestationSalonSlugOrderByDateEmissionDesc(clientId, slugSalon);
        List<FactureDTOResponse> factureDTOs = factureList.stream().map(facturationSalonService::mapFactureToResponse).toList();

        List<Paiement> paiementList = paiementRepository.findByClientIdAndSalonSlugOrderByDatePaiementDesc(clientId, slugSalon);
        List<PaiementDTOResponse> paiementDTOs = paiementList.stream().map(facturationSalonService::mapPaiementToResponse).toList();

        return FicheClientCompleteDTOResponse.builder()
                .clientId(client.getId())
                .nom(client.getNom())
                .prenom(client.getPrenom())
                .email(client.getEmail())
                .telephone(client.getTelephone())
                .dateNaissance(client.getDateNaissance())
                .profilCapillaire(profilCapillaire)
                .rendezVous(rdvDTOs)
                .prestations(prestationDTOs)
                .factures(factureDTOs)
                .paiements(paiementDTOs)
                .build();
    }

    public FicheClientManagerDTOResponse obtenirFicheClientPourManager(String slugSalon, Long clientId) {
        validerExistenceSalon(slugSalon);

        boolean isClient = affectationSalonRepository.isCompteRoleDuSalon(slugSalon, clientId, TypeRoleSalon.CLIENT);
        if (!isClient) {
            throw new EntityNotFoundException("Client non trouvé pour ce salon avec l'id : " + clientId);
        }

        Compte client = compteRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Compte client non trouvé avec l'id : " + clientId));

        ProfilCapillaireDTOResponse profilCapillaire = profilCapillaireRepository.findByCompteId(clientId)
                .map(profilCapillaireMapper::apply)
                .orElse(null);

        List<RendezVous> rdvList = rendezVousRepository.findByClientIdAndSalonSlugOrderByDateHeurePrevueDesc(clientId, slugSalon);
        List<RendezVousDTOResponse> rdvDTOs = rdvList.stream().map(rendezVousMapper::apply).toList();

        List<Prestation> prestationList = prestationRepository.findByClientIdAndSalonSlugOrderByDateHeureDebutDesc(clientId, slugSalon);
        List<PrestationDTOResponse> prestationDTOs = prestationList.stream().map(prestationSalonService::mapToResponse).toList();

        return FicheClientManagerDTOResponse.builder()
                .clientId(client.getId())
                .nom(client.getNom())
                .prenom(client.getPrenom())
                .email(client.getEmail())
                .telephone(client.getTelephone())
                .dateNaissance(client.getDateNaissance())
                .profilCapillaire(profilCapillaire)
                .rendezVous(rdvDTOs)
                .prestations(prestationDTOs)
                .build();
    }

    private void validerExistenceSalon(String slugSalon) {
        if (!salonRepository.existsBySlug(slugSalon)) {
            throw new EntityNotFoundException("Salon non trouvé avec le slug : " + slugSalon);
        }
    }
}

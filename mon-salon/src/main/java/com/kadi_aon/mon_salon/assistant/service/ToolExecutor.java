package com.kadi_aon.mon_salon.assistant.service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kadi_aon.mon_salon.account.dto.CompteDTOResponse;
import com.kadi_aon.mon_salon.account.dto.CompteUpdateDTORequest;
import com.kadi_aon.mon_salon.account.service.CompteService;
import com.kadi_aon.mon_salon.assistant.context.AssistantContext;
import com.kadi_aon.mon_salon.assistant.dto.AssistantActionUi;
import com.kadi_aon.mon_salon.assistant.dto.AssistantCarte;
import com.kadi_aon.mon_salon.assistant.dto.PendingActionDTO;
import com.kadi_aon.mon_salon.assistant.service.PendingActionService.PendingAction;
import com.kadi_aon.mon_salon.avis.dto.AvisPrestationCreateDTORequest;
import com.kadi_aon.mon_salon.avis.dto.AvisPrestationDTOResponse;
import com.kadi_aon.mon_salon.avis.dto.AvisPrestationUpdateDTORequest;
import com.kadi_aon.mon_salon.avis.dto.AvisSalonCreateDTORequest;
import com.kadi_aon.mon_salon.avis.dto.AvisSalonDTOResponse;
import com.kadi_aon.mon_salon.avis.dto.AvisSalonUpdateDTORequest;
import com.kadi_aon.mon_salon.avis.service.AvisSalonService;
import com.kadi_aon.mon_salon.coiffeur.dto.ProfilCoiffeurDTOResponse;
import com.kadi_aon.mon_salon.coiffeur.service.CoiffeurSalonService;
import com.kadi_aon.mon_salon.facturation.dto.PaiementDTOResponse;
import com.kadi_aon.mon_salon.facturation.service.FacturationSalonService;
import com.kadi_aon.mon_salon.favori.dto.FavoriCoiffeurDTOResponse;
import com.kadi_aon.mon_salon.favori.dto.FavoriSalonDTOResponse;
import com.kadi_aon.mon_salon.favori.service.FavoriSalonService;
import com.kadi_aon.mon_salon.prestation.dto.PrestationDTOResponse;
import com.kadi_aon.mon_salon.prestation.service.PrestationSalonService;
import com.kadi_aon.mon_salon.profilcapillaire.dto.ProfilCapillaireDTORequest;
import com.kadi_aon.mon_salon.profilcapillaire.dto.ProfilCapillaireDTOResponse;
import com.kadi_aon.mon_salon.profilcapillaire.service.ProfilCapillaireService;
import com.kadi_aon.mon_salon.realisation.dto.CommentaireCreateDTORequest;
import com.kadi_aon.mon_salon.realisation.dto.CommentaireDTOResponse;
import com.kadi_aon.mon_salon.realisation.dto.KadysRealisationDTOResponse;
import com.kadi_aon.mon_salon.realisation.dto.RealisationDTOResponse;
import com.kadi_aon.mon_salon.realisation.service.KadysInteractionService;
import com.kadi_aon.mon_salon.realisation.service.RealisationSalonService;
import com.kadi_aon.mon_salon.reclamation.dto.ReclamationCreateDTORequest;
import com.kadi_aon.mon_salon.reclamation.dto.ReclamationDTOResponse;
import com.kadi_aon.mon_salon.reclamation.service.ReclamationSalonService;
import com.kadi_aon.mon_salon.rendezvous.dto.CreneauDisponibleDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.DisponibiliteSearchDTORequest;
import com.kadi_aon.mon_salon.rendezvous.dto.LigneCreneauDTO;
import com.kadi_aon.mon_salon.rendezvous.dto.LigneRendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousAnnulationDTORequest;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousCreateDTORequest;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.service.DisponibiliteService;
import com.kadi_aon.mon_salon.rendezvous.service.RendezVousService;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeExportDTORequest;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeExportDTOResponse;
import com.kadi_aon.mon_salon.rgpd.enums.FormatExportDonnees;
import com.kadi_aon.mon_salon.rgpd.service.RgpdClientService;
import com.kadi_aon.mon_salon.salon.dto.HoraireOuvertureDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.SalonDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.ServiceSalonDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.VarianteServiceDTOResponse;
import com.kadi_aon.mon_salon.salon.enums.JourSemaine;
import com.kadi_aon.mon_salon.salon.entity.ServiceSalon;
import com.kadi_aon.mon_salon.salon.repository.ServiceSalonRepository;
import com.kadi_aon.mon_salon.salon.service.ExploreSalonService;
import com.kadi_aon.mon_salon.salon.service.HoraireSalonService;
import com.kadi_aon.mon_salon.salon.service.ServiceSalonService;
import com.kadi_aon.mon_salon.stock.dto.AjoutPanierDTORequest;
import com.kadi_aon.mon_salon.stock.dto.CategorieProduitDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.CommandeDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.ModificationQuantiteDTORequest;
import com.kadi_aon.mon_salon.stock.dto.PanierDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.ProduitDTOResponse;
import com.kadi_aon.mon_salon.stock.service.CommandeSalonService;
import com.kadi_aon.mon_salon.stock.service.PanierService;
import com.kadi_aon.mon_salon.stock.service.StockSalonService;
import com.kadi_aon.mon_salon.story.service.StorySalonService;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToolExecutor {

    private final ExploreSalonService exploreSalonService;
    private final ServiceSalonService serviceSalonService;
    private final StockSalonService stockSalonService;
    private final AvisSalonService avisSalonService;
    private final RealisationSalonService realisationSalonService;
    private final DisponibiliteService disponibiliteService;
    private final CoiffeurSalonService coiffeurSalonService;
    private final StorySalonService storySalonService;
    private final HoraireSalonService horaireSalonService;
    private final RendezVousService rendezVousService;
    private final PrestationSalonService prestationSalonService;
    private final FacturationSalonService facturationSalonService;
    private final FavoriSalonService favoriSalonService;
    private final PanierService panierService;
    private final CommandeSalonService commandeSalonService;
    private final ReclamationSalonService reclamationSalonService;
    private final ProfilCapillaireService profilCapillaireService;
    private final KadysInteractionService kadysInteractionService;
    private final CompteService compteService;
    private final RgpdClientService rgpdClientService;
    private final ServiceSalonRepository serviceSalonRepository;
    private final PendingActionService pendingActionService;
    private final ToolCatalog toolCatalog;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    @Value
    @Builder
    public static class ToolExecutionResult {
        Object rawResult;
        @Builder.Default
        List<AssistantCarte> cartes = Collections.emptyList();
        @Builder.Default
        List<AssistantActionUi> actionsUi = Collections.emptyList();
        PendingActionDTO pendingAction;
    }

    public ToolExecutionResult execute(String name, Map<String, Object> args, AssistantContext ctx, String sessionId) {
        if (!toolCatalog.isAllowed(name, ctx.getType())) {
            log.warn("Tentative d'accès non autorisé à l'outil '{}' dans le contexte '{}'", name, ctx.getType());
            return ToolExecutionResult.builder()
                    .rawResult(Map.of("erreur", "OUTIL_NON_AUTORISE", "message",
                            "Cet outil n'est pas autorisé dans ce contexte."))
                    .cartes(Collections.emptyList())
                    .actionsUi(Collections.emptyList())
                    .build();
        }

        try {
            if (name.startsWith("salon_")) {
                return executeSalonTool(name, args, ctx);
            } else if (name.startsWith("ui_")) {
                return executeUiTool(name, args, ctx);
            } else if (name.startsWith("client_salon_")) {
                return executeClientSalonTool(name, args, ctx, sessionId);
            } else if (name.startsWith("client_plateforme_")) {
                return executeClientPlateformeTool(name, args, ctx, sessionId);
            } else if (name.startsWith("plateforme_")) {
                return executePlateformeTool(name, args, ctx);
            } else if (name.startsWith("realisation_") || name.startsWith("client_")) {
                return executeSharedTool(name, args, ctx, sessionId);
            } else {
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("erreur", "OUTIL_INCONNU", "message", "Outil introuvable."))
                        .build();
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'exécution de l'outil '{}': {}", name, e.getMessage(), e);
            return ToolExecutionResult.builder()
                    .rawResult(Map.of("erreur", "ERREUR_EXECUTION", "message",
                            e.getMessage() != null ? e.getMessage() : "Une erreur est survenue."))
                    .cartes(Collections.emptyList())
                    .actionsUi(Collections.emptyList())
                    .build();
        }
    }

    // ==========================================
    // 1. OUTILS SALON (SV & SC)
    // ==========================================
    private ToolExecutionResult executeSalonTool(String name, Map<String, Object> args, AssistantContext ctx) {
        String slug = ctx.getSlugSalon();
        if (slug == null) {
            return errorResult("SLUG_MANQUANT", "Cette action requiert un contexte de salon.");
        }

        switch (name) {
            case "salon_obtenir_infos" -> {
                SalonDTOResponse salon = exploreSalonService.getSalonDetail(slug);
                Map<String, Object> map = new HashMap<>();
                map.put("nom", salon.nom());
                map.put("description", salon.description());
                map.put("adresse", salon.adresse());
                map.put("telephone", salon.telephone());
                map.put("email", salon.email());
                map.put("latitude", salon.latitude());
                map.put("longitude", salon.longitude());
                if (salon.latitude() != null && salon.longitude() != null) {
                    map.put("lienItineraire", "https://www.google.com/maps/dir/?api=1&destination=" + salon.latitude()
                            + "," + salon.longitude());
                }
                return ToolExecutionResult.builder()
                        .rawResult(map)
                        .cartes(List.of(new AssistantCarte("SALON", salon.nom(), map)))
                        .build();
            }
            case "salon_obtenir_horaires" -> {
                List<HoraireOuvertureDTOResponse> horaires = horaireSalonService.listerHoraires(slug);
                LocalTime nowTime = LocalTime.now();
                JourSemaine currentDay = JourSemaine.from(LocalDate.now().getDayOfWeek());
                boolean estOuvert = false;
                LocalTime fermeA = null;

                for (HoraireOuvertureDTOResponse h : horaires) {
                    if (h.jourSemaine() == currentDay && Boolean.TRUE.equals(h.actif())) {
                        if (h.heureOuverture() != null && h.heureFermeture() != null
                                && nowTime.isAfter(h.heureOuverture()) && nowTime.isBefore(h.heureFermeture())) {
                            estOuvert = true;
                            fermeA = h.heureFermeture();
                        }
                    }
                }

                Map<String, Object> res = new HashMap<>();
                res.put("horaires", horaires);
                res.put("estOuvertMaintenant", estOuvert);
                res.put("fermeAujourdhuiA", fermeA != null ? fermeA.toString() : null);

                return ToolExecutionResult.builder()
                        .rawResult(res)
                        .cartes(List.of(new AssistantCarte("HORAIRES", "Horaires du salon", res)))
                        .build();
            }
            case "salon_lister_services" -> {
                List<ServiceSalonDTOResponse> services = serviceSalonService.listerServices(slug, true);
                String recherche = (String) args.get("recherche");
                Integer prixMax = getInteger(args.get("prixMax"));
                Integer dureeMax = getInteger(args.get("dureeMaxMinutes"));

                List<Map<String, Object>> filtered = new ArrayList<>();
                for (ServiceSalonDTOResponse s : services) {
                    boolean match = true;
                    if (recherche != null && !recherche.isBlank()) {
                        String q = recherche.toLowerCase();
                        match = (s.nom() != null && s.nom().toLowerCase().contains(q)) ||
                                (s.description() != null && s.description().toLowerCase().contains(q));
                    }
                    if (match && s.variantes() != null) {
                        int prixMin = s.variantes().stream()
                                .map(VarianteServiceDTOResponse::prix)
                                .filter(Objects::nonNull)
                                .mapToInt(BigDecimal::intValue)
                                .min().orElse(0);
                        int dureeMin = s.variantes().stream().mapToInt(VarianteServiceDTOResponse::dureeMinutes).min()
                                .orElse(0);
                        if (prixMax != null && prixMin > prixMax)
                            match = false;
                        if (dureeMax != null && dureeMin > dureeMax)
                            match = false;
                        if (match) {
                            filtered.add(Map.of(
                                    "serviceId", s.id(),
                                    "nom", s.nom(),
                                    "description", s.description() != null ? s.description() : "",
                                    "prixMin", prixMin,
                                    "dureeMin", dureeMin,
                                    "nbVariantes", s.variantes().size()));
                        }
                    }
                }
                return ToolExecutionResult.builder()
                        .rawResult(truncateList(filtered))
                        .cartes(List.of(new AssistantCarte("SERVICES", "Prestations disponibles", filtered)))
                        .build();
            }
            case "salon_obtenir_service_detail" -> {
                Long serviceId = getLong(args.get("serviceId"));
                List<ServiceSalonDTOResponse> services = serviceSalonService.listerServices(slug, true);
                ServiceSalonDTOResponse found = services.stream()
                        .filter(s -> s.id().equals(serviceId))
                        .findFirst()
                        .orElse(null);
                if (found == null) {
                    return errorResult("SERVICE_INTROUVABLE", "Prestation introuvable avec l'ID " + serviceId);
                }
                return ToolExecutionResult.builder()
                        .rawResult(found)
                        .cartes(List.of(new AssistantCarte("VARIANTES", found.nom(), found)))
                        .build();
            }
            case "salon_rechercher_variantes" -> {
                String recherche = (String) args.get("recherche");
                Integer prixMax = getInteger(args.get("prixMax"));
                Integer dureeMax = getInteger(args.get("dureeMaxMinutes"));

                List<ServiceSalonDTOResponse> services = serviceSalonService.listerServices(slug, true);
                List<Map<String, Object>> variantes = new ArrayList<>();
                for (ServiceSalonDTOResponse s : services) {
                    if (s.variantes() != null) {
                        for (VarianteServiceDTOResponse v : s.variantes()) {
                            boolean match = true;
                            if (recherche != null && !recherche.isBlank()) {
                                String q = recherche.toLowerCase();
                                match = (v.nom() != null && v.nom().toLowerCase().contains(q)) ||
                                        (s.nom() != null && s.nom().toLowerCase().contains(q));
                            }
                            if (prixMax != null && v.prix() != null
                                    && v.prix().compareTo(BigDecimal.valueOf(prixMax)) > 0)
                                match = false;
                            if (dureeMax != null && v.dureeMinutes() > dureeMax)
                                match = false;
                            if (match) {
                                variantes.add(Map.of(
                                        "varianteId", v.id(),
                                        "varianteNom", v.nom(),
                                        "serviceId", s.id(),
                                        "serviceNom", s.nom(),
                                        "prix", v.prix(),
                                        "dureeMinutes", v.dureeMinutes()));
                            }
                        }
                    }
                }
                return ToolExecutionResult.builder()
                        .rawResult(truncateList(variantes))
                        .cartes(List.of(new AssistantCarte("VARIANTES", "Formules correspondantes", variantes)))
                        .build();
            }
            case "salon_calculer_devis" -> {
                List<Long> varianteIds = getLongList(args.get("varianteIds"));
                List<ServiceSalonDTOResponse> services = serviceSalonService.listerServices(slug, true);
                List<Map<String, Object>> lignes = new ArrayList<>();
                int totalPrix = 0;
                int totalDuree = 0;

                for (Long vid : varianteIds) {
                    for (ServiceSalonDTOResponse s : services) {
                        if (s.variantes() != null) {
                            for (VarianteServiceDTOResponse v : s.variantes()) {
                                if (v.id().equals(vid)) {
                                    lignes.add(Map.of(
                                            "varianteId", v.id(),
                                            "nom", s.nom() + " - " + v.nom(),
                                            "prix", v.prix(),
                                            "dureeMinutes", v.dureeMinutes()));
                                    totalPrix += (v.prix() != null ? v.prix().intValue() : 0);
                                    totalDuree += v.dureeMinutes();
                                }
                            }
                        }
                    }
                }
                Map<String, Object> devis = Map.of(
                        "lignes", lignes,
                        "prixTotal", totalPrix,
                        "dureeTotaleMinutes", totalDuree);
                return ToolExecutionResult.builder()
                        .rawResult(devis)
                        .cartes(List.of(new AssistantCarte("DEVIS", "Estimation tarifaire", devis)))
                        .build();
            }
            case "salon_verifier_disponibilites" -> {
                String dateStr = (String) args.get("date");
                List<Long> varianteIds = getLongList(args.get("varianteIds"));
                LocalDate date = LocalDate.parse(dateStr);
                DisponibiliteSearchDTORequest search = new DisponibiliteSearchDTORequest(date, varianteIds, null, null);
                List<CreneauDisponibleDTOResponse> creneaux = disponibiliteService.calculerDisponibilites(slug, search);

                Long coiffeurId = getLong(args.get("coiffeurId"));
                if (coiffeurId != null) {
                    creneaux = creneaux.stream().filter(c -> c.lignesProposees() != null && c.lignesProposees().stream()
                            .anyMatch(l -> coiffeurId.equals(l.coiffeurAffectationId()))).toList();
                }

                Map<String, Object> res = Map.of(
                        "date", dateStr,
                        "total", creneaux.size(),
                        "creneaux", creneaux);
                return ToolExecutionResult.builder()
                        .rawResult(res)
                        .cartes(List.of(new AssistantCarte("CRENEAUX", "Créneaux du " + dateStr, res)))
                        .build();
            }
            case "salon_trouver_prochain_creneau" -> {
                List<Long> varianteIds = getLongList(args.get("varianteIds"));
                String aPartirDeStr = (String) args.get("aPartirDe");
                LocalDate depart = aPartirDeStr != null ? LocalDate.parse(aPartirDeStr) : LocalDate.now();
                int joursMax = Optional.ofNullable(getInteger(args.get("joursMax"))).orElse(7);
                int limit = Optional.ofNullable(getInteger(args.get("nombreResultats"))).orElse(3);

                List<Map<String, Object>> propositions = new ArrayList<>();
                for (int i = 0; i < joursMax && propositions.size() < limit; i++) {
                    LocalDate current = depart.plusDays(i);
                    DisponibiliteSearchDTORequest search = new DisponibiliteSearchDTORequest(current, varianteIds, null,
                            null);
                    try {
                        List<CreneauDisponibleDTOResponse> creneaux = disponibiliteService.calculerDisponibilites(slug,
                                search);
                        for (CreneauDisponibleDTOResponse c : creneaux) {
                            Long cId = (c.lignesProposees() != null && !c.lignesProposees().isEmpty())
                                    ? c.lignesProposees().get(0).coiffeurAffectationId()
                                    : null;
                            String cNom = (c.lignesProposees() != null && !c.lignesProposees().isEmpty())
                                    ? ((c.lignesProposees().get(0).coiffeurPrenom() != null
                                            ? c.lignesProposees().get(0).coiffeurPrenom()
                                            : "")
                                            + " "
                                            + (c.lignesProposees().get(0).coiffeurNom() != null
                                                    ? c.lignesProposees().get(0).coiffeurNom()
                                                    : ""))
                                            .trim()
                                    : "Coiffeur disponible";

                            propositions.add(Map.of(
                                    "date", current.toString(),
                                    "heureDebut", c.heureDebut().toString(),
                                    "heureFin", c.heureFin().toString(),
                                    "coiffeurId", cId != null ? cId : 0L,
                                    "coiffeurNom", cNom.isBlank() ? "Coiffeur disponible" : cNom));
                            if (propositions.size() >= limit)
                                break;
                        }
                    } catch (Exception ignored) {
                        // Jour fermé ou indisponible
                    }
                }
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("propositions", propositions))
                        .cartes(List.of(new AssistantCarte("CRENEAUX", "Prochains créneaux disponibles",
                                Map.of("creneaux", propositions))))
                        .build();
            }
            case "salon_lister_coiffeurs" -> {
                List<ProfilCoiffeurDTOResponse> coiffeurs = coiffeurSalonService.listerCoiffeursVitrine(slug);
                return ToolExecutionResult.builder()
                        .rawResult(coiffeurs)
                        .cartes(List.of(new AssistantCarte("COIFFEURS", "L'équipe du salon", coiffeurs)))
                        .build();
            }
            case "salon_obtenir_coiffeur_detail" -> {
                Long coiffeurId = getLong(args.get("coiffeurId"));
                List<ProfilCoiffeurDTOResponse> list = coiffeurSalonService.listerCoiffeursVitrine(slug);
                ProfilCoiffeurDTOResponse found = list.stream().filter(c -> c.id().equals(coiffeurId)).findFirst()
                        .orElse(null);
                if (found == null)
                    return errorResult("COIFFEUR_INTROUVABLE", "Coiffeur introuvable");
                String coiffeurNom = (found.nomAffichage() != null && !found.nomAffichage().isBlank())
                        ? found.nomAffichage()
                        : ((found.coiffeurPrenom() != null ? found.coiffeurPrenom() : "") + " "
                                + (found.coiffeurNom() != null ? found.coiffeurNom() : "")).trim();
                return ToolExecutionResult.builder()
                        .rawResult(found)
                        .cartes(List.of(new AssistantCarte("COIFFEUR", coiffeurNom.isBlank() ? "Coiffeur" : coiffeurNom,
                                found)))
                        .build();
            }
            case "salon_lister_categories_produits" -> {
                List<CategorieProduitDTOResponse> cats = stockSalonService.listerCategories(slug, true);
                return ToolExecutionResult.builder()
                        .rawResult(cats)
                        .cartes(List.of(new AssistantCarte("CATEGORIES_PRODUITS", "Rayons boutique", cats)))
                        .build();
            }
            case "salon_rechercher_produits" -> {
                Long catId = getLong(args.get("categorieId"));
                String q = (String) args.get("recherche");
                Integer pMax = getInteger(args.get("prixMax"));
                List<ProduitDTOResponse> prods = stockSalonService.listerProduits(slug, catId, true);

                List<ProduitDTOResponse> filtered = prods.stream()
                        .filter(p -> q == null || q.isBlank() || p.nom().toLowerCase().contains(q.toLowerCase()))
                        .filter(p -> pMax == null
                                || (p.prixVente() != null && p.prixVente().compareTo(BigDecimal.valueOf(pMax)) <= 0))
                        .toList();

                return ToolExecutionResult.builder()
                        .rawResult(truncateList(filtered))
                        .cartes(List.of(new AssistantCarte("PRODUITS", "Soins & Cosmétiques", filtered)))
                        .build();
            }
            case "salon_obtenir_produit_detail" -> {
                Long produitId = getLong(args.get("produitId"));
                List<ProduitDTOResponse> prods = stockSalonService.listerProduits(slug, null, true);
                ProduitDTOResponse found = prods.stream().filter(p -> p.id().equals(produitId)).findFirst()
                        .orElse(null);
                if (found == null)
                    return errorResult("PRODUIT_INTROUVABLE", "Produit introuvable");
                return ToolExecutionResult.builder()
                        .rawResult(found)
                        .cartes(List.of(new AssistantCarte("PRODUIT", found.nom(), found)))
                        .build();
            }
            case "salon_recommander_produits" -> {
                String besoin = (String) args.get("besoin");
                List<ProduitDTOResponse> prods = stockSalonService.listerProduits(slug, null, true);
                List<Map<String, Object>> recos = new ArrayList<>();
                for (ProduitDTOResponse p : prods) {
                    if (recos.size() >= 3)
                        break;
                    recos.add(Map.of(
                            "produitId", p.id(),
                            "nom", p.nom(),
                            "prixVente", p.prixVente(),
                            "raison", "Formule enrichie adaptée au besoin capillaire : "
                                    + (besoin != null ? besoin : "soin profond")));
                }
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("profilUtilise", ctx.isAuthenticated(), "produits", recos))
                        .cartes(List.of(new AssistantCarte("PRODUITS", "Recommandations capillaires", recos)))
                        .build();
            }
            case "salon_obtenir_avis" -> {
                List<AvisSalonDTOResponse> avis = avisSalonService.listerAvisPubliesSalon(slug);
                double moyenne = avis.stream().mapToInt(AvisSalonDTOResponse::note).average().orElse(5.0);
                Map<String, Object> data = Map.of(
                        "noteMoyenne", moyenne,
                        "total", avis.size(),
                        "avis", avis.stream().limit(5).toList());
                return ToolExecutionResult.builder()
                        .rawResult(data)
                        .cartes(List.of(new AssistantCarte("AVIS", "Avis clients vérifiés", data)))
                        .build();
            }
            case "salon_lister_realisations" -> {
                List<RealisationDTOResponse> realisations = realisationSalonService.listerRealisationsPubliees(slug);
                return ToolExecutionResult.builder()
                        .rawResult(truncateList(realisations))
                        .cartes(List.of(new AssistantCarte("REALISATIONS", "Réalisations en salon", realisations)))
                        .build();
            }
            case "salon_lister_stories" -> {
                var stories = storySalonService.listerStoriesSalon(slug);
                return ToolExecutionResult.builder()
                        .rawResult(stories)
                        .build();
            }
            case "salon_obtenir_politiques" -> {
                String sujet = (String) args.get("sujet");
                Map<String, String> regles = Map.of(
                        "ANNULATION",
                        "Annulation gratuite jusqu'à 2 heures avant le rendez-vous. Au-delà, le créneau est considéré comme dû.",
                        "RETARD",
                        "Au-delà de 15 minutes de retard sans prévenance, la prestation pourra être adaptée ou reportée.",
                        "PAIEMENT", "Paiement sur place au salon par Espèces, Mobile Money ou Carte Bancaire.",
                        "CLICK_COLLECT",
                        "Retrait en boutique disponible sous 48h sur présentation de votre code de retrait.",
                        "ACOMPTE", "Aucun acompte n'est exigé en ligne.",
                        "TOUS",
                        "Politique globale : respect des horaires, hygiène garantie et paiement sécurisé au salon.");
                String texte = regles.getOrDefault(sujet, regles.get("TOUS"));
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("sujet", sujet, "regle", texte))
                        .build();
            }
            default -> {
                return errorResult("OUTIL_NON_GERE", "Outil salon non implémenté : " + name);
            }
        }
    }

    // ==========================================
    // 2. OUTILS UI (FRONTEND ANGULAR)
    // ==========================================
    private ToolExecutionResult executeUiTool(String name, Map<String, Object> args, AssistantContext ctx) {
        AssistantActionUi action = new AssistantActionUi(name, args);
        return ToolExecutionResult.builder()
                .rawResult(Map.of("statut", "ACTION_UI_PROGRAMMEE", "action", name))
                .actionsUi(List.of(action))
                .build();
    }

    // ==========================================
    // 3. OUTILS CLIENT SALON (SC)
    // ==========================================
    private ToolExecutionResult executeClientSalonTool(String name, Map<String, Object> args, AssistantContext ctx,
            String sessionId) {
        String slug = ctx.getSlugSalon();
        String email = ctx.getClientEmail();

        switch (name) {
            case "client_salon_lister_mes_rdv" -> {
                List<RendezVousDTOResponse> rdvs = rendezVousService.listerMesRendezVous(slug, email);
                return ToolExecutionResult.builder()
                        .rawResult(truncateList(rdvs))
                        .cartes(List.of(new AssistantCarte("RDV_LISTE", "Vos rendez-vous", rdvs)))
                        .build();
            }
            case "client_salon_obtenir_rdv_detail" -> {
                Long rdvId = getLong(args.get("rdvId"));
                RendezVousDTOResponse rdv = rendezVousService.getDetailRendezVous(slug, rdvId, email);
                return ToolExecutionResult.builder()
                        .rawResult(rdv)
                        .cartes(List.of(new AssistantCarte("RDV", "Détail du rendez-vous", rdv)))
                        .build();
            }
            case "client_salon_lister_mes_prestations" -> {
                List<PrestationDTOResponse> prestations = prestationSalonService.listerPrestationsClientSalon(slug,
                        email);
                return ToolExecutionResult.builder()
                        .rawResult(truncateList(prestations))
                        .cartes(List.of(new AssistantCarte("HISTORIQUE", "Historique de vos prestations", prestations)))
                        .build();
            }
            case "client_salon_obtenir_prestation_detail" -> {
                Long id = getLong(args.get("prestationId"));
                PrestationDTOResponse detail = prestationSalonService.obtenirDetailPrestationClient(id, email);
                return ToolExecutionResult.builder()
                        .rawResult(detail)
                        .cartes(List.of(new AssistantCarte("HISTORIQUE", "Détail de la prestation", detail)))
                        .build();
            }
            case "client_salon_lister_mes_paiements" -> {
                List<PaiementDTOResponse> paiements = facturationSalonService.listerPaiementsClient(slug, email);
                return ToolExecutionResult.builder()
                        .rawResult(truncateList(paiements))
                        .cartes(List.of(new AssistantCarte("PAIEMENTS", "Historique des règlements", paiements)))
                        .build();
            }
            case "client_salon_obtenir_paiement_detail" -> {
                Long id = getLong(args.get("paiementId"));
                PaiementDTOResponse p = facturationSalonService.obtenirDetailPaiementClient(slug, id, email);
                return ToolExecutionResult.builder()
                        .rawResult(p)
                        .cartes(List.of(new AssistantCarte("PAIEMENTS", "Reçu de paiement", p)))
                        .build();
            }
            case "client_salon_obtenir_panier" -> {
                PanierDTOResponse panier = panierService.obtenirPanier(slug, email);
                return ToolExecutionResult.builder()
                        .rawResult(panier)
                        .cartes(List.of(new AssistantCarte("PANIER", "Votre panier boutique", panier)))
                        .build();
            }
            case "client_salon_lister_mes_commandes" -> {
                List<CommandeDTOResponse> commandes = commandeSalonService.listerCommandesClient(slug, email);
                return ToolExecutionResult.builder()
                        .rawResult(truncateList(commandes))
                        .cartes(List.of(new AssistantCarte("COMMANDES", "Vos commandes Click & Collect", commandes)))
                        .build();
            }
            case "client_salon_obtenir_commande_detail" -> {
                Long id = getLong(args.get("commandeId"));
                CommandeDTOResponse cmd = commandeSalonService.obtenirCommandeClient(slug, email, id);
                return ToolExecutionResult.builder()
                        .rawResult(cmd)
                        .cartes(List.of(new AssistantCarte("COMMANDE", "Commande #" + cmd.id(), cmd)))
                        .build();
            }
            case "client_salon_lister_mes_reclamations" -> {
                List<ReclamationDTOResponse> recls = reclamationSalonService.listerReclamationsClient(slug, email);
                return ToolExecutionResult.builder()
                        .rawResult(recls)
                        .cartes(List.of(new AssistantCarte("RECLAMATIONS", "Vos réclamations", recls)))
                        .build();
            }
            case "client_salon_obtenir_reclamation_detail" -> {
                Long id = getLong(args.get("reclamationId"));
                ReclamationDTOResponse r = reclamationSalonService.obtenirReclamationClient(slug, id, email);
                return ToolExecutionResult.builder()
                        .rawResult(r)
                        .cartes(List.of(new AssistantCarte("RECLAMATIONS", "Détail réclamation", r)))
                        .build();
            }
            case "client_salon_obtenir_mon_avis_salon" -> {
                AvisSalonDTOResponse av = avisSalonService.obtenirMonAvisSalon(slug, email);
                return ToolExecutionResult.builder()
                        .rawResult(av)
                        .cartes(List.of(new AssistantCarte("AVIS", "Votre avis sur le salon", av)))
                        .build();
            }
            case "client_salon_lister_mes_avis_prestations" -> {
                List<AvisPrestationDTOResponse> avis = avisSalonService.listerMesAvisPrestations(slug, email);
                return ToolExecutionResult.builder()
                        .rawResult(avis)
                        .cartes(List.of(new AssistantCarte("AVIS", "Vos avis prestations", avis)))
                        .build();
            }
            case "client_salon_est_salon_favori" -> {
                boolean estFav = favoriSalonService.isSalonFavori(slug, email);
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("favori", estFav))
                        .build();
            }
            case "client_salon_lister_coiffeurs_favoris" -> {
                List<FavoriCoiffeurDTOResponse> favs = favoriSalonService.listerMesCoiffeursFavoris(slug, email);
                return ToolExecutionResult.builder()
                        .rawResult(favs)
                        .cartes(List.of(new AssistantCarte("COIFFEURS", "Vos coiffeurs favoris", favs)))
                        .build();
            }

            // Écritures directes (panier, favori)
            case "client_salon_ajouter_au_panier" -> {
                Long pid = getLong(args.get("produitId"));
                int qte = Optional.ofNullable(getInteger(args.get("quantite"))).orElse(1);
                PanierDTOResponse p = panierService.ajouterArticle(slug, email, new AjoutPanierDTORequest(pid, qte));
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("statut", "AJOUTE", "panier", p))
                        .cartes(List.of(new AssistantCarte("PANIER", "Panier mis à jour", p)))
                        .build();
            }
            case "client_salon_modifier_quantite_panier" -> {
                Long pid = getLong(args.get("produitId"));
                int qte = getInteger(args.get("quantite"));
                PanierDTOResponse p = panierService.modifierQuantite(slug, email, pid,
                        new ModificationQuantiteDTORequest(qte));
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("statut", "MODIFIE", "panier", p))
                        .cartes(List.of(new AssistantCarte("PANIER", "Panier mis à jour", p)))
                        .build();
            }
            case "client_salon_retirer_du_panier" -> {
                Long pid = getLong(args.get("produitId"));
                PanierDTOResponse p = panierService.supprimerArticle(slug, email, pid);
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("statut", "RETIRE", "panier", p))
                        .cartes(List.of(new AssistantCarte("PANIER", "Panier mis à jour", p)))
                        .build();
            }
            case "client_salon_basculer_favori_salon" -> {
                boolean ajouter = Boolean.TRUE.equals(args.get("ajouter"));
                if (ajouter) {
                    favoriSalonService.ajouterSalonFavori(slug, email);
                } else {
                    favoriSalonService.retirerSalonFavori(slug, email);
                }
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("statut", "FAVORI_MIS_A_JOUR", "favori", ajouter))
                        .build();
            }
            case "client_salon_basculer_favori_coiffeur" -> {
                Long cid = getLong(args.get("coiffeurId"));
                boolean ajouter = Boolean.TRUE.equals(args.get("ajouter"));
                if (ajouter) {
                    favoriSalonService.ajouterCoiffeurFavori(slug, cid, email);
                } else {
                    favoriSalonService.retirerCoiffeurFavori(slug, cid, email);
                }
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("statut", "FAVORI_COIFFEUR_MIS_A_JOUR", "favori", ajouter))
                        .build();
            }

            // Écritures avec confirmation (RDV, commande, vider panier, notation)
            case "client_salon_reserver_rdv" -> {
                String resume = "Réservation de votre rendez-vous le " + args.get("dateHeure");
                PendingActionDTO pending = pendingActionService.createPendingAction(
                        sessionId, email, name, resume, Map.of(
                                "slugSalon", slug,
                                "dateHeure", args.get("dateHeure"),
                                "varianteIds", args.get("varianteIds"),
                                "coiffeurId", args.get("coiffeurId") != null ? args.get("coiffeurId") : 0));
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("statut", "EN_ATTENTE_CONFIRMATION", "resume", resume))
                        .pendingAction(pending)
                        .cartes(List
                                .of(new AssistantCarte("CONFIRMATION", "Confirmation de votre réservation", pending)))
                        .build();
            }
            case "client_salon_annuler_rdv" -> {
                String resume = "Annulation du rendez-vous #" + args.get("rdvId");
                PendingActionDTO pending = pendingActionService.createPendingAction(
                        sessionId, email, name, resume, Map.of(
                                "slugSalon", slug,
                                "rdvId", args.get("rdvId"),
                                "motif", args.get("motif") != null ? args.get("motif") : "Annulé via assistant"));
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("statut", "EN_ATTENTE_CONFIRMATION", "resume", resume))
                        .pendingAction(pending)
                        .cartes(List.of(new AssistantCarte("CONFIRMATION", "Confirmation d'annulation", pending)))
                        .build();
            }
            case "client_salon_deplacer_rdv" -> {
                String resume = "Déplacement du rendez-vous #" + args.get("rdvId") + " vers le "
                        + args.get("nouvelleDateHeure");
                PendingActionDTO pending = pendingActionService.createPendingAction(
                        sessionId, email, name, resume, Map.of(
                                "slugSalon", slug,
                                "rdvId", args.get("rdvId"),
                                "nouvelleDateHeure", args.get("nouvelleDateHeure")));
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("statut", "EN_ATTENTE_CONFIRMATION", "resume", resume))
                        .pendingAction(pending)
                        .cartes(List.of(new AssistantCarte("CONFIRMATION", "Confirmation de déplacement", pending)))
                        .build();
            }
            case "client_salon_passer_commande" -> {
                String resume = "Validation de votre commande Click & Collect";
                PendingActionDTO pending = pendingActionService.createPendingAction(
                        sessionId, email, name, resume, Map.of("slugSalon", slug));
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("statut", "EN_ATTENTE_CONFIRMATION", "resume", resume))
                        .pendingAction(pending)
                        .cartes(List.of(new AssistantCarte("CONFIRMATION", "Confirmation de commande", pending)))
                        .build();
            }
            case "client_salon_vider_panier" -> {
                String resume = "Suppression intégrale du contenu de votre panier";
                PendingActionDTO pending = pendingActionService.createPendingAction(
                        sessionId, email, name, resume, Map.of("slugSalon", slug));
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("statut", "EN_ATTENTE_CONFIRMATION", "resume", resume))
                        .pendingAction(pending)
                        .cartes(List
                                .of(new AssistantCarte("CONFIRMATION", "Confirmation pour vider le panier", pending)))
                        .build();
            }
            case "client_salon_noter_prestation" -> {
                String resume = "Dépôt d'un avis noté " + args.get("note") + "/5 sur la prestation";
                PendingActionDTO pending = pendingActionService.createPendingAction(
                        sessionId, email, name, resume, Map.of(
                                "slugSalon", slug,
                                "lignePrestationId", args.get("lignePrestationId"),
                                "note", args.get("note"),
                                "commentaire", args.get("commentaire") != null ? args.get("commentaire") : ""));
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("statut", "EN_ATTENTE_CONFIRMATION", "resume", resume))
                        .pendingAction(pending)
                        .cartes(List.of(new AssistantCarte("CONFIRMATION", "Confirmation de votre avis", pending)))
                        .build();
            }
            default -> {
                return errorResult("OUTIL_NON_GERE", "Outil client salon non implémenté : " + name);
            }
        }
    }

    // ==========================================
    // 4. OUTILS PLATEFORME (PV & PC)
    // ==========================================
    private ToolExecutionResult executePlateformeTool(String name, Map<String, Object> args, AssistantContext ctx) {
        switch (name) {
            case "plateforme_rechercher_salons" -> {
                String q = (String) args.get("recherche");
                List<SalonDTOResponse> salons = exploreSalonService.rechercherSalons(q);
                return ToolExecutionResult.builder()
                        .rawResult(truncateList(salons))
                        .cartes(List.of(new AssistantCarte("SALONS", "Salons trouvés", salons)))
                        .build();
            }
            case "plateforme_lister_salons" -> {
                int page = Optional.ofNullable(getInteger(args.get("page"))).orElse(0);
                var pageResult = exploreSalonService
                        .listerSalonsActifs(PageRequest.of(page, 10, Sort.by("nom").ascending()));
                List<SalonDTOResponse> list = pageResult.getContent();
                return ToolExecutionResult.builder()
                        .rawResult(truncateList(list))
                        .cartes(List.of(new AssistantCarte("SALONS", "Salons partenaires", list)))
                        .build();
            }
            case "plateforme_salons_a_proximite" -> {
                if (ctx.getLatitude() == null || ctx.getLongitude() == null) {
                    return ToolExecutionResult.builder()
                            .rawResult(Map.of("erreur", "GEOLOCALISATION_REQUISE", "message",
                                    "Position GPS requise pour trouver les salons proches."))
                            .actionsUi(List.of(new AssistantActionUi("ui_demander_geolocalisation", Map.of())))
                            .build();
                }
                double rayon = args.get("rayonKm") != null ? ((Number) args.get("rayonKm")).doubleValue() : 5.0;
                List<SalonDTOResponse> salons = exploreSalonService.rechercherSalonsNearby(ctx.getLatitude(),
                        ctx.getLongitude(), rayon);
                return ToolExecutionResult.builder()
                        .rawResult(truncateList(salons))
                        .cartes(List.of(new AssistantCarte("SALONS", "Salons à proximité (" + rayon + " km)", salons)))
                        .build();
            }
            case "plateforme_salons_par_service" -> {
                String nom = (String) args.get("nomService");
                List<ServiceSalon> services = serviceSalonRepository.findByNomContainingIgnoreCase(nom);
                List<SalonDTOResponse> result = new ArrayList<>();
                for (ServiceSalon s : services) {
                    if (s.getSalon() != null) {
                        try {
                            SalonDTOResponse salonDto = exploreSalonService.getSalonDetail(s.getSalon().getSlug());
                            if (result.stream().noneMatch(x -> x.slug().equals(salonDto.slug()))) {
                                result.add(salonDto);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                return ToolExecutionResult.builder()
                        .rawResult(truncateList(result))
                        .cartes(List.of(new AssistantCarte("SALONS", "Salons pour « " + nom + " »", result)))
                        .build();
            }
            case "plateforme_obtenir_salon" -> {
                String sSlug = (String) args.get("slugSalon");
                SalonDTOResponse s = exploreSalonService.getSalonDetail(sSlug);
                return ToolExecutionResult.builder()
                        .rawResult(s)
                        .cartes(List.of(new AssistantCarte("SALON", s.nom(), s)))
                        .build();
            }
            case "plateforme_services_salon" -> {
                String sSlug = (String) args.get("slugSalon");
                List<ServiceSalonDTOResponse> services = serviceSalonService.listerServices(sSlug, true);
                return ToolExecutionResult.builder()
                        .rawResult(truncateList(services))
                        .cartes(List.of(new AssistantCarte("SERVICES", "Prestations chez " + sSlug, services)))
                        .build();
            }
            case "plateforme_avis_salon" -> {
                String sSlug = (String) args.get("slugSalon");
                List<AvisSalonDTOResponse> avis = avisSalonService.listerAvisPubliesSalon(sSlug);
                return ToolExecutionResult.builder()
                        .rawResult(truncateList(avis))
                        .cartes(List.of(new AssistantCarte("AVIS", "Avis sur " + sSlug, avis)))
                        .build();
            }
            case "plateforme_realisations_salon" -> {
                String sSlug = (String) args.get("slugSalon");
                List<RealisationDTOResponse> realisations = realisationSalonService.listerRealisationsPubliees(sSlug);
                return ToolExecutionResult.builder()
                        .rawResult(truncateList(realisations))
                        .cartes(List.of(new AssistantCarte("REALISATIONS", "Réalisations de " + sSlug, realisations)))
                        .build();
            }
            case "plateforme_verifier_disponibilites_salon" -> {
                String sSlug = (String) args.get("slugSalon");
                String dateStr = (String) args.get("date");
                List<Long> varianteIds = getLongList(args.get("varianteIds"));
                var creneaux = disponibiliteService.calculerDisponibilites(sSlug,
                        new DisponibiliteSearchDTORequest(LocalDate.parse(dateStr), varianteIds, null, null));
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("creneaux", creneaux))
                        .cartes(List.of(new AssistantCarte("CRENEAUX", "Disponibilités chez " + sSlug, creneaux)))
                        .build();
            }
            case "plateforme_comparer_salons" -> {
                @SuppressWarnings("unchecked")
                List<String> slugs = (List<String>) args.get("slugs");
                List<Map<String, Object>> comparatif = new ArrayList<>();
                for (String s : slugs) {
                    try {
                        SalonDTOResponse salon = exploreSalonService.getSalonDetail(s);
                        Map<String, Object> sMap = new HashMap<>();
                        sMap.put("slug", salon.slug());
                        sMap.put("nom", salon.nom());
                        sMap.put("adresse", salon.adresse());
                        sMap.put("telephone", salon.telephone());
                        sMap.put("description", salon.description());
                        comparatif.add(sMap);
                    } catch (Exception ignored) {
                    }
                }
                return ToolExecutionResult.builder()
                        .rawResult(comparatif)
                        .cartes(List.of(new AssistantCarte("COMPARATIF", "Comparatif de salons", comparatif)))
                        .build();
            }
            case "plateforme_rechercher_produits" -> {
                List<ProduitDTOResponse> prods = stockSalonService.listerProduitsTransversal(null);
                return ToolExecutionResult.builder()
                        .rawResult(truncateList(prods))
                        .cartes(List.of(new AssistantCarte("PRODUITS", "Catalogue transversal", prods)))
                        .build();
            }
            case "plateforme_explorer_realisations" -> {
                List<RealisationDTOResponse> reals = realisationSalonService.listerToutesLesRealisationsPubliees();
                return ToolExecutionResult.builder()
                        .rawResult(truncateList(reals))
                        .cartes(List.of(new AssistantCarte("REALISATIONS", "Inspirations capillaires", reals)))
                        .build();
            }
            case "plateforme_feed_kadys" -> {
                var feed = kadysInteractionService.listerFeedKadys(ctx.getClientEmail(),
                        PageRequest.of(0, 10, Sort.by("datePublication").descending()));
                return ToolExecutionResult.builder()
                        .rawResult(truncateList(feed.getContent()))
                        .cartes(List.of(new AssistantCarte("REALISATIONS", "Kady's Feed", feed.getContent())))
                        .build();
            }
            case "plateforme_obtenir_realisation" -> {
                Long rid = getLong(args.get("realisationId"));
                KadysRealisationDTOResponse r = kadysInteractionService.obtenirRealisationKadys(rid,
                        ctx.getClientEmail());
                return ToolExecutionResult.builder()
                        .rawResult(r)
                        .cartes(List.of(new AssistantCarte("REALISATION", r.titre(), r)))
                        .build();
            }
            case "plateforme_stories" -> {
                var stories = storySalonService.listerStoriesTousSalonsPourKadys();
                return ToolExecutionResult.builder().rawResult(stories).build();
            }
            case "plateforme_expliquer_fonctionnement" -> {
                String sujet = (String) args.get("sujet");
                Map<String, Object> faq = loadFaq(sujet);
                return ToolExecutionResult.builder()
                        .rawResult(faq)
                        .build();
            }
            default -> {
                return errorResult("OUTIL_NON_GERE", "Outil plateforme non implémenté : " + name);
            }
        }
    }

    // ==========================================
    // 5. OUTILS CLIENT PLATEFORME (PC)
    // ==========================================
    private ToolExecutionResult executeClientPlateformeTool(String name, Map<String, Object> args, AssistantContext ctx,
            String sessionId) {
        String email = ctx.getClientEmail();

        switch (name) {
            case "client_plateforme_obtenir_compte" -> {
                CompteDTOResponse compte = compteService.getProfil(email);
                return ToolExecutionResult.builder()
                        .rawResult(compte)
                        .cartes(List.of(new AssistantCarte("COMPTE", "Votre profil client", compte)))
                        .build();
            }
            case "client_plateforme_resume_activite" -> {
                List<RendezVousDTOResponse> rdvs = rendezVousService.listerTousMesRendezVous(email);
                List<CommandeDTOResponse> cmds = commandeSalonService.listerToutesCommandesClient(email);
                Map<String, Object> dashboard = Map.of(
                        "nbRdv", rdvs.size(),
                        "prochainRdv",
                        rdvs.stream().filter(r -> r.dateHeurePrevue().isAfter(LocalDateTime.now())).findFirst()
                                .orElse(null),
                        "nbCommandes", cmds.size());
                return ToolExecutionResult.builder()
                        .rawResult(dashboard)
                        .cartes(List.of(new AssistantCarte("TABLEAU_DE_BORD", "Tableau de bord personnel", dashboard)))
                        .build();
            }
            case "client_plateforme_lister_rdv" -> {
                List<RendezVousDTOResponse> rdvs = rendezVousService.listerTousMesRendezVous(email);
                return ToolExecutionResult.builder()
                        .rawResult(truncateList(rdvs))
                        .cartes(List.of(new AssistantCarte("RDV_LISTE", "Vos rendez-vous globaux", rdvs)))
                        .build();
            }
            case "client_plateforme_obtenir_rdv_detail" -> {
                Long id = getLong(args.get("rdvId"));
                RendezVousDTOResponse rdv = rendezVousService.getDetailRendezVousGlobal(id, email);
                return ToolExecutionResult.builder()
                        .rawResult(rdv)
                        .cartes(List.of(new AssistantCarte("RDV", "Détail du rendez-vous", rdv)))
                        .build();
            }
            case "client_plateforme_lister_prestations" -> {
                List<PrestationDTOResponse> prestations = prestationSalonService.listerToutesPrestationsClient(email);
                return ToolExecutionResult.builder()
                        .rawResult(truncateList(prestations))
                        .cartes(List.of(new AssistantCarte("HISTORIQUE", "Toutes vos prestations", prestations)))
                        .build();
            }
            case "client_plateforme_lister_paiements" -> {
                List<PaiementDTOResponse> paiements = facturationSalonService.listerTousPaiementsClient(email);
                return ToolExecutionResult.builder()
                        .rawResult(truncateList(paiements))
                        .cartes(List.of(new AssistantCarte("PAIEMENTS", "Tous vos règlements", paiements)))
                        .build();
            }
            case "client_plateforme_lister_salons_favoris" -> {
                List<FavoriSalonDTOResponse> favs = favoriSalonService.listerMesSalonsFavoris(email);
                return ToolExecutionResult.builder()
                        .rawResult(favs)
                        .cartes(List.of(new AssistantCarte("SALONS", "Vos salons favoris", favs)))
                        .build();
            }
            case "client_plateforme_lister_coiffeurs_favoris" -> {
                List<FavoriCoiffeurDTOResponse> favs = favoriSalonService.listerTousMesCoiffeursFavoris(email);
                return ToolExecutionResult.builder()
                        .rawResult(favs)
                        .cartes(List.of(new AssistantCarte("COIFFEURS", "Vos coiffeurs favoris", favs)))
                        .build();
            }
            case "client_plateforme_lister_commandes" -> {
                List<CommandeDTOResponse> cmds = commandeSalonService.listerToutesCommandesClient(email);
                return ToolExecutionResult.builder()
                        .rawResult(truncateList(cmds))
                        .cartes(List.of(new AssistantCarte("COMMANDES", "Vos commandes", cmds)))
                        .build();
            }
            case "client_plateforme_reserver_rdv" -> {
                String sSlug = (String) args.get("slugSalon");
                String resume = "Réservation chez " + sSlug + " le " + args.get("dateHeure");
                PendingActionDTO pending = pendingActionService.createPendingAction(
                        sessionId, email, name, resume, Map.of(
                                "slugSalon", sSlug,
                                "dateHeure", args.get("dateHeure"),
                                "varianteIds", args.get("varianteIds")));
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("statut", "EN_ATTENTE_CONFIRMATION", "resume", resume))
                        .pendingAction(pending)
                        .cartes(List.of(new AssistantCarte("CONFIRMATION", "Confirmation de réservation", pending)))
                        .build();
            }
            case "client_plateforme_annuler_rdv" -> {
                Long rdvId = getLong(args.get("rdvId"));
                RendezVousDTOResponse rdv = rendezVousService.getDetailRendezVousGlobal(rdvId, email);
                String resume = "Annulation de votre rendez-vous #" + rdvId;
                PendingActionDTO pending = pendingActionService.createPendingAction(
                        sessionId, email, name, resume, Map.of(
                                "slugSalon", rdv.salonSlug(),
                                "rdvId", rdvId,
                                "motif", args.get("motif") != null ? args.get("motif") : "Annulé via assistant"));
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("statut", "EN_ATTENTE_CONFIRMATION", "resume", resume))
                        .pendingAction(pending)
                        .cartes(List.of(new AssistantCarte("CONFIRMATION", "Confirmation d'annulation", pending)))
                        .build();
            }
            case "client_plateforme_demander_export_donnees" -> {
                String resume = "Demande d'export de l'intégralité de vos données personnelles (RGPD)";
                PendingActionDTO pending = pendingActionService.createPendingAction(
                        sessionId, email, name, resume, Map.of("email", email));
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("statut", "EN_ATTENTE_CONFIRMATION", "resume", resume))
                        .pendingAction(pending)
                        .cartes(List.of(new AssistantCarte("CONFIRMATION", "Export données personnelles", pending)))
                        .build();
            }
            default -> {
                return errorResult("OUTIL_NON_GERE", "Outil client plateforme non implémenté : " + name);
            }
        }
    }

    // ==========================================
    // 6. OUTILS PARTAGÉS (PROFIL, KADY'S INTERACTIONS)
    // ==========================================
    private ToolExecutionResult executeSharedTool(String name, Map<String, Object> args, AssistantContext ctx,
            String sessionId) {
        String email = ctx.getClientEmail();

        switch (name) {
            case "client_obtenir_profil_capillaire" -> {
                ProfilCapillaireDTOResponse profil = profilCapillaireService.getMonProfil(email);
                return ToolExecutionResult.builder()
                        .rawResult(profil)
                        .cartes(List.of(new AssistantCarte("PROFIL_CAPILLAIRE", "Votre profil capillaire", profil)))
                        .build();
            }
            case "client_mettre_a_jour_profil_capillaire" -> {
                String resume = "Mise à jour de votre diagnostic capillaire";
                PendingActionDTO pending = pendingActionService.createPendingAction(
                        sessionId, email, name, resume, args);
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("statut", "EN_ATTENTE_CONFIRMATION", "resume", resume))
                        .pendingAction(pending)
                        .cartes(List.of(new AssistantCarte("CONFIRMATION", "Mise à jour profil capillaire", pending)))
                        .build();
            }
            case "realisation_aimer" -> {
                Long rid = getLong(args.get("realisationId"));
                var toggle = kadysInteractionService.toggleLike(rid, email);
                return ToolExecutionResult.builder().rawResult(toggle).build();
            }
            case "realisation_lire_commentaires" -> {
                Long rid = getLong(args.get("realisationId"));
                List<CommentaireDTOResponse> coms = kadysInteractionService.listerCommentaires(rid, email);
                return ToolExecutionResult.builder().rawResult(truncateList(coms)).build();
            }
            case "realisation_commenter" -> {
                Long rid = getLong(args.get("realisationId"));
                String contenu = (String) args.get("contenu");
                String resume = "Publication de votre commentaire : « " + contenu + " »";
                PendingActionDTO pending = pendingActionService.createPendingAction(
                        sessionId, email, name, resume, Map.of("realisationId", rid, "contenu", contenu));
                return ToolExecutionResult.builder()
                        .rawResult(Map.of("statut", "EN_ATTENTE_CONFIRMATION", "resume", resume))
                        .pendingAction(pending)
                        .cartes(List.of(new AssistantCarte("CONFIRMATION", "Publication commentaire", pending)))
                        .build();
            }
            default -> {
                return errorResult("OUTIL_NON_GERE", "Outil partagé non implémenté : " + name);
            }
        }
    }

    // ==========================================
    // 7. CONFIRMATION DETERMINISTE D'UNE ACTION
    // ==========================================
    public AssistantCarte executeConfirmedAction(PendingAction action, AssistantContext ctx) {
        String email = action.getClientEmail();
        String outil = action.getOutil();
        Map<String, Object> data = action.getDonnees();

        try {
            switch (outil) {
                case "client_salon_reserver_rdv", "client_plateforme_reserver_rdv" -> {
                    String slug = (String) data.get("slugSalon");
                    String dateHeureStr = (String) data.get("dateHeure");
                    List<Long> varianteIds = getLongList(data.get("varianteIds"));
                    Long coiffeurId = getLong(data.get("coiffeurId"));
                    LocalDateTime dateHeure = LocalDateTime.parse(dateHeureStr);

                    RendezVousCreateDTORequest req = new RendezVousCreateDTORequest(dateHeure, varianteIds,
                            coiffeurId != null && coiffeurId > 0 ? coiffeurId : null);
                    RendezVousDTOResponse rdv = rendezVousService.creerRendezVousClient(slug, req, email);
                    return new AssistantCarte("RDV", "Rendez-vous confirmé avec succès", rdv);
                }
                case "client_salon_annuler_rdv", "client_plateforme_annuler_rdv" -> {
                    String slug = (String) data.get("slugSalon");
                    Long rdvId = getLong(data.get("rdvId"));
                    String motif = (String) data.get("motif");
                    RendezVousAnnulationDTORequest req = new RendezVousAnnulationDTORequest(
                            motif != null ? motif : "Annulé via assistant");
                    RendezVousDTOResponse rdv = rendezVousService.annulerRendezVousClient(slug, rdvId, req, email);
                    return new AssistantCarte("RDV", "Rendez-vous annulé", rdv);
                }
                case "client_salon_deplacer_rdv", "client_plateforme_deplacer_rdv" -> {
                    String slug = (String) data.get("slugSalon");
                    Long rdvId = getLong(data.get("rdvId"));
                    String nouvelleDateStr = (String) data.get("nouvelleDateHeure");
                    LocalDateTime nouvelleDate = LocalDateTime.parse(nouvelleDateStr);

                    RendezVousDTOResponse oldRdv = rendezVousService.getDetailRendezVous(slug, rdvId, email);
                    List<Long> varianteIds = oldRdv.lignes() != null
                            ? oldRdv.lignes().stream().map(LigneRendezVousDTOResponse::varianteId).toList()
                            : List.of();

                    RendezVousCreateDTORequest newReq = new RendezVousCreateDTORequest(nouvelleDate, varianteIds,
                            oldRdv.coiffeurAffectationId());
                    RendezVousDTOResponse newRdv = rendezVousService.creerRendezVousClient(slug, newReq, email);

                    try {
                        rendezVousService.annulerRendezVousClient(slug, rdvId,
                                new RendezVousAnnulationDTORequest("Déplacé vers le nouveau RDV #" + newRdv.id()),
                                email);
                    } catch (Exception e) {
                        log.warn("Impossible d'annuler l'ancien RDV après déplacement : {}", e.getMessage());
                    }

                    return new AssistantCarte("RDV", "Rendez-vous déplacé au " + nouvelleDateStr, newRdv);
                }
                case "client_salon_passer_commande" -> {
                    String slug = (String) data.get("slugSalon");
                    CommandeDTOResponse cmd = commandeSalonService.passerCommande(slug, email);
                    return new AssistantCarte("COMMANDE", "Commande Click & Collect validée", cmd);
                }
                case "client_salon_vider_panier", "client_plateforme_vider_tous_paniers" -> {
                    String slug = (String) data.get("slugSalon");
                    if (slug != null) {
                        panierService.viderPanier(slug, email);
                    }
                    return new AssistantCarte("PANIER", "Votre panier a été vidé avec succès",
                            Map.of("statut", "VIDE"));
                }
                case "client_salon_noter_prestation" -> {
                    String slug = (String) data.get("slugSalon");
                    Long ligneId = getLong(data.get("lignePrestationId"));
                    int note = getInteger(data.get("note"));
                    String com = (String) data.get("commentaire");
                    AvisPrestationCreateDTORequest req = new AvisPrestationCreateDTORequest(ligneId, note, com);
                    AvisPrestationDTOResponse avis = avisSalonService.creerAvisPrestation(slug, email, req);
                    return new AssistantCarte("AVIS", "Avis enregistré avec succès", avis);
                }
                case "client_mettre_a_jour_profil_capillaire" -> {
                    ProfilCapillaireDTORequest req = new ProfilCapillaireDTORequest(
                            (String) data.get("typeCheveux"),
                            (String) data.get("texture"),
                            (String) data.get("longueur"),
                            (String) data.get("densite"),
                            (String) data.get("cuirChevelu"),
                            (String) data.get("etatCheveux"),
                            (String) data.get("sensibilites"),
                            (String) data.get("allergiesProduits"),
                            (String) data.get("observations"));
                    ProfilCapillaireDTOResponse rep = profilCapillaireService.enregistrerOuModifierProfil(email, req);
                    return new AssistantCarte("PROFIL_CAPILLAIRE", "Diagnostic capillaire mis à jour", rep);
                }
                case "realisation_commenter" -> {
                    Long rid = getLong(data.get("realisationId"));
                    String contenu = (String) data.get("contenu");
                    CommentaireDTOResponse c = kadysInteractionService.ajouterCommentaire(rid, email,
                            new CommentaireCreateDTORequest(contenu));
                    return new AssistantCarte("REALISATION", "Commentaire publié avec succès", c);
                }
                case "client_plateforme_demander_export_donnees" -> {
                    DemandeExportDTOResponse rep = rgpdClientService.demanderExportDonnees(email,
                            new DemandeExportDTORequest(FormatExportDonnees.JSON));
                    return new AssistantCarte("RGPD", "Export de données en cours de traitement", rep);
                }
                default -> {
                    return new AssistantCarte("CONFIRMATION", "Action confirmée", Map.of("statut", "OK"));
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors de la confirmation déterministe de l'action '{}': {}", outil, e.getMessage(), e);
            return new AssistantCarte("ERREUR", "Échec de l'action : " + e.getMessage(),
                    Map.of("erreur", e.getMessage()));
        }
    }

    // ==========================================
    // HELPERS
    // ==========================================
    private Map<String, Object> loadFaq(String sujet) {
        try (InputStream is = resourceLoader.getResource("classpath:assistant/knowledge/plateforme-faq.json")
                .getInputStream()) {
            Map<String, Map<String, Object>> map = objectMapper.readValue(is, new TypeReference<>() {
            });
            return map.getOrDefault(sujet,
                    Map.of("sujet", sujet, "explication", "Service disponible sur la plateforme Mon Salon."));
        } catch (Exception e) {
            log.warn("Impossible de charger la FAQ assistant: {}", e.getMessage());
            return Map.of("sujet", sujet, "explication", "Information en cours de mise à jour.");
        }
    }

    private static ToolExecutionResult errorResult(String code, String message) {
        return ToolExecutionResult.builder()
                .rawResult(Map.of("erreur", code, "message", message))
                .cartes(Collections.emptyList())
                .actionsUi(Collections.emptyList())
                .build();
    }

    private static Object truncateList(List<?> list) {
        if (list == null)
            return Collections.emptyList();
        if (list.size() <= 10)
            return list;
        return Map.of(
                "total", list.size(),
                "tronque", true,
                "elements", list.subList(0, 10));
    }

    private static Long getLong(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof Number n)
            return n.longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer getInteger(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof Number n)
            return n.intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Long> getLongList(Object obj) {
        if (obj instanceof List<?> l) {
            List<Long> result = new ArrayList<>();
            for (Object o : l) {
                Long val = getLong(o);
                if (val != null)
                    result.add(val);
            }
            return result;
        }
        return Collections.emptyList();
    }
}

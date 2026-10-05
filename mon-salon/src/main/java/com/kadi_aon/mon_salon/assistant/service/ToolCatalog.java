package com.kadi_aon.mon_salon.assistant.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.kadi_aon.mon_salon.assistant.context.AssistantContextType;

import lombok.Value;

@Component
public class ToolCatalog {

    @Value
    public static class ToolDefinition {
        String name;
        String description;
        Map<String, Object> parameters;
        Set<AssistantContextType> contexts;
        boolean confirmationRequired;
    }

    private final Map<String, ToolDefinition> tools = new HashMap<>();

    public ToolCatalog() {
        initSalonTools();
        initClientSalonTools();
        initSharedTools();
        initPlateformeTools();
        initClientPlateformeTools();
    }

    public List<Map<String, Object>> getToolDeclarationsForContext(AssistantContextType contextType) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ToolDefinition def : tools.values()) {
            if (def.getContexts().contains(contextType)) {
                Map<String, Object> toolObj = new HashMap<>();
                toolObj.put("type", "function");
                toolObj.put("name", def.getName());
                toolObj.put("description", def.getDescription());
                toolObj.put("parameters", def.getParameters());
                list.add(toolObj);
            }
        }
        return list;
    }

    public ToolDefinition getDefinition(String name) {
        return tools.get(name);
    }

    public boolean isAllowed(String name, AssistantContextType contextType) {
        ToolDefinition def = tools.get(name);
        return def != null && def.getContexts().contains(contextType);
    }

    private void register(String name, String desc, Map<String, Object> schema, Set<AssistantContextType> ctxs, boolean confirm) {
        tools.put(name, new ToolDefinition(name, desc, schema, ctxs, confirm));
    }

    private static Map<String, Object> schema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> s = new HashMap<>();
        s.put("type", "object");
        s.put("properties", properties != null ? properties : Collections.emptyMap());
        if (required != null && !required.isEmpty()) {
            s.put("required", required);
        }
        return s;
    }

    private static Map<String, Object> prop(String type, String desc) {
        Map<String, Object> p = new HashMap<>();
        p.put("type", type);
        p.put("description", desc);
        return p;
    }

    private static Map<String, Object> propEnum(String type, String desc, List<String> enumVals) {
        Map<String, Object> p = prop(type, desc);
        p.put("enum", enumVals);
        return p;
    }

    private static Map<String, Object> propArray(String itemType, String desc) {
        Map<String, Object> p = new HashMap<>();
        p.put("type", "array");
        p.put("description", desc);
        Map<String, Object> items = new HashMap<>();
        items.put("type", itemType);
        p.put("items", items);
        return p;
    }

    private void initSalonTools() {
        Set<AssistantContextType> svSc = Set.of(AssistantContextType.SALON_VISITEUR, AssistantContextType.SALON_CLIENT);
        Set<AssistantContextType> svOnly = Set.of(AssistantContextType.SALON_VISITEUR);

        register("salon_obtenir_infos", "Retourne les informations publiques du salon actuel : nom, description, adresse, ville, téléphone, note moyenne.", schema(null, null), svSc, false);
        register("salon_obtenir_horaires", "Retourne les horaires d'ouverture hebdomadaires du salon et indique s'il est ouvert actuellement.", schema(null, null), svSc, false);

        Map<String, Object> pServ = new HashMap<>();
        pServ.put("recherche", prop("string", "Mot-clé de recherche"));
        pServ.put("prixMax", prop("integer", "Budget maximum en FCFA"));
        pServ.put("dureeMaxMinutes", prop("integer", "Durée maximum"));
        register("salon_lister_services", "Liste les prestations du salon avec durée et prix d'appel.", schema(pServ, null), svSc, false);

        Map<String, Object> pDet = Map.of("serviceId", prop("integer", "ID de la prestation"));
        register("salon_obtenir_service_detail", "Retourne les variantes et formules tarifaires d'une prestation.", schema(pDet, List.of("serviceId")), svSc, false);

        Map<String, Object> pVar = new HashMap<>();
        pVar.put("recherche", prop("string", "Mot-clé ex: knotless, lissage, barbe"));
        pVar.put("prixMax", prop("integer", "Prix max"));
        pVar.put("dureeMaxMinutes", prop("integer", "Durée max"));
        pVar.put("tri", propEnum("string", "Tri", List.of("PRIX_ASC", "PRIX_DESC", "DUREE_ASC", "PERTINENCE")));
        register("salon_rechercher_variantes", "Recherche les variantes de prestations directement réservables.", schema(pVar, List.of("recherche")), svSc, false);

        Map<String, Object> pDev = Map.of("varianteIds", propArray("integer", "IDs des variantes"));
        register("salon_calculer_devis", "Calcule le prix total et la durée totale d'une combinaison de variantes.", schema(pDev, List.of("varianteIds")), svSc, false);

        Map<String, Object> pDisp = new HashMap<>();
        pDisp.put("date", prop("string", "Date yyyy-MM-dd"));
        pDisp.put("varianteIds", propArray("integer", "IDs des variantes"));
        pDisp.put("heureMinimale", prop("string", "Heure min HH:mm"));
        pDisp.put("coiffeurId", prop("integer", "Filtre coiffeur"));
        pDisp.put("periode", propEnum("string", "Période", List.of("MATIN", "APRES_MIDI", "SOIR", "TOUTE_LA_JOURNEE")));
        register("salon_verifier_disponibilites", "Vérifie les créneaux disponibles pour une date et des prestations.", schema(pDisp, List.of("date", "varianteIds")), svSc, false);

        Map<String, Object> pProch = new HashMap<>();
        pProch.put("varianteIds", propArray("integer", "IDs des variantes"));
        pProch.put("aPartirDe", prop("string", "Date de départ"));
        pProch.put("joursMax", prop("integer", "Jours max (1-14)"));
        pProch.put("coiffeurId", prop("integer", "ID coiffeur"));
        pProch.put("nombreResultats", prop("integer", "Nombre de résultats"));
        register("salon_trouver_prochain_creneau", "Trouve les prochains créneaux disponibles dès aujourd'hui.", schema(pProch, List.of("varianteIds")), svSc, false);

        register("salon_lister_coiffeurs", "Liste l'équipe des coiffeurs du salon.", schema(null, null), svSc, false);
        register("salon_obtenir_coiffeur_detail", "Profil et réalisations d'un coiffeur.", schema(Map.of("coiffeurId", prop("integer", "ID coiffeur")), List.of("coiffeurId")), svSc, false);

        register("salon_lister_categories_produits", "Liste les catégories de produits en boutique.", schema(null, null), svSc, false);

        Map<String, Object> pProd = new HashMap<>();
        pProd.put("recherche", prop("string", "Nom ou ingrédient"));
        pProd.put("categorieId", prop("integer", "Catégorie"));
        pProd.put("prixMax", prop("integer", "Budget max"));
        pProd.put("enStockSeulement", prop("boolean", "En stock uniquement"));
        register("salon_rechercher_produits", "Recherche des cosmétiques en boutique Click & Collect.", schema(pProd, null), svSc, false);
        register("salon_obtenir_produit_detail", "Fiche détaillée d'un produit.", schema(Map.of("produitId", prop("integer", "ID produit")), List.of("produitId")), svSc, false);

        Map<String, Object> pReco = new HashMap<>();
        pReco.put("besoin", prop("string", "Description du besoin capillaire"));
        pReco.put("budgetMax", prop("integer", "Budget max"));
        register("salon_recommander_produits", "Recommande des soins adaptés au profil ou au besoin capillaire.", schema(pReco, List.of("besoin")), svSc, false);

        Map<String, Object> pAvis = new HashMap<>();
        pAvis.put("noteMin", prop("integer", "Note min 1-5"));
        pAvis.put("limite", prop("integer", "Nombre d'avis"));
        register("salon_obtenir_avis", "Avis clients vérifiés du salon.", schema(pAvis, null), svSc, false);

        Map<String, Object> pReal = new HashMap<>();
        pReal.put("coiffeurId", prop("integer", "Filtre coiffeur"));
        pReal.put("recherche", prop("string", "Mot-clé"));
        register("salon_lister_realisations", "Galerie vidéo des réalisations de coiffure du salon.", schema(pReal, null), svSc, false);
        register("salon_lister_stories", "Stories actives du salon.", schema(null, null), svSc, false);

        Map<String, Object> pPol = Map.of("sujet", propEnum("string", "Sujet de politique", List.of("ANNULATION", "RETARD", "PAIEMENT", "CLICK_COLLECT", "ACOMPTE", "TOUS")));
        register("salon_obtenir_politiques", "Politiques et conditions du salon.", schema(pPol, List.of("sujet")), svSc, false);

        // UI Salon
        Map<String, Object> pUiRdv = new HashMap<>();
        pUiRdv.put("varianteIds", propArray("integer", "IDs variantes à ajouter"));
        pUiRdv.put("coiffeurId", prop("integer", "ID coiffeur"));
        register("ui_ajouter_au_rdv", "Action UI : ajoute des prestations au devis / tiroir RDV.", schema(pUiRdv, List.of("varianteIds")), svSc, false);
        register("ui_retirer_du_rdv", "Action UI : retire une prestation du RDV en cours.", schema(Map.of("varianteId", prop("integer", "ID variante")), List.of("varianteId")), svSc, false);

        Map<String, Object> pUiCreneau = new HashMap<>();
        pUiCreneau.put("date", prop("string", "Date yyyy-MM-dd"));
        pUiCreneau.put("heureDebut", prop("string", "Heure début HH:mm"));
        pUiCreneau.put("coiffeurId", prop("integer", "ID coiffeur"));
        register("ui_preselectionner_creneau", "Action UI : pré-sélectionne le créneau et ouvre le tunnel.", schema(pUiCreneau, List.of("date", "heureDebut")), svSc, false);

        Map<String, Object> pUiNav = new HashMap<>();
        pUiNav.put("destination", prop("string", "Destination de navigation"));
        pUiNav.put("idCible", prop("integer", "ID ressource cible"));
        register("ui_naviguer", "Action UI : redirige vers une page du salon.", schema(pUiNav, List.of("destination")), svSc, false);
        register("ui_ouvrir_tiroir", "Action UI : ouvre le tiroir RDV ou Panier.", schema(Map.of("tiroir", propEnum("string", "Tiroir", List.of("RDV", "PANIER"))), List.of("tiroir")), svSc, false);
        register("ui_contacter_salon", "Action UI : déclenche l'appel, WhatsApp ou itinéraire.", schema(Map.of("canal", propEnum("string", "Canal", List.of("APPEL", "WHATSAPP", "EMAIL", "ITINERAIRE"))), List.of("canal")), svSc, false);

        // SV only
        Map<String, Object> pCartLocal = new HashMap<>();
        pCartLocal.put("produitId", prop("integer", "ID produit"));
        pCartLocal.put("quantite", prop("integer", "Quantité"));
        register("ui_ajouter_au_panier_local", "Action UI : ajoute au panier local invité.", schema(pCartLocal, List.of("produitId")), svOnly, false);
        register("ui_demander_connexion", "Action UI : ouvre la modale de connexion.", schema(Map.of("raison", prop("string", "Raison")), List.of("raison")), Set.of(AssistantContextType.SALON_VISITEUR, AssistantContextType.PLATEFORME_VISITEUR), false);
    }

    private void initClientSalonTools() {
        Set<AssistantContextType> scOnly = Set.of(AssistantContextType.SALON_CLIENT);

        register("client_salon_lister_mes_rdv", "Retourne les RDV du client dans ce salon.", schema(Map.of("statut", propEnum("string", "Statut", List.of("A_VENIR", "PASSES", "ANNULES", "TOUS"))), null), scOnly, false);
        register("client_salon_obtenir_rdv_detail", "Détail d'un RDV dans ce salon.", schema(Map.of("rdvId", prop("integer", "ID RDV")), List.of("rdvId")), scOnly, false);
        register("client_salon_lister_mes_prestations", "Historique des prestations passées dans ce salon.", schema(null, null), scOnly, false);
        register("client_salon_obtenir_prestation_detail", "Détail d'une prestation passée.", schema(Map.of("prestationId", prop("integer", "ID prestation")), List.of("prestationId")), scOnly, false);
        register("client_salon_lister_mes_paiements", "Paiements effectués dans ce salon.", schema(null, null), scOnly, false);
        register("client_salon_obtenir_paiement_detail", "Détail d'un paiement.", schema(Map.of("paiementId", prop("integer", "ID paiement")), List.of("paiementId")), scOnly, false);
        register("client_salon_obtenir_panier", "Panier Click & Collect actuel dans ce salon.", schema(null, null), scOnly, false);
        register("client_salon_lister_mes_commandes", "Commandes de produits dans ce salon.", schema(null, null), scOnly, false);
        register("client_salon_obtenir_commande_detail", "Détail d'une commande.", schema(Map.of("commandeId", prop("integer", "ID commande")), List.of("commandeId")), scOnly, false);
        register("client_salon_lister_mes_reclamations", "Réclamations dans ce salon.", schema(null, null), scOnly, false);
        register("client_salon_obtenir_reclamation_detail", "Détail d'une réclamation.", schema(Map.of("reclamationId", prop("integer", "ID réclamation")), List.of("reclamationId")), scOnly, false);
        register("client_salon_obtenir_mon_avis_salon", "Mon avis déposé sur ce salon.", schema(null, null), scOnly, false);
        register("client_salon_lister_mes_avis_prestations", "Mes avis sur les prestations de ce salon.", schema(null, null), scOnly, false);
        register("client_salon_lister_prestations_a_noter", "Prestations terminées en attente d'avis.", schema(null, null), scOnly, false);
        register("client_salon_est_salon_favori", "Indique si ce salon est en favori.", schema(null, null), scOnly, false);
        register("client_salon_lister_coiffeurs_favoris", "Coiffeurs favoris du client dans ce salon.", schema(null, null), scOnly, false);
        register("client_salon_suggerer_rebooking", "Suggère un nouveau RDV basé sur la dernière prestation.", schema(null, null), scOnly, false);

        // Ecritures SC avec confirmation
        Map<String, Object> pRes = new HashMap<>();
        pRes.put("dateHeure", prop("string", "Date-heure yyyy-MM-dd'T'HH:mm"));
        pRes.put("varianteIds", propArray("integer", "IDs variantes"));
        pRes.put("coiffeurId", prop("integer", "ID coiffeur"));
        register("client_salon_reserver_rdv", "Réserve un rendez-vous (nécessite confirmation).", schema(pRes, List.of("dateHeure", "varianteIds")), scOnly, true);

        Map<String, Object> pAnnul = new HashMap<>();
        pAnnul.put("rdvId", prop("integer", "ID RDV"));
        pAnnul.put("motif", prop("string", "Motif de l'annulation"));
        register("client_salon_annuler_rdv", "Annule un rendez-vous (nécessite confirmation).", schema(pAnnul, List.of("rdvId")), scOnly, true);

        Map<String, Object> pDepl = new HashMap<>();
        pDepl.put("rdvId", prop("integer", "ID RDV existant"));
        pDepl.put("nouvelleDateHeure", prop("string", "Nouvelle date-heure yyyy-MM-dd'T'HH:mm"));
        pDepl.put("coiffeurId", prop("integer", "ID coiffeur"));
        register("client_salon_deplacer_rdv", "Déplace un rendez-vous (nécessite confirmation).", schema(pDepl, List.of("rdvId", "nouvelleDateHeure")), scOnly, true);

        // Panier direct
        Map<String, Object> pAddPanier = new HashMap<>();
        pAddPanier.put("produitId", prop("integer", "ID produit"));
        pAddPanier.put("quantite", prop("integer", "Quantité (défaut 1)"));
        register("client_salon_ajouter_au_panier", "Ajoute un produit au panier serveur du client.", schema(pAddPanier, List.of("produitId")), scOnly, false);

        Map<String, Object> pModPanier = new HashMap<>();
        pModPanier.put("produitId", prop("integer", "ID produit"));
        pModPanier.put("quantite", prop("integer", "Nouvelle quantité"));
        register("client_salon_modifier_quantite_panier", "Modifie la quantité d'un produit dans le panier.", schema(pModPanier, List.of("produitId", "quantite")), scOnly, false);
        register("client_salon_retirer_du_panier", "Retire un produit du panier.", schema(Map.of("produitId", prop("integer", "ID produit")), List.of("produitId")), scOnly, false);
        register("client_salon_vider_panier", "Vide le panier du salon (nécessite confirmation).", schema(null, null), scOnly, true);
        register("client_salon_passer_commande", "Valide la commande Click & Collect (nécessite confirmation).", schema(null, null), scOnly, true);

        // Avis
        Map<String, Object> pNoter = new HashMap<>();
        pNoter.put("lignePrestationId", prop("integer", "ID ligne de prestation"));
        pNoter.put("note", prop("integer", "Note de 1 à 5"));
        pNoter.put("commentaire", prop("string", "Commentaire"));
        register("client_salon_noter_prestation", "Dépose un avis sur une prestation (nécessite confirmation).", schema(pNoter, List.of("lignePrestationId", "note")), scOnly, true);

        Map<String, Object> pModAvis = new HashMap<>();
        pModAvis.put("avisId", prop("integer", "ID de l'avis"));
        pModAvis.put("note", prop("integer", "Note de 1 à 5"));
        pModAvis.put("commentaire", prop("string", "Commentaire"));
        register("client_salon_modifier_avis_prestation", "Modifie un avis sur une prestation (nécessite confirmation).", schema(pModAvis, List.of("avisId")), scOnly, true);
        register("client_salon_supprimer_avis_prestation", "Supprime un avis prestation (nécessite confirmation).", schema(Map.of("avisId", prop("integer", "ID avis")), List.of("avisId")), scOnly, true);

        Map<String, Object> pAvisSal = new HashMap<>();
        pAvisSal.put("note", prop("integer", "Note 1 à 5"));
        pAvisSal.put("commentaire", prop("string", "Commentaire"));
        register("client_salon_donner_avis_salon", "Dépose ou met à jour son avis sur le salon (nécessite confirmation).", schema(pAvisSal, List.of("note")), scOnly, true);
        register("client_salon_supprimer_avis_salon", "Supprime son avis salon (nécessite confirmation).", schema(null, null), scOnly, true);

        // Favoris direct
        register("client_salon_basculer_favori_salon", "Ajoute ou retire le salon des favoris.", schema(Map.of("ajouter", prop("boolean", "Vrai pour ajouter")), List.of("ajouter")), scOnly, false);
        Map<String, Object> pFavCoiff = new HashMap<>();
        pFavCoiff.put("coiffeurId", prop("integer", "ID coiffeur"));
        pFavCoiff.put("ajouter", prop("boolean", "Vrai pour ajouter"));
        register("client_salon_basculer_favori_coiffeur", "Ajoute ou retire un coiffeur des favoris.", schema(pFavCoiff, List.of("coiffeurId", "ajouter")), scOnly, false);

        Map<String, Object> pRecl = new HashMap<>();
        pRecl.put("objet", prop("string", "Objet (max 120 car.)"));
        pRecl.put("description", prop("string", "Détail de la réclamation"));
        register("client_salon_deposer_reclamation", "Dépose une réclamation (nécessite confirmation).", schema(pRecl, List.of("objet", "description")), scOnly, true);
    }

    private void initSharedTools() {
        Set<AssistantContextType> clients = Set.of(AssistantContextType.SALON_CLIENT, AssistantContextType.PLATEFORME_CLIENT);
        Set<AssistantContextType> all = Set.of(AssistantContextType.SALON_VISITEUR, AssistantContextType.SALON_CLIENT, AssistantContextType.PLATEFORME_VISITEUR, AssistantContextType.PLATEFORME_CLIENT);

        register("client_obtenir_profil_capillaire", "Consulte le profil capillaire du client connecté.", schema(null, null), clients, false);

        Map<String, Object> pProfil = new HashMap<>();
        pProfil.put("typeCheveux", prop("string", "Type de cheveux (crépus, bouclés, lisses...)"));
        pProfil.put("texture", prop("string", "Texture (fine, moyenne, épaisse)"));
        pProfil.put("longueur", prop("string", "Longueur"));
        pProfil.put("cuirChevelu", prop("string", "État cuir chevelu"));
        pProfil.put("etatCheveux", prop("string", "État des cheveux (secs, abîmés, sains)"));
        pProfil.put("allergiesProduits", prop("string", "Allergies connues"));
        register("client_mettre_a_jour_profil_capillaire", "Met à jour le profil capillaire (nécessite confirmation).", schema(pProfil, null), clients, true);

        register("realisation_aimer", "Aime ou retire le like d'une réalisation vidéo Kady's.", schema(Map.of("realisationId", prop("integer", "ID réalisation")), List.of("realisationId")), clients, false);
        register("realisation_lire_commentaires", "Lit les commentaires publics d'une vidéo Kady's.", schema(Map.of("realisationId", prop("integer", "ID réalisation")), List.of("realisationId")), all, false);

        Map<String, Object> pComm = new HashMap<>();
        pComm.put("realisationId", prop("integer", "ID réalisation"));
        pComm.put("contenu", prop("string", "Texte du commentaire (max 500 car.)"));
        register("realisation_commenter", "Ajoute un commentaire public sur une vidéo Kady's (nécessite confirmation).", schema(pComm, List.of("realisationId", "contenu")), clients, true);
    }

    private void initPlateformeTools() {
        Set<AssistantContextType> pvPc = Set.of(AssistantContextType.PLATEFORME_VISITEUR, AssistantContextType.PLATEFORME_CLIENT);

        register("plateforme_rechercher_salons", "Recherche des salons par nom, ville ou mot-clé.", schema(Map.of("recherche", prop("string", "Mot-clé")), List.of("recherche")), pvPc, false);
        register("plateforme_lister_salons", "Liste les salons actifs de la plateforme.", schema(null, null), pvPc, false);
        register("plateforme_salons_a_proximite", "Recherche les salons à proximité (géolocalisation).", schema(Map.of("rayonKm", prop("number", "Rayon en km")), null), pvPc, false);
        register("plateforme_salons_par_service", "Recherche les salons proposant une prestation spécifique (ex: tresses, défrisage).", schema(Map.of("nomService", prop("string", "Nom de la prestation")), List.of("nomService")), pvPc, false);

        register("plateforme_obtenir_salon", "Informations publiques d'un salon via son slug.", schema(Map.of("slugSalon", prop("string", "Slug du salon")), List.of("slugSalon")), pvPc, false);
        register("plateforme_services_salon", "Services et tarifs d'un salon.", schema(Map.of("slugSalon", prop("string", "Slug salon")), List.of("slugSalon")), pvPc, false);
        register("plateforme_avis_salon", "Avis publiés d'un salon.", schema(Map.of("slugSalon", prop("string", "Slug salon")), List.of("slugSalon")), pvPc, false);
        register("plateforme_realisations_salon", "Réalisations d'un salon.", schema(Map.of("slugSalon", prop("string", "Slug salon")), List.of("slugSalon")), pvPc, false);

        Map<String, Object> pDispPlat = new HashMap<>();
        pDispPlat.put("slugSalon", prop("string", "Slug salon"));
        pDispPlat.put("date", prop("string", "Date yyyy-MM-dd"));
        pDispPlat.put("varianteIds", propArray("integer", "IDs variantes"));
        register("plateforme_verifier_disponibilites_salon", "Disponibilités d'un salon.", schema(pDispPlat, List.of("slugSalon", "date", "varianteIds")), pvPc, false);

        Map<String, Object> pComp = new HashMap<>();
        pComp.put("slugs", propArray("string", "Slugs des salons à comparer (2 à 4)"));
        pComp.put("prestationRecherchee", prop("string", "Prestation à comparer"));
        register("plateforme_comparer_salons", "Compare 2 à 4 salons (notes, prix, prestations).", schema(pComp, List.of("slugs")), pvPc, false);

        register("plateforme_rechercher_produits", "Recherche transversale de cosmétiques capillaires.", schema(null, null), pvPc, false);
        register("plateforme_explorer_realisations", "Galerie transversale de réalisations.", schema(null, null), pvPc, false);
        register("plateforme_feed_kadys", "Flux immersif de vidéos Kady's.", schema(null, null), pvPc, false);
        register("plateforme_obtenir_realisation", "Détail d'une vidéo Kady's.", schema(Map.of("realisationId", prop("integer", "ID réalisation")), List.of("realisationId")), pvPc, false);
        register("plateforme_stories", "Stories actives des salons partenaires.", schema(null, null), pvPc, false);

        Map<String, Object> pFaq = Map.of("sujet", propEnum("string", "Sujet FAQ", List.of("RESERVATION", "CLICK_COLLECT", "COMPTE", "KADYS", "PAIEMENT", "DONNEES_PERSONNELLES", "DEVENIR_PARTENAIRE", "ANNULATION")));
        register("plateforme_expliquer_fonctionnement", "Explique le fonctionnement d'un service de la plateforme.", schema(pFaq, List.of("sujet")), pvPc, false);

        // UI Plateforme
        Map<String, Object> pUiSal = new HashMap<>();
        pUiSal.put("slugSalon", prop("string", "Slug salon"));
        pUiSal.put("section", prop("string", "Section (ACCUEIL, SERVICES, BOUTIQUE...)"));
        register("ui_ouvrir_salon", "Action UI : ouvre la vitrine du salon spécifié.", schema(pUiSal, List.of("slugSalon")), pvPc, false);

        register("ui_plateforme_naviguer", "Action UI : navigation plateforme (EXPLORER, KADYS, MES_RDV...).", schema(Map.of("destination", prop("string", "Destination")), List.of("destination")), pvPc, false);
        register("ui_demander_geolocalisation", "Action UI : demande l'autorisation de géolocalisation au navigateur.", schema(null, null), pvPc, false);
        register("ui_ouvrir_realisation", "Action UI : ouvre le lecteur de vidéo Kady's.", schema(Map.of("realisationId", prop("integer", "ID vidéo")), List.of("realisationId")), pvPc, false);

        Map<String, Object> pUiPrepRdv = new HashMap<>();
        pUiPrepRdv.put("slugSalon", prop("string", "Slug du salon"));
        pUiPrepRdv.put("varianteIds", propArray("integer", "IDs variantes"));
        pUiPrepRdv.put("date", prop("string", "Date yyyy-MM-dd"));
        pUiPrepRdv.put("heureDebut", prop("string", "Heure début"));
        register("ui_preparer_rdv_salon", "Action UI : redirige vers le tunnel de réservation d'un salon avec pré-sélection.", schema(pUiPrepRdv, List.of("slugSalon", "varianteIds")), pvPc, false);
    }

    private void initClientPlateformeTools() {
        Set<AssistantContextType> pcOnly = Set.of(AssistantContextType.PLATEFORME_CLIENT);

        register("client_plateforme_obtenir_compte", "Informations du profil client connecté.", schema(null, null), pcOnly, false);
        register("client_plateforme_resume_activite", "Vue consolidée des activités du client (prochain RDV, commandes, alertes).", schema(null, null), pcOnly, false);
        register("client_plateforme_lister_rdv", "Tous les rendez-vous du client à travers tous les salons.", schema(null, null), pcOnly, false);
        register("client_plateforme_obtenir_rdv_detail", "Détail d'un rendez-vous multi-salons.", schema(Map.of("rdvId", prop("integer", "ID RDV")), List.of("rdvId")), pcOnly, false);
        register("client_plateforme_lister_prestations", "Historique multi-salons des prestations passées.", schema(null, null), pcOnly, false);
        register("client_plateforme_obtenir_prestation_detail", "Détail d'une prestation passée.", schema(Map.of("prestationId", prop("integer", "ID prestation")), List.of("prestationId")), pcOnly, false);
        register("client_plateforme_lister_paiements", "Historique de tous les paiements multi-salons.", schema(null, null), pcOnly, false);
        register("client_plateforme_obtenir_paiement_detail", "Détail d'un paiement.", schema(Map.of("paiementId", prop("integer", "ID paiement")), List.of("paiementId")), pcOnly, false);
        register("client_plateforme_lister_salons_favoris", "Liste de tous ses salons favoris.", schema(null, null), pcOnly, false);
        register("client_plateforme_lister_coiffeurs_favoris", "Liste de tous ses coiffeurs favoris.", schema(null, null), pcOnly, false);
        register("client_plateforme_lister_mes_avis", "Avis déposés par le client.", schema(null, null), pcOnly, false);
        register("client_plateforme_lister_paniers", "Paniers Click & Collect en cours dans les différents salons.", schema(null, null), pcOnly, false);
        register("client_plateforme_lister_commandes", "Toutes ses commandes Click & Collect.", schema(null, null), pcOnly, false);
        register("client_plateforme_obtenir_commande_detail", "Détail d'une commande.", schema(Map.of("commandeId", prop("integer", "ID commande")), List.of("commandeId")), pcOnly, false);
        register("client_plateforme_lister_reclamations", "Réclamations déposées par le client.", schema(null, null), pcOnly, false);
        register("client_plateforme_obtenir_reclamation_detail", "Détail d'une réclamation.", schema(Map.of("reclamationId", prop("integer", "ID réclamation")), List.of("reclamationId")), pcOnly, false);
        register("client_plateforme_mes_inspirations", "Vidéos Kady's aimées par le client.", schema(null, null), pcOnly, false);
        register("client_plateforme_suivi_donnees_personnelles", "Historique des exports et demandes RGPD.", schema(null, null), pcOnly, false);
        register("client_plateforme_recommander_salons", "Recommandation de salons selon l'historique et les favoris du client.", schema(null, null), pcOnly, false);

        // Ecritures PC
        Map<String, Object> pResPlat = new HashMap<>();
        pResPlat.put("slugSalon", prop("string", "Slug du salon"));
        pResPlat.put("dateHeure", prop("string", "Date-heure yyyy-MM-dd'T'HH:mm"));
        pResPlat.put("varianteIds", propArray("integer", "IDs variantes"));
        pResPlat.put("coiffeurId", prop("integer", "ID coiffeur"));
        register("client_plateforme_reserver_rdv", "Réserve un RDV dans un salon (nécessite confirmation).", schema(pResPlat, List.of("slugSalon", "dateHeure", "varianteIds")), pcOnly, true);

        Map<String, Object> pAnnulPlat = new HashMap<>();
        pAnnulPlat.put("rdvId", prop("integer", "ID RDV"));
        pAnnulPlat.put("motif", prop("string", "Motif"));
        register("client_plateforme_annuler_rdv", "Annule un rendez-vous (nécessite confirmation).", schema(pAnnulPlat, List.of("rdvId")), pcOnly, true);

        Map<String, Object> pDeplPlat = new HashMap<>();
        pDeplPlat.put("rdvId", prop("integer", "ID RDV"));
        pDeplPlat.put("nouvelleDateHeure", prop("string", "Nouvelle date-heure"));
        register("client_plateforme_deplacer_rdv", "Déplace un RDV (nécessite confirmation).", schema(pDeplPlat, List.of("rdvId", "nouvelleDateHeure")), pcOnly, true);

        Map<String, Object> pFavPlat = new HashMap<>();
        pFavPlat.put("slugSalon", prop("string", "Slug salon"));
        pFavPlat.put("ajouter", prop("boolean", "Vrai pour ajouter"));
        register("client_plateforme_basculer_favori_salon", "Bascule un salon en favori.", schema(pFavPlat, List.of("slugSalon", "ajouter")), pcOnly, false);

        register("client_plateforme_vider_tous_paniers", "Vide tous les paniers en cours (nécessite confirmation).", schema(null, null), pcOnly, true);

        Map<String, Object> pReclPlat = new HashMap<>();
        pReclPlat.put("objet", prop("string", "Objet"));
        pReclPlat.put("description", prop("string", "Description"));
        pReclPlat.put("slugSalon", prop("string", "Slug salon éventuel"));
        register("client_plateforme_deposer_reclamation", "Dépose une réclamation (nécessite confirmation).", schema(pReclPlat, List.of("objet", "description")), pcOnly, true);

        Map<String, Object> pCompteUp = new HashMap<>();
        pCompteUp.put("prenom", prop("string", "Prénom"));
        pCompteUp.put("nom", prop("string", "Nom"));
        pCompteUp.put("telephone", prop("string", "Téléphone"));
        register("client_plateforme_mettre_a_jour_compte", "Met à jour son profil (nécessite confirmation).", schema(pCompteUp, null), pcOnly, true);

        register("client_plateforme_demander_export_donnees", "Demande un export RGPD (nécessite confirmation).", schema(null, null), pcOnly, true);
    }
}

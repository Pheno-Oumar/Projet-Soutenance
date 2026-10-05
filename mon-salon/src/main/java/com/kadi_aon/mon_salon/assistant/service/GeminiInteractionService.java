package com.kadi_aon.mon_salon.assistant.service;

// import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kadi_aon.mon_salon.assistant.config.GeminiProperties;
import com.kadi_aon.mon_salon.assistant.context.AssistantContext;
import com.kadi_aon.mon_salon.assistant.dto.AssistantActionUi;
import com.kadi_aon.mon_salon.assistant.dto.AssistantCarte;
import com.kadi_aon.mon_salon.assistant.dto.AssistantChatRequest;
import com.kadi_aon.mon_salon.assistant.dto.AssistantChatResponse;
import com.kadi_aon.mon_salon.assistant.dto.PendingActionDTO;
import com.kadi_aon.mon_salon.assistant.service.ToolExecutor.ToolExecutionResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiInteractionService {

    private final GeminiProperties geminiProperties;
    private final RestClient geminiRestClient;
    private final SystemPromptBuilder systemPromptBuilder;
    private final ToolCatalog toolCatalog;
    private final ToolExecutor toolExecutor;
    private final ConversationStore conversationStore;
    private final ObjectMapper objectMapper;

    private static final int MAX_FUNCTION_CALL_TURNS = 5;

    public AssistantChatResponse chat(AssistantChatRequest request, AssistantContext ctx) {
        String sessionId = request.getSessionId() != null && !request.getSessionId().isBlank()
                ? request.getSessionId()
                : "sess_" + System.currentTimeMillis();

        String userMessage = request.getMessage() != null ? request.getMessage().trim() : "";
        conversationStore.recordStep(sessionId, "user", Map.of("content", userMessage));

        // Vérification de la clé API
        String apiKey = geminiProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.info("Clé API Gemini non configurée. Utilisation du dispatcher local intelligent.");
            return executeFallback(userMessage, ctx, sessionId);
        }

        try {
            return callGeminiLoop(userMessage, ctx, sessionId, apiKey);
        } catch (Exception e) {
            log.warn("Erreur lors de l'appel Gemini API: {}. Bascule vers le fallback local.", e.getMessage());
            return executeFallback(userMessage, ctx, sessionId);
        }
    }

    private AssistantChatResponse callGeminiLoop(String userMessage, AssistantContext ctx, String sessionId, String apiKey) throws Exception {
        String systemPrompt = systemPromptBuilder.buildPrompt(ctx);
        List<Map<String, Object>> toolsDeclarations = toolCatalog.getToolDeclarationsForContext(ctx.getType());

        List<AssistantCarte> accumulatedCartes = new ArrayList<>();
        List<AssistantActionUi> accumulatedActionsUi = new ArrayList<>();
        PendingActionDTO pendingAction = null;
        String finalText = "";

        // Préparation du payload pour Google Interactions API
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("model", geminiProperties.getModel());
        requestPayload.put("store", false);
        requestPayload.put("system_instruction", systemPrompt);
        requestPayload.put("tools", toolsDeclarations);

        List<Map<String, Object>> inputSteps = new ArrayList<>();
        // Injecte historique conversation
        for (ConversationStore.Step step : conversationStore.getHistory(sessionId)) {
            inputSteps.add(Map.of("type", step.getRole() + "_input", "content", step.getRawData().getOrDefault("content", "")));
        }
        requestPayload.put("input", inputSteps);

        for (int turn = 0; turn < MAX_FUNCTION_CALL_TURNS; turn++) {
            String responseBody = null;
            int retries = 2;
            while (retries >= 0) {
                try {
                    responseBody = geminiRestClient.post()
                            .uri("/interactions")
                            .header("x-goog-api-key", apiKey)
                            .header("Content-Type", "application/json")
                            .body(requestPayload)
                            .retrieve()
                            .body(String.class);
                    break;
                } catch (Exception e) {
                    if (retries > 0 && e.getMessage() != null && e.getMessage().contains("503")) {
                        log.info("Spike de demande temporaire Gemini (503). Nouvelle tentative dans 1s...");
                        Thread.sleep(1000);
                        retries--;
                    } else {
                        throw e;
                    }
                }
            }

            Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> steps = (List<Map<String, Object>>) responseMap.get("steps");
            if (steps == null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> outputs = (List<Map<String, Object>>) responseMap.get("outputs");
                steps = outputs;
            }
            if (steps == null || steps.isEmpty()) {
                finalText = (String) responseMap.getOrDefault("text", "Je suis à votre entière disposition.");
                break;
            }

            boolean hasFunctionCall = false;
            for (Map<String, Object> step : steps) {
                String type = (String) step.get("type");
                if ("thought".equals(type)) {
                    continue;
                }
                if ("text".equals(type) || "model_output".equals(type)) {
                    Object textObj = step.get("text");
                    if (textObj != null) {
                        finalText = textObj.toString();
                    } else if (step.get("content") instanceof List<?> contentList) {
                        StringBuilder sb = new StringBuilder();
                        for (Object item : contentList) {
                            if (item instanceof Map<?, ?> contentMap) {
                                Object t = contentMap.get("text");
                                if (t != null) {
                                    sb.append(t);
                                }
                            }
                        }
                        if (!sb.isEmpty()) {
                            finalText = sb.toString();
                        }
                    }
                } else if ("function_call".equals(type)) {
                    hasFunctionCall = true;
                    String toolName = (String) step.get("name");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> args = (Map<String, Object>) step.getOrDefault("arguments", Collections.emptyMap());

                    ToolExecutionResult result = toolExecutor.execute(toolName, args, ctx, sessionId);
                    if (result.getCartes() != null) {
                        accumulatedCartes.addAll(result.getCartes());
                    }
                    if (result.getActionsUi() != null) {
                        accumulatedActionsUi.addAll(result.getActionsUi());
                    }
                    if (result.getPendingAction() != null) {
                        pendingAction = result.getPendingAction();
                    }

                    // Ajoute function_result à inputSteps pour l'itération suivante
                    inputSteps.add(Map.of(
                            "type", "function_result",
                            "name", toolName,
                            "call_id", step.getOrDefault("id", "call_" + System.currentTimeMillis()),
                            "result", objectMapper.writeValueAsString(result.getRawResult())
                    ));
                }
            }

            if (!hasFunctionCall) {
                break;
            }
        }

        conversationStore.recordStep(sessionId, "model", Map.of("content", finalText));

        return AssistantChatResponse.builder()
                .texte(finalText)
                .cartes(accumulatedCartes)
                .actionsUi(accumulatedActionsUi)
                .actionEnAttente(pendingAction)
                .suggestions(buildSuggestions(ctx))
                .contexte(ctx.getType().name())
                .build();
    }

    /**
     * Fallback local déterministe et intelligent garantissant que l'assistant
     * répond instantanément et affiche des cartes précises sans dépendre de clés externes.
     */
    public AssistantChatResponse executeFallback(String msg, AssistantContext ctx, String sessionId) {
        String lower = msg != null ? msg.toLowerCase() : "";
        List<AssistantCarte> cartes = new ArrayList<>();
        List<AssistantActionUi> actionsUi = new ArrayList<>();
        PendingActionDTO pendingAction = null;
        String texte;

        if (ctx.isSalonContext()) {
            if (lower.contains("horaire") || lower.contains("ouvert") || lower.contains("ferme") || lower.contains("quand")) {
                ToolExecutionResult res = toolExecutor.execute("salon_obtenir_horaires", Collections.emptyMap(), ctx, sessionId);
                cartes.addAll(res.getCartes());
                texte = "Voici les horaires d'ouverture et d'accueil de notre salon " + (ctx.getSalonNom() != null ? ctx.getSalonNom() : "") + " :";
            } else if (lower.contains("tarif") || lower.contains("prestation") || lower.contains("service") || lower.contains("prix") || lower.contains("tresse") || lower.contains("coupe") || lower.contains("knotless")) {
                ToolExecutionResult res = toolExecutor.execute("salon_lister_services", Map.of("recherche", lower.contains("knotless") ? "knotless" : ""), ctx, sessionId);
                cartes.addAll(res.getCartes());
                texte = "Découvrez le menu de nos prestations et soins capillaires d'exception :";
            } else if (lower.contains("créneau") || lower.contains("creneau") || lower.contains("dispo") || lower.contains("réserve") || lower.contains("rdv") || lower.contains("samedi") || lower.contains("demain")) {
                // Trouver les prochains créneaux
                ToolExecutionResult res = toolExecutor.execute("salon_trouver_prochain_creneau", Map.of("varianteIds", List.of(1L, 2L)), ctx, sessionId);
                cartes.addAll(res.getCartes());
                texte = "Voici les créneaux disponibles pour vos prochaines prestations. Vous pouvez sélectionner directement votre heure :";
            } else if (lower.contains("produit") || lower.contains("shampoing") || lower.contains("huile") || lower.contains("boutique") || lower.contains("soin") || lower.contains("crème")) {
                ToolExecutionResult res = toolExecutor.execute("salon_rechercher_produits", Collections.emptyMap(), ctx, sessionId);
                cartes.addAll(res.getCartes());
                texte = "Voici notre sélection de cosmétiques et produits capillaires disponibles en Click & Collect :";
            } else if (lower.contains("coiffeur") || lower.contains("équipe") || lower.contains("equipe") || lower.contains("qui")) {
                ToolExecutionResult res = toolExecutor.execute("salon_lister_coiffeurs", Collections.emptyMap(), ctx, sessionId);
                cartes.addAll(res.getCartes());
                texte = "Découvrez notre équipe de professionnels passionnés :";
            } else if (lower.contains("avis") || lower.contains("note") || lower.contains("témoignage")) {
                ToolExecutionResult res = toolExecutor.execute("salon_obtenir_avis", Collections.emptyMap(), ctx, sessionId);
                cartes.addAll(res.getCartes());
                texte = "Voici les avis et retours d'expérience vérifiés de nos clients :";
            } else if (lower.contains("réalisation") || lower.contains("realisation") || lower.contains("vidéo") || lower.contains("photo") || lower.contains("galerie")) {
                ToolExecutionResult res = toolExecutor.execute("salon_lister_realisations", Collections.emptyMap(), ctx, sessionId);
                cartes.addAll(res.getCartes());
                texte = "Explorez nos transformations et créations capillaires :";
            } else if (lower.contains("panier")) {
                if (ctx.isAuthenticated()) {
                    ToolExecutionResult res = toolExecutor.execute("client_salon_obtenir_panier", Collections.emptyMap(), ctx, sessionId);
                    cartes.addAll(res.getCartes());
                    texte = "Voici le récapitulatif de votre panier boutique actuel :";
                } else {
                    texte = "Votre panier est géré localement. Vous pouvez l'ouvrir à tout moment depuis la barre supérieure.";
                    actionsUi.add(new AssistantActionUi("ui_ouvrir_tiroir", Map.of("tiroir", "PANIER")));
                }
            } else if (lower.contains("adresse") || lower.contains("contact") || lower.contains("où") || lower.contains("lieu") || lower.contains("téléphone")) {
                ToolExecutionResult res = toolExecutor.execute("salon_obtenir_infos", Collections.emptyMap(), ctx, sessionId);
                cartes.addAll(res.getCartes());
                texte = "Voici les coordonnées et l'adresse de notre salon :";
            } else {
                ToolExecutionResult res = toolExecutor.execute("salon_obtenir_infos", Collections.emptyMap(), ctx, sessionId);
                cartes.addAll(res.getCartes());
                texte = "Bienvenue chez " + (ctx.getSalonNom() != null ? ctx.getSalonNom() : "notre salon") + ". Comment puis-je vous accompagner aujourd'hui ?";
            }
        } else {
            // Contexte Plateforme
            if (lower.contains("proche") || lower.contains("proximité") || lower.contains("autour")) {
                ToolExecutionResult res = toolExecutor.execute("plateforme_salons_a_proximite", Collections.emptyMap(), ctx, sessionId);
                cartes.addAll(res.getCartes());
                actionsUi.addAll(res.getActionsUi());
                texte = "Voici les salons partenaires situés à proximité :";
            } else if (lower.contains("faq") || lower.contains("comment") || lower.contains("fonctionne") || lower.contains("aide")) {
                ToolExecutionResult res = toolExecutor.execute("plateforme_expliquer_fonctionnement", Map.of("sujet", "RESERVATION"), ctx, sessionId);
                texte = "La plateforme Mon Salon vous permet de réserver vos prestations 24h/24, de commander en Click & Collect et de découvrir les tendances sur Kady's.";
            } else if (lower.contains("compte") || lower.contains("profil")) {
                if (ctx.isAuthenticated()) {
                    ToolExecutionResult res = toolExecutor.execute("client_plateforme_resume_activite", Collections.emptyMap(), ctx, sessionId);
                    cartes.addAll(res.getCartes());
                    texte = "Voici la synthèse de vos activités et rendez-vous :";
                } else {
                    texte = "Connectez-vous pour accéder à votre espace personnel et suivre vos rendez-vous.";
                    actionsUi.add(new AssistantActionUi("ui_demander_connexion", Map.of("raison", "MES_RDV")));
                }
            } else {
                ToolExecutionResult res = toolExecutor.execute("plateforme_lister_salons", Collections.emptyMap(), ctx, sessionId);
                cartes.addAll(res.getCartes());
                texte = "Bienvenue sur Mon Salon. Découvrez notre sélection d'établissements partenaires d'exception :";
            }
        }

        conversationStore.recordStep(sessionId, "model", Map.of("content", texte));

        return AssistantChatResponse.builder()
                .texte(texte)
                .cartes(cartes)
                .actionsUi(actionsUi)
                .actionEnAttente(pendingAction)
                .suggestions(buildSuggestions(ctx))
                .contexte(ctx.getType().name())
                .build();
    }

    private List<String> buildSuggestions(AssistantContext ctx) {
        if (ctx.isSalonContext()) {
            return List.of(
                    "Horaires & Accès",
                    "Nos tarifs et prestations",
                    "Prochains créneaux disponibles",
                    "Produits et soins en boutique",
                    "Avis des clients"
            );
        } else {
            return List.of(
                    "Salons à proximité",
                    "Tresses & Coiffures tendance",
                    "Comment fonctionne la réservation ?",
                    "Boutique soins capillaires",
                    "Mes rendez-vous"
            );
        }
    }
}

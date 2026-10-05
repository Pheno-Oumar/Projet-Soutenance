package com.kadi_aon.mon_salon.assistant.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kadi_aon.mon_salon.assistant.context.AssistantContext;
import com.kadi_aon.mon_salon.assistant.context.AssistantContextResolver;
import com.kadi_aon.mon_salon.assistant.dto.AssistantCarte;
import com.kadi_aon.mon_salon.assistant.dto.AssistantChatRequest;
import com.kadi_aon.mon_salon.assistant.dto.AssistantChatResponse;
import com.kadi_aon.mon_salon.assistant.service.GeminiInteractionService;
import com.kadi_aon.mon_salon.assistant.service.PendingActionService;
import com.kadi_aon.mon_salon.assistant.service.PendingActionService.PendingAction;
import com.kadi_aon.mon_salon.assistant.service.ToolExecutor;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
@Tag(name = "Assistant Virtuel IA (Gemini)", description = "Endpoints de conversation intelligente et confirmation d'actions assistées")
public class AssistantController {

    private final AssistantContextResolver contextResolver;
    private final GeminiInteractionService geminiInteractionService;
    private final PendingActionService pendingActionService;
    private final ToolExecutor toolExecutor;

    @PostMapping("/chat")
    @Operation(summary = "Échanger avec l'assistant virtuel IA (multi-contextes)")
    public ResponseEntity<APIResponse<AssistantChatResponse>> chat(
            @Valid @RequestBody AssistantChatRequest request,
            Principal principal) {

        AssistantContext ctx = contextResolver.resolve(request, principal);
        AssistantChatResponse response = geminiInteractionService.chat(request, ctx);

        return ResponseEntity.ok(new APIResponse<>(true, "Réponse assistant générée", response));
    }

    @PostMapping("/actions/{id}/confirmer")
    @Operation(summary = "Confirmer et exécuter de manière déterministe une action en attente")
    public ResponseEntity<APIResponse<AssistantCarte>> confirmerAction(
            @PathVariable String id,
            @RequestParam(required = false) String sessionId,
            Principal principal) {

        String email = principal != null ? principal.getName() : null;
        PendingAction action = pendingActionService.getValidAction(id, sessionId, email);

        if (action == null) {
            return ResponseEntity.badRequest().body(new APIResponse<>(
                    false,
                    "Cette action a expiré ou n'est plus valide. Veuillez reformuler votre demande auprès de l'assistant.",
                    new AssistantCarte("ERREUR", "Action expirée", Map.of("code", "ACTION_EXPIREE"))
            ));
        }

        // Résolution du contexte minimal pour l'exécution
        AssistantChatRequest pseudoReq = new AssistantChatRequest();
        if (action.getDonnees() != null && action.getDonnees().containsKey("slugSalon")) {
            pseudoReq.setSlugSalon((String) action.getDonnees().get("slugSalon"));
        }
        AssistantContext ctx = contextResolver.resolve(pseudoReq, principal);

        AssistantCarte resultatCarte = toolExecutor.executeConfirmedAction(action, ctx);
        pendingActionService.removeAction(id);

        return ResponseEntity.ok(new APIResponse<>(true, "Action confirmée et exécutée", resultatCarte));
    }

    @PostMapping("/actions/{id}/annuler")
    @Operation(summary = "Annuler une action en attente de confirmation")
    public ResponseEntity<APIResponse<Void>> annulerAction(@PathVariable String id) {
        pendingActionService.removeAction(id);
        return ResponseEntity.ok(new APIResponse<>(true, "Action annulée avec succès", null));
    }
}

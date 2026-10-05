package com.kadi_aon.mon_salon.assistant.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantChatResponse {

    private String texte;

    @Builder.Default
    private List<AssistantCarte> cartes = new ArrayList<>();

    @Builder.Default
    private List<AssistantActionUi> actionsUi = new ArrayList<>();

    @Builder.Default
    private List<String> suggestions = new ArrayList<>();

    private PendingActionDTO actionEnAttente;

    private String contexte;
}

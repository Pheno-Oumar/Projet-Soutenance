package com.kadi_aon.mon_salon.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantChatRequest {

    @NotBlank(message = "Le message utilisateur est obligatoire")
    private String message;

    private String sessionId;

    private String slugSalon;

    private String pageCourante;

    private Long ressourceCouranteId;

    private Double latitude;

    private Double longitude;
}

package com.kadi_aon.mon_salon.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantCarte {
    private String type;
    private String titre;
    private Object donnees;
}

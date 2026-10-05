package com.kadi_aon.mon_salon.assistant.dto;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingActionDTO {
    private String actionId;
    private String outil;
    private String resume;
    private LocalDateTime expireA;
    private Map<String, Object> donnees;
}

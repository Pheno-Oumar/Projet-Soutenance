package com.kadi_aon.mon_salon.assistant.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantActionUi {
    private String type;
    private Map<String, Object> payload;
}

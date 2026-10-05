package com.kadi_aon.mon_salon.account.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresIn;
    private CompteSummaryDTO compte;
    private String slugSalon;
    private String nomSalon;
    private String logoUrl;
    private List<String> rolesSalon;
}

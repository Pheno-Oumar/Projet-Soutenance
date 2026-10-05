package com.kadi_aon.mon_salon.story.dto;

import java.util.List;

public record SalonStoriesDTOResponse(
        Long salonId,
        String salonNom,
        String salonSlug,
        String salonLogoUrl,
        int nombreStories,
        List<StoryDTOResponse> stories
) {}

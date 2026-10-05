package com.kadi_aon.mon_salon.story.dto;

import java.time.LocalDateTime;

import com.kadi_aon.mon_salon.story.enums.TypeMediaStory;

public record StoryDTOResponse(
        Long id,
        String mediaUrl,
        TypeMediaStory typeMedia,
        LocalDateTime dateCreation,
        LocalDateTime dateExpiration,
        String salonSlug,
        String salonNom,
        String salonLogoUrl
) {}

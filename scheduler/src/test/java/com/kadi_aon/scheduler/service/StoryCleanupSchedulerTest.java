package com.kadi_aon.scheduler.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kadi_aon.scheduler.entity.StorySalon;
import com.kadi_aon.scheduler.enums.TypeMediaStory;
import com.kadi_aon.scheduler.repository.StorySalonRepository;

@ExtendWith(MockitoExtension.class)
class StoryCleanupSchedulerTest {

    @Mock
    private StorySalonRepository storySalonRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private StoryCleanupScheduler storyCleanupScheduler;

    @Test
    @DisplayName("Scheduler : purger les stories de plus de 24h sur Cloudinary et en BDD")
    void testPurgerStoriesExpirees() {
        StorySalon expiredVideo = StorySalon.builder()
                .id(1L)
                .salonId(2L)
                .auteurId(5L)
                .mediaUrl("https://cloudinary.com/story1.mp4")
                .typeMedia(TypeMediaStory.VIDEO)
                .dateExpiration(LocalDateTime.now().minusMinutes(10))
                .build();

        StorySalon expiredImage = StorySalon.builder()
                .id(2L)
                .salonId(2L)
                .auteurId(5L)
                .mediaUrl("https://cloudinary.com/story2.webp")
                .typeMedia(TypeMediaStory.IMAGE)
                .dateExpiration(LocalDateTime.now().minusHours(2))
                .build();

        List<StorySalon> list = List.of(expiredVideo, expiredImage);

        when(storySalonRepository.findByDateExpirationBefore(any(LocalDateTime.class)))
                .thenReturn(list);

        storyCleanupScheduler.purgerStoriesExpirees();

        verify(cloudinaryService).deleteMediaByUrl("https://cloudinary.com/story1.mp4", "video");
        verify(cloudinaryService).deleteMediaByUrl("https://cloudinary.com/story2.webp", "image");
        verify(storySalonRepository).deleteAll(list);
    }

    @Test
    @DisplayName("Scheduler : aucune story expirée -> aucune suppression")
    void testPurgerStoriesExpirees_AucuneExpiree() {
        when(storySalonRepository.findByDateExpirationBefore(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        storyCleanupScheduler.purgerStoriesExpirees();

        verify(cloudinaryService, never()).deleteMediaByUrl(any(), any());
        verify(storySalonRepository, never()).deleteAll(any());
    }
}

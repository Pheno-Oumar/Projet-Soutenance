package com.kadi_aon.mon_salon.common.deserializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlexibleLocalDateTimeDeserializerTest {

    static class TestDto {
        @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
        public LocalDateTime dateHeure;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should successfully parse timestamp with duplicate seconds (e.g. 2026-10-05T09:30:00:00)")
    void shouldParseDuplicateSeconds() throws Exception {
        String json = "{\"dateHeure\":\"2026-10-05T09:30:00:00\"}";
        TestDto dto = objectMapper.readValue(json, TestDto.class);

        assertThat(dto.dateHeure).isEqualTo(LocalDateTime.of(2026, 10, 5, 9, 30, 0));
    }

    @Test
    @DisplayName("Should successfully parse standard ISO timestamp without seconds (e.g. 2026-10-05T09:30)")
    void shouldParseWithoutSeconds() throws Exception {
        String json = "{\"dateHeure\":\"2026-10-05T09:30\"}";
        TestDto dto = objectMapper.readValue(json, TestDto.class);

        assertThat(dto.dateHeure).isEqualTo(LocalDateTime.of(2026, 10, 5, 9, 30, 0));
    }

    @Test
    @DisplayName("Should successfully parse standard ISO timestamp with seconds (e.g. 2026-10-05T09:30:45)")
    void shouldParseWithSeconds() throws Exception {
        String json = "{\"dateHeure\":\"2026-10-05T09:30:45\"}";
        TestDto dto = objectMapper.readValue(json, TestDto.class);

        assertThat(dto.dateHeure).isEqualTo(LocalDateTime.of(2026, 10, 5, 9, 30, 45));
    }

    @Test
    @DisplayName("Should successfully parse ISO timestamp with offset or UTC Z (e.g. 2026-10-05T09:30:00Z)")
    void shouldParseWithUtcZone() throws Exception {
        String json = "{\"dateHeure\":\"2026-10-05T09:30:00Z\"}";
        TestDto dto = objectMapper.readValue(json, TestDto.class);

        assertThat(dto.dateHeure).isEqualTo(LocalDateTime.of(2026, 10, 5, 9, 30, 0));
    }

    @Test
    @DisplayName("Should return null when string is blank or null")
    void shouldHandleNullOrBlank() throws Exception {
        String json = "{\"dateHeure\":\"\"}";
        TestDto dto = objectMapper.readValue(json, TestDto.class);

        assertThat(dto.dateHeure).isNull();
    }
}

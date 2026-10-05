package com.kadi_aon.mon_salon.common.deserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Désérialiseur Jackson tolérant et sécurisé pour {@link LocalDateTime}.
 * <p>
 * Permet de parser de manière résiliente différentes variantes de représentations ISO 8601 :
 * <ul>
 *   <li>ISO standard avec secondes (ex: "2026-10-05T09:30:00")</li>
 *   <li>ISO standard sans secondes (ex: "2026-10-05T09:30")</li>
 *   <li>ISO avec fuseau horaire / UTC (ex: "2026-10-05T09:30:00Z" ou "+02:00")</li>
 *   <li>Nettoyage des doublons de secondes accidentels (ex: "2026-10-05T09:30:00:00")</li>
 * </ul>
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        text = text.trim();

        // Nettoyage si double suffixe de secondes comme "2026-10-05T09:30:00:00"
        if (text.matches("^.*T\\d{1,2}:\\d{2}:\\d{2}:\\d{2}$")) {
            text = text.substring(0, text.lastIndexOf(':'));
        }

        try {
            return LocalDateTime.parse(text);
        } catch (Exception e) {
            try {
                return OffsetDateTime.parse(text).toLocalDateTime();
            } catch (Exception e2) {
                try {
                    return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
                } catch (Exception e3) {
                    throw new IOException("Format de date et heure invalide : " + text, e3);
                }
            }
        }
    }
}

package com.kadi_aon.scheduler.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud_name}") String cloudName,
            @Value("${cloudinary.api_key}") String apiKey,
            @Value("${cloudinary.api_secret}") String apiSecret) {

        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        this.cloudinary = new Cloudinary(config);
    }

    public void deleteRawFile(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "raw"));
            log.info("Fichier raw Cloudinary supprimé - publicId: {}, résultat: {}", publicId, result.get("result"));
        } catch (IOException e) {
            log.error("Erreur lors de la suppression du fichier Cloudinary {}: {}", publicId, e.getMessage());
            throw new RuntimeException("Impossible de supprimer le fichier sur Cloudinary: " + e.getMessage(), e);
        }
    }

    public void deleteMediaByUrl(String url, String resourceType) {
        if (url == null || url.isBlank() || !url.contains("cloudinary.com")) {
            return;
        }

        try {
            String publicId = extractPublicId(url);
            if (publicId != null && !publicId.isBlank()) {
                Map<String, Object> params = ObjectUtils.asMap("resource_type", resourceType != null ? resourceType : "image");
                cloudinary.uploader().destroy(publicId, params);
                log.info("Média Cloudinary supprimé (type: {}, public_id: {})", resourceType, publicId);
            }
        } catch (Exception e) {
            log.warn("Impossible de supprimer le média Cloudinary {} : {}", url, e.getMessage());
        }
    }

    private String extractPublicId(String url) {
        int uploadIndex = url.indexOf("/upload/");
        if (uploadIndex == -1) {
            return null;
        }

        String pathAfterUpload = url.substring(uploadIndex + "/upload/".length());
        if (pathAfterUpload.matches("^v\\d+/.*")) {
            pathAfterUpload = pathAfterUpload.replaceFirst("^v\\d+/", "");
        }

        int dotIndex = pathAfterUpload.lastIndexOf('.');
        if (dotIndex != -1) {
            pathAfterUpload = pathAfterUpload.substring(0, dotIndex);
        }

        return pathAfterUpload;
    }
}

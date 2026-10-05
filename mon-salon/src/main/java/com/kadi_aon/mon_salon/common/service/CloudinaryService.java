package com.kadi_aon.mon_salon.common.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
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

    public String uploadLogo(MultipartFile file, String slugSalon) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier fourni est vide ou manquant.");
        }

        Map<String, Object> options = ObjectUtils.asMap(
                "folder", "mon-salon/logos/" + slugSalon,
                "format", "webp",         // 🔥 Force la conversion en WebP
                "quality", "auto",        // Compression optimisée
                "width", 800,             // Redimensionne
                "crop", "scale"           // Conserve proportions
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(), options);
        String secureUrl = (String) uploadResult.get("secure_url");
        log.info("Logo uploadé avec succès sur Cloudinary (format WebP) : {}", secureUrl);
        return secureUrl;
    }

    public String uploadImageService(MultipartFile file, String slugSalon) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier image du service est vide ou manquant.");
        }

        Map<String, Object> options = ObjectUtils.asMap(
                "folder", "mon-salon/services/" + slugSalon,
                "format", "webp",
                "quality", "auto",
                "width", 800,
                "crop", "scale"
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(), options);
        String secureUrl = (String) uploadResult.get("secure_url");
        log.info("Image service uploadée avec succès sur Cloudinary (WebP) : {}", secureUrl);
        return secureUrl;
    }

    public String uploadImageVariante(MultipartFile file, String slugSalon) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier image de la variante est vide ou manquant.");
        }

        Map<String, Object> options = ObjectUtils.asMap(
                "folder", "mon-salon/variantes/" + slugSalon,
                "format", "webp",
                "quality", "auto",
                "width", 800,
                "crop", "scale"
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(), options);
        String secureUrl = (String) uploadResult.get("secure_url");
        log.info("Image variante uploadée avec succès sur Cloudinary (WebP) : {}", secureUrl);
        return secureUrl;
    }

    public String uploadImageCategorieProduit(MultipartFile file, String slugSalon) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier image de la catégorie est vide ou manquant.");
        }

        Map<String, Object> options = ObjectUtils.asMap(
                "folder", "mon-salon/categories-produits/" + slugSalon,
                "format", "webp",
                "quality", "auto",
                "width", 800,
                "crop", "scale"
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(), options);
        String secureUrl = (String) uploadResult.get("secure_url");
        log.info("Image catégorie produit uploadée avec succès sur Cloudinary (WebP) : {}", secureUrl);
        return secureUrl;
    }

    public String uploadImageProduit(MultipartFile file, String slugSalon) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier image du produit est vide ou manquant.");
        }

        Map<String, Object> options = ObjectUtils.asMap(
                "folder", "mon-salon/produits/" + slugSalon,
                "format", "webp",
                "quality", "auto",
                "width", 800,
                "crop", "scale"
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(), options);
        String secureUrl = (String) uploadResult.get("secure_url");
        log.info("Image produit uploadée avec succès sur Cloudinary (WebP) : {}", secureUrl);
        return secureUrl;
    }

    public String uploadPhotoProfilCoiffeur(MultipartFile file, String slugSalon, Long coiffeurId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier image fourni est vide ou manquant.");
        }

        Map<String, Object> options = ObjectUtils.asMap(
                "folder", "mon-salon/coiffeurs/" + slugSalon,
                "public_id", "coiffeur_" + coiffeurId + "_" + System.currentTimeMillis(),
                "format", "webp",
                "quality", "auto",
                "width", 500,
                "crop", "scale"
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(), options);
        String secureUrl = (String) uploadResult.get("secure_url");
        log.info("Photo profil coiffeur uploadée sur Cloudinary (WebP) : {}", secureUrl);
        return secureUrl;
    }

    public String uploadVideoRealisation(MultipartFile file, String slugSalon) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier vidéo fourni est vide ou manquant.");
        }

        // Configuration des paramètres d'upload spécifiques au streaming adaptatif (HLS)
        Map<String, Object> options = ObjectUtils.asMap(
                "folder", "mon-salon/realisations/" + slugSalon,
                "resource_type", "video",
                "eager", Arrays.asList(
                        new Transformation().rawTransformation("sp_hd/m3u8")
                ),
                "eager_async", true // Laisse Cloudinary fragmenter en arrière-plan sans bloquer l'upload
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(), options);
        String finalUrl = extractHlsUrl(uploadResult);
        log.info("Vidéo de réalisation uploadée sur Cloudinary avec fragmentation HLS : {}", finalUrl);
        return finalUrl;
    }

    public String uploadMediaStory(MultipartFile file, String slugSalon) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier média de la story est vide ou manquant.");
        }

        String contentType = file.getContentType();
        boolean isVideo = contentType != null && contentType.startsWith("video");

        Map<String, Object> options;
        if (isVideo) {
            // Configuration streaming adaptatif HLS pour vidéo story
            options = ObjectUtils.asMap(
                    "folder", "mon-salon/stories/" + slugSalon,
                    "resource_type", "video",
                    "eager", Arrays.asList(
                            new Transformation().rawTransformation("sp_hd/m3u8")
                    ),
                    "eager_async", true
            );
        } else {
            options = ObjectUtils.asMap(
                    "folder", "mon-salon/stories/" + slugSalon,
                    "resource_type", "image"
            );
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(), options);
        String finalUrl = isVideo ? extractHlsUrl(uploadResult) : (String) uploadResult.get("secure_url");
        log.info("Média de story ({}) uploadé avec succès sur Cloudinary : {}", isVideo ? "video (HLS m3u8)" : "image", finalUrl);
        return finalUrl;
    }

    @SuppressWarnings("unchecked")
    private String extractHlsUrl(Map<String, Object> uploadResult) {
        if (uploadResult == null) return null;

        // 1. Vérifier si l'URL eager est directement retournée
        Object eagerObj = uploadResult.get("eager");
        if (eagerObj instanceof List<?> eagerList && !eagerList.isEmpty()) {
            Object first = eagerList.get(0);
            if (first instanceof Map<?, ?> eagerMap) {
                String eagerSecureUrl = (String) eagerMap.get("secure_url");
                if (eagerSecureUrl != null && !eagerSecureUrl.isBlank()) {
                    return eagerSecureUrl;
                }
            }
        }

        // 2. Si eager_async=true, Cloudinary génère l'URL HLS canonique avec sp_hd/<version>/<public_id>.m3u8
        String secureUrl = (String) uploadResult.get("secure_url");
        if (secureUrl != null && secureUrl.contains("/video/upload/")) {
            String hls = secureUrl.replace("/video/upload/", "/video/upload/sp_hd/");
            int lastDot = hls.lastIndexOf('.');
            if (lastDot > 0) {
                return hls.substring(0, lastDot) + ".m3u8";
            }
            return hls + ".m3u8";
        }

        return secureUrl;
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

    public void destroy(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Image Cloudinary supprimée : {}", publicId);
        } catch (Exception e) {
            log.warn("Impossible de supprimer l'image Cloudinary {} : {}", publicId, e.getMessage());
        }
    }

    private String extractPublicId(String url) {
        // Ex: https://res.cloudinary.com/cloud/image/upload/v12345/mon-salon/logos/test.webp
        // Ou HLS: https://res.cloudinary.com/cloud/video/upload/sp_hd/v12345/mon-salon/realisations/test.m3u8
        int uploadIndex = url.indexOf("/upload/");
        if (uploadIndex == -1) {
            return null;
        }

        String pathAfterUpload = url.substring(uploadIndex + "/upload/".length());
        
        // Supprimer d'éventuels préfixes de transformation Cloudinary (ex: sp_hd/)
        if (pathAfterUpload.startsWith("sp_hd/")) {
            pathAfterUpload = pathAfterUpload.substring("sp_hd/".length());
        }

        // Enlever le préfixe de version s'il existe (ex: v123456789/)
        if (pathAfterUpload.matches("^v\\d+/.*")) {
            pathAfterUpload = pathAfterUpload.replaceFirst("^v\\d+/", "");
        }

        // Enlever l'extension (.jpg, .png, .webp, .mp4, .m3u8, etc.)
        int dotIndex = pathAfterUpload.lastIndexOf('.');
        if (dotIndex != -1) {
            pathAfterUpload = pathAfterUpload.substring(0, dotIndex);
        }

        return pathAfterUpload;
    }

    public Map<String, String> uploadExportRgpd(byte[] content, String filename) throws IOException {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Le contenu de l'export est vide.");
        }

        Map<String, Object> options = ObjectUtils.asMap(
                "folder", "mon-salon/rgpd_exports",
                "public_id", filename,
                "resource_type", "raw",
                "access_mode", "public"
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(content, options);
        String secureUrl = (String) uploadResult.get("secure_url");
        String publicId = (String) uploadResult.get("public_id");

        // Génération d'une URL signée pour contourner les restrictions strictes Cloudinary sur les PDF
        String signedUrl = null;
        try {
            signedUrl = cloudinary.url()
                    .resourceType("raw")
                    .type("upload")
                    .signed(true)
                    .generate(publicId != null ? publicId : filename);
        } catch (Exception e) {
            log.warn("Impossible de générer l'URL signée Cloudinary : {}", e.getMessage());
        }

        String finalUrl = (signedUrl != null && !signedUrl.isBlank()) ? signedUrl : secureUrl;
        log.info("Export téléversé sur Cloudinary : {} (public_id: {})", finalUrl, publicId);

        Map<String, String> result = new HashMap<>();
        result.put("secure_url", finalUrl);
        result.put("public_id", publicId);
        return result;
    }

    public byte[] downloadRawFile(String publicId, String fallbackUrl) throws IOException {
        String urlToFetch = null;

        if (publicId != null && !publicId.isBlank()) {
            try {
                urlToFetch = cloudinary.url()
                        .resourceType("raw")
                        .type("upload")
                        .signed(true)
                        .generate(publicId);
            } catch (Exception e) {
                log.warn("Impossible de générer l'URL signée pour publicId {}: {}", publicId, e.getMessage());
            }
        }

        if (urlToFetch == null || urlToFetch.isBlank()) {
            urlToFetch = fallbackUrl;
        }

        if (urlToFetch == null || urlToFetch.isBlank()) {
            throw new IllegalArgumentException("Aucune URL ou identifiant disponible pour télécharger le fichier Cloudinary.");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlToFetch))
                    .GET()
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "HairStyle-Backend/1.0")
                    .build();

            HttpResponse<byte[]> response = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(15))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }

            // Tenter avec le fallback si différent de l'URL signée
            if (fallbackUrl != null && !fallbackUrl.equals(urlToFetch)) {
                HttpRequest fallbackReq = HttpRequest.newBuilder()
                        .uri(URI.create(fallbackUrl))
                        .GET()
                        .timeout(Duration.ofSeconds(20))
                        .header("User-Agent", "HairStyle-Backend/1.0")
                        .build();

                HttpResponse<byte[]> fallbackResp = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(15))
                        .build()
                        .send(fallbackReq, HttpResponse.BodyHandlers.ofByteArray());

                if (fallbackResp.statusCode() >= 200 && fallbackResp.statusCode() < 300) {
                    return fallbackResp.body();
                }
            }

            throw new IOException("Échec du téléchargement Cloudinary (HTTP " + response.statusCode() + ")");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Téléchargement Cloudinary interrompu", e);
        }
    }

    public void deleteRawFile(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            Map<String, Object> params = ObjectUtils.asMap("resource_type", "raw");
            cloudinary.uploader().destroy(publicId, params);
            log.info("Fichier brut RGPD Cloudinary supprimé : {}", publicId);
        } catch (Exception e) {
            log.warn("Impossible de supprimer le fichier brut Cloudinary {} : {}", publicId, e.getMessage());
        }
    }
}


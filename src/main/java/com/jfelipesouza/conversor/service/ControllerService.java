package com.jfelipesouza.conversor.service;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.jfelipesouza.conversor.config.AppProperties;
import com.jfelipesouza.conversor.converter.AudioConverter;
import com.jfelipesouza.conversor.download.DownloadLink;
import com.jfelipesouza.conversor.download.DownloadTokenRegistry;
import com.jfelipesouza.conversor.download.YouTubeUrlValidator;
import com.jfelipesouza.conversor.dto.AudioFormat;
import com.jfelipesouza.conversor.dto.ConversorResponse;
import com.jfelipesouza.conversor.storage.AudioStorage;

@Service
public class ControllerService {
    private static final Logger logger = LoggerFactory.getLogger(ControllerService.class);
    private final AudioConverter converter;
    private final AudioStorage storage;
    private final DownloadTokenRegistry tokenRegistry;
    private final YouTubeUrlValidator urlValidator;
    private final String downloadBaseUrl;

    public ControllerService(
            AudioConverter converter,
            AudioStorage storage,
            DownloadTokenRegistry tokenRegistry,
            YouTubeUrlValidator urlValidator,
            AppProperties properties) {
        this.converter = converter;
        this.storage = storage;
        this.tokenRegistry = tokenRegistry;
        this.urlValidator = urlValidator;
        this.downloadBaseUrl = properties.download().baseUrl().replaceAll("/+$", "");
    }

    public ConversorResponse generateDownloadUrl(String url, AudioFormat format, String audioName) {
        if (format != AudioFormat.MP3) {
            throw new IllegalArgumentException("Somente o formato MP3 é aceito.");
        }
        urlValidator.validate(url);
        return converter.convert(url, file -> {
            String fileName = sanitizeFileName(audioName);
            String objectKey = storage.store(file);
            try {
                String token = tokenRegistry.register(objectKey, fileName);
                return new ConversorResponse(
                        "CONCLUÍDO",
                        fileName,
                        downloadBaseUrl + "/downloads/" + token);
            } catch (RuntimeException exception) {
                storage.delete(objectKey);
                throw exception;
            }
        });
    }

    static String sanitizeFileName(String requestedName) {
        String baseName = requestedName.replaceFirst("(?i)\\.mp3$", "");
        String normalized = Normalizer.normalize(baseName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String safeName = normalized
                .replaceAll("[^A-Za-z0-9_-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^[._-]+|[._-]+$", "");
        if (safeName.isBlank()) {
            safeName = "audio";
        }
        if (safeName.length() > 100) {
            safeName = safeName.substring(0, 100).replaceAll("[._-]+$", "");
        }
        return safeName + ".mp3";
    }

    public DownloadPayload openDownload(String token) {
        DownloadLink link = tokenRegistry.claim(token);
        try {
            InputStream inputStream = storage.open(link.objectKey());
            return new DownloadPayload(token, link.objectKey(), link.fileName(), inputStream);
        } catch (RuntimeException exception) {
            tokenRegistry.release(token);
            throw exception;
        }
    }

    public void completeDownload(DownloadPayload download) {
        try {
            storage.delete(download.objectKey());
            tokenRegistry.complete(download.token());
        } catch (RuntimeException exception) {
            tokenRegistry.release(download.token());
            logger.error("Could not delete audio after download: {}", exception.getClass().getSimpleName());
            throw exception;
        }
    }

    public void releaseDownload(String token) {
        tokenRegistry.release(token);
    }

    public record DownloadPayload(String token, String objectKey, String fileName, InputStream inputStream) {
    }
}

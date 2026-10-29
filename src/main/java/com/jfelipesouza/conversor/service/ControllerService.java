package com.jfelipesouza.conversor.service;

import java.io.InputStream;
import java.util.UUID;

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

    public ConversorResponse generateDownloadUrl(String url, AudioFormat format) {
        if (format != AudioFormat.MP3) {
            throw new IllegalArgumentException("Only MP3 audio is supported.");
        }
        urlValidator.validate(url);
        return converter.convert(url, file -> {
            String fileName = "audio-" + UUID.randomUUID() + ".mp3";
            String objectKey = storage.store(file);
            try {
                String token = tokenRegistry.register(objectKey, fileName);
                return new ConversorResponse(
                        "COMPLETED",
                        fileName,
                        downloadBaseUrl + "/downloads/" + token);
            } catch (RuntimeException exception) {
                storage.delete(objectKey);
                throw exception;
            }
        });
    }

    public DownloadPayload openDownload(String token) {
        DownloadLink link = tokenRegistry.resolve(token);
        InputStream inputStream = storage.open(link.objectKey());
        return new DownloadPayload(link.fileName(), inputStream);
    }

    public record DownloadPayload(String fileName, InputStream inputStream) {
    }
}

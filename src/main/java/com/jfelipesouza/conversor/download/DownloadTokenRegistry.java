package com.jfelipesouza.conversor.download;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.jfelipesouza.conversor.config.AppProperties;
import com.jfelipesouza.conversor.converter.ConversionCapacityException;

@Component
public class DownloadTokenRegistry {
    private static final String TOKEN_PATTERN = "[A-Za-z0-9_-]{43}";
    private final ConcurrentHashMap<String, DownloadLink> links = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final AppProperties.Download properties;

    public DownloadTokenRegistry(AppProperties appProperties) {
        this.properties = appProperties.download();
    }

    public synchronized String register(String objectKey, String fileName) {
        Instant now = Instant.now();
        links.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        if (links.size() >= properties.maxLinks()) {
            throw new ConversionCapacityException();
        }

        byte[] tokenBytes = new byte[32];
        String token;
        do {
            secureRandom.nextBytes(tokenBytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        } while (links.containsKey(token));
        links.put(token, new DownloadLink(objectKey, fileName, now.plus(properties.validity())));
        return token;
    }

    public DownloadLink resolve(String token) {
        if (token == null || !token.matches(TOKEN_PATTERN)) {
            throw new DownloadNotFoundException();
        }

        DownloadLink link = links.get(token);
        if (link == null) {
            throw new DownloadNotFoundException();
        }
        if (!link.expiresAt().isAfter(Instant.now())) {
            links.remove(token, link);
            throw new DownloadNotFoundException();
        }
        return link;
    }
}

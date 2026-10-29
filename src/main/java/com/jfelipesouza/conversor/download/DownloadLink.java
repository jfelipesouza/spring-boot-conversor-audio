package com.jfelipesouza.conversor.download;

import java.time.Instant;

public record DownloadLink(String objectKey, String fileName, Instant expiresAt) {
}

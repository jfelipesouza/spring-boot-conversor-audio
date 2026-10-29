package com.jfelipesouza.conversor.download;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.jfelipesouza.conversor.config.AppProperties;
import com.jfelipesouza.conversor.converter.ConversionCapacityException;

class DownloadTokenRegistryTest {
    @Test
    void createsUnpredictableTokensAndResolvesTheirMetadata() {
        DownloadTokenRegistry registry = registry(Duration.ofMinutes(1), 10);

        String token = registry.register("9d0493bd-a078-4c8f-a277-2e3367b1f262.mp3", "audio-test.mp3");

        assertEquals(43, token.length());
        assertTrue(token.matches("[A-Za-z0-9_-]+"));
        assertEquals("audio-test.mp3", registry.resolve(token).fileName());
        assertThrows(DownloadNotFoundException.class, () -> registry.resolve("test"));
    }

    @Test
    void rejectsExpiredLinksAndEnforcesItsCapacity() throws InterruptedException {
        DownloadTokenRegistry expiringRegistry = registry(Duration.ofMillis(1), 1);
        String expired = expiringRegistry.register("9d0493bd-a078-4c8f-a277-2e3367b1f262.mp3", "audio-test.mp3");
        Thread.sleep(10);

        assertThrows(DownloadNotFoundException.class, () -> expiringRegistry.resolve(expired));

        DownloadTokenRegistry fullRegistry = registry(Duration.ofMinutes(1), 1);
        fullRegistry.register("9d0493bd-a078-4c8f-a277-2e3367b1f262.mp3", "audio-test.mp3");
        assertThrows(ConversionCapacityException.class,
                () -> fullRegistry.register("5f3d9db3-45eb-4ca6-884b-fcf44091900a.mp3", "audio-test-2.mp3"));
    }

    @Test
    void claimsAValidDownloadOnceAndRemovesItsTokenOnCompletion() {
        DownloadTokenRegistry registry = registry(Duration.ofMinutes(1), 10);
        String token = registry.register("9d0493bd-a078-4c8f-a277-2e3367b1f262.mp3", "audio-test.mp3");

        assertEquals("audio-test.mp3", registry.claim(token).fileName());
        assertThrows(DownloadNotFoundException.class, () -> registry.claim(token));
        registry.complete(token);

        assertThrows(DownloadNotFoundException.class, () -> registry.resolve(token));
    }

    @Test
    void releasesAClaimForRetryWhenTheDownloadDoesNotComplete() {
        DownloadTokenRegistry registry = registry(Duration.ofMinutes(1), 10);
        String token = registry.register("9d0493bd-a078-4c8f-a277-2e3367b1f262.mp3", "audio-test.mp3");

        registry.claim(token);
        registry.release(token);

        assertEquals("audio-test.mp3", registry.claim(token).fileName());
    }

    private DownloadTokenRegistry registry(Duration validity, int maxLinks) {
        AppProperties properties = new AppProperties(
                new AppProperties.Converter(java.nio.file.Path.of("tmp"), Duration.ofSeconds(1),
                        1024, 1, "yt-dlp", "ffmpeg", "ffprobe"),
                new AppProperties.Storage("unused", "audios"),
                new AppProperties.Download("http://localhost:8080", validity, maxLinks));
        return new DownloadTokenRegistry(properties);
    }
}

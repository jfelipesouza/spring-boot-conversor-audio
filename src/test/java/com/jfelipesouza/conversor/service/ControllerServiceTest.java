package com.jfelipesouza.conversor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.jfelipesouza.conversor.config.AppProperties;
import com.jfelipesouza.conversor.converter.AudioConverter;
import com.jfelipesouza.conversor.download.DownloadTokenRegistry;
import com.jfelipesouza.conversor.download.YouTubeUrlValidator;
import com.jfelipesouza.conversor.dto.AudioFormat;
import com.jfelipesouza.conversor.dto.ConversorResponse;
import com.jfelipesouza.conversor.storage.AudioStorage;

class ControllerServiceTest {
    @Test
    void usesTheRequestedNameAfterMakingItSafeForDownload() {
        AudioConverter converter = mock(AudioConverter.class);
        AudioStorage storage = mock(AudioStorage.class);
        DownloadTokenRegistry tokenRegistry = mock(DownloadTokenRegistry.class);
        YouTubeUrlValidator urlValidator = mock(YouTubeUrlValidator.class);
        String url = "https://www.youtube.com/watch?v=test";
        String expectedFileName = "Mangue-seco-rm-rf.mp3";
        String objectKey = "9d0493bd-a078-4c8f-a277-2e3367b1f262.mp3";
        String token = "secure-token";

        when(converter.<ConversorResponse>convert(eq(url), any()))
                .thenAnswer(invocation -> invocation
                        .<Function<Path, ConversorResponse>>getArgument(1)
                        .apply(Path.of("converted-audio.mp3")));
        when(storage.store(Path.of("converted-audio.mp3"))).thenReturn(objectKey);
        when(tokenRegistry.register(objectKey, expectedFileName)).thenReturn(token);

        ControllerService service = new ControllerService(
                converter,
                storage,
                tokenRegistry,
                urlValidator,
                properties());
        ConversorResponse response = service.generateDownloadUrl(
                url, AudioFormat.MP3, "../Mángue seco; rm -rf.mp3");

        assertEquals("CONCLUÍDO", response.status());
        assertEquals(expectedFileName, response.fileName());
        assertEquals("http://localhost:8080/downloads/" + token, response.downloadUrl());
        verify(urlValidator).validate(url);
        verify(tokenRegistry).register(objectKey, expectedFileName);
    }

    @Test
    void fallsBackToASafeDefaultForNamesWithoutUsableCharacters() {
        assertEquals("audio.mp3", ControllerService.sanitizeFileName("../../🎵"));
    }

    private AppProperties properties() {
        return new AppProperties(
                new AppProperties.Converter(Path.of("tmp"), Duration.ofMinutes(1),
                        1024, 1, "yt-dlp", "ffmpeg", "ffprobe"),
                new AppProperties.Storage("unused", "audios"),
                new AppProperties.Download("http://localhost:8080", Duration.ofHours(1), 10));
    }
}

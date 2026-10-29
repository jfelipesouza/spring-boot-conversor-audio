package com.jfelipesouza.conversor.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.jfelipesouza.conversor.service.ControllerService;

class ConversorControllerTest {
    @Test
    void streamsAudioWithBrowserDownloadHeaders() throws Exception {
        ControllerService service = mock(ControllerService.class);
        byte[] audio = { 'I', 'D', '3' };
        when(service.openDownload("secure-token")).thenReturn(
                new ControllerService.DownloadPayload(
                        "secure-token", "object-key.mp3", "audio-safe.mp3", new ByteArrayInputStream(audio)));
        ConversorController controller = new ConversorController(service);

        var response = controller.download("secure-token");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        response.getBody().writeTo(output);

        assertEquals(MediaType.parseMediaType("audio/mpeg"), response.getHeaders().getContentType());
        assertEquals("no-store", response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
        assertTrueAttachment(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertArrayEquals(audio, output.toByteArray());
        verify(service).openDownload("secure-token");
        verify(service).completeDownload(org.mockito.ArgumentMatchers.any());
        verifyNoMoreInteractions(service);
    }

    @Test
    void releasesTheDownloadClaimWhenTheClientStreamFails() {
        ControllerService service = mock(ControllerService.class);
        InputStream brokenStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("stream interrupted");
            }
        };
        when(service.openDownload("secure-token")).thenReturn(
                new ControllerService.DownloadPayload(
                        "secure-token", "object-key.mp3", "audio-safe.mp3", brokenStream));

        var response = new ConversorController(service).download("secure-token");

        assertThrows(IOException.class, () -> response.getBody().writeTo(new ByteArrayOutputStream()));
        verify(service).openDownload("secure-token");
        verify(service).releaseDownload("secure-token");
        verifyNoMoreInteractions(service);
    }

    private void assertTrueAttachment(String value) {
        org.junit.jupiter.api.Assertions.assertTrue(value.startsWith("attachment;"));
        org.junit.jupiter.api.Assertions.assertTrue(value.contains("audio-safe.mp3"));
    }
}

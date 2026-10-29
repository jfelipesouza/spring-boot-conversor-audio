package com.jfelipesouza.conversor.storage;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.specialized.BlobInputStream;

@ExtendWith(MockitoExtension.class)
class AzureBlobAudioStorageTest {
    @TempDir
    Path tempDirectory;

    @Mock
    BlobContainerClient container;
    @Mock
    BlobClient blob;

    @Test
    void uploadsWithAnApplicationGeneratedKeyAndCanReadItBack() throws Exception {
        Path file = tempDirectory.resolve("audio.mp3");
        Files.write(file, new byte[] { 'I', 'D', '3' });
        when(container.getBlobClient(anyString())).thenReturn(blob);
        BlobInputStream downloaded = org.mockito.Mockito.mock(BlobInputStream.class);
        when(blob.openInputStream()).thenReturn(downloaded);
        AudioStorage storage = new AzureBlobAudioStorage(container);

        String key = storage.store(file);
        var result = storage.open(key);

        assertTrue(key.matches("[0-9a-f-]{36}\\.mp3"));
        assertSame(downloaded, result);
        verify(blob).uploadFromFile(file.toString(), true);
    }
}

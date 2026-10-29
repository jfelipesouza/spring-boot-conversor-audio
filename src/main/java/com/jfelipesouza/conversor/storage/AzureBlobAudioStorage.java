package com.jfelipesouza.conversor.storage;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;

@Component
public class AzureBlobAudioStorage implements AudioStorage {
    private final BlobContainerClient container;

    public AzureBlobAudioStorage(BlobContainerClient container) {
        this.container = container;
    }

    @Override
    public String store(Path file) {
        String objectKey = UUID.randomUUID() + ".mp3";
        try {
            BlobClient blob = container.getBlobClient(objectKey);
            blob.uploadFromFile(file.toString(), true);
            blob.setHttpHeaders(new BlobHttpHeaders().setContentType("audio/mpeg"));
            return objectKey;
        } catch (RuntimeException exception) {
            throw new StorageUnavailableException("Audio could not be uploaded to storage.", exception);
        }
    }

    @Override
    public InputStream open(String objectKey) {
        validateObjectKey(objectKey);
        try {
            return container.getBlobClient(objectKey).openInputStream();
        } catch (RuntimeException exception) {
            throw new StorageUnavailableException("Audio could not be read from storage.", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        validateObjectKey(objectKey);
        try {
            container.getBlobClient(objectKey).deleteIfExists();
        } catch (RuntimeException exception) {
            throw new StorageUnavailableException("Stored audio could not be removed.", exception);
        }
    }

    private void validateObjectKey(String objectKey) {
        if (objectKey == null || !objectKey.matches("[0-9a-fA-F-]{36}\\.mp3")) {
            throw new IllegalArgumentException("Invalid audio object key.");
        }
    }
}

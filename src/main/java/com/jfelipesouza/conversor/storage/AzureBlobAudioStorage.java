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
            throw new StorageUnavailableException("Não foi possível enviar o áudio para o armazenamento.", exception);
        }
    }

    @Override
    public InputStream open(String objectKey) {
        validateObjectKey(objectKey);
        try {
            return container.getBlobClient(objectKey).openInputStream();
        } catch (RuntimeException exception) {
            throw new StorageUnavailableException("Não foi possível ler o áudio do armazenamento.", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        validateObjectKey(objectKey);
        try {
            container.getBlobClient(objectKey).deleteIfExists();
        } catch (RuntimeException exception) {
            throw new StorageUnavailableException("Não foi possível excluir o áudio do armazenamento.", exception);
        }
    }

    private void validateObjectKey(String objectKey) {
        if (objectKey == null || !objectKey.matches("[0-9a-fA-F-]{36}\\.mp3")) {
            throw new IllegalArgumentException("A chave do arquivo de áudio é inválida.");
        }
    }
}

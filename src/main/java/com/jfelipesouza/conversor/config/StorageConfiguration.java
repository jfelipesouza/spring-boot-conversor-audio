package com.jfelipesouza.conversor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

@Configuration
public class StorageConfiguration {
    @Bean
    BlobContainerClient blobContainerClient(AppProperties properties) {
        try {
            BlobContainerClient client = new BlobServiceClientBuilder()
                    .connectionString(properties.storage().connectionString())
                    .buildClient()
                    .getBlobContainerClient(properties.storage().container());
            client.createIfNotExists();
            return client;
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Azure Blob Storage initialization failed. Verify the storage connection and container settings.");
        }
    }
}

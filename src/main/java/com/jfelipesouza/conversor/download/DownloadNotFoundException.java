package com.jfelipesouza.conversor.download;

public class DownloadNotFoundException extends RuntimeException {
    public DownloadNotFoundException() {
        super("O link de download não foi encontrado ou expirou.");
    }
}

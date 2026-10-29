package com.jfelipesouza.conversor.download;

public class InvalidYouTubeUrlException extends RuntimeException {
    public InvalidYouTubeUrlException() {
        super("URL must be an HTTPS URL from an allowed YouTube domain.");
    }
}

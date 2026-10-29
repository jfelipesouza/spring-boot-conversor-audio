package com.jfelipesouza.conversor.download;

public class InvalidYouTubeUrlException extends RuntimeException {
    public InvalidYouTubeUrlException() {
        super("A URL deve usar HTTPS e pertencer a um domínio permitido do YouTube.");
    }
}

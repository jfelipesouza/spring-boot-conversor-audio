package com.jfelipesouza.conversor.download;

public class DownloadNotFoundException extends RuntimeException {
    public DownloadNotFoundException() {
        super("Download link was not found or has expired.");
    }
}

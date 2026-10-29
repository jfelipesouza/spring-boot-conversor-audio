package com.jfelipesouza.conversor.converter;

public class ConversionTimeoutException extends RuntimeException {
    public ConversionTimeoutException() {
        super("Audio conversion exceeded its configured time limit.");
    }
}

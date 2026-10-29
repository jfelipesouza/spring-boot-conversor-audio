package com.jfelipesouza.conversor.converter;

public class ConversionFailedException extends RuntimeException {
    public ConversionFailedException() {
        super("Audio conversion could not be completed.");
    }
}

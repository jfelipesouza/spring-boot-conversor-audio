package com.jfelipesouza.conversor.converter;

public class ConversionCapacityException extends RuntimeException {
    public ConversionCapacityException() {
        super("The conversion service is temporarily at capacity.");
    }
}

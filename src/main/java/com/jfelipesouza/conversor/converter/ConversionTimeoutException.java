package com.jfelipesouza.conversor.converter;

public class ConversionTimeoutException extends RuntimeException {
    public ConversionTimeoutException() {
        super("A conversão do áudio excedeu o tempo limite configurado.");
    }
}

package com.jfelipesouza.conversor.converter;

public class ConversionCapacityException extends RuntimeException {
    public ConversionCapacityException() {
        super("O serviço de conversão está temporariamente sem capacidade.");
    }
}

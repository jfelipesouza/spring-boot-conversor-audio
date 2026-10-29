package com.jfelipesouza.conversor.converter;

public class ConversionFailedException extends RuntimeException {
    public ConversionFailedException() {
        super("Não foi possível concluir a conversão do áudio.");
    }
}

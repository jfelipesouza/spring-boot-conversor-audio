package com.jfelipesouza.conversor.converter;

public class ConvertedFileTooLargeException extends RuntimeException {
    public ConvertedFileTooLargeException() {
        super("O áudio convertido excede o limite de tamanho configurado.");
    }
}

package com.jfelipesouza.conversor.converter;

public class ConvertedFileTooLargeException extends RuntimeException {
    public ConvertedFileTooLargeException() {
        super("Converted audio exceeds its configured size limit.");
    }
}

package com.jfelipesouza.conversor.download;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class YouTubeUrlValidatorTest {
    private final YouTubeUrlValidator validator = new YouTubeUrlValidator();

    @Test
    void rejectsNonHttpsAndNonAllowlistedHosts() {
        assertThrows(InvalidYouTubeUrlException.class,
                () -> validator.validate("http://youtube.com/watch?v=test"));
        assertThrows(InvalidYouTubeUrlException.class,
                () -> validator.validate("https://youtube.com.attacker.example/watch?v=test"));
        assertThrows(InvalidYouTubeUrlException.class,
                () -> validator.validate("https://youtube.com@127.0.0.1/watch?v=test"));
        assertThrows(InvalidYouTubeUrlException.class,
                () -> validator.validate("https://youtube.com:444/watch?v=test"));
    }
}

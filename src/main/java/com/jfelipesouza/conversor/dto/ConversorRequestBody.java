package com.jfelipesouza.conversor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConversorRequestBody(
        @NotBlank
        @Schema(description = "HTTPS URL of a YouTube video", example = "https://www.youtube.com/watch?v=dCtEzM-cir8")
        String url,
        @NotNull
        @Schema(description = "Requested audio format. MP3 is the only supported value.", example = "MP3")
        AudioFormat type) {
}

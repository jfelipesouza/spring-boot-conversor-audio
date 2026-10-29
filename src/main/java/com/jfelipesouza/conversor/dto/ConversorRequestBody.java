package com.jfelipesouza.conversor.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConversorRequestBody(
        @NotBlank
        @Schema(description = "URL HTTPS de um vídeo do YouTube.", example = "https://www.youtube.com/watch?v=dCtEzM-cir8")
        String url,
        @NotNull
        @Schema(description = "Formato de áudio solicitado. Nesta versão, somente MP3 é aceito.", example = "MP3")
        AudioFormat type,
        @NotBlank
        @Size(max = 120)
        @Schema(description = "Nome desejado para o arquivo MP3. A extensão .mp3 é opcional.", example = "mangue-seco.mp3", maxLength = 120)
        @JsonAlias("fileName")
        String filename
) {
}

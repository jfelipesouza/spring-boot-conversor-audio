package com.jfelipesouza.conversor.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ConversorResponse(
        @Schema(description = "Situação da conversão.", example = "CONCLUÍDO")
        String status,
        @Schema(description = "Nome seguro do arquivo disponível para download.", example = "mangue-seco.mp3")
        String fileName,
        @Schema(description = "Link de uso único para baixar o arquivo.", example = "http://localhost:8080/downloads/2JxdgKH5vNAhs5h8YJ8D3CRVM5He1Ds9uE7YVhSxjDc")
        String downloadUrl) {
}

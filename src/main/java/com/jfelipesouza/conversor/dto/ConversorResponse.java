package com.jfelipesouza.conversor.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ConversorResponse(
        @Schema(example = "COMPLETED")
        String status,
        @Schema(example = "audio-57dcd523-bcea-4781-92d4-0b3e493eea85.mp3")
        String fileName,
        @Schema(example = "http://localhost:8080/downloads/2JxdgKH5vNAhs5h8YJ8D3CRVM5He1Ds9uE7YVhSxjDc")
        String downloadUrl) {
}

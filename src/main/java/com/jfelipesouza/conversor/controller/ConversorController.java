package com.jfelipesouza.conversor.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.jfelipesouza.conversor.dto.ConversorRequestBody;
import com.jfelipesouza.conversor.dto.ConversorResponse;
import com.jfelipesouza.conversor.service.ControllerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping
public class ConversorController {
    private final ControllerService controllerService;

    public ConversorController(ControllerService controllerService) {
        this.controllerService = controllerService;
    }

    @PostMapping("/conversor")
    @Operation(
            summary = "Converter áudio do YouTube para MP3",
            description = "Extrai de forma síncrona o áudio autorizado do YouTube, armazena o MP3 em um container "
                    + "privado do Blob Storage e retorna um link de download. O link expira conforme "
                    + "DOWNLOAD_LINK_VALIDITY e só funciona na instância da API que o criou.")
    @ApiResponse(responseCode = "200", description = "Conversão e upload concluídos",
            content = @Content(schema = @Schema(implementation = ConversorResponse.class),
                    examples = @ExampleObject(value = """
                            {"status":"CONCLUÍDO","fileName":"mangue-seco.mp3","downloadUrl":"http://localhost:8080/downloads/2JxdgKH5vNAhs5h8YJ8D3CRVM5He1Ds9uE7YVhSxjDc"}
                            """)))
    @ApiResponse(responseCode = "400", description = "JSON, URL, formato ou nome de arquivo inválido")
    @ApiResponse(responseCode = "413", description = "O arquivo convertido excede o limite configurado")
    @ApiResponse(responseCode = "502", description = "Não foi possível converter o vídeo de origem")
    @ApiResponse(responseCode = "503", description = "Serviço de conversão ou armazenamento indisponível")
    @ApiResponse(responseCode = "504", description = "O tempo limite da conversão foi excedido")
    public ResponseEntity<ConversorResponse> convert(@Valid @RequestBody ConversorRequestBody request) {
        return ResponseEntity.ok(controllerService.generateDownloadUrl(
                request.url(),
                request.type(),
                request.filename()));
    }

    @GetMapping("/downloads/{token}")
    @Operation(
            summary = "Baixar um MP3 convertido",
            description = "Transmite o MP3 armazenado como anexo e exclui o Blob após o download ser concluído. "
                    + "O link pode ser usado uma única vez. Links inexistentes ou expirados retornam 404.")
    @ApiResponse(responseCode = "200", description = "Download do MP3",
            content = @Content(mediaType = "audio/mpeg"))
    @ApiResponse(responseCode = "404", description = "Token inexistente ou expirado")
    @ApiResponse(responseCode = "503", description = "Armazenamento indisponível")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable String token) {
        ControllerService.DownloadPayload download = controllerService.openDownload(token);
        StreamingResponseBody body = outputStream -> {
            boolean transferred = false;
            try {
                try (var inputStream = download.inputStream()) {
                    inputStream.transferTo(outputStream);
                    transferred = true;
                }
            } finally {
                if (transferred) {
                    controllerService.completeDownload(download);
                } else {
                    controllerService.releaseDownload(download.token());
                }
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(body);
    }
}

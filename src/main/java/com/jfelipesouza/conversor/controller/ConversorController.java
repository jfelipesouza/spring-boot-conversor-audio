package com.jfelipesouza.conversor.controller;

import java.io.IOException;
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
            summary = "Convert YouTube audio to MP3",
            description = "Synchronously extracts authorized YouTube audio, stores the MP3 in private Blob Storage, "
                    + "and returns an application download link. Links expire according to DOWNLOAD_LINK_VALIDITY "
                    + "and are valid only on the API instance that created them.")
    @ApiResponse(responseCode = "200", description = "Conversion and upload completed",
            content = @Content(schema = @Schema(implementation = ConversorResponse.class),
                    examples = @ExampleObject(value = """
                            {"status":"COMPLETED","fileName":"audio-57dcd523-bcea-4781-92d4-0b3e493eea85.mp3","downloadUrl":"http://localhost:8080/downloads/2JxdgKH5vNAhs5h8YJ8D3CRVM5He1Ds9uE7YVhSxjDc"}
                            """)))
    @ApiResponse(responseCode = "400", description = "Invalid JSON, URL, or format")
    @ApiResponse(responseCode = "413", description = "Converted file exceeds the configured limit")
    @ApiResponse(responseCode = "502", description = "Video source or conversion failed")
    @ApiResponse(responseCode = "503", description = "Conversion or storage service is unavailable")
    @ApiResponse(responseCode = "504", description = "Conversion timed out")
    public ResponseEntity<ConversorResponse> convert(@Valid @RequestBody ConversorRequestBody request) {
        return ResponseEntity.ok(controllerService.generateDownloadUrl(request.url(), request.type()));
    }

    @GetMapping("/downloads/{token}")
    @Operation(
            summary = "Download a converted MP3",
            description = "Streams the private stored MP3 as an attachment. Unknown or expired links return 404.")
    @ApiResponse(responseCode = "200", description = "MP3 download",
            content = @Content(mediaType = "audio/mpeg"))
    @ApiResponse(responseCode = "404", description = "Token not found or expired")
    @ApiResponse(responseCode = "503", description = "Storage is unavailable")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable String token) {
        ControllerService.DownloadPayload download = controllerService.openDownload(token);
        StreamingResponseBody body = outputStream -> {
            try (var inputStream = download.inputStream()) {
                inputStream.transferTo(outputStream);
            } catch (IOException exception) {
                throw new IllegalStateException("Audio download stream failed.");
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

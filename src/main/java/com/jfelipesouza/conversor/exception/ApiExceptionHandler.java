package com.jfelipesouza.conversor.exception;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.jfelipesouza.conversor.converter.ConversionCapacityException;
import com.jfelipesouza.conversor.converter.ConversionFailedException;
import com.jfelipesouza.conversor.converter.ConversionTimeoutException;
import com.jfelipesouza.conversor.converter.ConvertedFileTooLargeException;
import com.jfelipesouza.conversor.download.DownloadNotFoundException;
import com.jfelipesouza.conversor.download.InvalidYouTubeUrlException;
import com.jfelipesouza.conversor.storage.StorageUnavailableException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentNotValidException.class,
            InvalidYouTubeUrlException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiError> badRequest(Exception exception, WebRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Request is invalid.", request);
    }

    @ExceptionHandler(DownloadNotFoundException.class)
    public ResponseEntity<ApiError> notFound(DownloadNotFoundException exception, WebRequest request) {
        return response(HttpStatus.NOT_FOUND, "Download link was not found or has expired.", request);
    }

    @ExceptionHandler(ConvertedFileTooLargeException.class)
    public ResponseEntity<ApiError> fileTooLarge(ConvertedFileTooLargeException exception, WebRequest request) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "Converted audio exceeds the configured size limit.", request);
    }

    @ExceptionHandler(ConversionTimeoutException.class)
    public ResponseEntity<ApiError> timeout(ConversionTimeoutException exception, WebRequest request) {
        return response(HttpStatus.GATEWAY_TIMEOUT, "Audio conversion timed out.", request);
    }

    @ExceptionHandler(ConversionCapacityException.class)
    public ResponseEntity<ApiError> capacity(ConversionCapacityException exception, WebRequest request) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "Service is temporarily at capacity.", request);
    }

    @ExceptionHandler(StorageUnavailableException.class)
    public ResponseEntity<ApiError> storageUnavailable(StorageUnavailableException exception, WebRequest request) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "Audio storage is temporarily unavailable.", request);
    }

    @ExceptionHandler(ConversionFailedException.class)
    public ResponseEntity<ApiError> conversionFailed(ConversionFailedException exception, WebRequest request) {
        return response(HttpStatus.BAD_GATEWAY, "The video source could not be converted.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception exception, WebRequest request) {
        logger.error("Unexpected API failure: {}", exception.getClass().getSimpleName());
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request);
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String message, WebRequest request) {
        String path = request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI()
                : "/";
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path));
    }
}

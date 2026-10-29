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
        return response(HttpStatus.BAD_REQUEST, "A requisição é inválida.", request);
    }

    @ExceptionHandler(DownloadNotFoundException.class)
    public ResponseEntity<ApiError> notFound(DownloadNotFoundException exception, WebRequest request) {
        return response(HttpStatus.NOT_FOUND, "O link de download não foi encontrado ou expirou.", request);
    }

    @ExceptionHandler(ConvertedFileTooLargeException.class)
    public ResponseEntity<ApiError> fileTooLarge(ConvertedFileTooLargeException exception, WebRequest request) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "O áudio convertido excede o limite de tamanho configurado.", request);
    }

    @ExceptionHandler(ConversionTimeoutException.class)
    public ResponseEntity<ApiError> timeout(ConversionTimeoutException exception, WebRequest request) {
        return response(HttpStatus.GATEWAY_TIMEOUT, "O tempo limite da conversão de áudio foi excedido.", request);
    }

    @ExceptionHandler(ConversionCapacityException.class)
    public ResponseEntity<ApiError> capacity(ConversionCapacityException exception, WebRequest request) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "O serviço está temporariamente sem capacidade para novas conversões.", request);
    }

    @ExceptionHandler(StorageUnavailableException.class)
    public ResponseEntity<ApiError> storageUnavailable(StorageUnavailableException exception, WebRequest request) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "O armazenamento de áudio está temporariamente indisponível.", request);
    }

    @ExceptionHandler(ConversionFailedException.class)
    public ResponseEntity<ApiError> conversionFailed(ConversionFailedException exception, WebRequest request) {
        return response(HttpStatus.BAD_GATEWAY, "Não foi possível converter o vídeo de origem.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception exception, WebRequest request) {
        logger.error("Unexpected API failure: {}", exception.getClass().getSimpleName());
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno inesperado.", request);
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String message, WebRequest request) {
        String path = request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI()
                : "/";
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), localizedError(status), message, path));
    }

    private String localizedError(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Requisição inválida";
            case NOT_FOUND -> "Não encontrado";
            case PAYLOAD_TOO_LARGE -> "Conteúdo muito grande";
            case GATEWAY_TIMEOUT -> "Tempo limite excedido";
            case SERVICE_UNAVAILABLE -> "Serviço indisponível";
            case BAD_GATEWAY -> "Falha no gateway";
            case INTERNAL_SERVER_ERROR -> "Erro interno";
            default -> "Erro";
        };
    }
}

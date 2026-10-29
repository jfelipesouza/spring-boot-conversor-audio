package com.jfelipesouza.conversor.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @Valid @NotNull Converter converter,
        @Valid @NotNull Storage storage,
        @Valid @NotNull Download download) {

    public record Converter(
            @NotNull java.nio.file.Path tempDirectory,
            @NotNull Duration timeout,
            @Positive long maxFileSize,
            @Positive int maxConcurrent,
            @NotBlank String ytDlpExecutable,
            @NotBlank String ffmpegExecutable,
            @NotBlank String ffprobeExecutable) {
        @AssertTrue(message = "app.converter.timeout must be greater than zero")
        public boolean isTimeoutPositive() {
            return timeout != null && !timeout.isZero() && !timeout.isNegative();
        }
    }

    public record Storage(
            @NotBlank String connectionString,
            @NotBlank String container) {
    }

    public record Download(
            @NotBlank String baseUrl,
            @NotNull Duration validity,
            @Positive int maxLinks) {
        @AssertTrue(message = "app.download.validity must be greater than zero")
        public boolean isValidityPositive() {
            return validity != null && !validity.isZero() && !validity.isNegative();
        }

        @AssertTrue(message = "app.download.base-url must be an absolute HTTP or HTTPS URL without credentials")
        public boolean isBaseUrlValid() {
            try {
                URI uri = URI.create(baseUrl);
                return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                        && uri.getHost() != null
                        && uri.getUserInfo() == null
                        && uri.getQuery() == null
                        && uri.getFragment() == null;
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }
    }
}

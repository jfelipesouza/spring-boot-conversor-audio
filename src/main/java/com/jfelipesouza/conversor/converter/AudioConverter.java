package com.jfelipesouza.conversor.converter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.jfelipesouza.conversor.config.AppProperties;

@Component
public class AudioConverter {
    private static final Logger logger = LoggerFactory.getLogger(AudioConverter.class);
    private final AppProperties.Converter properties;
    private final Semaphore slots;

    public AudioConverter(AppProperties appProperties) {
        this.properties = appProperties.converter();
        this.slots = new Semaphore(properties.maxConcurrent());
        try {
            Files.createDirectories(properties.tempDirectory());
            if (!Files.isWritable(properties.tempDirectory())) {
                throw new IllegalStateException("Conversion temporary directory is not writable.");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Conversion temporary directory could not be initialized.");
        }
    }

    public <T> T convert(String url, Function<Path, T> afterConversion) {
        if (!slots.tryAcquire()) {
            throw new ConversionCapacityException();
        }

        Path operationDirectory = null;
        try {
            operationDirectory = Files.createTempDirectory(properties.tempDirectory(), "conversion-");
            Path processLog = operationDirectory.resolve("process.log");
            Path outputTemplate = operationDirectory.resolve("audio.%(ext)s");
            Path audioFile = operationDirectory.resolve("audio.mp3");
            List<String> command = new java.util.ArrayList<>(List.of(
                    properties.ytDlpExecutable(),
                    "--no-playlist",
                    "--extract-audio",
                    "--audio-format", "mp3",
                    "--audio-quality", "0",
                    "--output", outputTemplate.toString()));
            Path ffmpeg = Path.of(properties.ffmpegExecutable());
            if (ffmpeg.isAbsolute()) {
                command.add("--ffmpeg-location");
                command.add(ffmpeg.getParent().toString());
            }
            command.add(url);
            execute(command, processLog, properties.timeout());

            if (!Files.isRegularFile(audioFile) || Files.size(audioFile) == 0) {
                throw new ConversionFailedException();
            }
            if (Files.size(audioFile) > properties.maxFileSize()) {
                throw new ConvertedFileTooLargeException();
            }

            Path probeOutput = operationDirectory.resolve("probe.out");
            execute(List.of(
                    properties.ffprobeExecutable(),
                    "-v", "error",
                    "-show_entries", "format=format_name",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    audioFile.toString()), probeOutput, Duration.ofSeconds(15));
            String format = Files.readString(probeOutput, StandardCharsets.UTF_8).trim();
            if (!List.of(format.split(",")).contains("mp3")) {
                throw new ConversionFailedException();
            }
            return afterConversion.apply(audioFile);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ConversionFailedException();
        } catch (IOException exception) {
            throw new ConversionFailedException();
        } finally {
            if (operationDirectory != null) {
                deleteTemporaryDirectory(operationDirectory);
            }
            slots.release();
        }
    }

    private void execute(List<String> command, Path output, Duration timeout) throws IOException, InterruptedException {
        Process process;
        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(output.toFile())
                    .start();
        } catch (IOException exception) {
            throw new ConversionFailedException();
        }

        if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            process.toHandle().descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            process.waitFor(2, TimeUnit.SECONDS);
            throw new ConversionTimeoutException();
        }
        if (process.exitValue() != 0) {
            throw new ConversionFailedException();
        }
    }

    private void deleteTemporaryDirectory(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException exception) {
            logger.error("Failed to remove conversion temporary files.");
        }
    }
}

package com.jfelipesouza.conversor.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.jfelipesouza.conversor.config.AppProperties;

class AudioConverterTest {
    @TempDir
    Path tempDirectory;

    @Test
    void passesTheUrlAsOneArgumentAndCleansUpAfterConversion() throws Exception {
        Path capture = tempDirectory.resolve("arguments.txt");
        AudioConverter converter = converter(
                script("yt-dlp", "printf '%s\\n' \"$@\" > '" + capture + "'\n"
                        + "for arg in \"$@\"; do case \"$arg\" in *'%(ext)s') output=\"$arg\";; esac; done\n"
                        + "outdir=$(dirname \"$output\")\nprintf 'ID3' > \"$outdir/audio.mp3\"\n"),
                script("ffprobe", "printf 'mp3\\n'\n"),
                Duration.ofSeconds(2),
                100);
        String url = "https://www.youtube.com/watch?v=sample&part=one;not-a-command";

        long size = converter.convert(url, file -> {
            assertTrue(Files.isRegularFile(file));
            try {
                List<String> arguments = Files.readAllLines(capture);
                assertTrue(arguments.contains("--no-playlist"));
                assertEquals(1, arguments.stream().filter(url::equals).count());
                return Files.size(file);
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        });

        assertEquals(3, size);
        try (var files = Files.list(tempDirectory.resolve("conversions"))) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void reportsProcessFailureAndStillCleansUp() throws Exception {
        AudioConverter converter = converter(
                script("yt-dlp", "exit 17\n"),
                script("ffprobe", "printf 'mp3\\n'\n"),
                Duration.ofSeconds(2),
                100);

        assertThrows(ConversionFailedException.class,
                () -> converter.convert("https://www.youtube.com/watch?v=test", file -> file));
        try (var files = Files.list(tempDirectory.resolve("conversions"))) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void terminatesAProcessThatExceedsItsTimeout() throws Exception {
        AudioConverter converter = converter(
                script("yt-dlp", "sleep 5\n"),
                script("ffprobe", "printf 'mp3\\n'\n"),
                Duration.ofMillis(100),
                100);

        assertThrows(ConversionTimeoutException.class,
                () -> converter.convert("https://www.youtube.com/watch?v=test", file -> file));
        try (var files = Files.list(tempDirectory.resolve("conversions"))) {
            assertFalse(files.findAny().isPresent());
        }
    }

    @Test
    void rejectsAnOutputAboveTheConfiguredSize() throws Exception {
        AudioConverter converter = converter(
                script("yt-dlp", """
                        for arg in "$@"; do
                          case "$arg" in *'%(ext)s') output="$arg";; esac
                        done
                        outdir=$(dirname "$output")
                        printf 'ID3-too-large' > "$outdir/audio.mp3"
                        """),
                script("ffprobe", "printf 'mp3\\n'\n"),
                Duration.ofSeconds(2),
                4);

        assertThrows(ConvertedFileTooLargeException.class,
                () -> converter.convert("https://www.youtube.com/watch?v=test", file -> file));
    }

    private AudioConverter converter(Path ytDlp, Path ffprobe, Duration timeout, long maxSize) {
        Path workDirectory = tempDirectory.resolve("conversions");
        AppProperties properties = new AppProperties(
                new AppProperties.Converter(workDirectory, timeout, maxSize, 1, ytDlp.toString(),
                        "ffmpeg", ffprobe.toString()),
                new AppProperties.Storage("unused", "audios"),
                new AppProperties.Download("http://localhost:8080", Duration.ofHours(1), 100));
        return new AudioConverter(properties);
    }

    private Path script(String name, String body) throws Exception {
        Path path = tempDirectory.resolve(name);
        Files.writeString(path, "#!/bin/sh\n" + body);
        assertTrue(path.toFile().setExecutable(true));
        return path;
    }
}

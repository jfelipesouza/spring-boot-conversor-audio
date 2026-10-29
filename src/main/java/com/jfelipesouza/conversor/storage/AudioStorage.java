package com.jfelipesouza.conversor.storage;

import java.io.InputStream;
import java.nio.file.Path;

public interface AudioStorage {
    String store(Path audioFile);

    InputStream open(String objectKey);

    void delete(String objectKey);
}

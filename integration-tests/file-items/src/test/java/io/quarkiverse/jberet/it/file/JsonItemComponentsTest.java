package io.quarkiverse.jberet.it.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkiverse.jberet.components.runtime.item.json.JsonItemReader;
import io.quarkiverse.jberet.components.runtime.item.json.JsonItemWriter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class JsonItemComponentsTest {
    static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();
    static final TypeReference<List<Auction>> AUCTIONS = new TypeReference<>() {
    };
    static final Auction FIRST = new Auction(1, 10, 100, 200, 1);
    static final Auction SECOND = new Auction(2, 20, 300, 400, 2);
    static final Auction THIRD = new Auction(3, 30, 500, 600, 3);

    @Test
    void readerSkipsNullElements(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.json");
        Files.writeString(inputFile, "[null, " + json(FIRST) + ", null, null, " + json(SECOND) + ", null]");

        JsonItemReader<Auction> reader = new JsonItemReader<>(inputFile, Auction.class, OBJECT_MAPPER);
        reader.open(null);
        assertEquals(FIRST, reader.read());
        Serializable checkpoint = reader.checkpointInfo();
        assertEquals(SECOND, reader.read());
        assertNull(reader.read());
        reader.close();

        reader.open(checkpoint);
        assertEquals(SECOND, reader.read());
        assertNull(reader.read());
        reader.close();
    }

    @Test
    void readerRequiresArray(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.json");
        Files.writeString(inputFile, json(FIRST));

        JsonItemReader<Auction> reader = new JsonItemReader<>(inputFile, Auction.class, OBJECT_MAPPER);
        assertThrows(IllegalStateException.class, () -> reader.open(null));
        // resources are already released, so close is a no-op
        reader.close();
    }

    @Test
    void readerFromJar(@TempDir Path tempDir) throws Exception {
        Path jar = tempDir.resolve("auctions.jar");
        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jar))) {
            jarOutputStream.putNextEntry(new JarEntry("jar-auctions.json"));
            jarOutputStream.write(("[" + json(FIRST) + "," + json(SECOND) + "]").getBytes(StandardCharsets.UTF_8));
            jarOutputStream.closeEntry();
        }

        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        try (URLClassLoader classLoader = new URLClassLoader(new URL[] { jar.toUri().toURL() }, null)) {
            thread.setContextClassLoader(classLoader);
            JsonItemReader<Auction> reader = new JsonItemReader<>("jar-auctions.json", Auction.class, OBJECT_MAPPER);
            reader.open(null);
            assertEquals(FIRST, reader.read());
            assertEquals(SECOND, reader.read());
            assertNull(reader.read());
            reader.close();
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    @Test
    void writerDiscardsFailedChunk(@TempDir Path tempDir) throws Exception {
        Path outputFile = tempDir.resolve("output.json");

        JsonItemWriter<Object> writer = new JsonItemWriter<>(outputFile, OBJECT_MAPPER);
        writer.open(null);
        writer.write(List.of(FIRST));
        // the second item fails to serialize
        assertThrows(Exception.class, () -> writer.write(List.of(SECOND, new Failing(), THIRD)));
        writer.write(List.of(THIRD));
        writer.close();

        assertEquals(List.of(FIRST, THIRD), OBJECT_MAPPER.readValue(outputFile.toFile(), AUCTIONS));
    }

    @Test
    void writerRestart(@TempDir Path tempDir) throws Exception {
        Path outputFile = tempDir.resolve("output.json");

        JsonItemWriter<Auction> writer = new JsonItemWriter<>(outputFile, OBJECT_MAPPER);
        writer.open(null);
        writer.write(List.of(FIRST));
        Serializable checkpoint = writer.checkpointInfo();
        writer.write(List.of(SECOND));
        writer.close();

        writer.open(checkpoint);
        writer.write(List.of(THIRD));
        writer.close();

        assertEquals(List.of(FIRST, THIRD), OBJECT_MAPPER.readValue(outputFile.toFile(), AUCTIONS));
    }

    @Test
    void writerRestartMissingFile(@TempDir Path tempDir) throws Exception {
        Path outputFile = tempDir.resolve("output.json");

        JsonItemWriter<Auction> writer = new JsonItemWriter<>(outputFile, OBJECT_MAPPER);
        writer.open(null);
        writer.write(List.of(FIRST));
        Serializable checkpoint = writer.checkpointInfo();
        writer.close();

        Files.delete(outputFile);
        assertThrows(IllegalStateException.class, () -> writer.open(checkpoint));
        writer.close();
    }

    @Test
    void missingProperties() {
        assertThrows(NullPointerException.class, () -> new JsonItemReader<>((String) null, Auction.class, OBJECT_MAPPER));
        assertThrows(NullPointerException.class, () -> new JsonItemReader<>("auctions.json", null, OBJECT_MAPPER));
    }

    public static class Failing {
        public String getValue() {
            throw new IllegalStateException();
        }
    }

    static String json(Auction auction) {
        return OBJECT_MAPPER.writeValueAsString(auction);
    }
}

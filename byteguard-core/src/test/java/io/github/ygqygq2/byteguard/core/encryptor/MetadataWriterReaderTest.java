package io.github.ygqygq2.byteguard.core.encryptor;

import io.github.ygqygq2.byteguard.core.loader.MetadataReader;
import io.github.ygqygq2.byteguard.core.model.EncryptionMetadata;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MetadataWriterReaderTest {

    private final MetadataWriter writer = new MetadataWriter();
    private final MetadataReader reader = new MetadataReader();

    @Test
    void shouldRoundTripMetadata() throws IOException {
        byte[] salt = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
                                 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32};
        EncryptionMetadata original = new EncryptionMetadata(salt);
        original.addClass("com/example/Main.class", 1024);
        original.addClass("com/example/Foo.class", 512);

        String json = writer.toJson(original);
        assertNotNull(json);
        assertTrue(json.contains("\"version\""));
        assertTrue(json.contains("AES-256-GCM"));
        assertTrue(json.contains("com/example/Main.class"));

        EncryptionMetadata parsed = reader.parse(json);

        assertEquals(original.getVersion(), parsed.getVersion());
        assertEquals(original.getAlgorithm(), parsed.getAlgorithm());
        assertEquals(original.getKeyDerivation(), parsed.getKeyDerivation());
        assertArrayEquals(original.getSalt(), parsed.getSalt());
        assertEquals(original.getTotalClasses(), parsed.getTotalClasses());
        assertEquals(2, parsed.getClasses().size());
        assertEquals(1024, parsed.getClasses().get("com/example/Main.class"));
        assertEquals(512, parsed.getClasses().get("com/example/Foo.class"));
    }

    @Test
    void shouldReadFromInputStream() throws IOException {
        EncryptionMetadata original = new EncryptionMetadata(new byte[32]);
        original.addClass("com/test/App.class", 256);
        String json = writer.toJson(original);

        ByteArrayInputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        EncryptionMetadata parsed = reader.read(is);

        assertNotNull(parsed);
        assertTrue(parsed.containsClass("com/test/App.class"));
    }

    @Test
    void shouldHandleEmptyClasses() throws IOException {
        EncryptionMetadata original = new EncryptionMetadata(new byte[32]);
        String json = writer.toJson(original);
        EncryptionMetadata parsed = reader.parse(json);

        assertNotNull(parsed.getClasses());
        assertTrue(parsed.getClasses().isEmpty());
        assertEquals(0, parsed.getTotalClasses());
    }
}

package io.github.ygqygq2.byteguard.core.encryptor;

import io.github.ygqygq2.byteguard.core.crypto.CryptoException;
import io.github.ygqygq2.byteguard.core.loader.MetadataReader;
import io.github.ygqygq2.byteguard.core.model.EncryptionMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MetadataWriterReaderTest {

    private MetadataWriter writer;
    private MetadataReader reader;
    private MetadataIntegrity integrity;
    private byte[] masterKey;

    @BeforeEach
    void setUp() {
        writer = new MetadataWriter();
        reader = new MetadataReader();
        integrity = new MetadataIntegrity();
        masterKey = new byte[32];
        for (int i = 0; i < masterKey.length; i++) {
            masterKey[i] = (byte) (i + 1);
        }
    }

    @Test
    void shouldRoundTripMetadata() throws IOException, CryptoException {
        byte[] salt = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
                                 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32};
        EncryptionMetadata original = new EncryptionMetadata(salt);
        original.addClass("com/example/Main.class", 1024);
        original.addClass("com/example/Foo.class", 512);
        original.setMetadataMac(integrity.computeMac(original, masterKey));

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
        assertArrayEquals(original.getMetadataMac(), parsed.getMetadataMac());
        assertEquals(original.getTotalClasses(), parsed.getTotalClasses());
        assertEquals(2, parsed.getClasses().size());
        assertEquals(1024, parsed.getClasses().get("com/example/Main.class"));
        assertEquals(512, parsed.getClasses().get("com/example/Foo.class"));
        assertTrue(integrity.verify(parsed, masterKey));
    }

    @Test
    void shouldReadFromInputStream() throws IOException, CryptoException {
        EncryptionMetadata original = new EncryptionMetadata(new byte[32]);
        original.addClass("com/test/App.class", 256);
        original.setMetadataMac(integrity.computeMac(original, masterKey));
        String json = writer.toJson(original);

        ByteArrayInputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        EncryptionMetadata parsed = reader.read(is);

        assertNotNull(parsed);
        assertTrue(parsed.containsClass("com/test/App.class"));
        assertTrue(integrity.verify(parsed, masterKey));
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

    @Test
    void shouldFailIntegrityCheckAfterTampering() throws Exception {
        EncryptionMetadata metadata = new EncryptionMetadata(new byte[32]);
        metadata.addClass("com/example/Main.class", 1024);
        metadata.setMetadataMac(integrity.computeMac(metadata, masterKey));

        String json = writer.toJson(metadata).replace("1024", "2048");
        EncryptionMetadata tampered = reader.parse(json);

        assertFalse(integrity.verify(tampered, masterKey));
    }
}

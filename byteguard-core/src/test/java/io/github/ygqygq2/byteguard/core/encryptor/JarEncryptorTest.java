package io.github.ygqygq2.byteguard.core.encryptor;

import io.github.ygqygq2.byteguard.core.crypto.CryptoException;
import io.github.ygqygq2.byteguard.core.loader.DecryptingClassLoader;
import io.github.ygqygq2.byteguard.core.loader.MetadataReader;
import io.github.ygqygq2.byteguard.core.model.EncryptionConfig;
import io.github.ygqygq2.byteguard.core.model.EncryptionMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JarEncryptor 集成测试
 */
class JarEncryptorTest {

    @TempDir
    Path tempDir;

    /**
     * 创建最小测试 JAR（含真实的 .class 字节码：从当前 classpath 取一个类）
     */
    private Path buildMinimalJar() throws IOException {
        Path jarPath = tempDir.resolve("test-input.jar");

        // 使用 JarEncryptor 本身的 .class 字节作为测试素材（它存在于 classpath 中）
        String testClass = "io/github/ygqygq2/byteguard/core/encryptor/ClassEncryptor.class";
        byte[] classBytes;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(testClass)) {
            assertNotNull(is, "Test class not found in classpath: " + testClass);
            classBytes = is.readAllBytes();
        }

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");

        try (JarOutputStream jos = new JarOutputStream(java.nio.file.Files.newOutputStream(jarPath), manifest)) {
            jos.putNextEntry(new JarEntry(testClass));
            jos.write(classBytes);
            jos.closeEntry();

            // Add a resource file (should not be encrypted)
            jos.putNextEntry(new JarEntry("META-INF/app.properties"));
            jos.write("app.name=test\n".getBytes());
            jos.closeEntry();
        }

        return jarPath;
    }

    @Test
    void shouldEncryptJarAndProduceMetadata() throws IOException, CryptoException {
        Path inputJar = buildMinimalJar();
        Path outputJar = tempDir.resolve("test-encrypted.jar");

        JarEncryptor encryptor = new JarEncryptor("password123",
            EncryptionConfig.builder().includePackage("io.github.ygqygq2.byteguard").build());
        EncryptionMetadata metadata = encryptor.encrypt(inputJar, outputJar);

        assertNotNull(metadata);
        assertTrue(metadata.getTotalClasses() > 0, "Should have encrypted at least one class");
        assertTrue(java.nio.file.Files.exists(outputJar));
    }

    @Test
    void shouldWriteMetadataJsonToOutputJar() throws IOException, CryptoException {
        Path inputJar = buildMinimalJar();
        Path outputJar = tempDir.resolve("test-encrypted.jar");

        new JarEncryptor("password123",
            EncryptionConfig.builder().includePackage("io.github.ygqygq2").build())
            .encrypt(inputJar, outputJar);

        try (JarFile jf = new JarFile(outputJar.toFile())) {
            JarEntry metaEntry = jf.getJarEntry(EncryptionMetadata.METADATA_PATH);
            assertNotNull(metaEntry, "metadata.json should be present");

            MetadataReader mr = new MetadataReader();
            EncryptionMetadata parsed = mr.read(jf.getInputStream(metaEntry));
            assertEquals("1.0", parsed.getVersion());
            assertEquals("AES-256-GCM", parsed.getAlgorithm());
            assertNotNull(parsed.getSalt());
            assertTrue(parsed.getTotalClasses() > 0);
        }
    }

    @Test
    void shouldPreserveResourceFiles() throws IOException, CryptoException {
        Path inputJar = buildMinimalJar();
        Path outputJar = tempDir.resolve("test-encrypted.jar");

        new JarEncryptor("password123").encrypt(inputJar, outputJar);

        try (JarFile jf = new JarFile(outputJar.toFile())) {
            // Resource file should be preserved as-is
            JarEntry propEntry = jf.getJarEntry("META-INF/app.properties");
            assertNotNull(propEntry, "Resource file should be preserved");
        }
    }

    @Test
    void shouldLoadEncryptedClassWithDecryptingClassLoader()
            throws IOException, CryptoException, ClassNotFoundException {
        Path inputJar = buildMinimalJar();
        Path outputJar = tempDir.resolve("test-encrypted.jar");

        String password = "testLoadPassword";
        JarEncryptor encryptor = new JarEncryptor(password,
            EncryptionConfig.builder().includePackage("io.github.ygqygq2").build());
        encryptor.encrypt(inputJar, outputJar);

        // Read salt from metadata and derive master key
        MetadataReader mr = new MetadataReader();
        EncryptionMetadata metadata;
        try (JarFile jf = new JarFile(outputJar.toFile())) {
            JarEntry metaEntry = jf.getJarEntry(EncryptionMetadata.METADATA_PATH);
            metadata = mr.read(jf.getInputStream(metaEntry));
        }

        byte[] masterKey = new io.github.ygqygq2.byteguard.core.crypto.KeyDerivation()
            .deriveMasterKey(password, metadata.getSalt());

        io.github.ygqygq2.byteguard.core.loader.ClassDecryptor decryptor =
            new io.github.ygqygq2.byteguard.core.loader.ClassDecryptor(masterKey);

        URL[] urls = {outputJar.toUri().toURL()};
        try (DecryptingClassLoader loader = new DecryptingClassLoader(urls, getClass().getClassLoader(), decryptor)) {
            Class<?> clazz = loader.loadClass("io.github.ygqygq2.byteguard.core.encryptor.ClassEncryptor");
            assertNotNull(clazz);
            assertEquals("io.github.ygqygq2.byteguard.core.encryptor.ClassEncryptor", clazz.getName());
        }
    }
}

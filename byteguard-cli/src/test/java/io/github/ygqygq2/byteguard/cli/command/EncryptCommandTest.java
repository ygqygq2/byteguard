package io.github.ygqygq2.byteguard.cli.command;

import io.github.ygqygq2.byteguard.core.loader.MetadataReader;
import io.github.ygqygq2.byteguard.core.model.EncryptionMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncryptCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldShowProgressAndWriteReport() throws Exception {
        Path inputJar = buildTestJar();
        Path outputJar = tempDir.resolve("app-encrypted.jar");
        Path reportFile = tempDir.resolve("reports/encryption-report.json");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        executeWithCapturedStdout(output, () -> new EncryptCommand().execute(new String[] {
            "--input", inputJar.toString(),
            "--output", outputJar.toString(),
            "--password", "test123",
            "--threads", "2",
            "--report", reportFile.toString()
        }));

        String console = output.toString(StandardCharsets.UTF_8);
        assertTrue(console.contains("100%"), "Progress output should contain 100%");
        assertTrue(console.contains("Classes encrypted: 2"), "Summary should contain encrypted count");
        assertTrue(Files.exists(reportFile), "Report file should be created");

        String reportJson = Files.readString(reportFile, StandardCharsets.UTF_8);
        assertTrue(reportJson.contains("\"totalClasses\": 2"));
        assertTrue(reportJson.contains("\"threads\": 2"));

        EncryptionMetadata metadata = readMetadata(outputJar);
        assertEquals(2, metadata.getTotalClasses());
    }

    @Test
    void shouldRespectPackageAndExcludeFilters() throws Exception {
        Path inputJar = buildTestJar();
        Path outputJar = tempDir.resolve("filtered-encrypted.jar");

        executeWithCapturedStdout(new ByteArrayOutputStream(), () -> new EncryptCommand().execute(new String[] {
            "--input", inputJar.toString(),
            "--output", outputJar.toString(),
            "--password", "test123",
            "--packages", "io.github.ygqygq2.byteguard.core",
            "--exclude", "io.github.ygqygq2.byteguard.core.crypto"
        }));

        EncryptionMetadata metadata = readMetadata(outputJar);
        assertEquals(1, metadata.getTotalClasses());
        assertTrue(metadata.getClasses().containsKey("io/github/ygqygq2/byteguard/core/encryptor/ClassEncryptor.class"));
    }

    @Test
    void shouldResolvePasswordFromEnvironment() {
        EncryptCommand command = new EncryptCommand() {
            @Override
            String lookupEnv(String name) {
                return "BYTEGUARD_PASSWORD".equals(name) ? "env-secret" : null;
            }
        };

        assertEquals("env-secret", command.resolvePassword(null));
        assertEquals("cli-secret", command.resolvePassword("cli-secret"));
    }

    private Path buildTestJar() throws IOException {
        Path jarPath = tempDir.resolve("test-app.jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath), manifest)) {
            addClassEntry(jos, "io/github/ygqygq2/byteguard/core/encryptor/ClassEncryptor.class");
            addClassEntry(jos, "io/github/ygqygq2/byteguard/core/crypto/AESGCMCipher.class");
            jos.putNextEntry(new JarEntry("META-INF/app.properties"));
            jos.write("app.name=test\n".getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }

        return jarPath;
    }

    private void addClassEntry(JarOutputStream jos, String resourcePath) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertTrue(is != null, "Missing class resource: " + resourcePath);
            jos.putNextEntry(new JarEntry(resourcePath));
            jos.write(is.readAllBytes());
            jos.closeEntry();
        }
    }

    private EncryptionMetadata readMetadata(Path outputJar) throws Exception {
        try (JarFile jarFile = new JarFile(outputJar.toFile())) {
            JarEntry metadataEntry = jarFile.getJarEntry(EncryptionMetadata.METADATA_PATH);
            assertTrue(metadataEntry != null, "Encrypted JAR should contain metadata");
            return new MetadataReader().read(jarFile.getInputStream(metadataEntry));
        }
    }

    private void executeWithCapturedStdout(ByteArrayOutputStream buffer, ThrowingRunnable runnable) throws Exception {
        PrintStream originalOut = System.out;
        try (PrintStream replacement = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            System.setOut(replacement);
            runnable.run();
        } finally {
            System.setOut(originalOut);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}

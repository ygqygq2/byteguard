package io.github.ygqygq2.byteguard.cli.command;

import io.github.ygqygq2.byteguard.core.encryptor.JarEncryptor;
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
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerifyCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldVerifyEncryptedJarSuccessfully() throws Exception {
        Path encryptedJar = buildEncryptedJar();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        executeWithCapturedStdout(output, () -> new VerifyCommand().execute(new String[] {
            "--jar", encryptedJar.toString(),
            "--password", "test123",
            "--verbose"
        }));

        String console = output.toString(StandardCharsets.UTF_8);
        assertTrue(console.contains("[3/3] Verifying password ... OK"));
        assertTrue(console.contains("✓ Verification passed"));
    }

    @Test
    void shouldResolvePasswordFromEnvironment() {
        VerifyCommand command = new VerifyCommand() {
            @Override
            String lookupEnv(String name) {
                return "BYTEGUARD_PASSWORD".equals(name) ? "env-secret" : null;
            }
        };

        assertEquals("env-secret", command.resolvePassword(null));
        assertEquals("cli-secret", command.resolvePassword("cli-secret"));
    }

    private Path buildEncryptedJar() throws Exception {
        Path inputJar = buildTestJar();
        Path outputJar = tempDir.resolve("app-encrypted.jar");
        new JarEncryptor("test123").encrypt(inputJar, outputJar);
        return outputJar;
    }

    private Path buildTestJar() throws IOException {
        Path jarPath = tempDir.resolve("test-app.jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath), manifest)) {
            addClassEntry(jos, "io/github/ygqygq2/byteguard/core/encryptor/ClassEncryptor.class");
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

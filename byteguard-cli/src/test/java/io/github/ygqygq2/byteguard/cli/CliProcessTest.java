package io.github.ygqygq2.byteguard.cli;

import io.github.ygqygq2.byteguard.core.encryptor.JarEncryptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliProcessTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRunVersionCommandInSubprocess() throws Exception {
        CliResult result = runCli(tempDir, "version");

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("ByteGuard - Java Bytecode Encryption Tool"));
        assertTrue(result.output().contains("Version"));
    }

    @Test
    void shouldEncryptVerifyAndAnalyzeJarInSubprocess() throws Exception {
        Path inputJar = buildTestJar();
        Path outputJar = tempDir.resolve("app-encrypted.jar");
        Path reportFile = tempDir.resolve("reports/cli-report.json");

        CliResult encrypt = runCli(
            tempDir,
            "encrypt",
            "--input", inputJar.toString(),
            "--output", outputJar.toString(),
            "--password", "test123",
            "--report", reportFile.toString(),
            "--threads", "2"
        );
        assertEquals(0, encrypt.exitCode(), encrypt.output());
        assertTrue(encrypt.output().contains("Encryption completed"), encrypt.output());
        assertTrue(Files.exists(outputJar), "Encrypted JAR should exist");
        assertTrue(Files.exists(reportFile), "Report file should exist");

        CliResult verify = runCli(
            tempDir,
            "verify",
            "--jar", outputJar.toString(),
            "--password", "test123"
        );
        assertEquals(0, verify.exitCode(), verify.output());
        assertTrue(verify.output().contains("Verification passed"), verify.output());

        CliResult analyze = runCli(
            tempDir,
            "analyze",
            "--jar", outputJar.toString(),
            "--verbose"
        );
        assertEquals(0, analyze.exitCode(), analyze.output());
        assertTrue(analyze.output().contains("Mode: Encrypted JAR"), analyze.output());
        assertTrue(analyze.output().contains("AES-256-GCM"), analyze.output());
    }

    @Test
    void shouldFailVerifyWithWrongPassword() throws Exception {
        Path encryptedJar = buildEncryptedJar();

        CliResult result = runCli(
            tempDir,
            "verify",
            "--jar", encryptedJar.toString(),
            "--password", "wrong-password"
        );

        assertNotEquals(0, result.exitCode(), result.output());
        assertTrue(
            result.output().contains("wrong password") || result.output().contains("Verification FAILED"),
            result.output()
        );
    }

    @Test
    void shouldFailEncryptWithoutPassword() throws Exception {
        Path inputJar = buildTestJar();

        CliResult result = runCli(
            tempDir,
            "encrypt",
            "--input", inputJar.toString(),
            "--output", tempDir.resolve("missing-password.jar").toString()
        );

        assertNotEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("Missing required arguments"), result.output());
    }

    @Test
    void shouldFailForUnknownCommand() throws Exception {
        CliResult result = runCli(tempDir, "totally-unknown-command");

        assertNotEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("Unknown command"), result.output());
        assertTrue(result.output().contains("Commands:"), result.output());
    }

    private CliResult runCli(Path workingDirectory, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(resolveJavaCommand());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add("io.github.ygqygq2.byteguard.cli.Main");
        command.addAll(List.of(args));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDirectory.toFile());
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream inputStream = process.getInputStream()) {
            inputStream.transferTo(output);
        }

        int exitCode = process.waitFor();
        return new CliResult(exitCode, output.toString(StandardCharsets.UTF_8));
    }

    private String resolveJavaCommand() {
        Path javaHome = Path.of(System.getProperty("java.home"));
        Path javaBinary = javaHome.resolve("bin").resolve("java");
        return javaBinary.toString();
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

    private record CliResult(int exitCode, String output) {}
}

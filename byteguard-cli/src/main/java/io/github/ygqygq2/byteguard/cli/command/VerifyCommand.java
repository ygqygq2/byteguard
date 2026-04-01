package io.github.ygqygq2.byteguard.cli.command;

import io.github.ygqygq2.byteguard.core.crypto.AESGCMCipher;
import io.github.ygqygq2.byteguard.core.crypto.KeyDerivation;
import io.github.ygqygq2.byteguard.core.loader.MetadataReader;
import io.github.ygqygq2.byteguard.core.model.EncryptionMetadata;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Verify 命令 - 验证加密 JAR 的完整性及密码正确性
 *
 * <p>检查项：
 * <ol>
 *   <li>元数据文件存在</li>
 *   <li>元数据解析正常</li>
 *   <li>使用给定密码尝试解密首个加密类，确认密码正确</li>
 * </ol>
 *
 * @author ygqygq2
 */
public class VerifyCommand {

    public void execute(String[] args) throws Exception {
        System.out.println("[ByteGuard] Verify Encrypted JAR");

        String inputJar = null;
        String password = null;
        boolean verbose = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--jar":
                case "-j":
                    inputJar = args[++i];
                    break;
                case "--password":
                case "-p":
                    password = args[++i];
                    break;
                case "--verbose":
                case "-v":
                    verbose = true;
                    break;
                case "--help":
                case "-h":
                    printUsage();
                    return;
            }
        }

        password = resolvePassword(password);

        if (inputJar == null) {
            System.err.println("Error: --jar is required");
            printUsage();
            System.exit(1);
        }
        if (password == null) {
            System.err.println("Error: --password is required (or set BYTEGUARD_PASSWORD)");
            System.exit(1);
        }

        Path jarPath = Paths.get(inputJar);
        if (!Files.exists(jarPath)) {
            System.err.println("Error: JAR not found: " + jarPath.toAbsolutePath());
            System.exit(1);
        }

        System.out.println("  JAR: " + jarPath.toAbsolutePath());
        System.out.println();

        boolean ok = verify(jarPath, password, verbose);
        if (!ok) {
            System.exit(1);
        }
    }

    String resolvePassword(String password) {
        if (password != null && !password.isBlank()) {
            return password;
        }
        String envPassword = lookupEnv("BYTEGUARD_PASSWORD");
        return (envPassword == null || envPassword.isBlank()) ? null : envPassword;
    }

    String lookupEnv(String name) {
        return System.getenv(name);
    }

    private boolean verify(Path jarPath, String password, boolean verbose) throws Exception {
        boolean allPassed = true;

        try (JarFile jar = new JarFile(jarPath.toFile())) {

            // --- 1. 检查元数据 ---
            System.out.print("  [1/3] Checking metadata ... ");
            ZipEntry metaEntry = jar.getEntry(EncryptionMetadata.METADATA_PATH);
            if (metaEntry == null) {
                System.out.println("FAIL");
                System.out.println("      ✗ Metadata not found: " + EncryptionMetadata.METADATA_PATH);
                System.out.println("      This JAR does not appear to be encrypted by ByteGuard.");
                return false;
            }
            System.out.println("OK");

            // --- 2. 解析元数据 ---
            System.out.print("  [2/3] Parsing metadata  ... ");
            EncryptionMetadata metadata;
            try (InputStream is = jar.getInputStream(metaEntry)) {
                metadata = new MetadataReader().read(is);
            }
            System.out.println("OK");

            if (verbose) {
                System.out.println();
                System.out.println("      Algorithm    : " + metadata.getAlgorithm());
                System.out.println("      Key derivation: " + metadata.getKeyDerivation());
                System.out.println("      Encrypted at : " + Instant.ofEpochMilli(metadata.getEncryptedAt()));
                System.out.println("      Total classes: " + metadata.getTotalClasses());
                System.out.println();
            }

            // --- 3. 验证密码 ---
            System.out.print("  [3/3] Verifying password ... ");
            Map<String, Integer> classes = metadata.getClasses();
            if (classes == null || classes.isEmpty()) {
                System.out.println("SKIP (no encrypted classes in metadata)");
            } else {
                // 取第一个加密类验证
                String firstClass = classes.keySet().iterator().next();
                // 加密字节写在原始路径（JarEncryptor 覆盖原条目）
                ZipEntry encEntry = jar.getEntry(firstClass);

                if (encEntry == null) {
                    System.out.println("FAIL");
                    System.err.println("      ✗ Encrypted class not found: " + firstClass);
                    allPassed = false;
                } else {
                    byte[] encryptedBytes;
                    try (InputStream is = jar.getInputStream(encEntry)) {
                        encryptedBytes = is.readAllBytes();
                    }
                    try {
                        byte[] salt = metadata.getSalt();
                        KeyDerivation kd = new KeyDerivation();
                        byte[] masterKey = kd.deriveMasterKey(password, salt);
                        // 密钥派生用点分类名（与 ClassDecryptor 一致）
                        String dottedName = firstClass
                            .substring(0, firstClass.length() - 6)
                            .replace('/', '.');
                        byte[] classKey = kd.deriveClassKey(masterKey, dottedName);
                        new AESGCMCipher().decrypt(encryptedBytes, classKey);
                        System.out.println("OK");
                    } catch (Exception e) {
                        System.out.println("FAIL");
                        System.err.println("      ✗ Decryption failed: wrong password or corrupted data");
                        if (verbose) {
                            System.err.println("        " + e.getMessage());
                        }
                        allPassed = false;
                    }
                }
            }
        }

        System.out.println();
        if (allPassed) {
            System.out.println("✓ Verification passed");
        } else {
            System.out.println("✗ Verification FAILED");
        }
        return allPassed;
    }

    private void printUsage() {
        System.out.println("Usage: byteguard verify [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --jar,      -j <path>      Encrypted JAR file (required)");
        System.out.println("  --password, -p <password>  Decryption password (or BYTEGUARD_PASSWORD env)");
        System.out.println("  --verbose,  -v             Show detailed information");
        System.out.println("  --help,     -h             Show this help");
    }
}

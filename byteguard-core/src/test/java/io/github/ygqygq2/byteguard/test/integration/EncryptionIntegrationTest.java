package io.github.ygqygq2.byteguard.test.integration;

import io.github.ygqygq2.byteguard.core.crypto.AESGCMCipher;
import io.github.ygqygq2.byteguard.core.crypto.KeyDerivation;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试 - 验证加密完整流程
 */
@DisplayName("ByteGuard 集成测试")
@Tag("integration")
public class EncryptionIntegrationTest {

    private AESGCMCipher cipher;
    private KeyDerivation keyDerivation;
    
    private static final String ARTHAS_DOWNLOAD_URL = "https://arthas.aliyun.com/arthas-boot.jar";
    private static final String TEST_APPS_CACHE = ".test-apps";

    @BeforeEach
    @DisplayName("初始化加密模块")
    void setUp() {
        cipher = new AESGCMCipher();
        keyDerivation = new KeyDerivation();
    }

    @Test
    @DisplayName("完整流程: 密钥派生 → 加密 → 解密")
    void testEncryptionDecryptionFlow() throws Exception {
        // 原始数据
        String originalData = "ByteGuard Integration Test - Confidential Data 🔐";
        byte[] originalBytes = originalData.getBytes("UTF-8");

        // 密钥派生
        String password = "test-password-2026";
        byte[] salt = keyDerivation.generateSalt();
        byte[] masterKey = keyDerivation.deriveMasterKey(password, salt);

        // 加密
        byte[] encrypted = cipher.encrypt(originalBytes, masterKey);

        // 验证加密后数据不同
        assertNotEquals(new String(originalBytes, "UTF-8"), 
                       new String(encrypted, "UTF-8"),
                       "加密后数据应该与原始数据不同");

        // 解密
        byte[] decrypted = cipher.decrypt(encrypted, masterKey);

        // 验证解密结果
        assertEquals(originalData, new String(decrypted, "UTF-8"),
                    "解密后数据应该等于原始数据");
        
        System.out.println("✓ 加密/解密流程测试通过");
    }

    @Test
    @DisplayName("完整流程: 密钥派生一致性")
    void testKeyDerivationConsistency() throws Exception {
        String password = "consistency-test-password";
        byte[] salt = keyDerivation.generateSalt();

        // 同一密码和盐派生两次
        byte[] key1 = keyDerivation.deriveMasterKey(password, salt);
        byte[] key2 = keyDerivation.deriveMasterKey(password, salt);

        // 验证派生结果一致
        assertArrayEquals(key1, key2, "相同密码和盐派生的密钥应相同");

        // 验证不同密码产生不同密钥
        byte[] differentKey = keyDerivation.deriveMasterKey("different-password", salt);
        assertFalse(java.util.Arrays.equals(key1, differentKey),
                   "不同密码应派生不同的密钥");
        
        System.out.println("✓ 密钥派生一致性测试通过");
    }

    @Test
    @DisplayName("完整流程: 多次加密解密")
    void testMultipleEncryptionRounds() throws Exception {
        String password = "multi-round-password";
        byte[] salt = keyDerivation.generateSalt();
        byte[] key = keyDerivation.deriveMasterKey(password, salt);

        String[] testData = {
            "Data 1",
            "数据 2 - 中文测试",
            "🚀 Emoji Test 🔐",
            "Very long data ".repeat(100)
        };

        for (String data : testData) {
            byte[] original = data.getBytes("UTF-8");
            byte[] encrypted = cipher.encrypt(original, key);
            byte[] decrypted = cipher.decrypt(encrypted, key);

            assertArrayEquals(original, decrypted,
                            "多轮加密解密应保持数据完整性");
        }
        
        System.out.println("✓ 多轮加密解密测试通过");
    }

    @Test
    @DisplayName("端到端测试: 下载并加密 Arthas")
    @Tag("integration")
    void testEncryptRealApplication(@TempDir Path tempDir) throws Exception {
        System.out.println("\n=== 开始 Arthas 真实应用加密测试 ===");
        
        // 1. 下载 Arthas JAR (首次运行会下载，后续使用缓存)
        Path arthasJar = downloadArthas();
        System.out.println("Arthas JAR 路径: " + arthasJar);
        System.out.println("Arthas JAR 大小: " + formatSize(Files.size(arthasJar)));
        
        // 2. 准备输出路径
        Path encryptedJar = tempDir.resolve("arthas-encrypted.jar");
        
        // 3. 使用 CLI 工具加密 Halo
        Path cliJar = Paths.get("../byteguard-cli/build/libs/byteguard-cli-1.0.0-SNAPSHOT.jar");
        assertTrue(Files.exists(cliJar), "CLI JAR 应该存在: " + cliJar.toAbsolutePath());
        
        String password = "test-arthas-password";
        ProcessBuilder pb = new ProcessBuilder(
            "java", "-jar", cliJar.toAbsolutePath().toString(),
            "encrypt",
            "--input", arthasJar.toAbsolutePath().toString(),
            "--output", encryptedJar.toAbsolutePath().toString(),
            "--password", password
        );
        pb.inheritIO();
        
        System.out.println("\n执行加密命令:");
        System.out.println(String.join(" ", pb.command()));
        
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        assertEquals(0, exitCode, "CLI 加密命令应该成功执行");
        assertTrue(Files.exists(encryptedJar), "加密后的 JAR 应该被创建");
        System.out.println("\n加密后 JAR 大小: " + formatSize(Files.size(encryptedJar)));
        
        // 4. 验证加密后的 JAR 结构
        try (var fs = FileSystems.newFileSystem(encryptedJar, (ClassLoader) null)) {
            Path metadataPath = fs.getPath("META-INF", ".byteguard", "metadata.json");
            assertTrue(Files.exists(metadataPath), "加密元数据应该存在");
            System.out.println("✓ 加密元数据验证通过");
        }
        
        // 5. 使用预生成的测试 License 文件（由 byteguard-license-server 生成）
        Path licenseFile = Paths.get(".test-apps/test-integration.lic");
        assertTrue(Files.exists(licenseFile), 
            "测试 License 应该存在: " + licenseFile.toAbsolutePath() +
            "\n请运行: cd ../byteguard-license-server && ./bin/license-generator -type TRIAL -to 'Integration Test' -days 365 -output ../byteguard/.test-apps/test-integration.lic");
        System.out.println("\n✓ 使用测试 License: " + licenseFile.toAbsolutePath());
        
        // 6. 验证加密后的 Arthas 可以使用 JavaAgent 运行
        System.out.println("\n测试加密后的 Arthas 运行:");
        ProcessBuilder testPb = new ProcessBuilder(
            "java",
            "-Dbyteguard.license=" + licenseFile.toAbsolutePath().toString(),
            "-javaagent:" + cliJar.toAbsolutePath().toString() + "=password=" + password,
            "-jar", encryptedJar.toAbsolutePath().toString(),
            "-h"
        );
        testPb.redirectErrorStream(true);
        testPb.directory(tempDir.toFile()); // 设置工作目录
        
        Process testProcess = testPb.start();
        
        // 读取输出
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(testProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                if (output.length() < 500) { // 只打印前500字符
                    System.out.println(line);
                }
            }
        }
        
        int testExitCode = testProcess.waitFor();
        String outputStr = output.toString();
        
        // 验证 Arthas 帮助信息出现
        assertTrue(outputStr.contains("arthas-boot") || outputStr.contains("Bootstrap Arthas") 
                  || testExitCode == 0,
                  "加密后的 Arthas 应该能正常显示帮助信息");
        
        System.out.println("✓ 加密后的 Arthas 运行测试通过 (exitCode=" + testExitCode + ")");
        
        System.out.println("\n=== Arthas 加密测试完成! ===\n");
    }

    /**
     * 下载 Arthas JAR 到本地缓存目录
     * 如果已存在则跳过下载
     */
    private Path downloadArthas() throws IOException {
        Path cacheDir = Paths.get(TEST_APPS_CACHE);
        Files.createDirectories(cacheDir);
        
        Path arthasJar = cacheDir.resolve("arthas-boot.jar");
        
        if (Files.exists(arthasJar)) {
            System.out.println("✓ 使用缓存的 Arthas JAR: " + arthasJar);
            return arthasJar;
        }
        
        System.out.println("正在从阿里云下载 Arthas JAR...");
        System.out.println("下载地址: " + ARTHAS_DOWNLOAD_URL);
        System.out.println("(首次下载约 15-20MB，请耐心等待...)");
        
        try (InputStream in = new URL(ARTHAS_DOWNLOAD_URL).openStream()) {
            Files.copy(in, arthasJar);
        }
        
        System.out.println("✓ 下载完成: " + arthasJar);
        return arthasJar;
    }

    /**
     * 格式化文件大小为可读格式
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}

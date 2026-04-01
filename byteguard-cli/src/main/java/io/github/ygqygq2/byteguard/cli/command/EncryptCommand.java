package io.github.ygqygq2.byteguard.cli.command;

import io.github.ygqygq2.byteguard.core.encryptor.JarEncryptor;
import io.github.ygqygq2.byteguard.core.model.EncryptionConfig;
import io.github.ygqygq2.byteguard.core.model.EncryptionMetadata;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Encrypt 命令 - JAR 文件加密
 *
 * <p>使用核心 {@link JarEncryptor} 完成加密，支持包过滤和排除规则。
 *
 * @author ygqygq2
 */
public class EncryptCommand {

    public void execute(String[] args) throws Exception {
        System.out.println("[ByteGuard] Encrypt JAR");

        // 解析参数
        String inputJar = null;
        String outputJar = null;
        String password = null;
        String reportPath = null;
        List<String> packages = new ArrayList<>();
        List<String> excludes = new ArrayList<>();
        int threads = Runtime.getRuntime().availableProcessors();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--input":
                    inputJar = args[++i];
                    break;
                case "--output":
                    outputJar = args[++i];
                    break;
                case "--password":
                    password = args[++i];
                    break;
                case "--threads":
                    threads = Integer.parseInt(args[++i]);
                    break;
                case "--report":
                    reportPath = args[++i];
                    break;
                case "--packages":
                    // 逗号分隔的包名列表，如 com.example,org.foo
                    for (String pkg : args[++i].split(",")) {
                        packages.add(pkg.trim());
                    }
                    break;
                case "--exclude":
                    // 逗号分隔的排除包名列表
                    for (String exc : args[++i].split(",")) {
                        excludes.add(exc.trim());
                    }
                    break;
                default:
                    System.err.println("[ByteGuard] Warning: unknown argument: " + args[i]);
            }
        }

        password = resolvePassword(password);

        if (inputJar == null || outputJar == null || password == null) {
            throw new IllegalArgumentException(
                "Missing required arguments: --input, --output, --password (or BYTEGUARD_PASSWORD)\n" +
                "Usage: encrypt --input <jar> --output <jar> [--password <pwd>] [--packages <pkg1,pkg2>] [--exclude <pkg1,pkg2>] [--threads <n>] [--report <file>]"
            );
        }

        File input = new File(inputJar);
        if (!input.exists()) {
            throw new FileNotFoundException("Input JAR not found: " + inputJar);
        }

        System.out.println("Input:    " + inputJar);
        System.out.println("Output:   " + outputJar);
        System.out.println("Password: ****");
        System.out.println("Threads:  " + threads);
        if (!packages.isEmpty()) {
            System.out.println("Packages: " + String.join(", ", packages));
        }
        if (!excludes.isEmpty()) {
            System.out.println("Excludes: " + String.join(", ", excludes));
        }
        if (reportPath != null) {
            System.out.println("Report:   " + reportPath);
        }

        // 构建加密配置
        EncryptionConfig.Builder configBuilder = EncryptionConfig.builder().threads(threads);
        for (String pkg : packages) {
            configBuilder.includePackage(pkg);
        }
        for (String exc : excludes) {
            configBuilder.excludePackage(exc);
        }
        EncryptionConfig config = configBuilder.build();

        // 使用核心 JarEncryptor 加密
        JarEncryptor encryptor = new JarEncryptor(password, config);
        Path inputPath = input.toPath();
        Path outputPath = new File(outputJar).toPath();
        ConsoleProgressRenderer progressRenderer = new ConsoleProgressRenderer();

        System.out.println();
        EncryptionMetadata metadata = encryptor.encrypt(inputPath, outputPath, progressRenderer);

        System.out.println();
        System.out.println("✓ Encryption completed!");
        System.out.println("  - Classes encrypted: " + metadata.getTotalClasses());
        System.out.println("  - Algorithm:         " + metadata.getAlgorithm());
        System.out.println("  - Key derivation:    " + metadata.getKeyDerivation());
        System.out.println("  - Output:            " + outputJar);

        if (reportPath != null) {
            Path reportFile = new File(reportPath).toPath();
            writeReport(reportFile, inputPath, outputPath, metadata, packages, excludes, threads);
            System.out.println("  - Report:            " + reportFile.toAbsolutePath());
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

    private void writeReport(Path reportFile, Path inputPath, Path outputPath, EncryptionMetadata metadata,
                             List<String> packages, List<String> excludes, int threads) throws IOException {
        if (reportFile.getParent() != null) {
            Files.createDirectories(reportFile.getParent());
        }
        Files.writeString(
            reportFile,
            buildReportJson(inputPath, outputPath, metadata, packages, excludes, threads),
            StandardCharsets.UTF_8
        );
    }

    private String buildReportJson(Path inputPath, Path outputPath, EncryptionMetadata metadata,
                                   List<String> packages, List<String> excludes, int threads) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"input\": \"").append(escapeJson(inputPath.toAbsolutePath().toString())).append("\",\n");
        json.append("  \"output\": \"").append(escapeJson(outputPath.toAbsolutePath().toString())).append("\",\n");
        json.append("  \"algorithm\": \"").append(escapeJson(metadata.getAlgorithm())).append("\",\n");
        json.append("  \"keyDerivation\": \"").append(escapeJson(metadata.getKeyDerivation())).append("\",\n");
        json.append("  \"encryptedAt\": \"")
            .append(Instant.ofEpochMilli(metadata.getEncryptedAt()))
            .append("\",\n");
        json.append("  \"totalClasses\": ").append(metadata.getTotalClasses()).append(",\n");
        json.append("  \"threads\": ").append(threads).append(",\n");
        json.append("  \"packages\": ").append(toJsonArray(packages)).append(",\n");
        json.append("  \"excludes\": ").append(toJsonArray(excludes)).append("\n");
        json.append("}\n");
        return json.toString();
    }

    private String toJsonArray(List<String> values) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                json.append(", ");
            }
            json.append('"').append(escapeJson(values.get(i))).append('"');
        }
        json.append(']');
        return json.toString();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static final class ConsoleProgressRenderer implements JarEncryptor.ProgressListener {
        private static final int BAR_WIDTH = 24;

        @Override
        public void onStart(int totalClasses) {
            System.out.println("Encrypting...");
            System.out.println("  Classes matched: " + totalClasses);
            if (totalClasses == 0) {
                System.out.println("  Nothing to encrypt for the current filter.");
            }
        }

        @Override
        public void onProgress(String classPath, int completed, int totalClasses) {
            if (totalClasses <= 0) {
                return;
            }

            int percent = (completed * 100) / totalClasses;
            int filled = (completed * BAR_WIDTH) / totalClasses;
            String bar = "=".repeat(filled) + "-".repeat(BAR_WIDTH - filled);
            String current = abbreviate(classPath, 48);

            System.out.printf("\r  [%s] %3d%% (%d/%d) %s", bar, percent, completed, totalClasses, current);
            if (completed == totalClasses) {
                System.out.println();
            }
        }

        @Override
        public void onFinish(EncryptionMetadata metadata) {
            // no-op
        }

        private String abbreviate(String value, int maxLength) {
            if (value.length() <= maxLength) {
                return value;
            }
            return "..." + value.substring(value.length() - (maxLength - 3));
        }
    }
}

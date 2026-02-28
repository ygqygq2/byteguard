package io.github.ygqygq2.byteguard.cli.command;

import io.github.ygqygq2.byteguard.core.encryptor.JarEncryptor;
import io.github.ygqygq2.byteguard.core.model.EncryptionConfig;
import io.github.ygqygq2.byteguard.core.model.EncryptionMetadata;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
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
        List<String> packages = new ArrayList<>();
        List<String> excludes = new ArrayList<>();

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

        if (inputJar == null || outputJar == null || password == null) {
            throw new IllegalArgumentException(
                "Missing required arguments: --input, --output, --password\n" +
                "Usage: encrypt --input <jar> --output <jar> --password <pwd> [--packages <pkg1,pkg2>] [--exclude <pkg1,pkg2>]"
            );
        }

        File input = new File(inputJar);
        if (!input.exists()) {
            throw new FileNotFoundException("Input JAR not found: " + inputJar);
        }

        System.out.println("Input:    " + inputJar);
        System.out.println("Output:   " + outputJar);
        System.out.println("Password: ****");
        if (!packages.isEmpty()) {
            System.out.println("Packages: " + String.join(", ", packages));
        }
        if (!excludes.isEmpty()) {
            System.out.println("Excludes: " + String.join(", ", excludes));
        }

        // 构建加密配置
        EncryptionConfig.Builder configBuilder = EncryptionConfig.builder();
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

        System.out.println();
        System.out.println("Encrypting...");
        EncryptionMetadata metadata = encryptor.encrypt(inputPath, outputPath);

        System.out.println();
        System.out.println("✓ Encryption completed!");
        System.out.println("  - Classes encrypted: " + metadata.getTotalClasses());
        System.out.println("  - Algorithm:         " + metadata.getAlgorithm());
        System.out.println("  - Key derivation:    " + metadata.getKeyDerivation());
        System.out.println("  - Output:            " + outputJar);
    }
}

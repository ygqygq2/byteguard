package io.github.ygqygq2.byteguard.cli.command;

import io.github.ygqygq2.byteguard.core.analyzer.ClassInfo;
import io.github.ygqygq2.byteguard.core.analyzer.JarAnalyzer;
import io.github.ygqygq2.byteguard.core.loader.MetadataReader;
import io.github.ygqygq2.byteguard.core.model.EncryptionConfig;
import io.github.ygqygq2.byteguard.core.model.EncryptionMetadata;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Analyze 命令 - 分析 JAR，展示哪些类会被加密
 *
 * <p>支持两种模式：
 * <ul>
 *   <li>普通 JAR：按包过滤规则预测哪些类将被加密</li>
 *   <li>已加密 JAR：读取元数据，展示已加密类的详情</li>
 * </ul>
 *
 * @author ygqygq2
 */
public class AnalyzeCommand {

    public void execute(String[] args) throws Exception {
        System.out.println("[ByteGuard] Analyze JAR");

        String inputJar = null;
        List<String> packages = new ArrayList<>();
        List<String> excludes = new ArrayList<>();
        boolean verbose = false;
        boolean showAll = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--jar":
                case "-j":
                    inputJar = args[++i];
                    break;
                case "--packages":
                    for (String p : args[++i].split(",")) {
                        String t = p.trim();
                        if (!t.isEmpty()) packages.add(t);
                    }
                    break;
                case "--exclude":
                    for (String e : args[++i].split(",")) {
                        String t = e.trim();
                        if (!t.isEmpty()) excludes.add(t);
                    }
                    break;
                case "--verbose":
                case "-v":
                    verbose = true;
                    break;
                case "--all":
                    showAll = true;
                    break;
                case "--help":
                case "-h":
                    printUsage();
                    return;
            }
        }

        if (inputJar == null) {
            System.err.println("Error: --jar is required");
            printUsage();
            System.exit(1);
        }

        Path jarPath = Paths.get(inputJar);
        if (!Files.exists(jarPath)) {
            System.err.println("Error: JAR not found: " + jarPath.toAbsolutePath());
            System.exit(1);
        }

        System.out.println("  JAR: " + jarPath.toAbsolutePath());
        System.out.println();

        // 检测是否为已加密 JAR
        boolean isEncrypted = isEncryptedJar(jarPath);
        if (isEncrypted) {
            analyzeEncrypted(jarPath, verbose);
        } else {
            analyzePlain(jarPath, packages, excludes, verbose, showAll);
        }
    }

    /** 检测 JAR 是否已被 ByteGuard 加密 */
    private boolean isEncryptedJar(Path jarPath) throws Exception {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            return jar.getEntry(EncryptionMetadata.METADATA_PATH) != null;
        }
    }

    /** 分析已加密 JAR：读取元数据并展示 */
    private void analyzeEncrypted(Path jarPath, boolean verbose) throws Exception {
        System.out.println("  Mode: Encrypted JAR (ByteGuard metadata detected)");
        System.out.println();

        EncryptionMetadata meta;
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            ZipEntry entry = jar.getEntry(EncryptionMetadata.METADATA_PATH);
            try (InputStream is = jar.getInputStream(entry)) {
                meta = new MetadataReader().read(is);
            }
        }

        System.out.println("  Algorithm    : " + meta.getAlgorithm());
        System.out.println("  Key derivation: " + meta.getKeyDerivation());
        System.out.println("  Encrypted at : " + Instant.ofEpochMilli(meta.getEncryptedAt()));
        System.out.println("  Total classes: " + meta.getTotalClasses());
        System.out.println();

        if (verbose && meta.getClasses() != null) {
            System.out.println("  Encrypted classes:");
            meta.getClasses().forEach((cls, size) ->
                System.out.printf("    %-60s %6d bytes%n", cls, size));
            System.out.println();
        }

        System.out.println("✓ Analysis complete: " + meta.getTotalClasses() + " encrypted class(es)");
    }

    /** 分析普通 JAR：模拟加密过滤，展示哪些类会被加密 */
    private void analyzePlain(Path jarPath, List<String> packages, List<String> excludes,
                               boolean verbose, boolean showAll) throws Exception {
        System.out.println("  Mode: Plain JAR (simulating encryption filter)");
        if (!packages.isEmpty()) {
            System.out.println("  Include packages: " + String.join(", ", packages));
        }
        if (!excludes.isEmpty()) {
            System.out.println("  Exclude packages: " + String.join(", ", excludes));
        }
        System.out.println();

        JarAnalyzer analyzer = new JarAnalyzer();
        JarAnalyzer.JarAnalysisReport report = analyzer.analyze(jarPath);

        // 应用过滤规则（与 JarEncryptor 保持一致）
        EncryptionConfig.Builder configBuilder = EncryptionConfig.builder();
        for (String p : packages) configBuilder.includePackage(p);
        for (String e : excludes) configBuilder.excludePackage(e);
        EncryptionConfig config = configBuilder.build();

        List<ClassInfo> toEncrypt = new ArrayList<>();
        List<ClassInfo> toSkip = new ArrayList<>();

        for (ClassInfo ci : report.getClasses()) {
            // className 形如 com/example/Main.class，去掉 .class 后与 shouldEncrypt 一致
            String classPath = ci.getClassName();
            String classPathNoExt = classPath.endsWith(".class")
                    ? classPath.substring(0, classPath.length() - 6)
                    : classPath;
            if (config.shouldEncrypt(classPathNoExt)) {
                toEncrypt.add(ci);
            } else {
                toSkip.add(ci);
            }
        }

        // 统计特殊类型
        long lambdas = toEncrypt.stream().filter(ClassInfo::isLambdaClass).count();
        long inners  = toEncrypt.stream().filter(ClassInfo::isInnerClass).count();
        long anons   = toEncrypt.stream().filter(ClassInfo::isAnonymousClass).count();

        System.out.printf("  Total classes  : %d%n", report.getTotalClasses());
        System.out.printf("  Will encrypt   : %d%n", toEncrypt.size());
        System.out.printf("    └─ lambdas   : %d%n", lambdas);
        System.out.printf("    └─ inner     : %d%n", inners);
        System.out.printf("    └─ anonymous : %d%n", anons);
        System.out.printf("  Will skip      : %d%n", toSkip.size());

        if (!report.getErrors().isEmpty()) {
            System.out.printf("  Parse errors   : %d%n", report.getErrors().size());
        }
        System.out.println();

        if (verbose && !toEncrypt.isEmpty()) {
            System.out.println("  Classes to encrypt:");
            for (ClassInfo ci : toEncrypt) {
                String flag = ci.isLambdaClass() ? " [lambda]"
                            : ci.isInnerClass()  ? " [inner]"
                            : ci.isAnonymousClass() ? " [anon]"
                            : "";
                System.out.println("    + " + ci.getClassName() + flag);
            }
            System.out.println();
        }

        if (showAll && !toSkip.isEmpty()) {
            System.out.println("  Classes to skip:");
            for (ClassInfo ci : toSkip) {
                System.out.println("    - " + ci.getClassName());
            }
            System.out.println();
        }

        System.out.println("✓ Analysis complete");
    }

    private void printUsage() {
        System.out.println("Usage: byteguard analyze [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --jar,      -j <path>     JAR file to analyze (required)");
        System.out.println("  --packages     <pkgs>     Comma-sep package whitelist (plain JAR mode)");
        System.out.println("  --exclude      <pkgs>     Comma-sep package blacklist");
        System.out.println("  --verbose,  -v            Show class-level details");
        System.out.println("  --all                     Also list skipped classes");
        System.out.println("  --help,     -h            Show this help");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  byteguard analyze --jar app.jar --verbose");
        System.out.println("  byteguard analyze --jar app.jar --packages com.example --verbose");
        System.out.println("  byteguard analyze --jar app-encrypted.jar   # auto-detects encrypted");
    }
}

package io.github.ygqygq2.byteguard.cli.command;

/**
 * Version 命令 - 显示版本及构建信息
 *
 * @author ygqygq2
 */
public class VersionCommand {

    /** 从 JAR Manifest 读取，fallback 到常量 */
    private static final String VERSION;
    private static final String BUILD_TIME;

    static {
        String v = VersionCommand.class.getPackage().getImplementationVersion();
        VERSION = (v != null && !v.isBlank()) ? v : "1.0.0-SNAPSHOT";

        // Manifest Implementation-Title 里存构建时间（build.gradle.kts 写入）
        String t = VersionCommand.class.getPackage().getImplementationTitle();
        BUILD_TIME = (t != null && !t.isBlank()) ? t : "unknown";
    }

    public void execute(String[] args) {
        boolean json = false;
        for (String arg : args) {
            if ("--json".equals(arg)) {
                json = true;
            } else if ("--help".equals(arg) || "-h".equals(arg)) {
                printUsage();
                return;
            }
        }

        if (json) {
            printJson();
        } else {
            printHuman();
        }
    }

    private void printHuman() {
        System.out.println("ByteGuard - Java Bytecode Encryption Tool");
        System.out.println();
        System.out.println("  Version   : " + VERSION);
        System.out.println("  Build time: " + BUILD_TIME);
        System.out.println("  Java      : " + System.getProperty("java.version")
                + " (" + System.getProperty("java.vendor") + ")");
        System.out.println("  OS        : " + System.getProperty("os.name")
                + " " + System.getProperty("os.arch"));
        System.out.println();
        System.out.println("  Algorithm : AES-256-GCM");
        System.out.println("  Key derive: PBKDF2-HmacSHA256 + HKDF");
        System.out.println("  License   : Apache 2.0");
        System.out.println("  Homepage  : https://github.com/ygqygq2/byteguard");
    }

    private void printJson() {
        System.out.println("{");
        System.out.println("  \"version\": \"" + VERSION + "\",");
        System.out.println("  \"buildTime\": \"" + BUILD_TIME + "\",");
        System.out.println("  \"java\": \"" + System.getProperty("java.version") + "\",");
        System.out.println("  \"algorithm\": \"AES-256-GCM\",");
        System.out.println("  \"keyDerivation\": \"PBKDF2-HKDF\"");
        System.out.println("}");
    }

    private void printUsage() {
        System.out.println("Usage: byteguard version [--json]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --json   Output as JSON");
        System.out.println("  --help   Show this help");
    }
}

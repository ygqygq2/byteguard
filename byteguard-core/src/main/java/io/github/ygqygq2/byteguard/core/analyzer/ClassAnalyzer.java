package io.github.ygqygq2.byteguard.core.analyzer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 类文件分析器
 *
 * <p>通过解析 .class 文件头和类名模式，检测类的类型（内部类、Lambda、Record 等），
 * 所有检测均基于纯 JDK，无外部依赖。
 *
 * @author ygqygq2
 */
public class ClassAnalyzer {

    /** Java .class 文件魔数 */
    private static final int MAGIC = 0xCAFEBABE;

    /** record 添加于 Java 16，major version = 60 */
    private static final int JAVA_16_MAJOR = 60;

    /**
     * 分析字节数组形式的 .class 文件
     *
     * @param className  类的全限定名（如 {@code com.example.Main}）或 JAR 内路径
     * @param classBytes .class 字节码
     * @return 分析结果
     * @throws IOException 字节码格式不合法
     */
    public ClassInfo analyze(String className, byte[] classBytes) throws IOException {
        if (classBytes == null || classBytes.length < 8) {
            throw new IOException("Invalid class file: too short");
        }

        int magic = readInt(classBytes, 0);
        if (magic != MAGIC) {
            throw new IOException("Invalid class file: wrong magic (0x" + Integer.toHexString(magic) + ")");
        }

        int majorVersion = readUnsignedShort(classBytes, 6);
        return buildClassInfo(className, classBytes, majorVersion);
    }

    /**
     * 分析本地 .class 文件
     */
    public ClassInfo analyzeFile(String className, Path classFile) throws IOException {
        byte[] bytes = Files.readAllBytes(classFile);
        return analyze(className, bytes);
    }

    /**
     * 分析来自 InputStream 的 .class 文件
     */
    public ClassInfo analyzeStream(String className, InputStream is) throws IOException {
        byte[] bytes = is.readAllBytes();
        return analyze(className, bytes);
    }

    // ---- private ----

    private ClassInfo buildClassInfo(String className, byte[] classBytes, int majorVersion) {
        // 规范化（路径 → 类名风格）
        String simpleName = className.replace('/', '.').replace('\\', '.');
        if (simpleName.endsWith(".class")) {
            simpleName = simpleName.substring(0, simpleName.length() - 6);
        }
        // 取最后一段（包含 $ 的 simple name 部分）
        String shortName = simpleName.contains(".")
            ? simpleName.substring(simpleName.lastIndexOf('.') + 1)
            : simpleName;

        boolean lambdaClass = shortName.contains("$$Lambda") || shortName.contains("$$");
        boolean anonymousClass = !lambdaClass && isAnonymous(shortName);
        boolean innerClass = !lambdaClass && !anonymousClass && shortName.contains("$");
        // record：Java 16+ 且 ACC_RECORD flag (0x1000)。需解析 access_flags。
        boolean recordClass = majorVersion >= JAVA_16_MAJOR && hasRecordFlag(classBytes);

        return ClassInfo.builder(simpleName)
            .majorVersion(majorVersion)
            .lambdaClass(lambdaClass)
            .anonymousClass(anonymousClass)
            .innerClass(innerClass)
            .recordClass(recordClass)
            .build();
    }

    /** 匿名类特征：简名中 '$' 后跟一个或多个纯数字 */
    private boolean isAnonymous(String shortName) {
        int dollar = shortName.lastIndexOf('$');
        if (dollar < 0 || dollar + 1 >= shortName.length()) return false;
        String suffix = shortName.substring(dollar + 1);
        return suffix.matches("\\d+");
    }

    /**
     * 尝试解析 access_flags 来判断是否有 ACC_RECORD (0x1000)。
     * 由于 constant_pool 是变长的，直接跳过 CP 解析。
     * 返回保守值（如解析失败返回 false）。
     */
    private boolean hasRecordFlag(byte[] bytes) {
        try {
            int cpCount = readUnsignedShort(bytes, 8) - 1; // constant_pool_count - 1
            int pos = 10; // 从第一个 CP 条目开始

            for (int i = 0; i < cpCount; i++) {
                int tag = bytes[pos] & 0xFF;
                pos++;
                pos = skipCpEntry(bytes, pos, tag);
                if (pos < 0) return false;
                // tags 5 (Long) 和 6 (Double) 占两个槽
                if (tag == 5 || tag == 6) i++;
            }

            // access_flags 位于 CP 之后
            int accessFlags = readUnsignedShort(bytes, pos);
            return (accessFlags & 0x1000) != 0; // ACC_RECORD
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    /** 跳过一个 CP 条目，返回下一个条目起始位置；出错返回 -1 */
    private int skipCpEntry(byte[] bytes, int pos, int tag) {
        return switch (tag) {
            case 1  -> pos + 2 + readUnsignedShort(bytes, pos); // Utf8
            case 3, 4, 9, 10, 11, 12, 18 -> pos + 4;           // Integer/Float/Fieldref/…/InvokeDynamic
            case 5, 6  -> pos + 8;                              // Long/Double
            case 7, 8, 16, 19, 20 -> pos + 2;                  // Class/String/MethodType/…
            case 15 -> pos + 3;                                  // MethodHandle
            case 17 -> pos + 4;                                  // Dynamic
            default -> -1;
        };
    }

    private int readInt(byte[] bytes, int pos) {
        return ((bytes[pos] & 0xFF) << 24)
             | ((bytes[pos + 1] & 0xFF) << 16)
             | ((bytes[pos + 2] & 0xFF) << 8)
             |  (bytes[pos + 3] & 0xFF);
    }

    private int readUnsignedShort(byte[] bytes, int pos) {
        return ((bytes[pos] & 0xFF) << 8) | (bytes[pos + 1] & 0xFF);
    }
}

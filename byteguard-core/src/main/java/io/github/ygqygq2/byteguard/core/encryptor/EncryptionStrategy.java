package io.github.ygqygq2.byteguard.core.encryptor;

import io.github.ygqygq2.byteguard.core.model.EncryptionConfig;

/**
 * 加密策略：决定哪些类需要被加密
 *
 * <p>默认实现基于 {@link EncryptionConfig} 的包名过滤规则。
 * 可通过实现此接口提供自定义策略。
 *
 * @author ygqygq2
 */
@FunctionalInterface
public interface EncryptionStrategy {

    /**
     * 判断指定类（JAR 内路径）是否应被加密
     *
     * @param classPath JAR 内路径，如 {@code com/example/Main.class}
     * @return true 表示需要加密
     */
    boolean shouldEncrypt(String classPath);

    /**
     * 基于 {@link EncryptionConfig} 创建默认策略
     *
     * @param config 加密配置
     * @return 过滤策略
     */
    static EncryptionStrategy fromConfig(EncryptionConfig config) {
        return classPath -> {
            // 只处理 .class 文件
            if (!classPath.endsWith(".class")) return false;
            // 忽略 module-info
            if (classPath.equals("module-info.class") || classPath.endsWith("/module-info.class")) return false;
            // 转换路径为类名（com/example/Main.class → com/example/Main）供 config 判断
            String className = classPath.substring(0, classPath.length() - 6); // strip .class
            return config.shouldEncrypt(className);
        };
    }
}

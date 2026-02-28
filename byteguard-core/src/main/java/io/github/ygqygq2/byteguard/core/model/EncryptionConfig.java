package io.github.ygqygq2.byteguard.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 加密配置模型
 *
 * <p>控制哪些包/类需要被加密，以及加密时的行为参数。
 *
 * @author ygqygq2
 */
public class EncryptionConfig {

    /** 需要加密的包名前缀列表（为空则加密所有） */
    private final List<String> includePackages;

    /** 排除加密的包名前缀列表 */
    private final List<String> excludePackages;

    /** 并发线程数（默认 CPU 核心数） */
    private final int threads;

    /** 是否加密 Lambda 合成类（默认 true） */
    private final boolean encryptLambdas;

    private EncryptionConfig(Builder builder) {
        this.includePackages = Collections.unmodifiableList(new ArrayList<>(builder.includePackages));
        this.excludePackages = Collections.unmodifiableList(new ArrayList<>(builder.excludePackages));
        this.threads = builder.threads;
        this.encryptLambdas = builder.encryptLambdas;
    }

    /** 判断指定类是否应被加密（基于包名过滤规则） */
    public boolean shouldEncrypt(String className) {
        if (className == null) return false;

        // 系统类永不加密
        if (isSystemClass(className)) return false;

        // 黑名单优先
        for (String exclude : excludePackages) {
            if (className.startsWith(exclude)) return false;
        }

        // 白名单：为空则全部包含
        if (includePackages.isEmpty()) return true;
        for (String include : includePackages) {
            if (className.startsWith(include)) return true;
        }
        return false;
    }

    private boolean isSystemClass(String className) {
        return className.startsWith("java.")
            || className.startsWith("javax.")
            || className.startsWith("sun.")
            || className.startsWith("com.sun.")
            || className.startsWith("jdk.")
            || className.startsWith("module-info");
    }

    public List<String> getIncludePackages() { return includePackages; }
    public List<String> getExcludePackages() { return excludePackages; }
    public int getThreads() { return threads; }
    public boolean isEncryptLambdas() { return encryptLambdas; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<String> includePackages = new ArrayList<>();
        private final List<String> excludePackages = new ArrayList<>();
        private int threads = Runtime.getRuntime().availableProcessors();
        private boolean encryptLambdas = true;

        public Builder includePackage(String pkg) {
            includePackages.add(pkg.replace('.', '/'));
            return this;
        }

        public Builder excludePackage(String pkg) {
            excludePackages.add(pkg.replace('.', '/'));
            return this;
        }

        public Builder threads(int threads) {
            if (threads < 1) throw new IllegalArgumentException("threads must be >= 1");
            this.threads = threads;
            return this;
        }

        public Builder encryptLambdas(boolean encryptLambdas) {
            this.encryptLambdas = encryptLambdas;
            return this;
        }

        public EncryptionConfig build() {
            return new EncryptionConfig(this);
        }
    }
}

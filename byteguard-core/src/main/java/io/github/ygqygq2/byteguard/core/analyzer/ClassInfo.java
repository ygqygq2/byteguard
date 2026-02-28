package io.github.ygqygq2.byteguard.core.analyzer;

/**
 * 类文件分析结果
 *
 * @author ygqygq2
 */
public class ClassInfo {

    private final String className;
    private final boolean innerClass;
    private final boolean anonymousClass;
    private final boolean lambdaClass;
    private final boolean recordClass;
    private final int majorVersion;

    private ClassInfo(Builder builder) {
        this.className = builder.className;
        this.innerClass = builder.innerClass;
        this.anonymousClass = builder.anonymousClass;
        this.lambdaClass = builder.lambdaClass;
        this.recordClass = builder.recordClass;
        this.majorVersion = builder.majorVersion;
    }

    /** 对应的 Java 版本（如 majorVersion=61 → Java 17） */
    public int getJavaVersion() {
        return majorVersion - 44;
    }

    public String getClassName() { return className; }
    public boolean isInnerClass() { return innerClass; }
    public boolean isAnonymousClass() { return anonymousClass; }
    public boolean isLambdaClass() { return lambdaClass; }
    public boolean isRecordClass() { return recordClass; }
    public int getMajorVersion() { return majorVersion; }

    @Override
    public String toString() {
        return String.format("ClassInfo{name='%s', inner=%b, anonymous=%b, lambda=%b, record=%b, java=%d}",
            className, innerClass, anonymousClass, lambdaClass, recordClass, getJavaVersion());
    }

    static Builder builder(String className) {
        return new Builder(className);
    }

    static class Builder {
        final String className;
        boolean innerClass;
        boolean anonymousClass;
        boolean lambdaClass;
        boolean recordClass;
        int majorVersion;

        Builder(String className) { this.className = className; }
        Builder innerClass(boolean v) { innerClass = v; return this; }
        Builder anonymousClass(boolean v) { anonymousClass = v; return this; }
        Builder lambdaClass(boolean v) { lambdaClass = v; return this; }
        Builder recordClass(boolean v) { recordClass = v; return this; }
        Builder majorVersion(int v) { majorVersion = v; return this; }
        ClassInfo build() { return new ClassInfo(this); }
    }
}

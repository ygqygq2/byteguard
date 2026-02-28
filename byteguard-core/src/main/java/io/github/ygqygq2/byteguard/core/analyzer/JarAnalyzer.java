package io.github.ygqygq2.byteguard.core.analyzer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * JAR 文件分析器
 *
 * <p>统计 JAR 内所有类的信息，生成 {@link JarAnalysisReport}。
 *
 * @author ygqygq2
 */
public class JarAnalyzer {

    private final ClassAnalyzer classAnalyzer;

    public JarAnalyzer() {
        this.classAnalyzer = new ClassAnalyzer();
    }

    /**
     * 分析指定 JAR 文件
     *
     * @param jarPath JAR 路径
     * @return 分析报告
     * @throws IOException 读取失败
     */
    public JarAnalysisReport analyze(Path jarPath) throws IOException {
        List<ClassInfo> classes = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        String className = entry.getName();
                        ClassInfo info = classAnalyzer.analyzeStream(className, is);
                        classes.add(info);
                    } catch (IOException e) {
                        errors.add(entry.getName() + ": " + e.getMessage());
                    }
                }
            }
        }

        return new JarAnalysisReport(jarPath.getFileName().toString(), classes, errors);
    }

    /**
     * JAR 分析报告
     */
    public static class JarAnalysisReport {
        private final String jarName;
        private final List<ClassInfo> classes;
        private final List<String> errors;

        JarAnalysisReport(String jarName, List<ClassInfo> classes, List<String> errors) {
            this.jarName = jarName;
            this.classes = Collections.unmodifiableList(classes);
            this.errors = Collections.unmodifiableList(errors);
        }

        public String getJarName() { return jarName; }
        public List<ClassInfo> getClasses() { return classes; }
        public List<String> getErrors() { return errors; }
        public int getTotalClasses() { return classes.size(); }
        public long getLambdaCount() { return classes.stream().filter(ClassInfo::isLambdaClass).count(); }
        public long getInnerClassCount() { return classes.stream().filter(ClassInfo::isInnerClass).count(); }
        public long getAnonymousClassCount() { return classes.stream().filter(ClassInfo::isAnonymousClass).count(); }
        public long getRecordCount() { return classes.stream().filter(ClassInfo::isRecordClass).count(); }

        @Override
        public String toString() {
            return String.format(
                "JarAnalysisReport{jar='%s', total=%d, lambda=%d, inner=%d, anonymous=%d, record=%d, errors=%d}",
                jarName, getTotalClasses(), getLambdaCount(), getInnerClassCount(),
                getAnonymousClassCount(), getRecordCount(), errors.size());
        }
    }
}

package io.github.ygqygq2.byteguard.core.encryptor;

import io.github.ygqygq2.byteguard.core.crypto.CryptoException;
import io.github.ygqygq2.byteguard.core.crypto.KeyDerivation;
import io.github.ygqygq2.byteguard.core.crypto.SaltGenerator;
import io.github.ygqygq2.byteguard.core.model.EncryptedClass;
import io.github.ygqygq2.byteguard.core.model.EncryptionConfig;
import io.github.ygqygq2.byteguard.core.model.EncryptionMetadata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * JAR 文件批量加密器
 *
 * <p>加密流程：
 * <ol>
 *   <li>生成随机盐，PBKDF2 派生主密钥
 *   <li>遍历 JAR，对符合策略的 {@code .class} 文件并发加密
 *   <li>加密后的字节码写到<strong>原始类路径</strong>（替换原始内容）
 *   <li>写入 {@code META-INF/.byteguard/metadata.json}
 *   <li>所有资源文件原样保留
 * </ol>
 *
 * <p>加密后的 JAR 需要配合 {@code ByteGuardAgent} 通过 JavaAgent 机制运行：
 * <pre>{@code
 * java -javaagent:byteguard.jar=password=<pwd> -jar app-encrypted.jar
 * }</pre>
 *
 * @author ygqygq2
 */
public class JarEncryptor {

    private final String password;
    private final EncryptionConfig config;
    private final EncryptionStrategy strategy;
    private final SaltGenerator saltGenerator;
    private final KeyDerivation keyDerivation;
    private final MetadataWriter metadataWriter;
    private final MetadataIntegrity metadataIntegrity;

    /**
     * 使用默认配置（加密所有类）
     *
     * @param password 用户密码
     */
    public JarEncryptor(String password) {
        this(password, EncryptionConfig.builder().build());
    }

    /**
     * @param password 用户密码
     * @param config   加密配置（包名过滤、线程数等）
     */
    public JarEncryptor(String password, EncryptionConfig config) {
        this.password = password;
        this.config = config;
        this.strategy = EncryptionStrategy.fromConfig(config);
        this.saltGenerator = new SaltGenerator();
        this.keyDerivation = new KeyDerivation();
        this.metadataWriter = new MetadataWriter();
        this.metadataIntegrity = new MetadataIntegrity();
    }

    /**
     * 加密 JAR 文件
     *
     * @param inputJar  原始 JAR 路径
     * @param outputJar 输出加密 JAR 路径
     * @return 加密元数据（包含统计信息）
     * @throws IOException     IO 错误
     * @throws CryptoException 加密错误
     */
    public EncryptionMetadata encrypt(Path inputJar, Path outputJar) throws IOException, CryptoException {
        return encrypt(inputJar, outputJar, ProgressListener.noOp());
    }

    /**
     * 加密 JAR 文件，并通过监听器上报进度。
     *
     * @param inputJar  原始 JAR 路径
     * @param outputJar 输出加密 JAR 路径
     * @param listener  进度监听器（可为 {@code null}）
     * @return 加密元数据（包含统计信息）
     * @throws IOException     IO 错误
     * @throws CryptoException 加密错误
     */
    public EncryptionMetadata encrypt(Path inputJar, Path outputJar, ProgressListener listener)
            throws IOException, CryptoException {
        ProgressListener progressListener = listener != null ? listener : ProgressListener.noOp();
        byte[] salt = saltGenerator.generate();
        byte[] masterKey = keyDerivation.deriveMasterKey(password, salt);
        EncryptionMetadata metadata = new EncryptionMetadata(salt);

        ClassEncryptor classEncryptor = new ClassEncryptor(masterKey);

        try (JarFile jarFile = new JarFile(inputJar.toFile());
             JarOutputStream jos = new JarOutputStream(
                 java.nio.file.Files.newOutputStream(outputJar),
                 getManifest(jarFile))) {

            // 收集需要加密的条目
            List<JarEntry> toEncrypt = new ArrayList<>();
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && strategy.shouldEncrypt(entry.getName())) {
                    toEncrypt.add(entry);
                } else if (!entry.getName().equals("META-INF/MANIFEST.MF")) {
                    // 非加密条目原样复制（跳过 MANIFEST.MF，已在构造函数中处理）
                    copyEntry(jarFile, entry, jos);
                }
            }

            progressListener.onStart(toEncrypt.size());

            // 并发加密
            int threads = config.getThreads();
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CompletionService<EncryptionResult> completionService = new ExecutorCompletionService<>(executor);

            for (JarEntry entry : toEncrypt) {
                byte[] classBytes = readEntry(jarFile, entry);
                String classPath = entry.getName();
                completionService.submit(() -> {
                    String className = classPath.substring(0, classPath.length() - 6).replace('/', '.');
                    EncryptedClass ec = classEncryptor.encrypt(className, classBytes);
                    return new EncryptionResult(classPath, ec.getEncryptedBytes());
                });
            }

            executor.shutdown();

            // 写入加密条目（加密字节写到原始类路径，替换原始内容）
            for (int completed = 1; completed <= toEncrypt.size(); completed++) {
                Future<EncryptionResult> future;
                try {
                    future = completionService.take();
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                    throw new CryptoException("Encryption interrupted", e);
                }

                EncryptionResult result;
                try {
                    result = future.get();
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                    throw new CryptoException("Encryption interrupted", e);
                } catch (ExecutionException e) {
                    executor.shutdownNow();
                    Throwable cause = e.getCause();
                    if (cause instanceof CryptoException ce) throw ce;
                    throw new CryptoException("Encryption failed: " + cause.getMessage(), cause);
                }

                // 写到原始类路径（替换原始字节码）
                JarEntry encEntry = new JarEntry(result.classPath);
                jos.putNextEntry(encEntry);
                jos.write(result.encryptedBytes);
                jos.closeEntry();

                metadata.addClass(result.classPath, result.encryptedBytes.length);
                progressListener.onProgress(result.classPath, completed, toEncrypt.size());
            }

            // 写入元数据
            metadata.setMetadataMac(metadataIntegrity.computeMac(metadata, masterKey));
            String metadataJson = metadataWriter.toJson(metadata);
            JarEntry metaEntry = new JarEntry(EncryptionMetadata.METADATA_PATH);
            jos.putNextEntry(metaEntry);
            jos.write(metadataJson.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();

            progressListener.onFinish(metadata);
        }

        return metadata;
    }

    private Manifest getManifest(JarFile jarFile) throws IOException {
        Manifest manifest = jarFile.getManifest();
        return manifest != null ? manifest : new Manifest();
    }

    private void copyEntry(JarFile jarFile, JarEntry entry, JarOutputStream jos) throws IOException {
        jos.putNextEntry(new JarEntry(entry.getName()));
        try (InputStream is = jarFile.getInputStream(entry)) {
            is.transferTo(jos);
        }
        jos.closeEntry();
    }

    private byte[] readEntry(JarFile jarFile, JarEntry entry) throws IOException {
        try (InputStream is = jarFile.getInputStream(entry)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            is.transferTo(baos);
            return baos.toByteArray();
        }
    }

    /** 内部结果载体 */
    private record EncryptionResult(String classPath, byte[] encryptedBytes) {}

    /**
     * 加密进度监听器。
     */
    public interface ProgressListener {
        void onStart(int totalClasses);

        void onProgress(String classPath, int completed, int totalClasses);

        void onFinish(EncryptionMetadata metadata);

        static ProgressListener noOp() {
            return new ProgressListener() {
                @Override
                public void onStart(int totalClasses) {
                    // no-op
                }

                @Override
                public void onProgress(String classPath, int completed, int totalClasses) {
                    // no-op
                }

                @Override
                public void onFinish(EncryptionMetadata metadata) {
                    // no-op
                }
            };
        }
    }
}

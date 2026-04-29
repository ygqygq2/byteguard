package io.github.ygqygq2.byteguard.core.encryptor;

import io.github.ygqygq2.byteguard.core.model.EncryptionMetadata;
import io.github.ygqygq2.byteguard.core.util.Base64Utils;

import java.util.Map;

/**
 * 加密元数据序列化器
 *
 * <p>将 {@link EncryptionMetadata} 序列化为 JSON 字符串，写入 JAR 的
 * {@code META-INF/byteguard-metadata.json}。使用纯 JDK 实现，无外部依赖。
 *
 * @author ygqygq2
 */
public class MetadataWriter {

    /**
     * 序列化为 JSON 字符串
     *
     * @param metadata 元数据
     * @return JSON 字符串（UTF-8）
     */
    public String toJson(EncryptionMetadata metadata) {
        return buildJson(metadata, true);
    }

    /**
     * 生成不含 metadataMac 字段的规范化 JSON，供完整性校验使用。
     */
    public String toCanonicalJson(EncryptionMetadata metadata) {
        return buildJson(metadata, false);
    }

    private String buildJson(EncryptionMetadata metadata, boolean includeMac) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        appendField(sb, "version", metadata.getVersion(), true);
        appendField(sb, "algorithm", metadata.getAlgorithm(), true);
        appendField(sb, "keyDerivation", metadata.getKeyDerivation(), true);
        appendField(sb, "salt", Base64Utils.encode(metadata.getSalt()), true);
        if (includeMac && metadata.getMetadataMac() != null) {
            appendField(sb, "metadataMac", Base64Utils.encode(metadata.getMetadataMac()), true);
        }
        appendLongField(sb, "encryptedAt", metadata.getEncryptedAt());
        appendIntField(sb, "totalClasses", metadata.getTotalClasses());
        appendClassesField(sb, metadata.getClasses());
        sb.append("}\n");
        return sb.toString();
    }

    // ---- private helpers ----

    private void appendField(StringBuilder sb, String key, String value, boolean comma) {
        sb.append("  \"").append(key).append("\": ");
        if (value == null) {
            sb.append("null");
        } else {
            sb.append("\"").append(escapeJson(value)).append("\"");
        }
        if (comma) sb.append(",");
        sb.append("\n");
    }

    private void appendLongField(StringBuilder sb, String key, long value) {
        sb.append("  \"").append(key).append("\": ").append(value).append(",\n");
    }

    private void appendIntField(StringBuilder sb, String key, int value) {
        sb.append("  \"").append(key).append("\": ").append(value).append(",\n");
    }

    private void appendClassesField(StringBuilder sb, Map<String, Integer> classes) {
        sb.append("  \"classes\": {\n");
        if (classes != null && !classes.isEmpty()) {
            int i = 0;
            int size = classes.size();
            for (Map.Entry<String, Integer> entry : classes.entrySet()) {
                sb.append("    \"").append(escapeJson(entry.getKey())).append("\": ")
                  .append(entry.getValue());
                if (++i < size) sb.append(",");
                sb.append("\n");
            }
        }
        sb.append("  }\n");
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

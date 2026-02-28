package io.github.ygqygq2.byteguard.core.loader;

import io.github.ygqygq2.byteguard.core.model.EncryptionMetadata;
import io.github.ygqygq2.byteguard.core.util.Base64Utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 加密元数据反序列化器
 *
 * <p>从 JAR 的 {@code META-INF/byteguard-metadata.json} 读取元数据，
 * 无外部 JSON 依赖，手写轻量级解析器。
 *
 * @author ygqygq2
 */
public class MetadataReader {

    /**
     * 从 InputStream 读取并解析元数据
     *
     * @param inputStream JSON 输入流
     * @return 解析后的元数据
     * @throws IOException 读取或解析失败
     */
    public EncryptionMetadata read(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        String json = new String(bytes, StandardCharsets.UTF_8);
        return parse(json);
    }

    /**
     * 从 JSON 字符串解析元数据
     */
    public EncryptionMetadata parse(String json) throws IOException {
        EncryptionMetadata metadata = new EncryptionMetadata();

        metadata.setVersion(extractString(json, "version"));
        metadata.setAlgorithm(extractString(json, "algorithm"));
        metadata.setKeyDerivation(extractString(json, "keyDerivation"));

        String saltB64 = extractString(json, "salt");
        if (saltB64 != null) {
            metadata.setSalt(Base64Utils.decode(saltB64));
        }

        String encryptedAt = extractScalar(json, "encryptedAt");
        if (encryptedAt != null) {
            metadata.setEncryptedAt(Long.parseLong(encryptedAt.trim()));
        }

        String totalClasses = extractScalar(json, "totalClasses");
        if (totalClasses != null) {
            metadata.setTotalClasses(Integer.parseInt(totalClasses.trim()));
        }

        Map<String, Integer> classes = extractClassesMap(json);
        if (classes != null) {
            metadata.setClasses(classes);
        }

        return metadata;
    }

    // ---- simple regex-free parsers ----

    /** 提取 JSON 字符串字段值 */
    private String extractString(String json, String key) {
        String pattern = "\"" + key + "\"";
        int keyIdx = json.indexOf(pattern);
        if (keyIdx < 0) return null;
        int colon = json.indexOf(':', keyIdx + pattern.length());
        if (colon < 0) return null;
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return null;
        int end = indexOfUnescapedQuote(json, start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    /** 提取 JSON 数值/布尔字段（非字符串） */
    private String extractScalar(String json, String key) {
        String pattern = "\"" + key + "\"";
        int keyIdx = json.indexOf(pattern);
        if (keyIdx < 0) return null;
        int colon = json.indexOf(':', keyIdx + pattern.length());
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length()) return null;
        int end = start;
        while (end < json.length() && !",}\n\r".contains(String.valueOf(json.charAt(end)))) end++;
        return json.substring(start, end).trim();
    }

    /** 提取 classes 对象（key→int） */
    private Map<String, Integer> extractClassesMap(String json) throws IOException {
        int classesIdx = json.indexOf("\"classes\"");
        if (classesIdx < 0) return null;
        int open = json.indexOf('{', classesIdx);
        if (open < 0) return null;
        int close = matchingBrace(json, open);
        if (close < 0) throw new IOException("Malformed 'classes' object in metadata JSON");

        String block = json.substring(open + 1, close);
        Map<String, Integer> result = new LinkedHashMap<>();
        if (block.isBlank()) return result;

        int pos = 0;
        while (pos < block.length()) {
            int ks = block.indexOf('"', pos);
            if (ks < 0) break;
            int ke = indexOfUnescapedQuote(block, ks + 1);
            if (ke < 0) break;
            String classKey = block.substring(ks + 1, ke);
            int colon = block.indexOf(':', ke + 1);
            if (colon < 0) break;
            int vs = colon + 1;
            while (vs < block.length() && Character.isWhitespace(block.charAt(vs))) vs++;
            int ve = vs;
            while (ve < block.length() && !",}\n\r".contains(String.valueOf(block.charAt(ve)))) ve++;
            String val = block.substring(vs, ve).trim();
            result.put(classKey, Integer.parseInt(val));
            pos = ve + 1;
        }
        return result;
    }

    private int indexOfUnescapedQuote(String s, int from) {
        for (int i = from; i < s.length(); i++) {
            if (s.charAt(i) == '"' && (i == 0 || s.charAt(i - 1) != '\\')) return i;
        }
        return -1;
    }

    private int matchingBrace(String s, int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            if (s.charAt(i) == '{') depth++;
            else if (s.charAt(i) == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }
}

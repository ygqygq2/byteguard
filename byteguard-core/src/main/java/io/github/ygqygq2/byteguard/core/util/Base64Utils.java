package io.github.ygqygq2.byteguard.core.util;

import java.util.Base64;

/**
 * Base64 编解码工具类
 *
 * <p>统一使用 {@link Base64#getUrlEncoder()} 无填充模式，保证 JSON 元数据中 Base64 字段的 URL 安全。
 *
 * @author ygqygq2
 */
public final class Base64Utils {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private Base64Utils() {}

    /**
     * 字节数组 → Base64 字符串
     */
    public static String encode(byte[] data) {
        if (data == null) return null;
        return ENCODER.encodeToString(data);
    }

    /**
     * Base64 字符串 → 字节数组
     */
    public static byte[] decode(String base64) {
        if (base64 == null) return null;
        return DECODER.decode(base64);
    }
}

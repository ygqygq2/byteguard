package io.github.ygqygq2.byteguard.core.license;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * License 序列化/反序列化（简单的 JSON 实现，无外部依赖）
 * 
 * @author ygqygq2
 */
public class LicenseSerializer {
    
    /**
     * 序列化 License 为 JSON
     */
    public String toJson(License license) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"version\": \"").append(escape(license.getVersion())).append("\",\n");
        json.append("  \"licenseId\": \"").append(escape(license.getLicenseId())).append("\",\n");
        json.append("  \"licenseType\": \"").append(license.getLicenseType()).append("\",\n");
        json.append("  \"issuedTo\": \"").append(escape(license.getIssuedTo())).append("\",\n");
        json.append("  \"issuedAt\": \"").append(license.getIssuedAt()).append("\",\n");
        
        if (license.getExpireAt() != null) {
            json.append("  \"expireAt\": \"").append(license.getExpireAt()).append("\",\n");
        } else {
            json.append("  \"expireAt\": null,\n");
        }
        
        if (license.getAuthorization() != null) {
            json.append("  \"authorization\": {\n");
            License.Authorization auth = license.getAuthorization();
            json.append("    \"maxInstances\": ").append(auth.getMaxInstances()).append(",\n");
            json.append("    \"features\": ").append(listToJson(auth.getFeatures())).append(",\n");
            json.append("    \"bindingMode\": \"").append(auth.getBindingMode()).append("\",\n");
            json.append("    \"allowedMachineIds\": ").append(listToJson(auth.getAllowedMachineIds())).append("\n");
            json.append("  },\n");
        }
        
        json.append("  \"signature\": \"").append(escape(license.getSignature())).append("\"\n");
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * 从 JSON 反序列化 License
     */
    public License fromJson(String json) throws LicenseException {
        try {
            License license = new License();
            
            license.setVersion(extractString(json, "version"));
            license.setLicenseId(extractString(json, "licenseId"));
            license.setLicenseType(License.LicenseType.valueOf(extractString(json, "licenseType")));
            license.setIssuedTo(extractString(json, "issuedTo"));
            license.setIssuedAt(Instant.parse(extractString(json, "issuedAt")));
            
            String expireAt = extractString(json, "expireAt");
            if (expireAt != null && !expireAt.equals("null")) {
                license.setExpireAt(Instant.parse(expireAt));
            }
            
            // 解析 authorization
            if (json.contains("\"authorization\"")) {
                License.Authorization auth = new License.Authorization();
                String authBlock = extractObject(json, "authorization");
                
                auth.setMaxInstances(extractInt(authBlock, "maxInstances"));
                auth.setFeatures(extractStringList(authBlock, "features"));
                auth.setBindingMode(License.BindingMode.valueOf(extractString(authBlock, "bindingMode")));
                auth.setAllowedMachineIds(extractStringList(authBlock, "allowedMachineIds"));
                
                license.setAuthorization(auth);
            }
            
            license.setSignature(extractString(json, "signature"));
            
            return license;
            
        } catch (Exception e) {
            throw new LicenseException("Failed to parse license JSON", e);
        }
    }
    
    /**
     * 保存 License 到文件
     */
    public void saveToFile(License license, File file) throws IOException {
        String json = toJson(license);
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            writer.write(json);
        }
    }
    
    /**
     * 从文件加载 License
     */
    public License loadFromFile(File file) throws IOException, LicenseException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return fromJson(content.toString());
    }
    
    // 辅助方法
    
    private String escape(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r");
    }
    
    private String listToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(escape(list.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
    
    private String extractString(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
    
    private int extractInt(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }
    
    private String extractObject(String json, String key) {
        int start = json.indexOf("\"" + key + "\"");
        if (start == -1) return null;
        
        start = json.indexOf("{", start);
        int braceCount = 1;
        int end = start + 1;
        
        while (braceCount > 0 && end < json.length()) {
            char c = json.charAt(end);
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            end++;
        }
        
        return json.substring(start, end);
    }
    
    private List<String> extractStringList(String json, String key) {
        List<String> result = new ArrayList<>();
        String pattern = "\"" + key + "\"\\s*:\\s*\\[([^\\]]*)\\]";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        
        if (m.find()) {
            String content = m.group(1);
            if (!content.trim().isEmpty()) {
                String[] items = content.split(",");
                for (String item : items) {
                    String cleaned = item.trim().replaceAll("^\"|\"$", "");
                    if (!cleaned.isEmpty()) {
                        result.add(cleaned);
                    }
                }
            }
        }
        
        return result;
    }
}

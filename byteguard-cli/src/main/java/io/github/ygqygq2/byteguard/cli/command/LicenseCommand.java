package io.github.ygqygq2.byteguard.cli.command;

import io.github.ygqygq2.byteguard.core.license.License;
import io.github.ygqygq2.byteguard.core.license.LicenseSerializer;
import io.github.ygqygq2.byteguard.core.license.RSASignature;

import java.io.File;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.UUID;

/**
 * License 命令
 * 
 * @author ygqygq2
 */
public class LicenseCommand {
    
    public void execute(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }
        
        String subCommand = args[0];
        
        if ("generate".equals(subCommand)) {
            generateLicense(args);
        } else {
            System.err.println("Unknown subcommand: " + subCommand);
            printUsage();
        }
    }
    
    private void generateLicense(String[] args) throws Exception {
        System.out.println("[ByteGuard] Generate License");
        
        // 解析参数
        String issuedTo = "Unknown";
        String expireDate = null;
        String output = "license.lic";
        License.LicenseType type = License.LicenseType.STANDARD;
        int maxInstances = 0;
        
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--issued-to":
                    issuedTo = args[++i];
                    break;
                case "--expire":
                    expireDate = args[++i];
                    break;
                case "--output":
                    output = args[++i];
                    break;
                case "--type":
                    type = License.LicenseType.valueOf(args[++i].toUpperCase());
                    break;
                case "--max-instances":
                    maxInstances = Integer.parseInt(args[++i]);
                    break;
            }
        }
        
        // 创建 License
        License license = new License();
        license.setLicenseId("LIC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        license.setLicenseType(type);
        license.setIssuedTo(issuedTo);
        license.setIssuedAt(Instant.now());
        
        if (expireDate != null) {
            LocalDate date = LocalDate.parse(expireDate);
            license.setExpireAt(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        
        License.Authorization auth = new License.Authorization();
        auth.setMaxInstances(maxInstances);
        auth.setFeatures(new ArrayList<>());
        auth.getFeatures().add("FULL");
        license.setAuthorization(auth);
        
        // TODO: 生成 RSA 签名
        System.out.println("TODO: Generate RSA signature");
        
        // 生成签名（临时：需要加载私钥）
        RSASignature rsaSignature = new RSASignature();
        KeyPair keyPair = rsaSignature.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        
        String dataToSign = license.toSignatureData();
        String signature = rsaSignature.sign(dataToSign, privateKey);
        license.setSignature(signature);
        
        // 打印信息
        System.out.println("✓ License generated:");
        System.out.println("  - License ID: " + license.getLicenseId());
        System.out.println("  - Type: " + license.getLicenseType());
        System.out.println("  - Issued to: " + license.getIssuedTo());
        System.out.println("  - Expires at: " + license.getExpireAt());
        System.out.println("  - Max instances: " + (maxInstances == 0 ? "Unlimited" : maxInstances));
        System.out.println();
        
        // 保存到文件
        LicenseSerializer serializer = new LicenseSerializer();
        File outputFile = new File(output);
        serializer.saveToFile(license, outputFile);
        
        System.out.println("✓ License saved to: " + outputFile.getAbsolutePath());
    }
    
    private void printUsage() {
        System.out.println("License Command Usage:");
        System.out.println();
        System.out.println("  license generate [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --issued-to <name>     Company or person name");
        System.out.println("  --expire <date>        Expiration date (YYYY-MM-DD)");
        System.out.println("  --type <type>          TRIAL, STANDARD, or ENTERPRISE");
        System.out.println("  --max-instances <n>    Maximum concurrent instances (0=unlimited)");
        System.out.println("  --output <file>        Output file path");
    }
}

# ByteGuard 安全设计文档

## 概述

ByteGuard 采用军事级加密标准保护 Java 字节码，本文档详细说明安全设计原理、威胁模型和最佳实践。

## 密码学设计

### 加密算法栈

```
用户密码 (User Password)
    ↓
PBKDF2-HMAC-SHA256 (100,000 iterations)
    ↓
主密钥 (Master Key, 256-bit)
    ↓
HKDF-SHA256 (类名作为 context)
    ↓
类专用密钥 (Per-Class Key, 256-bit)
    ↓
AES-256-GCM (加密 + 认证)
    ↓
加密字节码 (Encrypted Bytecode + Auth Tag)
```

### 算法选择理由

#### 1. AES-256-GCM

- **为什么选择 GCM 模式？**
  - 提供加密 + 认证（AEAD）
  - 防止密文篡改
  - 高性能（硬件加速支持）
  - 并行化友好

- **为什么不用 CBC/CTR？**
  - CBC: 无认证，易受 Padding Oracle 攻击
  - CTR: 无认证，易受 Bit-flipping 攻击

#### 2. PBKDF2 (100,000 iterations)

- **目的**: 密钥加强，抵御暴力破解
- **参数选择**:
  - 迭代次数: 100,000（NIST SP 800-132 推荐）
  - PRF: HMAC-SHA256
  - 盐长度: 32 字节（256-bit）
  - 输出长度: 32 字节（256-bit）

- **为什么不用 Argon2？**
  - PBKDF2 是 Java 内置，零依赖
  - Argon2 需要第三方库，增加依赖

#### 3. HKDF (Per-Class Key Derivation)

- **目的**: 从主密钥派生独立的类专用密钥
- **优势**:
  - 密钥隔离（单个类泄露不影响其他类）
  - 使用类全名作为 context，确保唯一性
  - 符合 RFC 5869 标准

### 密钥管理

#### 密钥派生链

```java
// 1. 生成随机盐
byte[] salt = SaltGenerator.generate(32);

// 2. PBKDF2 派生主密钥
SecretKey masterKey = KeyDerivation.deriveKeyPBKDF2(
    password.toCharArray(),
    salt,
    100_000,  // iterations
    256       // key length
);

// 3. HKDF 派生类密钥
byte[] classKey = KeyDerivation.deriveKeyHKDF(
    masterKey.getEncoded(),
    className.getBytes(UTF_8),  // context
    32  // output length
);

// 4. AES-GCM 加密
byte[] iv = SaltGenerator.generate(12);  // GCM 推荐 96-bit IV
byte[] ciphertext = AESGCMCipher.encrypt(classKey, iv, plaintext);
```

#### 密钥存储

- ✅ **不存储密钥**: 所有密钥实时派生
- ✅ **只存储盐**: 存储在 `META-INF/.encrypted/metadata.json`
- ✅ **密码由用户管理**: 通过 JavaAgent 参数或环境变量传入

## 威胁模型

### 攻击场景分析

| 攻击类型 | 防护措施 | 安全级别 |
|---------|---------|---------|
| **静态分析攻击** | 字节码加密，无法反编译 | ✅ 高 |
| **内存dump攻击** | 运行时解密，内存中仍为明文 | ⚠️ 中（需配合JVM参数） |
| **暴力破解密码** | PBKDF2 100k 迭代，成本极高 | ✅ 高 |
| **密文篡改攻击** | GCM 认证标签验证 | ✅ 高 |
| **重放攻击** | 每个类独立密钥，IV唯一 | ✅ 高 |
| **侧信道攻击** | 时间恒定比较（密码验证） | ✅ 高 |
| **ClassLoader劫持** | 自定义ClassLoader，签名验证 | ⚠️ 中 |

### 不防护的场景

ByteGuard **无法防护**以下场景：

1. **运行时内存dump**
   - 类加载到内存后为明文字节码
   - 可通过 JVM 工具（jmap、Attach API）dump

2. **调试器附加**
   - JDWP 调试器可读取运行时状态
   - 建议生产环境禁用 `-agentlib:jdwp`

3. **恶意 JavaAgent**
   - 其他 Agent 可拦截类加载
   - 建议限制 JavaAgent 使用

4. **Root/Admin 权限攻击**
   - 系统级权限可绕过所有保护

## 安全最佳实践

### 1. 密码管理

#### ✅ 推荐做法

```bash
# 使用环境变量
export BYTEGUARD_PASSWORD="your-strong-password"
java -javaagent:byteguard-cli.jar=password=${BYTEGUARD_PASSWORD} -jar app.jar

# 使用 Kubernetes Secret
kubectl create secret generic byteguard-secret --from-literal=password=xxx
```

#### ❌ 不推荐做法

```bash
# 硬编码密码
java -javaagent:byteguard-cli.jar=password=123456 -jar app.jar

# 明文配置文件
echo "password=123456" > config.properties
```

#### 密码强度要求

- ✅ 至少 16 字符
- ✅ 包含大小写字母、数字、特殊字符
- ✅ 使用密码管理器生成
- ✅ 定期轮换（建议每 90 天）

### 2. 部署安全

#### JVM 安全参数

```bash
java \
  -Djava.security.egd=file:/dev/urandom \     # 高质量随机数
  -Dcom.sun.management.jmxremote=false \      # 禁用 JMX
  -XX:+DisableAttachMechanism \               # 禁用 Attach API
  -agentlib:jdwp=... \                        # 生产环境移除
  -javaagent:byteguard-cli.jar=password=${PWD} \
  -jar app-encrypted.jar
```

#### Docker 安全

```dockerfile
# 使用 Docker Secret
FROM openjdk:17-slim
COPY app-encrypted.jar /app.jar

# 运行时挂载 secret
# docker run -e BYTEGUARD_PASSWORD_FILE=/run/secrets/byteguard_pwd ...
CMD java -javaagent:/byteguard-cli.jar=password=$(cat $BYTEGUARD_PASSWORD_FILE) -jar /app.jar
```

### 3. 包过滤策略

#### 建议加密范围

```bash
# 只加密业务代码
--packages com.yourcompany,org.yourdomain

# 排除第三方库（已有混淆）
--exclude org.springframework,com.fasterxml.jackson
```

#### 不建议加密

- ❌ JDK 核心类（`java.*`, `javax.*`）
- ❌ 已混淆的第三方库
- ❌ 开源框架代码

### 4. 密钥轮换

定期更换加密密码：

```bash
# 1. 解密旧 JAR（需要旧密码）
# 2. 重新加密（使用新密码）
byteguard encrypt --input app.jar --output app-new.jar --password ${NEW_PASSWORD}
```

## 合规性

### 标准遵循

- ✅ **NIST SP 800-132**: PBKDF2 密钥派生
- ✅ **NIST SP 800-38D**: AES-GCM 加密模式
- ✅ **RFC 5869**: HKDF 密钥派生
- ✅ **FIPS 140-2**: 使用 Java 内置加密库（可配置 FIPS Provider）

### GDPR 合规

ByteGuard 本身不收集或处理个人数据，但请注意：

- ✅ 加密日志不包含敏感信息
- ✅ 密码不记录到文件
- ✅ 无网络通信

## 安全审计

### 推荐审计工具

1. **静态分析**
   - SonarQube（代码质量）
   - SpotBugs（漏洞检测）
   - Dependency-Check（依赖漏洞）

2. **动态分析**
   - OWASP ZAP（应用安全）
   - JProfiler（性能分析）

3. **密码学验证**
   - Cryptosense（密码学审计）
   - Keyczar（密钥管理审计）

### 安全测试清单

- [ ] 密钥派生参数验证
- [ ] IV/Nonce 唯一性测试
- [ ] 认证标签验证测试
- [ ] 密码强度策略测试
- [ ] 侧信道攻击测试
- [ ] 模糊测试（Fuzzing）

## 已知限制

### 技术限制

1. **无法防护内存攻击**
   - JVM 内存中字节码为明文
   - 需结合操作系统级保护（SELinux、AppArmor）

2. **依赖 Java 安全库**
   - 受 Java 版本影响
   - JDK 8 早期版本部分加密算法受限

3. **性能开销**
   - 启动时间增加 100-200ms
   - 首次类加载延迟 5-10ms

### 法律免责

ByteGuard 提供的是代码保护而非数据加密，不应用于：

- ❌ 加密用户数据或敏感信息
- ❌ 替代传输层加密（TLS/SSL）
- ❌ 替代数据库加密
- ❌ 满足特定行业加密标准（需咨询专业人士）

## 漏洞报告

如发现安全漏洞，请遵循负责任披露原则：

1. **私下报告**: security@yourdomain.com
2. **提供详情**: POC、影响范围、修复建议
3. **等待响应**: 我们将在 48 小时内回复
4. **公开披露**: 修复后 90 天（可协商）

## 版本历史

| 版本 | 日期 | 安全更新 |
|-----|------|---------|
| 1.0.0 | 2026-01 | 初始版本 |

## 参考资料

- [NIST SP 800-132](https://csrc.nist.gov/publications/detail/sp/800-132/final)
- [NIST SP 800-38D](https://csrc.nist.gov/publications/detail/sp/800-38d/final)
- [RFC 5869 - HKDF](https://tools.ietf.org/html/rfc5869)
- [OWASP Cryptographic Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html)

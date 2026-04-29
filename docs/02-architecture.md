# ByteGuard 架构设计

## 🏗️ 整体架构

本文档只描述 **公开仓库 `byteguard` 本身的开源架构**。

```
┌────────────────────────────────────────────────────────┐
│                 ByteGuard Open Source                  │
├────────────────────────────────────────────────────────┤
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │  byteguard-core                                  │  │
│  │  • AES-256-GCM                                   │  │
│  │  • PBKDF2 + HKDF                                 │  │
│  │  • Class Encryptor / Decryptor                   │  │
│  └─────────────────┬────────────────────────────────┘  │
│                    │                                   │
│      ┌─────────────┴─────────────┐                     │
│      ▼                           ▼                     │
│  ┌──────────────┐          ┌──────────────┐            │
│  │ byteguard-cli│          │ Maven Plugin │            │
│  │ CLI + Agent  │          │ Build Integr │            │
│  └──────────────┘          └──────────────┘            │
│                                                        │
└────────────────────────────────────────────────────────┘
```

### 职责划分

| 模块 | 职责 |
|------|------|
| **byteguard-core** | 加密算法、密钥派生、类加密与解密 |
| **byteguard-cli** | CLI 命令、Java Agent、运行验证入口 |
| **byteguard-maven-plugin** | 构建期集成与自动加密 |

> 本文档仅描述公开仓库中的核心架构与实现。

## 🔐 ByteGuard Core 架构

核心加密引擎的内部设计：

```
┌─────────────────────────────────────────────────────────────┐
│              ByteGuard Core (Open Source)                    │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐      ┌──────────────┐                     │
│  │   CLI Tool   │      │ Maven Plugin │                     │
│  │ (Encryptor)  │      │ (Build Tool) │                     │
│  └──────┬───────┘      └──────┬───────┘                     │
│         │                     │                               │
│         └─────────────────────┘                               │
│                               │                               │
│                               ▼                               │
│                  ┌──────────────────────┐                    │
│                  │   byteguard-core     │                    │
│                  │  - AES-256-GCM       │                    │
│                  │  - PBKDF2 + HKDF     │                    │
│                  │  - Class Encryptor   │                    │
│                  │  - Class Decryptor   │                    │
│                  └──────────┬───────────┘                    │
│                             │                                 │
│                             ▼                                 │
│                  [Encrypted JAR + Metadata]                  │
│                             │                                 │
│                             ▼                                 │
│                  ┌──────────────────────┐                    │
│                  │   JavaAgent          │                    │
│                  │  - Password Auth     │                    │
│                  │  - ClassTransformer  │                    │
│                  │  - AES Decryptor     │                    │
│                  └──────────────────────┘                    │
│                                                               │
└─────────────────────────────────────────────────────────────┘


```

## 🔐 加密流程

### 1. 密钥派生（Key Derivation）

```
User Password
     │
     ▼
PBKDF2 (100,000 iterations, SHA-256)
     │
     ├─── Random Salt (32 bytes)
     │
     ▼
Master Key (32 bytes)
     │
     ├─── HKDF-Expand
     │    └─── Context: className
     │
     ▼
Per-Class Key (32 bytes)
```

**设计理念**：
- **PBKDF2**：抵御暴力破解（100k 迭代）
- **HKDF**：每个类使用独立密钥，即使一个类被破解也不影响其他类
- **Random Salt**：每个 JAR 唯一，防止预计算攻击

### 2. 类加密（Class Encryption）

```java
for (ClassFile class : jar.classes) {
    // 1. 派生类专属密钥
    byte[] classKey = HKDF.expand(masterKey, class.name);
    
    // 2. AES-256-GCM 加密
    byte[] iv = SecureRandom.generateIV(12);  // 12 bytes
    byte[] encrypted = AES_GCM.encrypt(
        plaintext: class.bytecode,
        key: classKey,
        iv: iv,
        aad: class.name  // Additional Authenticated Data
    );
    
    // 3. 存储格式
    output = iv + encrypted + authTag;  // 12 + N + 16 bytes
}
```

**GCM 模式优势**：
- ✅ 认证加密（AEAD）：同时保证保密性和完整性
- ✅ 上下文绑定：通过 AAD 绑定类名，降低密文在错误类上下文中被接受的风险
- ✅ 防篡改：任何修改都会导致认证失败
- ✅ 性能优秀：硬件加速（AES-NI）

### 3. 元数据完整性保护

```
Master Key
     │
     ├─── HKDF-Expand
     │    └─── Context: "byteguard-metadata"
     │
     ▼
Metadata Key (32 bytes)
     │
     ├─── HMAC-SHA256
     │    └─── Input: Canonical JSON (excluding metadataMac)
     │
     ▼
Metadata MAC (32 bytes)
```

**防护目标**：
- ✅ 防止篡改加密类列表
- ✅ 防止修改算法标识或版本信息
- ✅ 防止替换 salt 进行降级攻击
- ✅ 确保元数据与密钥一致性

### 4. 元数据生成

加密后的 JAR 结构：

```
encrypted-app.jar
├── META-INF/
│   ├── MANIFEST.MF
│   ├── byteguard-metadata.json      # 加密元数据
│   └── .encrypted/
│       ├── com.example.Main         # 加密后的类（二进制）
│       ├── com.example.Service
│       └── ...
├── static/                           # 未加密资源
├── application.yml
└── lib/                              # 依赖 JAR（未加密）
```

`byteguard-metadata.json` 示例（v1.1 格式）：

```json
{
  "version": "1.1",
  "algorithm": "AES-256-GCM",
  "keyDerivation": "PBKDF2-HKDF",
  "salt": "base64EncodedSalt==",
  "metadataMac": "base64EncodedHMAC==",
  "encryptedAt": 1705593600000,
  "totalClasses": 42,
  "classes": {
    "com/example/Main.class": 2048,
    "com/example/Service.class": 1536
  }
}
```

**版本说明**：
- **v1.0**（旧版）：无 `metadataMac` 字段，兼容但不提供元数据完整性保护
- **v1.1**（当前）：包含 `metadataMac`，提供完整的元数据完整性验证

## 🚀 运行时解密

### JavaAgent 加载流程

```
JVM Startup
     │
     ▼
1. JavaAgent.premain()
     │
     ├─── Load license.lic
     ├─── Validate license
     ├─── Parse metadata
     └─── Derive master key
     │
     ▼
2. Register ClassFileTransformer
     │
     ▼
3. JVM loads class "com.example.Main"
     │
     ▼
4. ByteGuardTransformer.transform()
     │
     ├─── Read encrypted bytes from JAR
     ├─── Derive class key (HKDF)
     ├─── Decrypt with AES-GCM
     └─── Return plaintext bytecode
     │
     ▼
5. JVM defines class normally
     │
     ▼
Application runs
```

### ClassFileTransformer 实现

```java
public class ByteGuardTransformer implements ClassFileTransformer {
    
    @Override
    public byte[] transform(ClassLoader loader, String className,
                           Class<?> classBeingRedefined,
                           ProtectionDomain protectionDomain,
                           byte[] classfileBuffer) {
        
        // 1. 检查是否需要解密
        if (!encryptedClasses.containsKey(className)) {
            return null;  // 不处理
        }
        
        // 2. 读取加密数据
        byte[] encrypted = readEncryptedClass(className);
        
        // 3. 解密
        byte[] decrypted = decryptor.decrypt(className, encrypted);
        
        // 4. 返回原始字节码
        return decrypted;
    }
}
```

##  性能优化

### 启动时间优化

| 优化项 | 效果 | 实现 |
|--------|------|------|
| Lazy 解密 | 按需解密，不预加载 | ClassFileTransformer |
| 主密钥缓存 | 仅派生一次 | 静态变量 |
| 元数据预读 | 避免多次读 JAR | 启动时加载 |
| 硬件加速 | AES-NI 指令集 | JDK 内置 |

**基准测试结果**（100 个类）：

- 首次启动：~150ms overhead
- 后续类加载：~2ms per class
- 总计：< 200ms（小型应用）

### 内存优化

- ❌ **不缓存解密后的类**：避免内存泄漏
- ✅ **仅缓存元数据**：< 1MB
- ✅ **流式处理**：不加载整个 JAR 到内存

## 🔍 安全分析

### 攻击面分析

| 攻击类型 | 防护措施 | 安全等级 |
|----------|----------|----------|
| 静态反编译 | 类文件加密 | ✅ 高 |
| 内存 Dump | 仅运行时存在明文 | ⚠️ 中 |
| 调试器附加 | 当前公开版本未覆盖专门对抗 | ⚠️ 中 |
| JAR 篡改 | 元数据校验 | ⚠️ 中 |
| 密码爆破 | PBKDF2 100k 迭代 | ✅ 高 |

### 威胁模型

**已防御**：
- ✅ JAR 反编译工具（JD-GUI, Fernflower）
- ✅ 离线暴力破解（时间成本高）
- ✅ 密钥派生安全（PBKDF2 + HKDF）

**当前公开版本未覆盖的方向**：
- 更强的运行时防护
- 更严格的完整性保护
- 更复杂的分发与授权策略

## 📚 相关文档

- [快速开始](01-quick-start.md)
- [API 参考](03-api-reference.md)
- [测试指南](04-testing.md)

# ByteGuard Architecture Design

## Overview

ByteGuard is a modern Java bytecode encryption tool designed to provide 100% encryption coverage including Lambda expressions, Method References, and all modern Java features.

## Architecture Principles

### Core Design

```
┌─────────────────────────────────────────────────────────┐
│                    ByteGuard CLI/Agent                   │
├─────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Encryptor   │  │    Loader    │  │   Analyzer   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
├─────────────────────────────────────────────────────────┤
│                   ByteGuard Core                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ KeyDerivation│  │ AESGCMCipher │  │SaltGenerator │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Module Structure

### byteguard-core

核心加密解密引擎，包含：

- **crypto/**: 加密核心
  - `KeyDerivation.java`: PBKDF2 + HKDF 密钥派生
  - `AESGCMCipher.java`: AES-256-GCM 加密解密
  - `SaltGenerator.java`: 安全随机盐生成
  - `EncryptionMetadata.java`: 加密元数据模型

- **encryptor/**: 加密器
  - `ClassEncryptor.java`: 单个类加密
  - `JarEncryptor.java`: JAR 文件加密
  - `EncryptionStrategy.java`: 加密策略接口

- **loader/**: 类加载器
  - `DecryptingClassLoader.java`: 解密类加载器
  - `ClassCache.java`: LRU 类缓存
  - `EncryptedClassRepository.java`: 加密类仓库

### byteguard-cli

命令行工具和 JavaAgent

- `Main.java`: CLI 入口
- `ByteGuardAgent.java`: JavaAgent 入口
- `EncryptCommand.java`: 加密命令
- `DecryptCommand.java`: 解密命令

### byteguard-maven-plugin

Maven 插件，用于构建时加密

## Encryption Flow

```
1. 读取 JAR → 2. 生成随机盐 → 3. PBKDF2 派生主密钥
                     ↓
4. 遍历 class 文件 → 5. HKDF 派生类密钥 → 6. AES-GCM 加密
                     ↓
7. 写入 META-INF/.encrypted/ → 8. 删除原始 class → 9. 保存元数据
```

## Decryption Flow (Runtime)

```
1. 应用启动 → 2. Agent 加载 → 3. 读取元数据 → 4. 验证密码
                     ↓
5. 自定义 ClassLoader → 6. 拦截类加载请求 → 7. HKDF 派生类密钥
                     ↓
8. AES-GCM 解密 → 9. 验证 TAG → 10. LRU 缓存 → 11. 返回字节码
```

## Key Derivation Chain

```
User Password
     ↓
PBKDF2 (100,000 iterations, SHA-256)
     ↓
Master Key (256-bit)
     ↓
HKDF (className as context)
     ↓
Per-Class Key (256-bit)
     ↓
AES-256-GCM Encryption
```

## Storage Format

### Encrypted JAR Structure

```
encrypted-app.jar
├── META-INF/
│   ├── MANIFEST.MF
│   ├── byteguard-metadata.json      # 加密元数据
│   └── .encrypted/                  # 加密的类
│       ├── com/example/User.class   # [IV|Ciphertext|TAG]
│       └── com/example/Order.class
├── static/                          # 静态资源 (未加密)
└── application.yml
```

### Metadata Format

```json
{
  "version": "1.0.0",
  "algorithm": "AES-256-GCM",
  "kdfAlgorithm": "PBKDF2WithHmacSHA256",
  "kdfIterations": 100000,
  "salt": "<base64-encoded-32-bytes>",
  "passwordHash": "$argon2id$v=19$m=65536,t=10,p=1$...",
  "encryptedClasses": [
    "com/example/User.class",
    "com/example/Order.class"
  ],
  "timestamp": 1705334400000
}
```

### Encrypted Class Format

```
┌──────────┬─────────────────┬────────────┐
│    IV    │   Ciphertext    │    TAG     │
│ (12字节)  │   (变长)        │  (16字节)   │
└──────────┴─────────────────┴────────────┘
```

## Performance Optimization

1. **LRU Cache**: 首次解密后缓存，避免重复解密
2. **Lazy Decryption**: 按需解密，未使用的类不解密
3. **Parallel Processing**: 加密时并行处理多个类
4. **Memory Mapping**: 大文件使用内存映射 I/O

## Security Features

- ✅ AES-256-GCM 认证加密
- ✅ PBKDF2 密钥派生 (100k 迭代)
- ✅ HKDF 每类独立密钥
- ✅ Argon2id 密码验证
- ✅ 随机盐值 (32 字节)
- ✅ 随机 IV (12 字节)
- ✅ GCM 认证标签 (16 字节)

## Technology Stack

- **Build Tool**: Gradle 8.10+
- **Language**: Java 8+
- **Crypto**: Java Crypto API (JCA/JCE)
- **CLI**: Picocli 4.7+
- **Cache**: Caffeine 3.1+
- **Password**: Argon2-JVM 2.11
- **Testing**: JUnit 5 + AssertJ + Mockito

## Next Steps

1. Implement `byteguard-core` crypto modules
2. Implement `byteguard-cli` encryptor
3. Implement JavaAgent loader
4. Add integration tests
5. Add performance benchmarks

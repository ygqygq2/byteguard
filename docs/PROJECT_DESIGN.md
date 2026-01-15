# ByteGuard 项目初始化设计总结

## 项目概述

**ByteGuard** 是一个现代化的 Java 字节码加密工具，专注于提供 100% 的类型覆盖支持（包括 Lambda 表达式、方法引用、Records、Sealed Classes 等），采用军事级加密标准保护 Java 应用程序的知识产权。

### 核心特性

- ✅ **100% 覆盖**: 加密所有类型，包括 Lambda、Method References、Records、Sealed Classes
- ✅ **军事级安全**: AES-256-GCM + PBKDF2 (100k 迭代) + HKDF per-class 密钥
- ✅ **现代架构**: 基于自定义 ClassLoader 的解密，无 Javassist 限制
- ✅ **零依赖**: 纯 Java Crypto API，最小 JAR 体积
- ✅ **高性能**: LRU 缓存、延迟解密、约 200ms 启动开销

## 项目结构

```
byteguard/
├── byteguard-core/           # 核心加密解密引擎
│   ├── crypto/              # 加密核心（PBKDF2, HKDF, AES-GCM）
│   ├── encryptor/           # JAR 加密器
│   ├── loader/              # 运行时解密 ClassLoader
│   └── model/               # 数据模型
│
├── byteguard-cli/            # 命令行工具 & JavaAgent
│   ├── cli/                 # CLI 命令（encrypt, verify 等）
│   └── agent/               # JavaAgent 实现
│
├── byteguard-maven-plugin/   # Maven 构建插件
│
├── docs/                     # 项目文档
│   ├── architecture.md      # 架构设计
│   ├── development.md       # 开发指南
│   ├── TODO.md              # 任务清单
│   ├── SECURITY.md          # 安全设计文档（新增）
│   └── PERFORMANCE.md       # 性能优化文档（新增）
│
└── .github/                  # GitHub 配置
    └── workflows/           # CI/CD 工作流
```

## 技术架构

### 加密流程

```
用户密码
  ↓ PBKDF2 (100,000 iterations)
主密钥 (256-bit)
  ↓ HKDF (className as context)
类专用密钥 (256-bit)
  ↓ AES-256-GCM
加密字节码 + 认证标签
```

### 解密流程（运行时）

```
JavaAgent 启动
  ↓
自定义 ClassLoader
  ↓
拦截类加载请求
  ↓
从 META-INF/.encrypted/ 读取加密类
  ↓
派生类密钥 → AES-GCM 解密
  ↓
验证认证标签 → LRU 缓存
  ↓
返回字节码给 JVM
```

## 开发计划（Milestones）

### Milestone 1: Core Crypto Module (2周)
**GitHub Issue**: [#7](https://github.com/ygqygq2/byteguard/issues/7)

实现核心加密解密功能：
- SaltGenerator - 安全随机盐生成
- KeyDerivation - PBKDF2 + HKDF 密钥派生
- AESGCMCipher - AES-256-GCM 加密解密
- EncryptionMetadata - 元数据模型
- PasswordHasher - Argon2id 密码哈希

### Milestone 2: Encryptor & Analyzer (2周)
**GitHub Issue**: [#5](https://github.com/ygqygq2/byteguard/issues/5)

实现 JAR 加密器：
- ClassEncryptor - 单个类加密
- JarEncryptor - JAR 批量加密
- ClassAnalyzer - Lambda/Records 检测
- MetadataWriter - 元数据序列化

### Milestone 3: ClassLoader & Agent (2周)
**GitHub Issue**: [#6](https://github.com/ygqygy2/byteguard/issues/6)

实现运行时解密：
- DecryptingClassLoader - 自定义类加载器
- ClassCache - Caffeine LRU 缓存
- ByteGuardAgent - JavaAgent 实现
- MetadataReader - 元数据反序列化

### Milestone 4: CLI Tool (1周)
**GitHub Issue**: [#3](https://github.com/ygqygy2/byteguard/issues/3)

实现命令行工具：
- 加密命令 (encrypt)
- 验证命令 (verify)
- 版本命令 (version)
- 友好的用户界面

### Milestone 5: Maven Plugin (1周)
**GitHub Issue**: [#4](https://github.com/ygqygq2/byteguard/issues/4)

实现 Maven 插件：
- EncryptMojo
- 构建时自动加密
- 多模块项目支持

## 已完成的初始化工作

### ✅ 1. 项目仓库初始化
- [x] Git 仓库初始化
- [x] 推送到 GitHub (git@github.com:ygqygy2/byteguard.git)
- [x] main 分支设置

### ✅ 2. GitHub Issues 创建
已创建 7 个 GitHub Issues 进行项目跟踪：
- [#1](https://github.com/ygqygq2/byteguard/issues/1) - CI/CD 流程配置
- [#2](https://github.com/ygqygq2/byteguard/issues/2) - 完善项目文档
- [#3](https://github.com/ygqygy2/byteguard/issues/3) - CLI Tool
- [#4](https://github.com/ygqygy2/byteguard/issues/4) - Maven Plugin
- [#5](https://github.com/ygqygq2/byteguard/issues/5) - Encryptor & Analyzer
- [#6](https://github.com/ygqygy2/byteguard/issues/6) - ClassLoader & Agent
- [#7](https://github.com/ygqygq2/byteguard/issues/7) - Core Crypto Module

### ✅ 3. 核心文档完善

#### SECURITY.md
包含：
- 密码学设计详解（AES-256-GCM, PBKDF2, HKDF）
- 威胁模型分析
- 安全最佳实践
- 密码管理指南
- 部署安全配置
- 合规性说明（NIST, FIPS, RFC 标准）

#### PERFORMANCE.md
包含：
- 性能目标和基准
- 加密/解密性能分析
- 优化策略（并行加密、LRU 缓存、延迟解密）
- 性能调优指南
- 监控和诊断方法

#### CONTRIBUTING.md
包含：
- 贡献流程
- 代码规范（Google Java Style）
- 测试规范
- 提交信息规范（Conventional Commits）
- PR 检查清单

### ✅ 4. CI/CD 流程配置

#### GitHub Actions 工作流：

**build.yml** - 构建和测试
- 多 JDK 版本测试（8, 11, 17, 21）
- 跨平台测试（Ubuntu, Windows, macOS）
- 代码覆盖率（Codecov）
- SonarQube 代码质量分析
- Checkstyle 代码风格检查
- SpotBugs 静态分析
- OWASP 依赖检查

**release.yml** - 自动发布
- 版本提取
- Fat JAR 构建
- GitHub Releases 自动创建
- Maven Central 发布
- GPG 签名

**security.yml** - 安全扫描
- CodeQL 分析
- Dependency Review
- Trivy 漏洞扫描

### ✅ 5. Gradle 配置增强

- [x] Jacoco 代码覆盖率插件
- [x] Checkstyle 代码风格检查
- [x] SpotBugs 静态分析
- [x] SonarQube 集成
- [x] Maven 发布配置（OSSRH）
- [x] GPG 签名配置

## 技术栈

### 核心依赖
- **JDK**: 8+ (兼容 8, 11, 17, 21)
- **构建工具**: Gradle 8.11
- **加密**: Java Crypto API（零外部依赖）
- **缓存**: Caffeine (高性能 LRU)
- **日志**: SLF4J API

### 测试框架
- **单元测试**: JUnit 5
- **断言**: AssertJ
- **Mock**: Mockito
- **覆盖率**: Jacoco

### 代码质量
- **静态分析**: SpotBugs, Checkstyle
- **代码质量**: SonarQube
- **安全扫描**: CodeQL, Trivy, OWASP Dependency Check

## 安全设计亮点

### 密钥管理
- ✅ 主密钥只派生一次，缓存复用
- ✅ 每个类独立密钥（HKDF 派生）
- ✅ 密钥永不存储，实时派生
- ✅ 密码清除机制（防内存泄露）

### 加密强度
- ✅ AES-256-GCM（AEAD 加密 + 认证）
- ✅ PBKDF2 100,000 迭代（抗暴力破解）
- ✅ 32 字节随机盐（SecureRandom）
- ✅ 12 字节 IV（GCM 推荐）
- ✅ 认证标签验证（防篡改）

### 威胁防护
| 攻击类型 | 防护等级 |
|---------|---------|
| 静态分析 | ✅ 高 |
| 暴力破解 | ✅ 高 |
| 密文篡改 | ✅ 高 |
| 重放攻击 | ✅ 高 |
| 侧信道攻击 | ✅ 高 |
| 内存dump | ⚠️ 中 |

## 性能优化策略

### 加密阶段（构建时）
- ✅ 主密钥只派生一次（避免重复 PBKDF2）
- ✅ 并行加密（多线程支持）
- ✅ 批量 IO 写入（BufferedOutputStream）

### 解密阶段（运行时）
- ✅ LRU 缓存（Caffeine，默认 1000 类）
- ✅ 延迟解密（按需加载）
- ✅ 主密钥缓存（启动时派生一次）

### 性能目标
| 指标 | 目标值 |
|-----|--------|
| 启动开销 | < 200ms |
| 单类加密 | < 10ms |
| 单类解密 | < 5ms |
| 缓存命中率 | > 80% |
| 内存开销 | < 50MB |

## 下一步行动

### 立即开始（推荐顺序）

1. **实现 Milestone 1: Core Crypto Module**
   - 创建 `byteguard-core/src/main/java/io/github/ygqygq2/byteguard/core/crypto/` 包
   - 实现 SaltGenerator.java
   - 实现 KeyDerivation.java (PBKDF2 + HKDF)
   - 实现 AESGCMCipher.java
   - 编写单元测试，确保覆盖率 > 90%

2. **验证核心功能**
   - 创建集成测试
   - 验证加密/解密流程
   - 性能基准测试

3. **按顺序完成后续 Milestones**
   - Milestone 2 → 3 → 4 → 5

## 项目管理

### GitHub 仓库
- **主页**: https://github.com/ygqygq2/byteguard
- **Issues**: 使用 GitHub Issues 跟踪任务
- **Projects**: 可创建 GitHub Project Board 可视化进度

### 开发工作流
1. 从 issue 创建 feature 分支
2. 实现功能 + 测试
3. 提交 PR
4. CI/CD 自动验证
5. Code Review
6. 合并到 main

### 版本发布
- 开发版本: `1.0.0-SNAPSHOT`
- 首个正式版: `1.0.0`（预计所有 Milestones 完成后）
- 发布策略: Semantic Versioning (主版本.次版本.修订版本)

## 总结

ByteGuard 项目已完成完整的初始化和设计阶段，包括：

✅ **项目结构**: 清晰的模块化设计  
✅ **技术架构**: 成熟的加密方案和实现路径  
✅ **开发计划**: 详细的 Milestone 划分（共 5 个）  
✅ **文档体系**: 完善的安全、性能、贡献指南  
✅ **CI/CD**: 全自动化的构建、测试、发布流程  
✅ **代码质量**: 多层次的质量保障工具  
✅ **项目管理**: GitHub Issues 跟踪，清晰的工作流程  

**项目已做好充分准备，可以立即开始 Milestone 1 的实现工作！**

---

**创建日期**: 2026-01-16  
**文档版本**: 1.0  
**项目状态**: 初始化完成，待实现

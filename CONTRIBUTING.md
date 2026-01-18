# 贡献指南

感谢您对 ByteGuard 的关注！我们欢迎任何形式的贡献。

## 行为准则

请遵循友好、包容、尊重的沟通原则。

## 如何贡献

### 报告 Bug

在 [GitHub Issues](https://github.com/ygqygq2/byteguard/issues) 提交 Bug 报告时，请包含：

- **环境信息**: JDK 版本、操作系统、ByteGuard 版本
- **重现步骤**: 详细的步骤描述
- **期望行为**: 应该发生什么
- **实际行为**: 实际发生了什么
- **日志**: 相关错误日志

**模板**:

```markdown
### 环境
- JDK: OpenJDK 17.0.2
- OS: Ubuntu 22.04
- ByteGuard: 1.0.0

### 重现步骤
1. 运行命令 `byteguard encrypt ...`
2. 启动加密后的应用
3. 观察错误信息

### 期望行为
应该成功启动

### 实际行为
抛出异常 ...

### 日志
```
Exception in thread "main" ...
```
```

### 提出新功能

在提出新功能之前，请先：

1. 搜索现有 Issues，避免重复
2. 考虑功能的通用性和必要性
3. 提供详细的使用场景和设计思路

### 提交代码

#### 开发环境

```bash
# 克隆仓库
git clone https://github.com/ygqygq2/byteguard.git
cd byteguard

# 构建项目
./gradlew build

# 运行测试
./gradlew test
```

#### 分支策略

- `main`: 稳定版本
- `develop`: 开发分支（从这里创建 feature 分支）
- `feature/*`: 新功能分支
- `bugfix/*`: Bug 修复分支
- `release/*`: 发布分支

#### 工作流程

1. **Fork 仓库**

2. **创建分支**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **编写代码**
   - 遵循代码规范（见下文）
   - 添加单元测试
   - 更新文档

4. **提交代码**
   ```bash
   git add .
   git commit -m "feat: add new feature"
   ```

5. **推送分支**
   ```bash
   git push origin feature/your-feature-name
   ```

6. **创建 Pull Request**
   - 描述清楚改动内容
   - 关联相关 Issue
   - 确保 CI 通过

## 代码规范

### Java 编码规范

遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)

#### 关键点

- **缩进**: 4 空格
- **行宽**: 120 字符
- **命名**:
  - 类名: `UpperCamelCase`
  - 方法名: `lowerCamelCase`
  - 常量: `UPPER_SNAKE_CASE`
- **大括号**: K&R 风格

#### 示例

```java
public class AESGCMCipher {
    private static final int GCM_TAG_LENGTH = 128;
    
    public static byte[] encrypt(byte[] key, byte[] iv, byte[] plaintext) 
            throws GeneralSecurityException {
        // 实现
    }
}
```

### 注释规范

#### JavaDoc

所有 public 类和方法必须有 JavaDoc：

```java
/**
 * Encrypts plaintext using AES-256-GCM.
 *
 * @param key the 256-bit encryption key
 * @param iv the 96-bit initialization vector
 * @param plaintext the data to encrypt
 * @return encrypted data with authentication tag
 * @throws GeneralSecurityException if encryption fails
 */
public static byte[] encrypt(byte[] key, byte[] iv, byte[] plaintext) 
        throws GeneralSecurityException {
    // ...
}
```

#### 内联注释

复杂逻辑需要注释：

```java
// HKDF-Expand: OKM = T(1) | T(2) | ... | T(N)
byte[] okm = new byte[length];
byte[] t = new byte[0];
```

### 测试规范

#### 单元测试

- 每个 public 方法必须有测试
- 测试覆盖率 > 80%
- 使用 JUnit 5

```java
@Test
void testEncryptDecrypt() throws Exception {
    byte[] key = KeyDerivation.deriveKeyPBKDF2("password".toCharArray(), salt);
    byte[] iv = SaltGenerator.generate(12);
    byte[] plaintext = "Hello World".getBytes(UTF_8);
    
    byte[] ciphertext = AESGCMCipher.encrypt(key, iv, plaintext);
    byte[] decrypted = AESGCMCipher.decrypt(key, iv, ciphertext);
    
    assertArrayEquals(plaintext, decrypted);
}
```

#### 测试命名

```java
@Test
void methodName_StateUnderTest_ExpectedBehavior()
```

示例：
- `encrypt_WithValidKey_ReturnsEncryptedData()`
- `decrypt_WithWrongKey_ThrowsException()`

### 提交信息规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/)

#### 格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

#### Type

- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式（不影响功能）
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建/工具链

#### 示例

```
feat(core): add AES-256-GCM encryption support

Implement AESGCMCipher class with encrypt/decrypt methods.
Use Java Crypto API, no external dependencies.

Closes #12
```

```
fix(loader): handle Lambda classes correctly

Lambda classes have synthetic names like Main$$Lambda$1.
Update class name parsing logic.

Fixes #45
```

## Pull Request 检查清单

提交 PR 前请确认：

- [ ] 代码通过 `./gradlew build`
- [ ] 所有测试通过 `./gradlew test`
- [ ] 代码覆盖率未降低
- [ ] 添加了必要的单元测试
- [ ] 更新了相关文档
- [ ] 遵循代码规范
- [ ] 提交信息符合规范
- [ ] PR 描述清晰

## Code Review 流程

1. **自动检查**: CI 会运行测试和代码检查
2. **人工审查**: 至少 1 名 Maintainer 审查
3. **反馈修改**: 根据评审意见修改
4. **合并**: 通过后由 Maintainer 合并

## 发布流程

仅 Maintainers 执行：

1. 更新版本号（`gradle.properties`）
2. 更新 CHANGELOG.md
3. 创建 Release 分支
4. 构建并测试
5. 创建 Git Tag
6. 发布到 Maven Central
7. 创建 GitHub Release

## 需要帮助？

- 💬 [GitHub Discussions](https://github.com/ygqygq2/byteguard/discussions)
- 📧 Email: [your-email@example.com]
- 🐛 [GitHub Issues](https://github.com/ygqygq2/byteguard/issues)

## License

贡献的代码将采用 [Apache License 2.0](LICENSE)。

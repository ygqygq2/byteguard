# ByteGuard 测试指南

> 用于验证公开仓库中的核心加密链路、CLI 行为与运行时加载流程。

## 快速开始

### 本地运行测试

```bash
cd byteguard

# 构建项目
./gradlew build

# 运行单元测试
./gradlew test

# 运行集成测试
./gradlew integrationTest

# 运行完整校验（推荐）
./gradlew check
```

如果你更喜欢脚本入口，仓库中也提供了 `scripts/test.sh`，但当前推荐优先使用 `gradlew` 与 CI 保持一致。

## 测试架构

### 当前测试基线

公开仓库当前主要依赖：

- **JUnit 5** - 单元测试与集成测试
- **Gradle test / integrationTest** - 本地与 CI 的统一入口
- **JaCoCo** - 覆盖率报告
- **GitHub Actions** - 持续集成执行环境

### 单元测试

位于 `byteguard-core/src/test/java/io/github/ygqygq2/byteguard/test/`

**包含：**
- 加密算法测试 (`AESGCMCipherTest`)
- 密钥派生测试 (`KeyDerivationTest`)

**运行：**
```bash
./gradlew test
```

### 集成测试

位于 `byteguard-core/src/test/java/io/github/ygqygq2/byteguard/test/integration/`

**包含：**
- `testEncryptApplication()` - 验证 JAR 加密功能
- `testGenerateLicense()` - 验证 License 生成
- `testEndToEnd()` - 完整流程：加密 → 生成 License → 运行
- `testEncryptedJarStructure()` - 验证加密 JAR 格式

**运行：**
```bash
./gradlew integrationTest
```

## 工作流

### GitHub Actions

自动在以下情况下运行：
- Push 到 `main` 分支
- 提交 Pull Request 到 `main` 分支

**测试矩阵：**
- Java 版本: 21
- 操作系统: ubuntu-latest, windows-latest, macos-latest

**工作流程：**
1. 编译项目
2. 运行单元测试
3. 运行集成测试
4. 验证 JAR 结构
5. 检查代码质量

查看 [.github/workflows/build.yml](../.github/workflows/build.yml)

### 本地 CI 模拟

在本地环境中模拟 CI 行为：

```bash
CI=true ./gradlew check
```

这会禁用彩色输出，适合日志系统。

## 测试数据

### 测试应用

- **simple-app** - 简单的计算器应用，用于端到端测试
  - Location: `test-fixtures/test-apps/simple-app/`
  - Classes: `SimpleMain`, `Calculator`, `Greeter`

### 测试输出

临时文件存储在：
- `test-fixtures/test-apps/simple-app/build/simple-app-encrypted-test.jar` - 加密 JAR
- `test-fixtures/test-apps/simple-app/build/test-license.lic` - 测试 License

> 这些文件在 `@After` 阶段自动清理

## 编写新测试

### 单元测试

```java
package io.github.ygqygq2.byteguard.core.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MyTest {
    @Test
    void testSomething() {
        // 执行测试
        assertTrue(true);
    }
}
```

### 集成测试

```java
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class MyIntegrationTest {
    @Test
    @Tag("integration")
    void testIntegration() throws Exception {
        // 运行集成测试
        Process process = new ProcessBuilder("java", "-jar", "app.jar").start();
        int exitCode = process.waitFor();
        org.junit.jupiter.api.Assertions.assertEquals(0, exitCode);
    }
}
```

### 让测试进入默认流程

如果你新增的是普通单元测试或带 `integration` tag 的集成测试，放进现有测试源码目录后，`./gradlew test` 或 `./gradlew integrationTest` 就会自动拾取。

## 测试与验证建议

- 优先使用 `./gradlew test`、`./gradlew integrationTest` 和 `./gradlew check`
- 修改加密链路后，至少补一条单元测试或集成测试
- 涉及 JAR 结构或 Agent 加载时，优先补集成测试
- 合并前查看 `build/reports/tests/` 与 `build/reports/jacoco/`

## 故障排除

### 测试找不到 Java 类

```bash
# 确保编译输出正确
ls -la build/classes/io/github/ygqygq2/byteguard/

# 重新清理和构建
./gradlew clean
./gradlew build
```

### 集成测试超时

```bash
# 增加超时时间或调整测试
# 检查 java.class.path 是否正确
```

### JAR 创建失败

```bash
# 验证 jar 命令可用
which jar

# 检查 manifest.txt 格式
cat build/manifest.txt
```

## 最佳实践

1. **运行完整校验** - 在提交前运行 `./gradlew check`
2. **定期添加测试** - 为新功能添加单元和集成测试
3. **保持测试独立** - 每个测试应该能独立运行
4. **使用清晰的命名** - 测试方法名应描述测试内容
5. **优先和 CI 对齐** - 本地尽量使用与工作流一致的 Gradle 任务

## 维护

### 添加新的 Java 版本支持

编辑 `.github/workflows/build.yml`：

```yaml
strategy:
  matrix:
        java: ['21', '23']  # 添加新版本示例
```

### 更新测试框架

修改 `byteguard-core/src/test/java/io/github/ygqygq2/byteguard/test/framework/`

- `Test.java` - @Test 注解
- `Before.java` - @Before 注解  
- `After.java` - @After 注解
- `Assert.java` - 断言库
- `TestRunner.java` - 测试执行器

## 相关文档

- [文档索引](00-index.md)
- [项目架构](02-architecture.md)
- [API 参考](03-api-reference.md)

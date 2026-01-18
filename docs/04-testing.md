# ByteGuard 测试套件

> 统一的本地和 CI 测试框架

## 快速开始

### 本地运行测试

```bash
cd byteguard

# 构建项目
./test.sh build

# 运行单元测试
./test.sh unit

# 运行集成测试
./test.sh integration

# 运行所有测试（推荐）
./test.sh all

# 清理构建文件
./test.sh clean

# 帮助
./test.sh help
```

## 测试架构

### 测试框架

自实现的轻量级测试框架，零外部依赖：

- **`@Test`** - 标记测试方法
- **`@Before`** - 测试初始化
- **`@After`** - 测试清理
- **`Assert`** - 断言库
- **`TestRunner`** - 测试执行器

### 单元测试

位于 `byteguard-core/src/test/java/io/github/ygqygq2/byteguard/test/`

**包含：**
- 加密算法测试 (`AESGCMCipherTest`)
- 密钥派生测试 (`KeyDerivationTest`)

**运行：**
```bash
./test.sh unit
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
./test.sh integration
```

## 工作流

### GitHub Actions

自动在以下情况下运行：
- Push 到 `main` 分支
- 提交 Pull Request 到 `main` 分支

**测试矩阵：**
- Java 版本: 8, 11, 17, 21
- 操作系统: ubuntu-latest

**工作流程：**
1. 编译项目
2. 运行单元测试
3. 运行集成测试
4. 验证 JAR 结构
5. 检查代码质量

查看 [.github/workflows/build-and-test.yml](../../.github/workflows/build-and-test.yml)

### 本地 CI 模拟

在本地环境中模拟 CI 行为：

```bash
CI=true ./test.sh all
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
package io.github.ygqygq2.byteguard.test.crypto;

import io.github.ygqygq2.byteguard.test.framework.*;

public class MyTest {
    
    @Before
    public void setUp() {
        // 初始化测试环境
    }
    
    @Test("测试描述")
    public void testSomething() {
        // 执行测试
        Assert.assertTrue(true, "期望值与实际值不符");
    }
    
    @After
    public void tearDown() {
        // 清理资源
    }
}
```

### 集成测试

```java
import io.github.ygqygq2.byteguard.test.integration.IntegrationTest;

public class MyIntegrationTest extends IntegrationTest {
    
    @Test("集成测试")
    public void testIntegration() throws Exception {
        // 运行集成测试
        Process process = new ProcessBuilder("java", "-jar", "app.jar").start();
        int exitCode = process.waitFor();
        Assert.assertEquals(0, exitCode);
    }
}
```

### 注册测试

修改 `test.sh` 的 `Run_Unit_Tests()` 或 `Run_Integration_Tests()` 函数：

```bash
java -cp "${CLASSES_DIR}" \
    io.github.ygqygq2.byteguard.test.framework.TestRunner \
    io.github.ygqygq2.byteguard.test.crypto.MyTest \
    io.github.ygqygq2.byteguard.test.integration.MyIntegrationTest
```

## 脚本规范

遵循 [Google Shell Style Guide](https://google.github.io/styleguide/shellguide.html) 和项目规范：

- ✅ 使用 `#!/usr/bin/env bash`
- ✅ 设置 `set -euo pipefail`
- ✅ 函数名：`首字母大写_下划线` 格式
- ✅ 所有变量严格 `local` 化
- ✅ 使用 `function` 关键字声明
- ✅ 有 `Main()` 函数作为入口
- ✅ 常量全大写，使用 `readonly`
- ✅ 支持 CI 环境检测 (`CI` 环境变量)

## 故障排除

### 测试找不到 Java 类

```bash
# 确保编译输出正确
ls -la build/classes/io/github/ygqygq2/byteguard/

# 重新清理和构建
./test.sh clean
./test.sh build
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

1. **运行完整测试套件** - 在提交前运行 `./test.sh all`
2. **定期添加测试** - 为新功能添加单元和集成测试
3. **保持测试独立** - 每个测试应该能独立运行
4. **使用清晰的命名** - 测试方法名应描述测试内容
5. **快速反馈** - 单元测试应该快速运行

## 维护

### 添加新的 Java 版本支持

编辑 `.github/workflows/build-and-test.yml`：

```yaml
strategy:
  matrix:
    java-version: ['8', '11', '17', '21', '23']  # 添加新版本
```

### 更新测试框架

修改 `byteguard-core/src/test/java/io/github/ygqygq2/byteguard/test/framework/`

- `Test.java` - @Test 注解
- `Before.java` - @Before 注解  
- `After.java` - @After 注解
- `Assert.java` - 断言库
- `TestRunner.java` - 测试执行器

## 相关文档

- [开发指南](../development.md)
- [项目架构](../architecture.md)
- [Git 工作流](../.github/copilot-instructions.md)

# GitHub Actions 配置说明

本项目使用 GitHub Actions 实现完整的 CI/CD 流程。

## 工作流列表

### 1. Build and Test (`build.yml`)

**触发条件**:
- 推送到 `main` 或 `develop` 分支
- Pull Request 到 `main` 或 `develop` 分支

**功能**:
- ✅ 多 JDK 版本测试（8, 11, 17, 21）
- ✅ 跨平台测试（Ubuntu, Windows, macOS）
- ✅ 自动运行单元测试
- ✅ 生成代码覆盖率报告（Codecov）
- ✅ 代码质量分析（SonarQube）
- ✅ 代码风格检查（Checkstyle）
- ✅ 静态分析（SpotBugs）
- ✅ 依赖安全检查（OWASP）

**使用的 Actions**:
- `actions/checkout@v4`
- `actions/setup-java@v4`
- `codecov/codecov-action@v5`
- `actions/upload-artifact@v4`

### 2. Release (`release.yml`)

**触发条件**:
- 推送 tag（格式: `v*.*.*`，如 `v1.0.0`）

**功能**:
- ✅ 自动构建发布版本
- ✅ 生成 Fat JAR
- ✅ 从 CHANGELOG.md 提取发布说明
- ✅ 创建 GitHub Release
- ✅ 上传构建产物
- ✅ 发布到 Maven Central（需配置密钥）

**使用的 Actions**:
- `actions/checkout@v4`
- `actions/setup-java@v4`
- `ncipollo/release-action@v1`
- `crazy-max/ghaction-import-gpg@v6`

**发布步骤**:

1. **更新 CHANGELOG.md**
   
   在 CHANGELOG.md 中添加新版本记录：
   ```markdown
   ## [1.0.0] - 2026-xx-xx
   
   ### 新增
   - 功能1
   - 功能2
   
   ### 修复
   - Bug1
   ```

2. **创建并推送 tag**
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

3. **自动发布**
   - GitHub Actions 自动触发
   - 构建所有模块
   - 运行测试
   - 从 CHANGELOG.md 提取该版本的发布说明
   - 创建 GitHub Release
   - 上传 JAR 文件

### 3. Security Scan (`security.yml`)

**触发条件**:
- 推送到 `main` 或 `develop` 分支
- Pull Request 到 `main` 分支
- 每周日定时扫描

**功能**:
- ✅ CodeQL 代码安全分析
- ✅ 依赖审查（PR 时）
- ✅ Trivy 漏洞扫描

**使用的 Actions**:
- `actions/checkout@v4`
- `github/codeql-action/init@v3`
- `github/codeql-action/analyze@v3`
- `actions/dependency-review-action@v4`
- `aquasecurity/trivy-action@0.28.0`

## 密钥配置

### 必需的 Secrets

在 GitHub 仓库的 Settings → Secrets and variables → Actions 中配置：

#### Maven Central 发布（可选）

- `OSSRH_USERNAME`: Sonatype OSSRH 用户名
- `OSSRH_PASSWORD`: Sonatype OSSRH 密码
- `GPG_PRIVATE_KEY`: GPG 私钥（用于签名）
- `GPG_PASSPHRASE`: GPG 密钥密码
- `SIGNING_KEY_ID`: GPG 密钥 ID

#### SonarQube（可选）

- `SONAR_TOKEN`: SonarCloud Token

#### Codecov（可选）

- `CODECOV_TOKEN`: Codecov Token（公开仓库不需要）

## CHANGELOG.md 格式

release workflow 使用 awk 命令从 CHANGELOG.md 提取最新版本的发布说明。

**格式要求**:

```markdown
# Changelog

## [1.0.1] - 2026-02-01

### 新增
- 新功能

### 修复
- Bug 修复

## [1.0.0] - 2026-01-16

### 新增
- 初始版本
```

**提取逻辑**:
```bash
cat CHANGELOG.md | awk '/^## \[/ {if (flag) exit; flag=1} flag {print}' > body.md
```

这会提取第一个以 `## [` 开头的版本块，直到下一个版本块为止。

## 示例工作流

### 开发流程

1. **创建功能分支**
   ```bash
   git checkout -b feature/new-crypto-engine
   ```

2. **开发并提交**
   ```bash
   git commit -m "feat(core): implement AES-256-GCM encryption"
   ```

3. **推送并创建 PR**
   ```bash
   git push origin feature/new-crypto-engine
   ```
   
   → 自动触发 `build.yml` 和 `security.yml`

4. **合并到 main**
   
   → 再次触发 `build.yml` 进行验证

### 发布流程

1. **更新版本号**
   
   修改 `gradle.properties`:
   ```properties
   version=1.0.0
   ```

2. **更新 CHANGELOG.md**
   
   ```markdown
   ## [1.0.0] - 2026-01-20
   
   ### 新增
   - 核心加密引擎
   - CLI 工具
   ```

3. **提交变更**
   ```bash
   git add gradle.properties CHANGELOG.md
   git commit -m "chore: prepare release 1.0.0"
   git push origin main
   ```

4. **创建 tag**
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

5. **自动发布**
   
   → 触发 `release.yml`
   → 在 https://github.com/ygqygq2/byteguard/releases 查看发布

## 本地测试

### 测试构建

```bash
# 完整构建
./gradlew build

# 跳过测试
./gradlew build -x test

# 生成覆盖率报告
./gradlew jacocoTestReport
```

### 测试发布构建

```bash
# 模拟发布版本构建
./gradlew build -Pversion=1.0.0

# 构建 Fat JAR
./gradlew :byteguard-cli:shadowJar -Pversion=1.0.0
```

### 代码质量检查

```bash
# Checkstyle
./gradlew checkstyleMain checkstyleTest

# SpotBugs
./gradlew spotbugsMain spotbugsTest

# 所有质量检查
./gradlew check
```

## 徽章

在 README.md 中已配置以下徽章：

- [![Build Status](https://github.com/ygqygy2/byteguard/workflows/Build%20and%20Test/badge.svg)](https://github.com/ygqygy2/byteguard/actions)
- [![codecov](https://codecov.io/gh/ygqygq2/byteguard/branch/main/graph/badge.svg)](https://codecov.io/gh/ygqygq2/byteguard)

## 故障排查

### Gradle 权限问题

如果遇到 `gradlew: Permission denied` 错误，所有 workflow 已配置：

```yaml
- name: Grant execute permission for gradlew
  run: chmod +x gradlew
```

### 缓存问题

如果需要清除 Gradle 缓存，在 workflow 中手动触发或删除：

```yaml
cache: 'gradle'  # 自动缓存
```

### 发布失败

检查：
1. Tag 格式是否正确（必须是 `v*.*.*`）
2. CHANGELOG.md 格式是否正确
3. Secrets 是否配置（如需发布到 Maven Central）
4. 构建是否成功

## 性能优化

所有 Gradle 命令使用 `--no-daemon` 避免 CI 环境中的守护进程问题：

```yaml
run: ./gradlew build --no-daemon
```

## 参考资料

- [GitHub Actions 文档](https://docs.github.com/en/actions)
- [Keep a Changelog](https://keepachangelog.com/)
- [Semantic Versioning](https://semver.org/)
- [Conventional Commits](https://www.conventionalcommits.org/)

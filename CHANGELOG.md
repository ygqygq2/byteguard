# Changelog

所有重要的项目变更都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [未发布]

### 计划功能
- 核心加密引擎实现
- JAR 加密器开发
- 运行时解密 ClassLoader
- 命令行工具
- Maven 插件

## [0.0.1] - 2026-01-16

### 新增
- 项目初始化和结构搭建
- 完整的架构设计文档 (architecture.md)
- 性能优化指南 (PERFORMANCE.md)
- 安全设计文档 (SECURITY.md)
- 贡献指南 (CONTRIBUTING.md)
- CI/CD 自动化流程
  - 多 JDK 版本测试（8, 11, 17, 21）
  - 跨平台测试（Ubuntu, Windows, macOS）
  - 代码质量检查（Checkstyle, SpotBugs）
  - 安全扫描（CodeQL, Trivy）
- Gradle 构建配置
- 代码质量工具集成（Jacoco, SonarQube）

### 文档
- README.md 项目说明
- 开发指南 (development.md)
- 项目设计总结 (PROJECT_DESIGN.md)
- 任务清单 (TODO.md)

---

[未发布]: https://github.com/ygqygq2/byteguard/compare/v0.0.1...HEAD
[0.0.1]: https://github.com/ygqygq2/byteguard/releases/tag/v0.0.1

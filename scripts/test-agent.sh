#!/usr/bin/env bash
# 测试 JavaAgent 端到端流程的自动化脚本

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BYTEGUARD_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "==> ByteGuard JavaAgent 测试"
echo ""

# 检查必要文件
if [[ ! -f "$BYTEGUARD_ROOT/license.lic" ]]; then
    echo "❌ license.lic 未找到"
    echo ""
    echo "请先生成 license:"
    echo "  cd ../byteguard-license-server"
    echo "  ./scripts/test-e2e.sh"
    exit 1
fi

if [[ ! -f "$BYTEGUARD_ROOT/simple-app-encrypted.jar" ]]; then
    echo "❌ simple-app-encrypted.jar 未找到"
    echo ""
    echo "请先加密测试应用"
    exit 1
fi

AGENT_JAR=$(ls "$BYTEGUARD_ROOT/byteguard-cli/build/libs/byteguard-cli-"*.jar 2>/dev/null | grep -v javadoc | grep -v sources | head -1)
if [[ -z "$AGENT_JAR" ]]; then
    echo "❌ ByteGuard Agent JAR 未找到"
    echo ""
    echo "请先构建 agent:"
    echo "  ./gradlew :byteguard-cli:jar"
    exit 1
fi

echo "✓ 文件检查完成"
echo "  License: $BYTEGUARD_ROOT/license.lic"
echo "  App: $BYTEGUARD_ROOT/simple-app-encrypted.jar"
echo "  Agent: $AGENT_JAR"
echo ""

# 显示 license 信息
echo "==> License 信息:"
head -5 "$BYTEGUARD_ROOT/license.lic"
echo "..."
echo ""

# 运行加密应用
echo "==> 运行加密应用（使用 GPG license）"
echo ""
echo "命令: java -javaagent:$AGENT_JAR=password=test123 -jar simple-app-encrypted.jar"
echo ""
echo "─────────────────────────────────────────"

cd "$BYTEGUARD_ROOT"
java -javaagent:"$AGENT_JAR"=password=test123 -jar simple-app-encrypted.jar

echo "─────────────────────────────────────────"
echo ""
echo "✅ JavaAgent 测试完成！"

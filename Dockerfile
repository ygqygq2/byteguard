# ByteGuard CLI Docker Image
# 用于在容器中执行 JAR 加密

FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="ygqygq2"
LABEL description="ByteGuard CLI - Java class encryption tool"
LABEL version="1.0.0-SNAPSHOT"

# 安装必要的工具
RUN apk add --no-cache bash

# 创建工作目录
WORKDIR /app

# 复制 byteguard-cli JAR
COPY byteguard-cli/build/libs/byteguard-cli-1.0.0-SNAPSHOT.jar /app/byteguard-cli.jar

# 创建输入输出目录
RUN mkdir -p /data/input /data/output

# 创建加密脚本
RUN cat > /app/encrypt.sh << 'EOF'
#!/bin/bash
set -euo pipefail

# 默认参数
INPUT_FILE="${INPUT_FILE:-/data/input/input.jar}"
OUTPUT_FILE="${OUTPUT_FILE:-/data/output/output.jar}"
PASSWORD="${PASSWORD:-}"
PACKAGES="${PACKAGES:-}"
EXCLUDES="${EXCLUDES:-}"

# 检查必需参数
if [[ -z "$PASSWORD" ]]; then
    echo "Error: PASSWORD environment variable is required" >&2
    exit 1
fi

if [[ ! -f "$INPUT_FILE" ]]; then
    echo "Error: Input file not found: $INPUT_FILE" >&2
    exit 1
fi

# 构建命令
CMD="java -jar /app/byteguard-cli.jar encrypt"
CMD="$CMD --input $INPUT_FILE"
CMD="$CMD --output $OUTPUT_FILE"
CMD="$CMD --password $PASSWORD"

if [[ -n "$PACKAGES" ]]; then
    CMD="$CMD --packages $PACKAGES"
fi

if [[ -n "$EXCLUDES" ]]; then
    CMD="$CMD --exclude $EXCLUDES"
fi

# 执行加密
echo "Executing encryption..."
eval "$CMD"

echo "Encryption completed successfully!"
EOF

RUN chmod +x /app/encrypt.sh

# 设置入口点
ENTRYPOINT ["/app/encrypt.sh"]

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD java -version || exit 1

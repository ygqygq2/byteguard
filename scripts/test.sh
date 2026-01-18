#!/usr/bin/env bash
#
# * @file            scripts/test.sh
# * @description     
# * @author          ygqygq2 <ygqygq2@qq.com>
# * @createTime      2026-01-16 21:50:25
# * @lastModified    2026-01-16 22:21:51
# * Copyright ©ygqygq2 All rights reserved
#

set -euo pipefail

readonly PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly BYTEGUARD_CORE="${PROJECT_ROOT}/byteguard-core"
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly NC='\033[0m'

function Log_Info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

function Log_Error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

function Print_Usage() {
    cat << 'USAGE'
使用方法: ./test.sh [选项]

选项:
  build           构建项目
  unit            运行单元测试
  integration     运行集成测试
  all             构建 + 单元测试 + 集成测试
  clean           清理构建产物
  help            显示帮助信息

示例:
  ./test.sh all          # 运行所有测试
  ./test.sh integration  # 仅运行集成测试
USAGE
}

function Clean_Build() {
    Log_Info "清理构建产物..."
    cd "${BYTEGUARD_CORE}"
    rm -rf build/classes build/*.jar build/libs
    Log_Info "清理完成"
}

function Build_Project() {
    Log_Info "开始构建 ByteGuard..."
    cd "${BYTEGUARD_CORE}"
    
    local gradle_cmd
    if [ -f "${PROJECT_ROOT}/gradlew" ]; then
        gradle_cmd="${PROJECT_ROOT}/gradlew"
    elif command -v gradle &> /dev/null; then
        gradle_cmd="gradle"
    else
        Log_Error "未找到 Gradle，请先安装: https://gradle.org"
        return 1
    fi
    
    $gradle_cmd build -x test || return 1
    
    [ -f build/libs/byteguard-core-1.0.0-SNAPSHOT.jar ] || {
        Log_Error "JAR 创建失败"
        return 1
    }
    
    local jar_size
    jar_size=$(du -h build/libs/byteguard-core-1.0.0-SNAPSHOT.jar | cut -f1)
    Log_Info "构建成功: byteguard-core.jar (${jar_size})"
}

function Run_Unit_Tests() {
    Log_Info "运行单元测试..."
    cd "${BYTEGUARD_CORE}"
    
    local gradle_cmd
    if [ -f "${PROJECT_ROOT}/gradlew" ]; then
        gradle_cmd="${PROJECT_ROOT}/gradlew"
    elif command -v gradle &> /dev/null; then
        gradle_cmd="gradle"
    else
        Log_Error "未找到 Gradle"
        return 1
    fi
    
    $gradle_cmd test -x intTest || return 1
    Log_Info "单元测试完成"
}

function Run_Integration_Tests() {
    Log_Info "运行集成测试..."
    cd "${BYTEGUARD_CORE}"
    
    local gradle_cmd
    if [ -f "${PROJECT_ROOT}/gradlew" ]; then
        gradle_cmd="${PROJECT_ROOT}/gradlew"
    elif command -v gradle &> /dev/null; then
        gradle_cmd="gradle"
    else
        Log_Error "未找到 Gradle"
        return 1
    fi
    
    $gradle_cmd intTest || return 1
    Log_Info "集成测试完成"
}

function Main() {
    [ -d "${PROJECT_ROOT}/byteguard-core" ] || {
        Log_Error "项目目录不存在: ${PROJECT_ROOT}/byteguard-core"
        return 1
    }
    
    case "${1:-help}" in
        build)
            Build_Project
            ;;
        unit)
            Build_Project && Run_Unit_Tests
            ;;
        integration)
            Build_Project && Run_Integration_Tests
            ;;
        all)
            Build_Project && Run_Unit_Tests && Run_Integration_Tests
            Log_Info "所有测试通过! ✓"
            ;;
        clean)
            Clean_Build
            ;;
        help)
            Print_Usage
            ;;
        *)
            Log_Error "未知的选项: $1"
            Print_Usage
            return 1
            ;;
    esac
}

Main "$@"

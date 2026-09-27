#!/usr/bin/env bash
# ---------------------------------------------------------------
#  启动 Controlify 1.20.1 Forge 开发客户端（Linux / macOS）
#  首次启动会下载资源与原生库，耗时较长。
# ---------------------------------------------------------------
set -euo pipefail

cd "$(dirname "$0")"

echo "[1/3] 检查 Java 版本..."
if ! java -version 2>&1 | grep -q 'version "21'; then
    echo
    echo "[错误] 未检测到 JDK 21。详见 README-DEV.md 第 1 节。"
    echo
    exit 1
fi
echo "      OK"

echo "[2/3] 设置构建目标为 1.20.1-forge ..."
printf '1.20.1-forge\n' > versions/current

echo "[3/3] 启动客户端..."
echo
if ./gradlew runClientActive "$@"; then
    :
else
    echo
    echo "[失败] 客户端未正常退出。首次运行请确认网络可用"
    echo "       （需下载 Minecraft 资源与 SDL 原生库）。"
    exit 1
fi

#!/usr/bin/env bash
# ---------------------------------------------------------------
#  构建 Controlify 1.20.1 Forge 版本（Linux / macOS）
#  产物: build/finalJars/controlify-2.0.3+1.20.1-forge.jar
#        build/finalJars/controlify-2.0.3+1.20.1-forge-offline.jar
# ---------------------------------------------------------------
set -euo pipefail

cd "$(dirname "$0")"

echo "[1/3] 检查 Java 版本..."
if ! java -version 2>&1 | grep -q 'version "21'; then
    echo
    echo "[错误] 未检测到 JDK 21。"
    echo "       请安装 JDK 21 并设置 JAVA_HOME 后重试，例如："
    echo "         export JAVA_HOME=/usr/lib/jvm/zulu21"
    echo "         export PATH=\"\$JAVA_HOME/bin:\$PATH\""
    echo "       详见 README-DEV.md 第 1 节。"
    echo
    exit 1
fi
echo "      OK"

echo "[2/3] 设置构建目标为 1.20.1-forge ..."
printf '1.20.1-forge\n' > versions/current

echo "[3/3] 开始构建（首次构建较慢，请耐心等待）..."
echo
if ./gradlew chiseledBuildAndCollect "$@"; then
    echo
    echo "[成功] 产物位于 build/finalJars/"
    ls -1 build/finalJars/*.jar 2>/dev/null || true
else
    echo
    echo "[失败] 构建未通过。常见原因见 README-DEV.md 第 4 节。"
    exit 1
fi

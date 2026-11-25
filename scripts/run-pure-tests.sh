#!/bin/sh
# 纯函数断言：把不依赖设备的类编到 JVM 上跑一遍。
#
# 为什么单独一个脚本：这些逻辑错了不会崩、不会报错，只会在真机上「显示得不对」，
# 装机验证很难发现；放在构建前跑，几秒钟就能拦住。
#
# 用法：sh scripts/run-pure-tests.sh
set -e

ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"

SRC=src/io/github/vivibibi30/hyperpguard
OUT=build/pure-test
STUB=android-stub.jar

if [ ! -f "$STUB" ]; then
  echo "缺少 $STUB（编译期 Android API 桩）" >&2
  exit 2
fi

rm -rf "$OUT"
mkdir -p "$OUT"

javac -encoding UTF-8 -nowarn -cp "$STUB" -d "$OUT" \
  "$SRC/Logs.java" \
  "$SRC/Shell.java" \
  "$SRC/LogTools.java" \
  scripts/PureTest.java

java -cp "$OUT:$STUB" io.github.vivibibi30.hyperpguard.PureTest

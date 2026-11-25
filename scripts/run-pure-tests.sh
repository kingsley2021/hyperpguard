#!/bin/sh
# 纯函数断言：把不依赖设备的类编到 JVM 上跑一遍。
#
# 为什么单独一个脚本：这些逻辑错了不会崩、不会报错，只会在真机上「显示得不对」，
# 装机验证很难发现；放在构建前跑，几秒钟就能拦住。
#
# 依赖：只需要 JDK（CI 在 Set up JDK 之后跑）。Android API 用一个最小桩代替，
# 不依赖仓库里的 android-stub.jar —— 那个文件不入库。
#
# 用法：sh scripts/run-pure-tests.sh
set -e

ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"

SRC=src/io/github/vivibibi30/hyperpguard
OUT=build/pure-test
STUB_SRC="$OUT/stub-src"

rm -rf "$OUT"
mkdir -p "$OUT" "$STUB_SRC/android/util"

cat > "$STUB_SRC/android/util/Log.java" <<'EOF'
package android.util;

/** 只给纯函数测试用的最小桩：被调到时什么也不做 */
public final class Log {
    private Log() {
    }

    public static int v(String tag, String msg) { return 0; }
    public static int d(String tag, String msg) { return 0; }
    public static int i(String tag, String msg) { return 0; }
    public static int w(String tag, String msg) { return 0; }
    public static int e(String tag, String msg) { return 0; }
}
EOF

javac -encoding UTF-8 -nowarn -d "$OUT" "$STUB_SRC/android/util/Log.java"

javac -encoding UTF-8 -nowarn -cp "$OUT" -d "$OUT" \
  "$SRC/Logs.java" \
  "$SRC/Shell.java" \
  "$SRC/LogTools.java" \
  scripts/PureTest.java

java -cp "$OUT" io.github.vivibibi30.hyperpguard.PureTest

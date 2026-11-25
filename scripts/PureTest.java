package io.github.vivibibi30.hyperpguard;

/**
 * 不依赖设备的断言：跑在 JVM 上，只碰纯函数与字符串拼装。
 *
 * 覆盖两块最容易悄悄坏掉、又最难在真机上发现的逻辑：
 * - {@link Shell#q(String)} 的参数转义（错了会拼出第二条命令，界面上看不出来）
 * - {@link LogTools} 的统计／分类／注解（错了日志页只是显示得不对，不会报错）
 *
 * 由 scripts/run-pure-tests.sh 编译运行，CI 在构建前跑一遍。
 */
public final class PureTest {

    private static int failed = 0;
    private static int passed = 0;

    public static void main(String[] args) {
        // ---- Shell.q：单引号转义 ----
        eq("q(null)", "''", Shell.q(null));
        eq("q(空)", "''", Shell.q(""));
        eq("q(普通)", "'abc'", Shell.q("abc"));
        eq("q(带空格)", "'a b'", Shell.q("a b"));
        eq("q(单引号)", "'a'\\''b'", Shell.q("a'b"));
        eq("q(双引号保留)", "'say \"hi\"'", Shell.q("say \"hi\""));
        eq("q(分号不被拆)", "'x; rm -rf y'", Shell.q("x; rm -rf y"));
        eq("q(反引号)", "'$(id)'", Shell.q("$(id)"));
        eq("q(换行)", "'a\nb'", Shell.q("a\nb"));

        // ---- LogTools.blockStats ----
        eq("blockStats(空)", "", LogTools.blockStats(""));
        String log = "E HyperPGuard: blocked by HyperPGuard: tracking.miui.com /p\n"
                + "E HyperPGuard: blocked by HyperPGuard: tracking.miui.com /q\n"
                + "E HyperPGuard: blocked by HyperPGuard: data.sec.miui.com\n"
                + "W HyperPGuard: hook miss Analytics\n";
        String stats = LogTools.blockStats(log);
        contains("blockStats 计数", stats, "tracking.miui.com × 2");
        contains("blockStats 第二项", stats, "data.sec.miui.com × 1");
        contains("blockStats miss", stats, "hook miss：1 条");

        // ---- LogTools.classify ----
        String mixed = "I HyperPGuard: blocked by HyperPGuard: a.com\n"
                + "I HyperPGuard: ? cdn.example/a.png\n"
                + "I HyperPGuard: attach com.foo\n";
        String cls = LogTools.classify(mixed);
        contains("classify 已拦截段", cls, "已拦截（共 1 条）");
        contains("classify 未拦截段", cls, "未拦截（共 1 条）");
        contains("classify 模块日志段", cls, "模块日志（共 1 条）");

        // ---- LogTools.hintFor ----
        eq("hintFor 无目标", null, LogTools.hintFor("plain line"));
        eq("hintFor 命中提示",
                "统计埋点", LogTools.hintFor("I HyperPGuard: ? GET tracking.miui.com/x"));
        eq("hintFor 未收录", null, LogTools.hintFor("I HyperPGuard: ? GET unknown.example/x"));

        // ---- LogTools.section 截断 ----
        java.util.List<String> many = new java.util.ArrayList<>();
        for (int i = 1; i <= 25; i++) many.add("line" + i);
        String sec = LogTools.section("样例", many, null);
        contains("section 计数", sec, "样例（共 25 条，显示最近 20 条）");
        contains("section 末行", sec, "line25");
        eq("section 空", "", LogTools.section("空", new java.util.ArrayList<>(), null));

        System.out.println("通过 " + passed + " 项，失败 " + failed + " 项");
        if (failed > 0) System.exit(1);
    }

    private static void eq(String name, String expect, String actual) {
        if (expect == null ? actual == null : expect.equals(actual)) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name + "\n  期望: " + expect + "\n  实际: " + actual);
        }
    }

    private static void contains(String name, String haystack, String needle) {
        if (haystack != null && haystack.contains(needle)) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name + "：找不到「" + needle + "」\n  实际: " + haystack);
        }
    }
}

package io.github.vivibibi30.hyperpguard;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * root 命令的唯一出口。
 *
 * 一次调用 = 一次 `su -c <命令>`。拼接参数一律走 {@link #q(String)}，
 * 避免包名、路径、配置值里带空格或引号时拼出第二条命令。
 *
 * 超时与输出上限都在这里定：调用方只关心「退出码 + 输出」，
 * 不再各自 new Process / 各自写 while (readLine)。
 *
 * 约定：
 * - stdout 收集到结果里；stderr 单独起线程抽干，既不让管道阻塞，也不混进调用方要解析的文本
 *   （自检页要 grep 的行来自 stdout，混进 su 自己的提示会误判）
 * - 超时销毁进程，退出码返回 {@link #TIMEOUT}
 * - 连 su 都起不来返回 {@link #NO_SU}
 */
final class Shell {

    /** 进程超时（秒）。root 命令都是 cp / am / logcat 一类，20 秒足够 */
    static final int TIMEOUT_SEC = 20;
    /** 单次输出上限，防止 cat 大文件把内存吃满 */
    static final int MAX_OUT = 256 * 1024;

    /** 超时：进程被销毁 */
    static final int TIMEOUT = -1;
    /** 起不来：没有 su 或没有授权 */
    static final int NO_SU = -2;

    private Shell() {
    }

    /** 单引号转义，把任意字符串变成可安全放进 sh 命令的一个参数 */
    static String q(String s) {
        if (s == null) return "''";
        return "'" + s.replace("'", "'\\''") + "'";
    }

    static final class Result {
        final int code;
        final String out;

        Result(int code, String out) {
            this.code = code;
            this.out = out == null ? "" : out;
        }

        boolean ok() {
            return code == 0;
        }
    }

    /** 执行并等待，丢弃输出，只返回退出码 */
    static int run(String cmd) {
        return exec(cmd).code;
    }

    /** 执行并返回 stdout；失败时返回「执行失败：…」，与旧的 execShell 行为一致 */
    static String out(String cmd) {
        Result r = exec(cmd);
        return r.out;
    }

    static Result exec(String cmd) {
        Process p = null;
        StringBuilder sb = new StringBuilder();
        try {
            p = new ProcessBuilder("su", "-c", cmd).start();
            final Process proc = p;
            Thread drain = new Thread(() -> {
                try (InputStream err = proc.getErrorStream()) {
                    byte[] buf = new byte[512];
                    while (err.read(buf) != -1) {
                        // 读空即可，内容不参与判断
                    }
                } catch (Throwable ignored) {
                }
            }, "hyperpguard-su-err");
            drain.setDaemon(true);
            drain.start();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                char[] buf = new char[1024];
                int n;
                while ((n = br.read(buf)) != -1) {
                    if (sb.length() < MAX_OUT) sb.append(buf, 0, n);
                }
            }

            if (!p.waitFor(TIMEOUT_SEC, java.util.concurrent.TimeUnit.SECONDS)) {
                p.destroy();
                Logs.i("su timeout: " + cmd);
                return new Result(TIMEOUT, sb.toString());
            }
            return new Result(p.exitValue(), sb.toString());
        } catch (Throwable t) {
            Logs.i("su failed: " + cmd + " -> " + t);
            return new Result(NO_SU, "执行失败：" + t);
        } finally {
            if (p != null) {
                try {
                    p.destroy();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}

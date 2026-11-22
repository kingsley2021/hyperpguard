package io.github.vivibibi30.hyperpguard;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MiSight 事件库的只读快照。
 *
 * 系统把库放在 /data/miuilog/misight/event.db（root:system 0640），
 * 模块进程没有权限，所以先用 su 复制到自身 cache 再只读打开。
 * 复制的是快照：不改原库，查完即删。
 *
 * 表结构（HyperOS 2/3 实测）：
 *   event_info   每次事件的明细（event_id、occured_cnt、first_time、param_list）
 *   event_daily  按事件聚合（daily_cnt、over_cnt、last_happen_time）
 *   upload       上传记录（有行说明已有数据出过本机）
 *   version      库版本
 */
final class Misight {

    static final String SRC_DB = "/data/miuilog/misight/event.db";

    private Misight() {
    }

    static final class Event {
        long id;
        long count;
        long first;
        long last;
        String sample;
    }

    static final class Snapshot {
        boolean ok;
        String error;
        String version = "";
        long records;
        long hits;
        long uploads;
        long lastTime;
        long bytes;
        final List<Event> top = new ArrayList<>();
        /** 每次读取都记一句，失败时界面能说清卡在哪一步 */
        String stage = "";
    }

    /** 同步读取；调用方放后台线程。耗时最多几秒（12MB 复制）。 */
    static Snapshot load(Context ctx) {
        Snapshot s = new Snapshot();
        File dir = new File(ctx.getCacheDir(), "misight");
        File db = new File(dir, "event.db");
        File wal = new File(dir, "event.db-wal");
        File shm = new File(dir, "event.db-shm");

        SQLiteDatabase d = null;
        try {
            s.stage = "准备缓存目录";
            if (!dir.exists() && !dir.mkdirs()) {
                return fail(s, "无法创建缓存目录");
            }
            db.delete();
            wal.delete();
            shm.delete();

            s.stage = "请求 root 复制数据库";
            String cmd = "cp -f " + SRC_DB + " " + db.getAbsolutePath()
                    + "; cp -f " + SRC_DB + "-wal " + wal.getAbsolutePath() + " 2>/dev/null"
                    + "; cp -f " + SRC_DB + "-shm " + shm.getAbsolutePath() + " 2>/dev/null"
                    + "; true";
            Process p;
            try {
                p = new ProcessBuilder("su", "-c", cmd)
                        .redirectErrorStream(true).start();
            } catch (Throwable t) {
                return fail(s, "没有可用的 su（需要 root 授权）");
            }
            try {
                InputStream in = p.getInputStream();
                byte[] buf = new byte[512];
                while (in.read(buf) != -1) {
                    // su 的输出（授权提示等）不影响结果，读空避免管道阻塞
                }
                if (!p.waitFor(20, TimeUnit.SECONDS)) {
                    p.destroy();
                    return fail(s, "su 超时，请确认已允许本应用获取 root");
                }
            } finally {
                try {
                    p.destroy();
                } catch (Throwable ignored) {
                }
            }
            if (!db.isFile() || db.length() == 0) {
                return fail(s, "复制失败：数据库不存在或没有读权限");
            }
            s.bytes = db.length();

            s.stage = "打开快照";
            d = SQLiteDatabase.openDatabase(db.getPath(), null,
                    SQLiteDatabase.OPEN_READONLY);
            s.version = oneString(d, "select version from version order by id desc limit 1");

            s.stage = "统计事件";
            long[] rec = oneRow(d,
                    "select count(*), coalesce(sum(occured_cnt),0) from event_info");
            s.records = rec[0];
            s.hits = rec[1];
            s.uploads = oneRow(d, "select count(*) from upload")[0];
            s.lastTime = oneRow(d,
                    "select coalesce(max(last_happen_time),0) from event_daily")[0];

            s.stage = "读取明细";
            Cursor c = d.rawQuery(
                    "select event_id, count(*), coalesce(sum(occured_cnt),0),"
                            + " min(first_time), max(first_time), param_list"
                            + " from event_info group by event_id"
                            + " order by 3 desc limit 40", null);
            try {
                while (c.moveToNext()) {
                    Event e = new Event();
                    e.id = c.getLong(0);
                    e.count = c.getLong(2);
                    e.first = c.getLong(3);
                    e.last = c.getLong(4);
                    e.sample = c.isNull(5) ? "" : c.getString(5);
                    s.top.add(e);
                }
            } finally {
                c.close();
            }
            s.ok = true;
            return s;
        } catch (Throwable t) {
            return fail(s, String.valueOf(t));
        } finally {
            if (d != null) {
                try {
                    d.close();
                } catch (Throwable ignored) {
                }
            }
            // 12MB 副本不留在缓存里
            db.delete();
            wal.delete();
            shm.delete();
        }
    }

    private static Snapshot fail(Snapshot s, String why) {
        s.ok = false;
        s.error = (s.stage.isEmpty() ? "" : s.stage + "：") + why;
        return s;
    }

    private static String oneString(SQLiteDatabase d, String sql) {
        Cursor c = d.rawQuery(sql, null);
        try {
            return c.moveToFirst() && !c.isNull(0) ? c.getString(0) : "";
        } finally {
            c.close();
        }
    }

    private static long[] oneRow(SQLiteDatabase d, String sql) {
        Cursor c = d.rawQuery(sql, null);
        try {
            if (!c.moveToFirst()) return new long[]{0};
            long[] v = new long[c.getColumnCount()];
            for (int i = 0; i < v.length; i++) v[i] = c.isNull(i) ? 0 : c.getLong(i);
            return v;
        } finally {
            c.close();
        }
    }
}

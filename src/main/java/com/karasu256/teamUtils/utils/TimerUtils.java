package com.karasu256.teamUtils.utils;

import com.karasu256.teamUtils.TeamUtils;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * ゲーム内のタイマー機能を提供するユーティリティクラス
 */
public class TimerUtils {
    private static final Logger LOGGER = TeamUtils.LOGGER;
    private static BukkitTask timerTask;
    private static int remainingTicks;
    private static boolean isRunning = false;

    // 1ティック = 0.05秒、20ティック = 1秒
    public static final int TICKS_PER_SECOND = 20;

    // 1ティックごとに実行されるコールバック
    private static final List<Consumer<Integer>> tickCallbacks = new ArrayList<>();

    // 特定の時間範囲で実行されるコールバック
    private static final Map<Integer, List<Consumer<Integer>>> rangeCallbacks = new HashMap<>();

    /**
     * 指定したティック数のタイマーを開始します
     * 
     * @param ticks タイマーのティック数
     * @return タイマーの開始に成功した場合true
     */
    public static boolean start(int ticks) {
        if (isRunning) {
            LOGGER.warning("Timer is already running");
            return false;
        }

        if (ticks <= 0) {
            LOGGER.warning("Timer ticks must be greater than 0");
            return false;
        }

        remainingTicks = ticks;
        isRunning = true;

        timerTask = Bukkit.getScheduler().runTaskTimer(TeamUtils.getPlugin(), () -> {
            if (remainingTicks <= 0) {
                stop();
                return;
            }

            // 1ティックごとのコールバックを実行
            for (Consumer<Integer> callback : tickCallbacks) {
                try {
                    callback.accept(remainingTicks);
                } catch (Exception e) {
                    LOGGER.warning("Error executing timer tick callback: " + e.getMessage());
                }
            }

            // 範囲コールバックがあれば実行
            if (rangeCallbacks.containsKey(remainingTicks)) {
                for (Consumer<Integer> callback : rangeCallbacks.get(remainingTicks)) {
                    try {
                        callback.accept(remainingTicks);
                    } catch (Exception e) {
                        LOGGER.warning("Error executing timer range callback: " + e.getMessage());
                    }
                }
            }

            remainingTicks--;
        }, 0L, 1L); // 1 tick間隔で実行

        LOGGER.info("Timer started with " + ticks + " ticks (" + (ticks / TICKS_PER_SECOND) + " seconds)");
        return true;
    }

    /**
     * 指定した秒数のタイマーを開始します（ティックに変換されます）
     * 
     * @param seconds タイマーの秒数
     * @return タイマーの開始に成功した場合true
     */
    public static boolean startInSeconds(int seconds) {
        return start(seconds * TICKS_PER_SECOND);
    }

    /**
     * 実行中のタイマーを停止します
     * 
     * @return タイマーの停止に成功した場合true
     */
    public static boolean stop() {
        if (!isRunning) {
            LOGGER.warning("Timer is not running");
            return false;
        }

        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }

        isRunning = false;
        LOGGER.info("Timer stopped");
        return true;
    }

    /**
     * タイマーが実行中かどうかを取得します
     * 
     * @return タイマーが実行中の場合true
     */
    public static boolean isRunning() {
        return isRunning;
    }

    /**
     * 残り時間をティックで取得します
     * 
     * @return 残り時間（ティック）
     */
    public static int getRemainingTicks() {
        return remainingTicks;
    }

    /**
     * 残り時間を秒で取得します
     * 
     * @return 残り時間（秒）
     */
    public static int getRemainingSeconds() {
        return remainingTicks / TICKS_PER_SECOND;
    }

    /**
     * ティックを秒に変換します
     * 
     * @param ticks 変換するティック数
     * @return 変換された秒数
     */
    public static int ticksToSeconds(int ticks) {
        return ticks / TICKS_PER_SECOND;
    }

    /**
     * 秒をティックに変換します
     * 
     * @param seconds 変換する秒数
     * @return 変換されたティック数
     */
    public static int secondsToTicks(int seconds) {
        return seconds * TICKS_PER_SECOND;
    }

    /**
     * 残り時間を「hh時間 mm分 ss秒」の形式でフォーマットします
     * 0の単位は省略されます
     * 
     * @param ticks フォーマットするティック数
     * @return フォーマットされた時間文字列
     */
    public static String formatTime(int ticks) {
        int seconds = ticksToSeconds(ticks);

        if (seconds < 0) {
            return "0秒";
        }

        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        StringBuilder sb = new StringBuilder();

        if (hours > 0) {
            sb.append(hours).append("時間 ");
        }

        if (minutes > 0) {
            sb.append(minutes).append("分 ");
        }

        if (secs > 0 || (hours == 0 && minutes == 0)) {
            sb.append(secs).append("秒");
        }

        return sb.toString().trim();
    }

    /**
     * 残り時間をフォーマットして取得します
     * 
     * @return フォーマットされた残り時間
     */
    public static String getFormattedRemainingTime() {
        return formatTime(remainingTicks);
    }

    /**
     * 1ティックごとに実行されるコールバックを追加します
     * 
     * @param callback 残りティック数を引数に取るコールバック
     */
    public static void addTickCallback(Consumer<Integer> callback) {
        if (callback != null) {
            tickCallbacks.add(callback);
        }
    }

    /**
     * 1ティックごとに実行されるコールバックをすべて削除します
     */
    public static void clearTickCallbacks() {
        tickCallbacks.clear();
    }

    /**
     * 特定の時間で実行されるコールバックを追加します
     * 
     * @param second   実行する残り時間（ティック）
     * @param callback 残り時間を引数に取るコールバック
     */
    public static void addRangeCallback(int second, Consumer<Integer> callback) {
        if (callback != null) {
            rangeCallbacks.computeIfAbsent(second, k -> new ArrayList<>()).add(callback);
        }
    }

    /**
     * 特定の時間範囲で実行されるコールバックを追加します
     * 
     * @param startSecond 範囲の開始時間（ティック）
     * @param endSecond   範囲の終了時間（ティック）
     * @param callback    残り時間を引数に取るコールバック
     */
    public static void addRangeCallback(int startSecond, int endSecond, Consumer<Integer> callback) {
        if (callback == null || startSecond < 0 || endSecond < 0 || startSecond < endSecond) {
            return;
        }

        for (int i = endSecond; i <= startSecond; i++) {
            addRangeCallback(i, callback);
        }
    }

    /**
     * 特定の時間範囲のコールバックをすべて削除します
     */
    public static void clearRangeCallbacks() {
        rangeCallbacks.clear();
    }

    /**
     * すべてのコールバックを削除します
     */
    public static void clearAllCallbacks() {
        clearTickCallbacks();
        clearRangeCallbacks();
    }
}

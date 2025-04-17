package com.karasu256.teamUtils.utils;

import com.karasu256.teamUtils.TeamUtils;
import com.karasu256.teamUtils.config.GameConfig;
import java.util.logging.Logger;

/**
 * ゲーム全体の状態管理を行うユーティリティクラス
 */
public class GameUtils {
    private static final Logger LOGGER = TeamUtils.LOGGER;
    private static boolean isGameRunning = false;

    /**
     * 現在のゲーム実行状態を初期化します。
     * プラグインの設定ファイルから状態を読み込みます。
     */
    public static void initialize() {
        TeamUtils.getPlugin().load();
        GameConfig gameConfig = TeamUtils.getPlugin().getConfig(GameConfig.class);
        isGameRunning = gameConfig.isGameRunning();

        // TeamUtilityと状態を同期する
        TeamUtility.setGameRunning(isGameRunning);
    }

    /**
     * ゲームの実行状態を設定します。
     * 状態は設定ファイルに保存され、TeamUtilityとも同期されます。
     * 
     * @param running ゲームが実行中かどうか
     */
    public static void setGameRunning(boolean running) {
        if (isGameRunning == running) {
            // 状態が変わらない場合は何もしない
            return;
        }

        isGameRunning = running;
        LOGGER.info("Game state changed to: " + (running ? "Running" : "Not running"));

        // TeamUtilityと状態を同期する
        TeamUtility.setGameRunning(running);

        // 設定ファイルに状態を保存
        saveGameState();
    }

    /**
     * ゲームが実行中かどうかを取得します。
     * 
     * @return ゲームが実行中の場合true
     */
    public static boolean isGameRunning() {
        return isGameRunning;
    }

    /**
     * ゲームの状態を設定ファイルに保存します。
     */
    private static void saveGameState() {
        GameConfig gameConfig = TeamUtils.getPlugin().getConfig(GameConfig.class);;
        gameConfig.setGameRunning(isGameRunning);
        TeamUtils.getPlugin().saveConfig("game_config.json");
        LOGGER.info("Game state saved to config: " + (isGameRunning ? "Running" : "Not running"));
    }

    /**
     * ゲームを開始します。
     * 
     * @return 開始処理が成功した場合true
     */
    public static boolean startGame() {
        if (isGameRunning) {
            LOGGER.info("Game is already running");
            return false;
        }

        setGameRunning(true);
        return true;
    }

    /**
     * ゲームを終了します。
     */
    public static void endGame() {
        if (!isGameRunning) {
            LOGGER.info("Game is not running");
            return;
        }

        setGameRunning(false);

        // タイマーが実行中の場合は停止する
        if (TimerUtils.isRunning()) {
            TimerUtils.stop();
            TimerUtils.clearAllCallbacks();
        }

        // リスポーン待機とゲームモード待機をクリア
        TeamUtility.clearRespawnCallbacks();
        TeamUtility.clearGameModeCallbacks();

    }
}

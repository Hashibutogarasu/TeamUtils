package com.karasu256.teamUtils.config;

import com.karasu256.karasuConfigLib.annotation.Config;
import com.karasu256.teamUtils.TeamUtils;
import org.bukkit.GameMode;

/**
 * ゲームに関する設定を保存するクラスです。
 */
@Config(fileName = "game_config.json", pluginName = TeamUtils.PLUGIN_NAME)
public class GameConfig extends AbstractPluginBaseConfig{
    /**
     * 現在のゲームモード
     */
    public GameMode gameMode = GameMode.SURVIVAL;

    /**
     * チーム最大人数（デフォルト：4）
     */
    public int maxTeamMembers = 4;

    /**
     * チームリーダーの最大数（デフォルト：1）
     */
    public int maxTeamLeaders = 1;

    /**
     * ゲーム実行中かどうか
     */
    public boolean isGameRunning = false;

    /**
     * デフォルトコンストラクタ
     */
    public GameConfig() {

    }

    /**
     * ゲームモードを取得します。
     * 
     * @param gameMode ゲームモード
     */
    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    /**
     * ゲームが実行中かどうかを取得します。
     * 
     * @param isGameRunning ゲームが実行中かどうか
     */
    public void setGameRunning(boolean isGameRunning) {
        this.isGameRunning = isGameRunning;
    }

    /**
     * チームリーダーの最大数を設定します。
     * 
     * @param maxTeamLeaders 最大数
     */
    public void setMaxTeamLeaders(int maxTeamLeaders) {
        this.maxTeamLeaders = maxTeamLeaders;
    }

    /**
     * チームの最大人数を設定します。
     * 
     * @param maxTeamMembers 最大人数
     */
    public void setMaxTeamMembers(int maxTeamMembers) {
        this.maxTeamMembers = maxTeamMembers;
    }

    /**
     * ゲームが実行中かどうかを取得します。
     * 
     * @return ゲームが実行中の場合true
     *         ゲームが実行中でない場合false
     */
    public boolean isGameRunning() {
        return isGameRunning;
    }
}

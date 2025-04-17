package com.karasu256.teamUtils.listeners;

import com.karasu256.teamUtils.TeamUtils;
import com.karasu256.teamUtils.utils.GameUtils;
import com.karasu256.teamUtils.utils.TeamUtility;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * プレイヤーのゲームモード変更を監視するリスナークラス
 */
public class GameModeChangeListener implements Listener {
    private static final Logger LOGGER = TeamUtils.LOGGER;

    /**
     * プレイヤーのゲームモード変更イベントを処理します
     * 
     * @param event ゲームモード変更イベント
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        TeamUtility.handleGameModeChange(player, event.getNewGameMode());
    }

    /**
     * プレイヤーのログインイベントを処理します
     * 
     * @param event プレイヤーのログインイベント
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // ログイン時に現在のゲームモードでチェックを行う
        TeamUtility.handleGameModeChange(player, player.getGameMode());
    }

    /**
     * プレイヤーのログアウトイベントを処理します
     * オフラインのプレイヤーの待機をクリアする
     * 
     * @param event プレイヤーのログアウトイベント
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        // ゲームモード待機のコールバックをクリア
        if (GameUtils.isGameRunning()) {
            // ゲームモード変更の待機リストから削除
            TeamUtility.removeGameModeCallback(playerUuid);
        }
    }
}

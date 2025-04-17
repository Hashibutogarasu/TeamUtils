package com.karasu256.teamUtils.listeners;

import com.karasu256.teamUtils.TeamUtils;
import com.karasu256.teamUtils.utils.TeamUtility;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.Bukkit;

import java.util.logging.Logger;

/**
 * ゲーム中のプレイヤー参加・退出を処理するリスナークラス
 */
public class GameJoinQuitListener implements Listener {
    private static final Logger LOGGER = TeamUtils.LOGGER;

    /**
     * プレイヤーがサーバーに参加した時の処理
     * ゲーム中の場合、自動的に観戦チームに割り当てる
     *
     * @param event プレイヤー参加イベント
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // ゲームが実行中かチェック
        if (TeamUtility.isGameRunning()) {
            // 観戦チームを取得
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team spectatorTeam = scoreboard.getTeam("spectator");

            if (spectatorTeam != null && !spectatorTeam.hasEntry(player.getName())) {
                // プレイヤーが既に他のチームに所属していないことを確認
                Team currentTeam = scoreboard.getEntryTeam(player.getName());
                if (currentTeam != null) {
                    currentTeam.removeEntry(player.getName());
                }

                // 観戦チームに追加
                spectatorTeam.addEntry(player.getName());
                player.sendMessage("ゲームが進行中のため、観戦チームに追加されました。");
                LOGGER.info(String.format("Player %s joined during active game and was added to spectator team",
                        player.getName()));

                // チームサイズを更新
                TeamUtility.updateTeamSize();
            }
        }
    }

    /**
     * プレイヤーがサーバーから退出した時の処理
     * チームからプレイヤーを削除し、必要に応じてリーダーを再選出
     *
     * @param event プレイヤー退出イベント
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // ゲームが実行中かチェック
        if (TeamUtility.isGameRunning()) {
            // TeamUtilityのhandlePlayerQuitを呼び出して処理
            TeamUtility.handlePlayerQuit(player);
        }
    }
}

package com.karasu256.teamUtils.listeners;

import com.karasu256.teamUtils.TeamUtils;
import com.karasu256.teamUtils.utils.TeamUtility;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.logging.Logger;

public class PlayerListener implements Listener {
    private static final Logger LOGGER = TeamUtils.LOGGER;

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        TeamUtility.handlePlayerDeath(player);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        TeamUtility.handlePlayerRespawn(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        TeamUtility.handlePlayerQuit(player);
    }
}

package com.karasu256.teamUtils.command.subcommand;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import com.karasu256.kcapi.api.command.AbstractEndOfSubCommand;
import com.karasu256.kcapi.api.command.AbstractSubCommand;
import com.karasu256.kcapi.api.command.ICommand;
import com.karasu256.teamUtils.utils.LocationUtils;

public class TestSubCommand extends AbstractSubCommand {
    public TestSubCommand(ICommand parent) {
        super("test", parent);
        addSubCommand(new TestPlayerLocationRefleshSubCommand(this));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        return false;
    }

    private class TestPlayerLocationRefleshSubCommand extends AbstractEndOfSubCommand {
        public TestPlayerLocationRefleshSubCommand(ICommand parent) {
            super("locationreflesh", parent);
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
            return LocationUtils.teleportTeamLeadersToSafeLocations() > 0;
        }
    }
}

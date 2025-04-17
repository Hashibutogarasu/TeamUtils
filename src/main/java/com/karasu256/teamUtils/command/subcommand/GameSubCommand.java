package com.karasu256.teamUtils.command.subcommand;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import com.karasu256.kcapi.api.command.AbstractEndOfSubCommand;
import com.karasu256.kcapi.api.command.AbstractSubCommand;
import com.karasu256.kcapi.api.command.ICommand;

public class GameSubCommand extends AbstractSubCommand {
    public GameSubCommand(ICommand parent) {
        super("game", parent);
        addSubCommand(new GameStartSubCommand(this));
        addSubCommand(new GameStopSubCommand(this));
    }

    private class GameStartSubCommand extends AbstractEndOfSubCommand {
        public GameStartSubCommand(ICommand parent) {
            super("start", parent);
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
            return true;
        }
    }

    private class GameStopSubCommand extends AbstractEndOfSubCommand {
        public GameStopSubCommand(ICommand parent) {
            super("stop", parent);
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
            return true;
        }
    }
}

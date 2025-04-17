package com.karasu256.teamUtils.command.subcommand;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.karasu256.kcapi.api.command.ICommand;
import com.karasu256.teamUtils.utils.TeamUtility;

public class LeaveSubCommand extends AbstractJoinOrLeaveSubCommand {
    public LeaveSubCommand(ICommand parent) {
        super("leave", parent);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        return super.execute(sender, commandLabel, args);
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return TeamUtility.getTabCompletionsForJoinOrLeave(args);
    }

    @Override
    public boolean onJoinOrLeave(Player player, String teamName) {
        return TeamUtility.removePlayerFromTeam(player);
    }

    @Override
    public boolean isJoin() {
        return false;
    }
}

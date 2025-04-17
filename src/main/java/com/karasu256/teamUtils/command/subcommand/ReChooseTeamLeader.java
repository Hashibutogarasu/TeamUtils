package com.karasu256.teamUtils.command.subcommand;

import com.karasu256.kcapi.api.command.AbstractEndOfSubCommand;
import com.karasu256.kcapi.api.command.ICommand;
import com.karasu256.kcapi.api.command.ISubCommand;
import com.karasu256.teamUtils.utils.TeamUtility;
import org.bukkit.command.CommandSender;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class ReChooseTeamLeader extends AbstractEndOfSubCommand {
    public ReChooseTeamLeader(ICommand subCommand) {
        super("rechooseLeader", subCommand);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        return super.execute(sender, commandLabel, args);
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return TeamUtility.getTeams().stream().map(Team::getName).collect(Collectors.toList());
    }
}

package com.karasu256.teamUtils.command.subcommand;

import com.karasu256.kcapi.api.command.AbstractEndOfSubCommand;
import com.karasu256.kcapi.api.command.ICommand;
import com.karasu256.kcapi.api.command.ISubCommand;
import com.karasu256.teamUtils.utils.TeamUtility;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class TeamInfoSubCommand extends AbstractEndOfSubCommand {
    public TeamInfoSubCommand(ICommand subCommand) {
        super("info", subCommand);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if(args.length < 1 || args.length > 2) {
            return false;
        }

        var team = TeamUtility.getTeamByName(args[0]);

        if(team == null){
            sender.sendMessage("$c利用可能なチームが見つかりませんでした");
        }
        else{
            sender.sendMessage("§aチーム名: " + team.getName());
            var leaders = TeamUtility.getTeamLeaders(team.getName());
            StringBuilder leaderBuilder = new StringBuilder();
            for (Player leader : leaders) {
                leaderBuilder.append(leader.getName());

                if(leaders.lastIndexOf(leader) < leaders.size()){
                    leaderBuilder.append(", ");
                }
            }

            StringBuilder memberBuilder = new StringBuilder();

            for (OfflinePlayer member : team.getPlayers()) {
                memberBuilder.append(member.getName());

                if(team.getPlayers().stream().toList().lastIndexOf(member) < team.getPlayers().size()){
                    memberBuilder.append(", ");
                }
            }

            if(team.getPlayers().isEmpty()){
                sender.sendMessage("§aこのチームにメンバーはいません");
            }
            else{
                sender.sendMessage("§aチームのメンバー:" + memberBuilder);
            }

            if(leaders.isEmpty()){
                sender.sendMessage("§aこのチームにリーダーはいません");
            }
            else{
                sender.sendMessage("§aチームのリーダー:" + leaderBuilder);
            }

            return true;
        }

        return super.execute(sender, commandLabel, args);
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return TeamUtility.getTeams().stream().map(Team::getName).collect(Collectors.toList());
    }
}

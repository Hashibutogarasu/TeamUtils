package com.karasu256.teamUtils.command.subcommand;

import com.karasu256.kcapi.api.command.AbstractEndOfSubCommand;
import com.karasu256.kcapi.api.command.ICommand;
import com.karasu256.teamUtils.utils.TeamUtility;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class GetLeadersSubCommand extends AbstractEndOfSubCommand {
    public GetLeadersSubCommand(ICommand subCommand) {
        super("getLeader", subCommand);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        var player = Bukkit.getPlayer(args[0]);

        if(player == null){
            sender.sendMessage("§c指定されたプレイヤーはオンラインではありません。");
            return true;
        }

        if(TeamUtility.isTeamLeader(player)){
            sender.sendMessage("§a" + player.getName() + "はチームのリーダーです。");
        } else {
            sender.sendMessage("§c" + player.getName() + "はチームのリーダーではありません。");
        }

        return true;
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }
}

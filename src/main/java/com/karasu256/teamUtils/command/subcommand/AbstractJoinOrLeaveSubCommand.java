package com.karasu256.teamUtils.command.subcommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import com.karasu256.kcapi.api.command.AbstractEndOfSubCommand;
import com.karasu256.kcapi.api.command.ICommand;
import com.karasu256.teamUtils.utils.TeamUtility;

public abstract class AbstractJoinOrLeaveSubCommand extends AbstractEndOfSubCommand {
    public AbstractJoinOrLeaveSubCommand(String name, ICommand subCommand) {
        super(name, subCommand);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        // プレイヤー以外からのコマンド実行をチェック
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行できます。");
            return true;
        }

        TeamUtility.reloadTeamCache();

        // 引数なしの場合
        if (args.length == 0) {
            // チームから離脱する場合（isJoin() == false）で、既にチームに所属していない場合のチェック
            if (!isJoin() && TeamUtility.isPlayerInAnyTeam(player)) {
                player.sendMessage("§cあなたはどのチームにも所属していません。");
                return true;
            }

            Team randomTeam = TeamUtility.getRandomTeam();
            if (randomTeam == null) {
                player.sendMessage("§c利用可能なチームが見つかりませんでした。");
                return true;
            }

            boolean result = TeamUtility.movePlayerToTeam(player, randomTeam.getName());
            if (result) {
                if (isJoin()) {
                    //どこかのチームに所属していない場合
                    joinTeam(player, randomTeam, false);
                } else {
                    TeamUtility.removePlayerFromTeam(player);
                    player.sendMessage("§a" + randomTeam.getName() + " チームから離脱しました。");
                }
            } else {
                if (isJoin()) {
                    player.sendMessage("§cチームへの参加に失敗しました。");
                } else {
                    player.sendMessage("§cチームからの離脱に失敗しました。");
                }
            }
            return true;
        }

        // 第一引数がある場合（チーム名）
        String teamName = args[0];
        Team team = TeamUtility.getTeamByName(teamName);

        if (team == null) {
            player.sendMessage("§cチーム '" + teamName + "' が見つかりませんでした。");
            return true;
        }

        // 第二引数がある場合（プレイヤー名）
        if (args.length >= 2) {
            String targetPlayerName = args[1];
            Player targetPlayer = player.getServer().getPlayer(targetPlayerName);

            if (targetPlayer == null) {
                player.sendMessage("§cプレイヤー '" + targetPlayerName + "' が見つかりませんでした。");
                return true;
            }

            // チームから離脱する場合、既にチームに所属していないプレイヤーのチェック
            if (!isJoin() && TeamUtility.getPlayerTeam(targetPlayer) == null) {
                player.sendMessage("§cプレイヤー '" + targetPlayer.getName() + "' は既にどのチームにも所属していません。");
                return true;
            }

            boolean result = TeamUtility.movePlayerToTeam(targetPlayer, team.getName());
            if (result) {
                if (isJoin()) {
                    player.sendMessage(
                            "§aプレイヤー '" + targetPlayer.getName() + "' を " + team.getName() + " チームに追加しました。");
                    targetPlayer.sendMessage("§a" + team.getName() + " チームに追加されました。");
                } else {
                    player.sendMessage(
                            "§aプレイヤー '" + targetPlayer.getName() + "' を " + team.getName() + " チームから離脱させました。");
                    targetPlayer.sendMessage("§a" + team.getName() + " チームから離脱しました。");
                }
            } else {
                if (isJoin()) {
                    player.sendMessage("§cプレイヤーをチームに追加できませんでした。");
                } else {
                    player.sendMessage("§cプレイヤーをチームから離脱できませんでした。");
                }
            }
        } else {
            // チームから離脱する場合、既にチームに所属していない場合のチェック
            if (!isJoin() && TeamUtility.getPlayerTeam(player) == null) {
                player.sendMessage("§cあなたは既にどのチームにも所属していません。");
                return true;
            }

            var result = this.onJoinOrLeave(player, team.getName());
            if (result) {
                joinTeam(player, team);
            } else {
                if (isJoin()) {
                    player.sendMessage("§cチームへの参加に失敗しました。");
                } else {
                    player.sendMessage("§cチームからの離脱に失敗しました。");
                }
            }
        }
        return true;

    }

    private void joinTeam(Player player, Team team){
        this.joinTeam(player, team, true);
    }

    private void joinTeam(Player player, Team team, boolean moveTeam){
        if(moveTeam){
            if(TeamUtility.isPlayerInAnyTeam(player)){
                player.sendMessage("§aランダムに " + team.getName() + " チームに参加しました。");
            }
            else{
                var randomTeam = TeamUtility.getRandomTeam();
                if(randomTeam != null){
                    TeamUtility.movePlayerToTeam(player, randomTeam.getName());
                    player.sendMessage("§a" + randomTeam.getName() + " チームに参加しました。");
                }
                else{
                    player.sendMessage("§c利用可能なチームが見つかりませんでした。");
                }
            }
        }

    }

    public abstract boolean onJoinOrLeave(Player player, String teamName);

    public abstract boolean isJoin();
}

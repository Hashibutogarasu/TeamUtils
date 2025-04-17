package com.karasu256.teamUtils.command.subcommand;

import com.karasu256.kcapi.api.command.AbstractEndOfSubCommand;
import com.karasu256.kcapi.api.command.ICommand;
import com.karasu256.teamUtils.utils.TeamUtility;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class RefreshSubCommand extends AbstractEndOfSubCommand {
    public RefreshSubCommand(ICommand parent) {
        super("reflesh", parent);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        LOGGER.info("Refreshing teams...");
        int maxMember = 0;
        int teamLeaders = 0;

        try{
            if(args.length > 0){
                maxMember = Integer.parseInt(args[0]);
            }
            if(args.length > 1){
                teamLeaders = Integer.parseInt(args[1]);
            }
        }
        catch (NumberFormatException e){
            LOGGER.warning("Invalid number format: " + e.getMessage());
            return false;
        }
        catch (ArrayIndexOutOfBoundsException e){
            LOGGER.warning("Please specify a number of teams to shuffle.");
            return false;
        }
        catch (Exception e){
            LOGGER.warning("An error occurred while refreshing teams: " + e.getMessage());
            return false;
        }

        try{
            TeamUtility.shuffle(maxMember, teamLeaders);
        }
        catch (IllegalArgumentException e){
            LOGGER.warning("Invalid argument: " + e.getMessage());
            return false;
        }
        catch (Exception e){
            LOGGER.warning("An error occurred while refreshing teams: " + e.getMessage());
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                LOGGER.warning(stackTraceElement.toString());
            }
            return false;
        }

        LOGGER.info("Teams have been refreshed successfully.");
        return true;
    }
}

package com.karasu256.teamUtils.command.subcommand;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import com.karasu256.kcapi.api.command.AbstractEndOfSubCommand;
import com.karasu256.kcapi.api.command.ICommand;
import com.karasu256.teamUtils.utils.ItemUtils;

public class RepairEquipmentsSubCommand extends AbstractEndOfSubCommand {
    public RepairEquipmentsSubCommand(ICommand parent) {
        super("repair", parent);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {

        ItemUtils.repairPlayersInventoryInTeam();

        return true;
    }
}

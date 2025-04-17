package com.karasu256.teamUtils.command;

import com.karasu256.kcapi.api.command.AbstractCommand;
import com.karasu256.teamUtils.TeamUtils;
import com.karasu256.teamUtils.command.subcommand.*;

public class TeamUtilsCommand extends AbstractCommand {
    public TeamUtilsCommand(TeamUtils plugin) {
        super("kteam");
        addSubCommand(new RefreshSubCommand(this));
        addSubCommand(new ConfigSubCommand(plugin, this));
        addSubCommand(new RepairEquipmentsSubCommand(this));
        addSubCommand(new TestSubCommand(this));
        addSubCommand(new JoinSubCommand(this));
        addSubCommand(new LeaveSubCommand(this));
        addSubCommand(new GetLeadersSubCommand(this));
        addSubCommand(new TeamInfoSubCommand(this));
        addSubCommand(new ReChooseTeamLeader(this));
    }
}

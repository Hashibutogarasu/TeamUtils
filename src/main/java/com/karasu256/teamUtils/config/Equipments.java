package com.karasu256.teamUtils.config;

import com.karasu256.karasuConfigLib.annotation.Config;
import com.karasu256.teamUtils.TeamUtils;
import com.karasu256.teamUtils.utils.EquipmentEnum;

import java.util.HashMap;
import java.util.Map;

@Config(fileName = "equipments.json", pluginName = TeamUtils.PLUGIN_NAME)
public class Equipments extends AbstractPluginBaseConfig{
    private final Map<EquipmentEnum, EquipmentData> equipmentData = new HashMap<>();

    public Equipments() {}

    public Map<EquipmentEnum, EquipmentData> getEquipmentData() {
        return equipmentData;
    }
}

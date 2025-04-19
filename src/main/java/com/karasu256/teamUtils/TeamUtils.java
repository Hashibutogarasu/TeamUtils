package com.karasu256.teamUtils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.karasu256.karasuConfigLib.AbstractJavaPluginConfigable;
import com.karasu256.karasuConfigLib.config.BaseConfig;
import com.karasu256.teamUtils.command.TeamUtilsCommand;
import com.karasu256.teamUtils.config.AbstractPluginBaseConfig;
import com.karasu256.teamUtils.config.EquipmentData;
import com.karasu256.teamUtils.config.Equipments;
import com.karasu256.teamUtils.config.GameConfig;
import com.karasu256.teamUtils.listeners.GameModeChangeListener;
import com.karasu256.teamUtils.listeners.GameJoinQuitListener;
import com.karasu256.teamUtils.listeners.PlayerListener;
import com.karasu256.teamUtils.utils.EquipmentEnum;
import com.karasu256.teamUtils.utils.GameUtils;
import com.karasu256.teamUtils.utils.TeamUtility;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class TeamUtils extends AbstractJavaPluginConfigable<AbstractPluginBaseConfig> {
    public static Logger LOGGER = Logger.getLogger("TeamUtils");
    public static final String PLUGIN_NAME = "TeamUtils";
    private static final TypeAdapter<Map<EquipmentEnum, EquipmentData>> EQUIPMENT_MAP_TYPE = new TypeAdapter<>() {
        @Override
        public void write(JsonWriter out, Map<EquipmentEnum, EquipmentData> value) throws IOException {
            out.beginObject();
            for (Map.Entry<EquipmentEnum, EquipmentData> entry : value.entrySet()) {
                out.name(entry.getKey().name());
                out.jsonValue(entry.getValue().toString());
            }
            out.endObject();
        }

        @Override
        public Map<EquipmentEnum, EquipmentData> read(JsonReader in) throws IOException {
            return BaseConfig.getGson().fromJson(in, Map.class);
        }
    };
    public static Equipments EQUIPMENT_DATA = new Equipments();
    public static GameConfig GAME_CONFIG = new GameConfig();

    @Override
    public void onEnable() {
        LOGGER.info("Initializing TeamUtils...");
        BaseConfig.registerTypeAdapter(EquipmentEnum.class, EQUIPMENT_MAP_TYPE);

        super.onEnable();
        EQUIPMENT_DATA = getConfig(Equipments.class);
        GAME_CONFIG = getConfig(GameConfig.class);

        LOGGER.info("Registering commands...");
        PluginCommand command = getCommand("kteam");
        if (command != null) {
            command.setExecutor(new TeamUtilsCommand(this));
        }
        LOGGER.info("Commands registered successfully.");

        LOGGER.info("Initializing utilities...");
        TeamUtility.initialize();
        TeamUtility.reloadTeamCache();
        TeamUtility.reloadIsGameRunning();
        LOGGER.info("Utilities initialized successfully.");

        getServer().getPluginManager().registerEvents(new GameModeChangeListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new GameJoinQuitListener(), this);

        LOGGER.info("Completed initialization of TeamUtils.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        LOGGER.info("Disabling TeamUtils...");

        TeamUtility.clearGameModeCallbacks();
        TeamUtility.clearRespawnCallbacks();

        TeamUtility.update();

        GameUtils.endGame();

        save();

        LOGGER.info("Completed disabling of TeamUtils.");
    }

    @Override
    public List<Class<? extends AbstractPluginBaseConfig>> getDefaultConfigs() {
        return List.of(GameConfig.class, Equipments.class);
    }

    @Override
    public @NotNull Logger getLogger() {
        return LOGGER;
    }

    public static TeamUtils getPlugin() {
        return (TeamUtils) JavaPlugin.getProvidingPlugin(TeamUtils.class);
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }
}

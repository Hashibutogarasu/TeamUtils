package com.karasu256.teamUtils.utils;

import com.karasu256.teamUtils.TeamUtils;

import com.karasu256.teamUtils.config.EquipmentData;
import com.karasu256.teamUtils.config.Equipments;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scoreboard.Team;

import java.util.Arrays;
import java.util.List;

public class ItemUtils {
    public static ItemStack setUnbreakable(ItemStack item) {
        var meta = item.getItemMeta();
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    public static List<ItemStack> getDefaultItems() {
        return Arrays.asList(
                setUnbreakable(new ItemStack(Material.STONE_SWORD)),
                setUnbreakable(new ItemStack(Material.BOW)),
                new ItemStack(Material.ARROW, 64),
                new ItemStack(Material.COOKED_BEEF, 64));
    }

    public static ItemStack getLetherArmorWithColored(ArmorType type, Color color) {
        ItemStack armor = setUnbreakable(new ItemStack(type.getMaterial()));
        LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
        meta.setColor(color);
        armor.setItemMeta(meta);
        return armor;
    }

    public static void giveDefaultItems(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        getDefaultItems().forEach(item -> player.getInventory().addItem(item));
    }

    public static void repairPlayersInventoryInTeam() {
        TeamUtility.forEachTeamPlayer(ItemUtils::repairPlayerInventory);
    }

    /**
     * プレイヤーのインベントリを設定から読み込んだ装備で修復します。
     * 装備アイテムはホットバーに配置され、防具は自動で装備されます。
     * 革装備の場合はプレイヤーのチームの色が適用されます。
     * スタック不可のアイテムは1つだけ追加されます。
     * 
     * @param player 装備を与えるプレイヤー
     */
    public static void repairPlayerInventory(Player player) {
        // プレイヤーのインベントリをクリア
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // チームの色を取得
        TextColor teamColor = null;
        Team team = player.getScoreboard().getEntryTeam(player.getName());
        if (team != null) {
            try {
                teamColor = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(team.getName()).color();
            } catch (Exception e) {
                // チームの色が取得できない場合はデフォルトの色を使用
                teamColor = TextColor.color(0xFFFFFF);
            }
        }

        // 設定マネージャーから装備情報を取得
        TeamUtils.getPlugin().load();
        Equipments data = TeamUtils.getPlugin().getConfig(Equipments.class);

        // 設定から装備情報を読み込み、EquipmentEnumにセット
        for (EquipmentEnum equipment : EquipmentEnum.values()) {
            if(data != null){
                try {
                    EquipmentData equipmentData = data.getEquipmentData().get(equipment);

                    if(equipmentData == null){
                        data.getEquipmentData().put(equipment, new EquipmentData());
                    }
                    else{
                        equipmentData.setAmount(equipmentData.getAmount());
                        equipmentData.setItem(equipmentData.getItem());

                        data.getEquipmentData().put(equipment, equipmentData);
                    }
                } catch (Exception e) {
                    TeamUtils.getPlugin().getLogger()
                            .warning("Could not load the equipment of " + equipment.name() + ".");
                    TeamUtils.getPlugin().getLogger()
                            .warning("Error message:" + e.getMessage());
                }
            }
            else{
                TeamUtils.getPlugin().getLogger()
                        .warning("The data of config is null. Please reload the config.");
            }
        }

        // 装備以外のアイテムをホットバーに追加
        for (EquipmentEnum equipment : EquipmentEnum.values()) {
            if (!equipment.canRepair()) {
                continue;
            }

            ItemStack item = equipment.getItemStack();

            // スタック可能かどうかを確認
            int amount = equipment.getAmount();
            if (!item.getType().isItem() || item.getType().getMaxStackSize() == 1) {
                // スタック不可のアイテムは1つだけに制限
                amount = 1;
            }
            item.setAmount(amount);

            // アイテムの種類に応じて処理
            switch (equipment) {
                case HELMET:
                case CHESTPLATE:
                case LEGGINGS:
                case BOOTS:
                    // 防具の場合
                    if (teamColor != null && item.getType().name().contains("LEATHER_")) {
                        // 革装備かつチームの色がある場合は色を適用
                        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
                        meta.setColor(ColorUtils.toBukkitColor(teamColor));
                        item.setItemMeta(meta);
                    }

                    // 防具を自動で装備
                    switch (equipment) {
                        case HELMET:
                            player.getInventory().setHelmet(setUnbreakable(item));
                            break;
                        case CHESTPLATE:
                            player.getInventory().setChestplate(setUnbreakable(item));
                            break;
                        case LEGGINGS:
                            player.getInventory().setLeggings(setUnbreakable(item));
                            break;
                        case BOOTS:
                            player.getInventory().setBoots(setUnbreakable(item));
                            break;
                        default:
                            // 未知の防具タイプの場合は通常のアイテムとして追加
                            player.getInventory().addItem(setUnbreakable(item));
                            break;
                    }
                    break;
                default:
                    // 通常アイテムはホットバーに追加
                    if (equipment.isMeleeWeapon() || equipment.isRangedWeapon()) {
                        item = setUnbreakable(item);
                    }
                    player.getInventory().addItem(item);
                    break;
            }
        }
    }
}

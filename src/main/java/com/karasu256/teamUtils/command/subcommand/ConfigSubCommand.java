package com.karasu256.teamUtils.command.subcommand;

import com.karasu256.kcapi.api.command.AbstractEndOfSubCommand;
import com.karasu256.kcapi.api.command.AbstractSubCommand;
import com.karasu256.kcapi.api.command.ICommand;
import com.karasu256.kcapi.api.command.ISubCommand;
import com.karasu256.teamUtils.TeamUtils;
import com.karasu256.teamUtils.utils.ChatMenuBuilder;
import com.karasu256.teamUtils.utils.EquipmentEnum;

import net.kyori.adventure.text.Component;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigSubCommand extends AbstractSubCommand {
    private ICommand parentCommand;
    private final TeamUtils plugin;
    private int currentPage = 0;
    private final int itemsPerPage = 5;

    public ConfigSubCommand(JavaPlugin plugin, ICommand parent) {
        super("config", parent);
        this.plugin = (TeamUtils)plugin;
        addSubCommand(new SaveSubCommand(this));
        addSubCommand(new AddAmountSubCommand(this));
        addSubCommand(new SetItemSubCommand(this));
        addSubCommand(new SetAmountSubCommand(this));
        addSubCommand(new PageSubCommand(this));
        addSubCommand(new ReloadSubCommand(this));
    }

    @Override
    public void setParentCommand(ICommand parent) {
        this.parentCommand = parent;
    }

    @Override
    public ICommand getParentCommand() {
        return parentCommand;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ実行できます。");
            return true;
        }

        if (args.length > 0) {
            handleMenuAction(player, args);
            return true;
        }

        showConfigMenu(player);
        return true;
    }

    private void showConfigMenu(Player player) {
        ChatMenuBuilder menuBuilder = new ChatMenuBuilder("team utils 設定");

        List<EquipmentEnum> equipmentList = new ArrayList<>(List.of(EquipmentEnum.values()));
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, equipmentList.size());

        for (int i = startIndex; i < endIndex; i++) {
            EquipmentEnum equipment = equipmentList.get(i);
            ItemStack item = equipment.getItemStack();
            int amount = equipment.getAmount();

            Component itemText;
            itemText = Component.text("[").append(Component.translatable(item.getType().translationKey()))
                    .append(Component.text("]"));

            menuBuilder.addRow()
                    .addText(equipment.getDisplayName() + " ")
                    .addClickableText(itemText,
                            "/tu config setitem " + equipment.getDisplayName(),
                            "クリックして手持ちのアイテムを設定")
                    .addText(" " + amount + "個 ")
                    .addClickableText(" +32", "/tu config addamount " + equipment.getDisplayName() + " 32", "+32")
                    .addClickableText(" +1", "/tu config addamount " + equipment.getDisplayName() + " 1", "+1")
                    .addClickableText(" -1", "/tu config addamount " + equipment.getDisplayName() + " -1", "-1")
                    .addClickableText(" -32", "/tu config addamount " + equipment.getDisplayName() + " -32", "-32")
                    .addClickableText(" =64", "/tu config setamount " + equipment.getDisplayName() + " 64", "64に設定");
        }

        menuBuilder.addRow()
                .addClickableText(currentPage > 0 ? "前 " : "   ",
                        currentPage > 0 ? "/tu config page " + (currentPage - 1) : "", "前のページ")
                .addClickableText(endIndex < equipmentList.size() ? " 次" : "   ",
                        endIndex < equipmentList.size() ? "/tu config page " + (currentPage + 1) : "", "次のページ");

        menuBuilder.addRow()
                .addClickableText("読み込み ", "/tu config reload", "設定を読み込み")
                .addClickableText("保存", "/tu config save", "設定を保存");

        menuBuilder.send(player);
    }

    private void handleMenuAction(Player player, String[] args) {
        if (args.length < 1)
            return;

        switch (args[0].toLowerCase()) {
            case "page":
            case "setitem":
            case "addamount":
            case "setamount":
                break;
            default:
                player.sendMessage("不明なアクションです: " + args[0]);
                showConfigMenu(player);
                break;
        }
    }

    private class ReloadSubCommand extends AbstractEndOfSubCommand {
        public ReloadSubCommand(ISubCommand parent) {
            super("reload", parent);
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
            plugin.load();
            sender.sendMessage("設定を再読み込みしました。");
            return true;
        }
    }

    private class SaveSubCommand extends AbstractEndOfSubCommand {
        public SaveSubCommand(ISubCommand parent) {
            super("save", parent);
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
            plugin.save();
            sender.sendMessage("設定を保存しました。");
            return true;
        }
    }

    private class AddAmountSubCommand extends AbstractEndOfSubCommand {
        public AddAmountSubCommand(ISubCommand parent) {
            super("addamount", parent);
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("このコマンドはプレイヤーのみ実行できます。");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("使用方法: /tu config addamount <装備タイプ> <変更値>");
                return true;
            }

            try {
                EquipmentEnum equipment = EquipmentEnum.getByDisplayName(args[0]);
                String deltaStr = args[1];
                int delta;

                // +1, -1 形式の場合、先頭の記号を削除して解析
                if (deltaStr.startsWith("+")) {
                    delta = Integer.parseInt(deltaStr.substring(1));
                } else {
                    delta = Integer.parseInt(deltaStr);
                }

                plugin.load();
                int newAmount = Math.min(64, Math.max(1, equipment.getAmount() + delta));
                TeamUtils.EQUIPMENT_DATA.getEquipmentData().get(equipment).setAmount(newAmount);
                plugin.save();
                player.sendMessage(equipment.getDisplayName() + "の個数を" + newAmount + "に変更しました。");
            } catch (IllegalArgumentException e) {
                player.sendMessage("無効なパラメータです。");
            }

            showConfigMenu(player);
            return true;
        }
    }

    private class SetItemSubCommand extends AbstractEndOfSubCommand {
        public SetItemSubCommand(ISubCommand parent) {
            super("setitem", parent);
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("このコマンドはプレイヤーのみ実行できます。");
                return true;
            }

            if (args.length < 1) {
                player.sendMessage("使用方法: /tu config setitem <装備タイプ>");
                return true;
            }

            try {
                EquipmentEnum equipment = EquipmentEnum.getByDisplayName(args[0]);
                ItemStack handItem = player.getInventory().getItemInMainHand();
                if (!handItem.getType().isAir()) {
                    // 現在の個数を保持
                    int currentAmount = equipment.getAmount();
                    plugin.load();
                    TeamUtils.EQUIPMENT_DATA.getEquipmentData().get(equipment).setAmount(currentAmount);
                    plugin.save();
                    Component text = Component
                            .text(equipment.name() + "のアイテムを")
                            .append(Component.translatable(handItem.getType().translationKey()))
                            .append(Component.text("に設定しました。"))
                            .append(Component.text(" 現在の個数: " + currentAmount));
                    player.sendMessage(text);
                } else {
                    player.sendMessage("手に有効なアイテムを持ってください。");
                }
            } catch (IllegalArgumentException e) {
                player.sendMessage("無効な装備タイプです。");
            }

            showConfigMenu(player);
            return true;
        }
    }

    private class SetAmountSubCommand extends AbstractEndOfSubCommand {
        public SetAmountSubCommand(ISubCommand parent) {
            super("setamount", parent);
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("このコマンドはプレイヤーのみ実行できます。");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("使用方法: /tu config setamount <装備タイプ> <個数>");
                return true;
            }

            try {
                EquipmentEnum equipment = EquipmentEnum.getByDisplayName(args[0]);
                int amount = Integer.parseInt(args[1]);
                if (amount > 0 && amount <= 64) {
                    plugin.load();
                    TeamUtils.EQUIPMENT_DATA.getEquipmentData().get(equipment).setAmount(amount);
                    plugin.save();
                    player.sendMessage(equipment.getDisplayName() + "の個数を" + amount + "に設定しました。");
                } else if (amount > 64) {
                    player.sendMessage("個数は64以下である必要があります。");
                } else {
                    player.sendMessage("個数は1以上である必要があります。");
                }
            } catch (IllegalArgumentException e) {
                player.sendMessage("無効なパラメータです。");
            }

            showConfigMenu(player);
            return true;
        }
    }

    private class PageSubCommand extends AbstractEndOfSubCommand {
        public PageSubCommand(ISubCommand parent) {
            super("page", parent);
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("このコマンドはプレイヤーのみ実行できます。");
                return true;
            }

            if (args.length < 1) {
                player.sendMessage("使用方法: /tu config page <ページ番号>");
                return true;
            }

            try {
                currentPage = Integer.parseInt(args[0]);
                showConfigMenu(player);
            } catch (NumberFormatException e) {
                player.sendMessage("無効なページ番号です。");
            }

            return true;
        }
    }
}

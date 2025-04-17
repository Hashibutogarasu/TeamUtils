package com.karasu256.teamUtils.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum EquipmentEnum {
    MAIN_WEAPON(new ItemStack(Material.STONE_SWORD), true, 1, "メイン武器", true, false, true),
    SUB_WEAPON(new ItemStack(Material.BOW), true, 1, "サブ武器", false, true, true),
    HELMET(new ItemStack(Material.LEATHER_HELMET), false, 1, "ヘルメット", false, false, false),
    CHESTPLATE(new ItemStack(Material.LEATHER_CHESTPLATE), true, 1, "チェストプレート", false, false, true),
    LEGGINGS(new ItemStack(Material.LEATHER_LEGGINGS), false, 1, "レギンス", false, false, false),
    BOOTS(new ItemStack(Material.LEATHER_BOOTS), false, 1, "ブーツ", false, false, false),
    AMMO(new ItemStack(Material.ARROW), true, 64, "矢", false, false, true),
    FOOD(new ItemStack(Material.COOKED_BEEF), true, 64, "食料", false, false, true);

    private ItemStack itemStack;
    private int amount;
    private boolean equip;
    private String displayName;
    private boolean isMeleeWeapon;
    private boolean isRangedWeapon;
    private boolean canRepair;
    private final Map<Enchantment, Integer> enchantments = new HashMap<>();

    EquipmentEnum(ItemStack itemStack, boolean equip, int amount, String displayName, boolean isMeleeWeapon, boolean isRangedWeapon, boolean canRepair) {
        this.itemStack = itemStack;
        this.amount = amount;
        this.equip = equip;
        this.displayName = displayName;
        this.isMeleeWeapon = isMeleeWeapon;
        this.isRangedWeapon = isRangedWeapon;
        this.canRepair = canRepair;
    }

    public ItemStack getItemStack() {
        ItemStack stack = itemStack.clone();
        stack.setAmount(amount);
        enchantments.forEach((enchant, level) -> stack.addEnchantment(enchant, level));
        return stack;
    }

    public void setItemStack(ItemStack itemStack, int amount) {
        this.itemStack = itemStack;
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public boolean isEquip() {
        return equip;
    }

    public void setEquip(boolean equip) {
        this.equip = equip;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void addEnchant(Enchantment enchantment, int level) {
        enchantments.put(enchantment, level);
    }

    public void removeEnchant(Enchantment enchantment) {
        enchantments.remove(enchantment);
    }

    public void clearEnchants() {
        enchantments.clear();
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return new HashMap<>(enchantments);
    }

    public Component getTranslatedItemName() {
        return Component.translatable(this.itemStack.getType().translationKey());
    }

    public static EquipmentEnum getByDisplayName(String displayName){
        return Arrays.stream(EquipmentEnum.values()).filter(
            equipmentEnum -> equipmentEnum.getDisplayName().equalsIgnoreCase(displayName)
        ).findFirst().orElse(null);
    }

    public boolean isMeleeWeapon() {
        return isMeleeWeapon;
    }

    public void setMeleeWeapon(boolean meleeWeapon) {
        this.isMeleeWeapon = meleeWeapon;
    }
    
    public boolean isRangedWeapon() {
        return isRangedWeapon;
    }
    
    public void setRangedWeapon(boolean rangedWeapon) {
        this.isRangedWeapon = rangedWeapon;
    }

    /**
     * このアイテムがプレイヤーインベントリ修復時に追加されるかどうかを返します
     * 
     * @return 修復時に追加されるかどうか
     */
    public boolean canRepair() {
        return canRepair;
    }

    /**
     * このアイテムがプレイヤーインベントリ修復時に追加されるかどうかを設定します
     * 
     * @param canRepair 修復時に追加するかどうか
     */
    public void setCanRepair(boolean canRepair) {
        this.canRepair = canRepair;
    }
}

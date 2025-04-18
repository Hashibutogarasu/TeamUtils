package com.karasu256.teamUtils.config;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EquipmentData extends AbstractPluginBaseConfig {
    private ItemStack item;
    private int amount;
    private Map<Enchantment, Integer> enchantments;

    public EquipmentData(){

    }

    public EquipmentData(ItemStack item, int amount) {
        this.item = item;
        this.amount = amount;
        this.enchantments = new HashMap<>();
        if (item != null) {
            this.enchantments.putAll(item.getEnchantments());
        }
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return Collections.unmodifiableMap(enchantments);
    }
}

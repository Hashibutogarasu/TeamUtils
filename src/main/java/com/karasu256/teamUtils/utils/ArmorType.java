package com.karasu256.teamUtils.utils;

import org.bukkit.Material;

public enum ArmorType {
    HELMET(Material.LEATHER_HELMET),
    CHESTPLATE(Material.LEATHER_CHESTPLATE),
    LEGGINGS(Material.LEATHER_LEGGINGS),
    BOOTS(Material.LEATHER_BOOTS);

    private final Material material;

    ArmorType(Material material) {
        this.material = material;
    }

    public Material getMaterial() {
        return material;
    }
}

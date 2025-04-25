package com.karasu256.teamUtils.config;

import com.google.gson.*;
import com.karasu256.teamUtils.utils.EquipmentEnum;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class EquipmentTypeAdapter implements JsonSerializer<Map<EquipmentEnum, EquipmentData>>, JsonDeserializer<Map<EquipmentEnum, EquipmentData>> {

    @Override
    public JsonElement serialize(Map<EquipmentEnum, EquipmentData> src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        
        for (Map.Entry<EquipmentEnum, EquipmentData> entry : src.entrySet()) {
            JsonObject equipmentObj = new JsonObject();
            
            // ItemStackの情報をシリアライズ
            if (entry.getValue().getItem() != null) {
                equipmentObj.addProperty("material", entry.getValue().getItem().getType().name());
                
                // エンチャントがあれば追加
                if (!entry.getValue().getEnchantments().isEmpty()) {
                    JsonObject enchants = new JsonObject();
                    for (Map.Entry<Enchantment, Integer> enchant : entry.getValue().getEnchantments().entrySet()) {
                        enchants.addProperty(enchant.getKey().getKey().getKey(), enchant.getValue());
                    }
                    equipmentObj.add("enchantments", enchants);
                }
            }
            
            equipmentObj.addProperty("amount", entry.getValue().getAmount());
            
            result.add(entry.getKey().name(), equipmentObj);
        }
        
        return result;
    }

    @Override
    public Map<EquipmentEnum, EquipmentData> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Map<EquipmentEnum, EquipmentData> result = new HashMap<>();
        JsonObject jsonObject = json.getAsJsonObject();
        
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            try {
                EquipmentEnum equipType = EquipmentEnum.valueOf(entry.getKey());
                JsonObject equipObj = entry.getValue().getAsJsonObject();
                
                ItemStack item = null;
                if (equipObj.has("material")) {
                    Material material = Material.valueOf(equipObj.get("material").getAsString());
                    item = new ItemStack(material);
                    
                    // エンチャントの読み込み
                    if (equipObj.has("enchantments")) {
                        JsonObject enchants = equipObj.getAsJsonObject("enchantments");
                        for (Map.Entry<String, JsonElement> enchant : enchants.entrySet()) {
                            @SuppressWarnings("deprecation")
                            Enchantment enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchant.getKey()));
                            if (enchantment != null) {
                                item.addUnsafeEnchantment(enchantment, enchant.getValue().getAsInt());
                            }
                        }
                    }
                }
                
                int amount = equipObj.has("amount") ? equipObj.get("amount").getAsInt() : 1;
                
                result.put(equipType, new EquipmentData(item, amount));
            } catch (IllegalArgumentException e) {
                // 不明なEnum値や無効なMaterialの場合はスキップ
                continue;
            }
        }
        
        return result;
    }
}

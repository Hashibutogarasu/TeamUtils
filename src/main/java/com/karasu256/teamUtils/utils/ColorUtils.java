package com.karasu256.teamUtils.utils;

import org.bukkit.Color;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;  // ChatColorのインポート
import java.util.Random;

public class ColorUtils {
    public interface IColor {
        java.awt.Color toAWTColor();
        void fromAWTColor(java.awt.Color color);
        int getRGB(); // RGBの取得メソッド
    }
    public static class TextColorConverter implements IColor {
        private TextColor textColor;

        public TextColorConverter(TextColor textColor) {
            this.textColor = textColor;
        }

        @Override
        public java.awt.Color toAWTColor() {
            return new java.awt.Color((int) (textColor.red() * 255), (int) (textColor.green() * 255), (int) (textColor.blue() * 255));
        }

        @Override
        public void fromAWTColor(java.awt.Color awtColor) {
            this.textColor = TextColor.color(awtColor.getRed() / 255.0f, awtColor.getGreen() / 255.0f, awtColor.getBlue() / 255.0f);
        }

        @Override
        public int getRGB() {
            return (textColor.red() * 255) << 16 |
                    (textColor.green() * 255) << 8 |
                    (textColor.blue() * 255);
        }

        public static TextColorConverter fromAWT(java.awt.Color awtColor) {
            return new TextColorConverter(TextColor.color(awtColor.getRed() / 255.0f, awtColor.getGreen() / 255.0f, awtColor.getBlue() / 255.0f));
        }

        public TextColor getTextColor() {
            return textColor;
        }

        // TextColorからBukkit Colorへの変換
        public static Color toBukkitColor(TextColor textColor) {
            int r = textColor.red();   // 0-255 の値
            int g = textColor.green();
            int b = textColor.blue();
            return Color.fromRGB(r, g, b);
        }
    }

    // ColorUtils (変換処理のユーティリティ)
    public static <T extends IColor> T convert(java.awt.Color from, T toInstance) {
        toInstance.fromAWTColor(from);
        return toInstance;
    }

    public static <T extends IColor> T convert(IColor fromInstance, T toInstance) {
        return convert(fromInstance.toAWTColor(), toInstance);
    }

    // ChatColorをTextColorに変換するメソッド
    public static TextColor convert(ChatColor chatColor) {
        return TextColor.color(chatColor.getColor().getRed() / 255.0f, chatColor.getColor().getGreen() / 255.0f, chatColor.getColor().getBlue() / 255.0f);
    }

    // NamedTextColorからランダムな色を取得するメソッド
    public static NamedTextColor getRandomNamedTextColor() {
        // ランダムに生成されたTextColorを作成
        Random random = new Random();
        float r = random.nextFloat();
        float g = random.nextFloat();
        float b = random.nextFloat();
        TextColor randomTextColor = TextColor.color(r, g, b);

        // nearestToメソッドで最も近いNamedTextColorを取得
        return NamedTextColor.nearestTo(randomTextColor);
    }

    public static Color toBukkitColor(TextColor color) {
        return TextColorConverter.toBukkitColor(color);
    }
}

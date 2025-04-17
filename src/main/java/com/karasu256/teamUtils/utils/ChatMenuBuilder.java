package com.karasu256.teamUtils.utils;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;

public class ChatMenuBuilder {
    private final String title;
    private final List<RowBuilder> rows = new ArrayList<>();

    public ChatMenuBuilder(String title) {
        this.title = title;
    }

    public RowBuilder addRow() {
        RowBuilder rowBuilder = new RowBuilder();
        rows.add(rowBuilder);
        return rowBuilder;
    }

    public void send(Player player) {
        player.sendMessage("===========");
        player.spigot().sendMessage(new TextComponent(title));

        for (RowBuilder row : rows) {
            player.spigot().sendMessage(row.build());
        }

        player.sendMessage("===========");
    }

    public class RowBuilder {
        private final ComponentBuilder builder = new ComponentBuilder("");

        public RowBuilder addText(String text) {
            builder.append(text);
            return this;
        }

        public RowBuilder addClickableText(String text, String command, String hoverText) {
            TextComponent component = new TextComponent(text);

            if (command != null && !command.isEmpty()) {
                component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));

                if (hoverText != null && !hoverText.isEmpty()) {
                    // 修正: TextComponent を使用して HoverEvent を作成
                    TextComponent hoverComponent = new TextComponent(hoverText);
                    component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new BaseComponent[] { hoverComponent }));
                }
            }

            builder.append(component);
            return this;
        }
        
        public RowBuilder addClickableText(Component component, String command, String hoverText) {
            // Componentからプレーンテキストを抽出
            String text = PlainTextComponentSerializer.plainText().serialize(component);
            return addClickableText(text, command, hoverText);
        }

        public BaseComponent[] build() {
            return builder.create();
        }
    }
}

package com.chagui68.multiversenets.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Small icon helpers shared by the DRAM Loader and Unloader menus / Iconos comunes de esos menús. */
final class MenuIcons {

    private MenuIcons() {
    }

    static Component line(String text, TextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    static ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }

    /** Lines starting with "»" are yellow (actions), the rest gray / "»" en amarillo, el resto gris. */
    static ItemStack icon(Material material, String name, TextColor color, List<String> lore) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        List<Component> lines = new ArrayList<>();
        for (String text : lore) {
            lines.add(line(text, text.startsWith("»") ? NamedTextColor.YELLOW : NamedTextColor.GRAY));
        }
        meta.lore(lines);
        item.setItemMeta(meta);
        return item;
    }

    /** "■■■■□□□□□□ 40%" coloured by how full it is / Barra coloreada según lo lleno. */
    static Component bar(double ratio, int cells) {
        int filled = (int) Math.round(ratio * cells);
        NamedTextColor color = ratio >= 0.9 ? NamedTextColor.RED : ratio >= 0.7 ? NamedTextColor.YELLOW : NamedTextColor.GREEN;
        return Component.text("■".repeat(filled), color)
                .append(Component.text("□".repeat(cells - filled), NamedTextColor.DARK_GRAY))
                .append(Component.text(" " + Math.round(ratio * 100) + "%", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false);
    }
}

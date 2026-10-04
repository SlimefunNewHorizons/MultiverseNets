package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.MemoryModules;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Settings;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * [EN] DRAM Bay menu, top to bottom:
 * <pre>
 *  row 0    header      summary of the bay · help
 *  rows 1-2 modules     18 slots, one per module, each with its fill bar
 *  row 3    item gauge  how full the item modules are
 *  row 4    fluid gauge how full the Fluid DRAMs are
 *  row 5    footer      close
 * </pre>
 * Clicking an installed module takes it out as an item that keeps everything it stored; clicking a
 * free slot with a module on the cursor, or shift-clicking one in the inventory, installs it.
 *
 * [ES] Menú del DRAM Bay, de arriba abajo: cabecera con el resumen y la ayuda, 18 huecos de módulo
 * (cada uno con su barra de llenado), medidor de ítems, medidor de fluidos y pie con cerrar. Hacer
 * clic en un módulo instalado lo saca con todo su stock; hacer clic en un hueco libre con un módulo en
 * el cursor, o shift+clic a uno del inventario, lo instala.
 */
public class DramBayMenu extends MenuHolder {

    public static final int SIZE = 54;
    public static final int STATS_SLOT = 4;
    public static final int HELP_SLOT = 8;
    public static final int CLOSE_SLOT = 49;
    /** First slot of the module grid (rows 1-2) / Primer hueco de la cuadrícula de módulos. */
    public static final int FIRST_MODULE_SLOT = 9;
    public static final int[] MODULE_SLOTS = new int[MemoryModules.BAY_SLOTS];
    public static final int ITEM_GAUGE_ROW = 27;
    public static final int FLUID_GAUGE_ROW = 36;

    static {
        for (int i = 0; i < MODULE_SLOTS.length; i++) {
            MODULE_SLOTS[i] = FIRST_MODULE_SLOT + i;
        }
    }

    private static final int GAUGE_CELLS = 9;
    private static final int BAR_CELLS = 10;

    private final Block block;

    public DramBayMenu(MultiverseNets plugin, Player player, Block block) {
        super(plugin, player);
        this.block = block;
    }

    public void openMenu() {
        open(SIZE, Component.text("DRAM Bay", NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false));
    }

    // ------------------------------------------------------------------ drawing

    @Override
    protected void draw() {
        NodeBlob blob = NodeStore.get(block);
        List<NodeBlob> modules = MemoryModules.modules(blob);
        Totals totals = Totals.of(modules);

        ItemStack header = pane(Material.CYAN_STAINED_GLASS_PANE);
        ItemStack footer = pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, header);
            inv.setItem(45 + i, footer);
        }
        inv.setItem(STATS_SLOT, summary(modules, totals));
        inv.setItem(HELP_SLOT, icon(Material.KNOWLEDGE_BOOK, "How it works", NamedTextColor.GOLD, List.of(
                "Each DRAM Bay holds up to " + MemoryModules.BAY_SLOTS + " memory modules.",
                "Every module keeps its own stock, and the",
                "network uses all of them at once.",
                "",
                "» Install: click a free slot with a module",
                "  on the cursor, Shift-Click one in your",
                "  inventory, or right-click the bay with",
                "  a module in hand.",
                "» Take out: click the module. Its stock",
                "  goes with it and appears in the network",
                "  where you install it next.")));

        for (int i = 0; i < MODULE_SLOTS.length; i++) {
            inv.setItem(MODULE_SLOTS[i], i < modules.size() ? moduleIcon(i, modules.get(i)) : freeSlot(i));
        }

        drawGauge(ITEM_GAUGE_ROW, "Item memory", totals.items, totals.itemCap, "items",
                Material.LIME_STAINED_GLASS_PANE, "No item module installed.");
        drawGauge(FLUID_GAUGE_ROW, "Fluid memory", totals.fluids, totals.fluidCap, "mB",
                Material.LIGHT_BLUE_STAINED_GLASS_PANE, "No Fluid DRAM Module installed.");

        inv.setItem(CLOSE_SLOT, icon(Material.OAK_DOOR, "Close", NamedTextColor.WHITE, List.of()));
    }

    private ItemStack summary(List<NodeBlob> modules, Totals totals) {
        List<String> lines = new ArrayList<>();
        lines.add("Modules: " + modules.size() + " / " + MemoryModules.BAY_SLOTS);
        lines.add("");
        lines.add("Items: " + amount(totals.items, totals.itemCap, "") + "  (" + totals.itemModules + " modules)");
        lines.add("Fluids: " + amount(totals.fluids, totals.fluidCap, " mB") + "  (" + totals.fluidModules + " modules)");
        ItemStack item = icon(Material.WAXED_COPPER_BULB, "DRAM Bay", NamedTextColor.AQUA, lines);
        item.setAmount(Math.max(1, modules.size()));
        return item;
    }

    private ItemStack moduleIcon(int index, NodeBlob module) {
        DeviceType type = MemoryModules.typeOf(module);
        if (type == null) {
            return icon(Material.BARRIER, "Unknown module", NamedTextColor.RED, List.of("» Click to take it out."));
        }
        long stored;
        long cap;
        String unit;
        if (type.isCacheModule()) {
            stored = module.totalVirtualAmount();
            cap = MemoryModules.itemCapacity(type);
            unit = " items";
        } else {
            stored = module.totalDramFluid();
            cap = Settings.fluidDramCapacity();
            unit = " mB";
        }
        double ratio = cap <= 0 ? 0 : Math.min(1.0, (double) stored / cap);
        ItemStack shown = Items.create(type);
        var meta = shown.getItemMeta();
        meta.displayName(Component.text("#" + (index + 1) + "  " + type.display(), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(bar(ratio));
        lore.add(line(amount(stored, cap, unit), NamedTextColor.WHITE));
        if (type.isCacheModule()) {
            int kinds = module.virtualSamples == null ? 0 : module.virtualSamples.size();
            lore.add(line("Item types: " + kinds, NamedTextColor.GRAY));
        } else {
            for (int i = 0; i < module.dramFluids.size(); i++) {
                lore.add(line(" • " + module.dramFluids.get(i) + ": "
                        + Items.formatAmount(module.dramFluidAmounts.get(i)) + " mB", NamedTextColor.GRAY));
            }
        }
        lore.add(Component.empty());
        lore.add(line("» Click to take it out with its stock.", NamedTextColor.YELLOW));
        meta.lore(lore);
        if (stored > 0) {
            meta.setEnchantmentGlintOverride(true);
        }
        shown.setItemMeta(meta);
        return shown;
    }

    private ItemStack freeSlot(int index) {
        return icon(Material.BLACK_STAINED_GLASS_PANE, "Free slot #" + (index + 1), NamedTextColor.DARK_GRAY, List.of(
                "» Click here with a memory module on the",
                "  cursor, or Shift-Click one in your inventory.",
                "Item modules: L1, L2, L3, DRAM, Quantum.",
                "Fluid DRAM Module: fluids only."));
    }

    /** A row of 9 panes filled in proportion to {@code used / cap} / Fila de 9 paneles proporcional. */
    private void drawGauge(int rowStart, String title, long used, long cap, String unit,
                           Material fillMaterial, String emptyText) {
        if (cap <= 0) {
            ItemStack none = icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, title, NamedTextColor.GRAY, List.of(emptyText));
            for (int i = 0; i < GAUGE_CELLS; i++) {
                inv.setItem(rowStart + i, none);
            }
            return;
        }
        double ratio = Math.min(1.0, (double) used / cap);
        int filled = used <= 0 ? 0 : Math.max(1, (int) Math.round(ratio * GAUGE_CELLS));
        Material fill = ratio >= 0.9 ? Material.RED_STAINED_GLASS_PANE
                : ratio >= 0.7 ? Material.YELLOW_STAINED_GLASS_PANE : fillMaterial;
        NamedTextColor color = ratio >= 0.9 ? NamedTextColor.RED : ratio >= 0.7 ? NamedTextColor.YELLOW : NamedTextColor.GREEN;
        List<String> lore = List.of(amount(used, cap, " " + unit), "Free: " + Items.formatAmount(cap - used) + " " + unit);
        String name = title + ": " + percent(ratio);
        for (int i = 0; i < GAUGE_CELLS; i++) {
            inv.setItem(rowStart + i, i < filled
                    ? icon(fill, name, color, lore)
                    : icon(Material.BLACK_STAINED_GLASS_PANE, name, NamedTextColor.DARK_GRAY, lore));
        }
    }

    // ------------------------------------------------------------------ clicks

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        NodeBlob blob = NodeStore.get(block);
        if (blob == null) {
            player.closeInventory();
            return;
        }
        if (raw >= inv.getSize()) {
            // Solo llegan los shift-clic del inventario del jugador (ver GuiListener).
            ItemStack mover = event.getCurrentItem();
            DeviceType moverType = Items.typeOf(mover);
            if (moverType == null || !moverType.isMemoryModule()) {
                return;
            }
            int playerSlot = playerInventorySlot(event);
            if (install(blob, mover)) {
                if (mover.getAmount() > 1) {
                    mover.setAmount(mover.getAmount() - 1);
                    player.getInventory().setItem(playerSlot, mover);
                } else {
                    player.getInventory().setItem(playerSlot, null);
                }
                player.updateInventory();
            }
            return;
        }
        if (raw == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        int index = raw - FIRST_MODULE_SLOT;
        if (index < 0 || index >= MODULE_SLOTS.length) {
            return;
        }
        if (index < MemoryModules.modules(blob).size()) {
            ItemStack module = MemoryModules.eject(blob, index);
            NodeStore.put(block, blob);
            invalidate();
            giveOrDrop(module);
            player.sendMessage(Text.msg("Module taken out with its stock.", NamedTextColor.YELLOW));
            draw();
            return;
        }
        ItemStack cursor = event.getView().getCursor();
        DeviceType cursorType = Items.typeOf(cursor);
        if (cursorType != null && cursorType.isMemoryModule() && install(blob, cursor)) {
            if (cursor.getAmount() > 1) {
                cursor.setAmount(cursor.getAmount() - 1);
                event.getView().setCursor(cursor);
            } else {
                event.getView().setCursor(null);
            }
        }
    }

    private boolean install(NodeBlob blob, ItemStack moduleItem) {
        if (MemoryModules.freeSlots(blob) <= 0) {
            player.sendMessage(Text.msg("This DRAM Bay is full (" + MemoryModules.BAY_SLOTS
                    + " modules). Take one out or use another bay.", NamedTextColor.RED));
            return false;
        }
        if (!MemoryModules.install(blob, moduleItem)) {
            return false;
        }
        NodeStore.put(block, blob);
        invalidate();
        player.sendMessage(Text.msg("Installed " + Items.typeOf(moduleItem).display() + ".", NamedTextColor.GREEN));
        draw();
        return true;
    }

    private void invalidate() {
        Network net = plugin.networks().networkAt(block);
        if (net != null) {
            net.storage().invalidate();
        }
    }

    // ------------------------------------------------------------------ helpers

    /** What the bay holds, summed over its modules / Lo que guarda el bay, sumando sus módulos. */
    private static final class Totals {
        long items;
        long itemCap;
        int itemModules;
        long fluids;
        long fluidCap;
        int fluidModules;

        static Totals of(List<NodeBlob> modules) {
            Totals totals = new Totals();
            for (NodeBlob module : modules) {
                DeviceType type = MemoryModules.typeOf(module);
                if (type == null) {
                    continue;
                }
                if (type.isCacheModule()) {
                    totals.items += module.totalVirtualAmount();
                    totals.itemCap += MemoryModules.itemCapacity(type);
                    totals.itemModules++;
                } else {
                    totals.fluids += module.totalDramFluid();
                    totals.fluidCap += Settings.fluidDramCapacity();
                    totals.fluidModules++;
                }
            }
            return totals;
        }
    }

    private static String amount(long used, long cap, String unit) {
        return Items.formatAmount(used) + " / " + Items.formatAmount(cap) + unit;
    }

    private static String percent(double ratio) {
        return Math.round(ratio * 100) + "%";
    }

    /** "■■■■□□□□□□ 40%" coloured by how full it is / Barra coloreada según lo lleno. */
    private static Component bar(double ratio) {
        int filled = (int) Math.round(ratio * BAR_CELLS);
        NamedTextColor color = ratio >= 0.9 ? NamedTextColor.RED : ratio >= 0.7 ? NamedTextColor.YELLOW : NamedTextColor.GREEN;
        return Component.text("■".repeat(filled), color)
                .append(Component.text("□".repeat(BAR_CELLS - filled), NamedTextColor.DARK_GRAY))
                .append(Component.text(" " + percent(ratio), NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false);
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private static ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack icon(Material material, String name, NamedTextColor color, List<String> lore) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        List<Component> lines = new ArrayList<>();
        for (String text : lore) {
            NamedTextColor lineColor = text.startsWith("»") ? NamedTextColor.YELLOW : NamedTextColor.GRAY;
            lines.add(line(text, lineColor));
        }
        meta.lore(lines);
        item.setItemMeta(meta);
        return item;
    }
}

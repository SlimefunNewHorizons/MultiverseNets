package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.MemoryModules;
import com.chagui68.multiversenets.net.ModuleIO;
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
 * [EN] DRAM Unloader menu: a header with the summary and help, one row of 9 card slots, and a
 * footer. Cards put here are emptied into the network every transfer cycle; an empty card shows a
 * check mark and can be taken out. The menu refreshes itself while open.
 *
 * [ES] Menú del DRAM Unloader: cabecera con el resumen y la ayuda, una fila de 9 huecos de tarjeta y
 * un pie. Las tarjetas que se ponen aquí se vacían en la red en cada ciclo de transferencia; una
 * tarjeta vacía muestra un visto y se puede sacar. El menú se actualiza solo mientras está abierto.
 */
public class ModuleUnloaderMenu extends ModuleHolderMenu {

    public static final int SIZE = 27;
    public static final int STATS_SLOT = 4;
    public static final int HELP_SLOT = 8;
    public static final int FIRST_SLOT = 9;
    public static final int TAKE_EMPTY_SLOT = 21;
    public static final int CLOSE_SLOT = 22;
    private static final int BAR_CELLS = 10;

    public ModuleUnloaderMenu(MultiverseNets plugin, Player player, Block block) {
        super(plugin, player, block);
    }

    public void openMenu() {
        openMenu(SIZE, Component.text("DRAM Unloader", NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false));
    }

    @Override
    protected void draw() {
        NodeBlob blob = NodeStore.get(block);
        List<NodeBlob> modules = MemoryModules.modules(blob);
        ItemStack header = MenuIcons.pane(Material.ORANGE_STAINED_GLASS_PANE);
        ItemStack footer = MenuIcons.pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, header);
            inv.setItem(18 + i, footer);
        }
        long left = 0;
        int emptyCards = 0;
        for (NodeBlob module : modules) {
            long stored = ModuleIO.stored(module);
            left += stored;
            if (stored <= 0) {
                emptyCards++;
            }
        }
        ItemStack stats = MenuIcons.icon(Material.WAXED_EXPOSED_COPPER_BULB, "DRAM Unloader", NamedTextColor.AQUA, List.of(
                "Cards: " + modules.size() + " / " + ModuleIO.UNLOADER_SLOTS,
                "Still to unload: " + Items.formatAmount(left),
                "Empty cards: " + emptyCards,
                "",
                "Speed: " + Items.formatAmount(Settings.moduleItemsPerOp()) + " items and",
                "  " + Items.formatAmount(Settings.moduleFluidPerOp()) + " mB per cycle"));
        stats.setAmount(Math.max(1, modules.size()));
        inv.setItem(STATS_SLOT, stats);
        inv.setItem(HELP_SLOT, MenuIcons.icon(Material.KNOWLEDGE_BOOK, "How it works", NamedTextColor.GOLD, List.of(
                "Put memory modules (cards) here and",
                "their whole stock flows into the network.",
                "",
                "Items the network can not store stay on",
                "the card; the rest keeps unloading, so the",
                "card ends up as empty as possible.",
                "",
                "» Insert: click a free slot with a card,",
                "  Shift-Click one in your inventory, or",
                "  right-click the block with it.",
                "» Take out: click the card.")));
        for (int i = 0; i < ModuleIO.UNLOADER_SLOTS; i++) {
            inv.setItem(FIRST_SLOT + i, i < modules.size() ? cardIcon(i, modules.get(i)) : freeSlot(i));
        }
        inv.setItem(TAKE_EMPTY_SLOT, MenuIcons.icon(Material.HOPPER, "Take empty cards", NamedTextColor.GREEN, List.of(
                emptyCards + " empty card(s) waiting.",
                "» Click to take them all.")));
        inv.setItem(CLOSE_SLOT, MenuIcons.icon(Material.OAK_DOOR, "Close", NamedTextColor.WHITE, List.of()));
    }

    private ItemStack cardIcon(int index, NodeBlob module) {
        DeviceType type = MemoryModules.typeOf(module);
        if (type == null) {
            return MenuIcons.icon(Material.BARRIER, "Unknown module", NamedTextColor.RED, List.of("» Click to take it out."));
        }
        long stored = ModuleIO.stored(module);
        long cap = ModuleIO.capacity(type);
        String unit = type.isCacheModule() ? " items" : " mB";
        ItemStack shown = Items.create(type);
        var meta = shown.getItemMeta();
        meta.displayName(Component.text("#" + (index + 1) + "  " + type.display(), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(MenuIcons.bar(cap <= 0 ? 0 : Math.min(1.0, (double) stored / cap), BAR_CELLS));
        if (stored <= 0) {
            lore.add(MenuIcons.line("✔ Empty: ready to take out", NamedTextColor.GREEN));
        } else {
            lore.add(MenuIcons.line("Unloading… " + Items.formatAmount(stored) + unit + " left", NamedTextColor.YELLOW));
            lore.add(MenuIcons.line("Items the network can not take stay here.", NamedTextColor.DARK_GRAY));
        }
        lore.add(Component.empty());
        lore.add(MenuIcons.line("» Click to take it out.", NamedTextColor.YELLOW));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(stored > 0);
        shown.setItemMeta(meta);
        return shown;
    }

    private ItemStack freeSlot(int index) {
        return MenuIcons.icon(Material.BLACK_STAINED_GLASS_PANE, "Free slot #" + (index + 1), NamedTextColor.DARK_GRAY, List.of(
                "» Click here with a memory module on the",
                "  cursor, or Shift-Click one in your inventory."));
    }

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        NodeBlob blob = NodeStore.get(block);
        if (blob == null) {
            player.closeInventory();
            return;
        }
        if (raw >= inv.getSize()) {
            shiftInsert(event, blob);
            return;
        }
        if (raw == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        if (raw == TAKE_EMPTY_SLOT) {
            List<NodeBlob> modules = MemoryModules.modules(blob);
            int taken = 0;
            for (int i = modules.size() - 1; i >= 0; i--) {
                if (ModuleIO.stored(modules.get(i)) <= 0) {
                    giveOrDrop(MemoryModules.eject(blob, i));
                    taken++;
                }
            }
            if (taken > 0) {
                NodeStore.put(block, blob);
                player.sendMessage(Text.msg("Took out " + taken + " empty card(s).", NamedTextColor.GREEN));
            }
            draw();
            return;
        }
        int index = raw - FIRST_SLOT;
        if (index < 0 || index >= ModuleIO.UNLOADER_SLOTS) {
            return;
        }
        clickSlot(event, blob, index);
    }
}

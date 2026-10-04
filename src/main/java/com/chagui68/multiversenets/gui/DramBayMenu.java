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
 * [EN] DRAM Bay menu: a 4×4 grid with one slot per module (16 per bay). Clicking an installed module
 * takes it out as an item that keeps everything it stored; clicking a free slot with a module on the
 * cursor, or shift-clicking one in the inventory, installs it. The book on top sums up the bay.
 *
 * [ES] Menú del DRAM Bay: una cuadrícula de 4×4 con un hueco por módulo (16 por bay). Hacer clic en un
 * módulo instalado lo saca como ítem que conserva todo lo que guardaba; hacer clic en un hueco libre
 * con un módulo en el cursor, o shift+clic a uno del inventario, lo instala. El libro de arriba
 * resume el bay.
 */
public class DramBayMenu extends MenuHolder {

    public static final int SIZE = 54;
    public static final int STATS_SLOT = 4;
    public static final int CLOSE_SLOT = 49;
    /** One slot per module, in install order / Un hueco por módulo, en orden de instalación. */
    public static final int[] MODULE_SLOTS = {
            11, 12, 13, 14,
            20, 21, 22, 23,
            29, 30, 31, 32,
            38, 39, 40, 41};

    private final Block block;

    public DramBayMenu(MultiverseNets plugin, Player player, Block block) {
        super(plugin, player);
        this.block = block;
    }

    public void openMenu() {
        open(SIZE, Component.text("DRAM Bay", NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false));
    }

    @Override
    protected void draw() {
        ItemStack background = icon(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.DARK_GRAY, List.of());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, background);
        }
        NodeBlob blob = NodeStore.get(block);
        List<NodeBlob> modules = MemoryModules.modules(blob);
        inv.setItem(STATS_SLOT, statsIcon(modules));
        for (int i = 0; i < MODULE_SLOTS.length; i++) {
            inv.setItem(MODULE_SLOTS[i], i < modules.size() ? moduleIcon(modules.get(i))
                    : icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "Free module slot", NamedTextColor.GRAY, List.of(
                            "Click here with a memory module on the",
                            "cursor, or Shift-Click one in your inventory.",
                            "Item modules: L1, L2, L3, DRAM, Quantum.",
                            "Fluid DRAM Module: fluids only.")));
        }
        inv.setItem(CLOSE_SLOT, icon(Material.OAK_DOOR, "Close", NamedTextColor.WHITE, List.of()));
    }

    private ItemStack moduleIcon(NodeBlob module) {
        DeviceType type = MemoryModules.typeOf(module);
        if (type == null) {
            return icon(Material.BARRIER, "Unknown module", NamedTextColor.RED, List.of("» Click to take it out."));
        }
        ItemStack shown = Items.create(type);
        var meta = shown.getItemMeta();
        List<Component> lore = new ArrayList<>();
        if (type.isCacheModule()) {
            int kinds = module.virtualSamples == null ? 0 : module.virtualSamples.size();
            lore.add(line("Stored: " + Items.formatAmount(module.totalVirtualAmount()) + " / "
                    + Items.formatAmount(MemoryModules.itemCapacity(type)) + " items", NamedTextColor.AQUA));
            lore.add(line("Item types: " + kinds, NamedTextColor.GRAY));
        } else {
            lore.add(line("Stored: " + Items.formatAmount(module.totalDramFluid()) + " / "
                    + Items.formatAmount(Settings.fluidDramCapacity()) + " mB", NamedTextColor.AQUA));
            for (int i = 0; i < module.dramFluids.size(); i++) {
                lore.add(line(" • " + module.dramFluids.get(i) + ": "
                        + Items.formatAmount(module.dramFluidAmounts.get(i)) + " mB", NamedTextColor.GRAY));
            }
        }
        lore.add(Component.empty());
        lore.add(line("» Click to take it out with its stock.", NamedTextColor.YELLOW));
        meta.lore(lore);
        shown.setItemMeta(meta);
        return shown;
    }

    private ItemStack statsIcon(List<NodeBlob> modules) {
        long items = 0;
        long itemCap = 0;
        long fluids = 0;
        long fluidCap = 0;
        for (NodeBlob module : modules) {
            DeviceType type = MemoryModules.typeOf(module);
            if (type == null) {
                continue;
            }
            if (type.isCacheModule()) {
                items += module.totalVirtualAmount();
                itemCap += MemoryModules.itemCapacity(type);
            } else {
                fluids += module.totalDramFluid();
                fluidCap += Settings.fluidDramCapacity();
            }
        }
        List<String> lines = new ArrayList<>();
        lines.add("Modules: " + modules.size() + " / " + MemoryModules.BAY_SLOTS);
        if (itemCap > 0) {
            lines.add("Items: " + Items.formatAmount(items) + " / " + Items.formatAmount(itemCap));
        }
        if (fluidCap > 0) {
            lines.add("Fluids: " + Items.formatAmount(fluids) + " / " + Items.formatAmount(fluidCap) + " mB");
        }
        lines.add("");
        lines.add("Click a module to take it out: its stock");
        lines.add("leaves this network and appears wherever");
        lines.add("you install the module next.");
        return icon(Material.BOOK, "Memory", NamedTextColor.AQUA, lines);
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
        int index = moduleIndex(raw);
        if (index < 0) {
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

    private static int moduleIndex(int raw) {
        for (int i = 0; i < MODULE_SLOTS.length; i++) {
            if (MODULE_SLOTS[i] == raw) {
                return i;
            }
        }
        return -1;
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

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private static ItemStack icon(Material material, String name, NamedTextColor color, List<String> lore) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        List<Component> lines = new ArrayList<>();
        for (String text : lore) {
            lines.add(line(text, NamedTextColor.GRAY));
        }
        meta.lore(lines);
        item.setItemMeta(meta);
        return item;
    }
}

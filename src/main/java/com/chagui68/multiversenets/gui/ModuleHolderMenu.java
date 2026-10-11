package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.MemoryModules;
import com.chagui68.multiversenets.net.ModuleIO;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

/**
 * EN: Base of the DRAM Loader and Unloader menus: inserting and taking out cards, and a redraw
 * every second while the menu is open so progress is live.
 *
 * ES: Base de los menús del DRAM Loader y Unloader: meter y sacar tarjetas, y un redibujado cada
 * segundo mientras el menú está abierto para ver el progreso en vivo.
 */
abstract class ModuleHolderMenu extends MenuHolder {

    protected final Block block;
    private BukkitTask task;

    protected ModuleHolderMenu(MultiverseNets plugin, Player player, Block block) {
        super(plugin, player);
        this.block = block;
    }

    protected void openMenu(int size, Component title) {
        open(size, title);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (inv == null || player.getOpenInventory().getTopInventory().getHolder() != this) {
                stopTask();
                return;
            }
            draw();
        }, 20L, 20L);
    }

    @Override
    protected void onClose(InventoryCloseEvent event) {
        stopTask();
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** Shift-click on a card in the player's inventory / Shift+clic en una tarjeta del inventario. */
    protected boolean shiftInsert(InventoryClickEvent event, NodeBlob blob) {
        int playerSlot = playerInventorySlot(event);
        ItemStack mover = event.getCurrentItem() != null ? event.getCurrentItem()
                : player.getInventory().getItem(playerSlot);
        DeviceType type = Items.typeOf(mover);
        if (type == null || !type.isMemoryModule()) {
            return false;
        }
        if (insert(blob, mover)) {
            if (mover.getAmount() > 1) {
                mover.setAmount(mover.getAmount() - 1);
                player.getInventory().setItem(playerSlot, mover);
            } else {
                player.getInventory().setItem(playerSlot, null);
            }
            player.updateInventory();
        }
        return true;
    }

    /**
     * EN: Click on card slot {@code index}: takes the card out, or inserts the card on the cursor.
     * ES: Clic en el hueco {@code index}: saca la tarjeta, o mete la que hay en el cursor.
     */
    protected void clickSlot(InventoryClickEvent event, NodeBlob blob, int index) {
        if (index < MemoryModules.modules(blob).size()) {
            ItemStack module = MemoryModules.eject(blob, index);
            afterTakeOut(blob);
            NodeStore.put(block, blob);
            giveOrDrop(module);
            draw();
            return;
        }
        ItemStack cursor = event.getView().getCursor();
        DeviceType cursorType = Items.typeOf(cursor);
        if (cursorType != null && cursorType.isMemoryModule() && insert(blob, cursor)) {
            if (cursor.getAmount() > 1) {
                cursor.setAmount(cursor.getAmount() - 1);
                event.getView().setCursor(cursor);
            } else {
                event.getView().setCursor(null);
            }
        }
    }

    private boolean insert(NodeBlob blob, ItemStack moduleItem) {
        if (ModuleIO.freeSlots(blob) <= 0) {
            player.sendMessage(Text.msg("There is no free card slot. Take a card out first.", NamedTextColor.RED));
            return false;
        }
        if (!ModuleIO.install(blob, moduleItem)) {
            return false;
        }
        NodeStore.put(block, blob);
        player.playSound(block.getLocation(), org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.4f);
        draw();
        return true;
    }

    /** Hook for the Loader: a card taken out stops it / Al sacar la tarjeta el Loader se para. */
    protected void afterTakeOut(NodeBlob blob) {
    }
}

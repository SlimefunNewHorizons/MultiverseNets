package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.Camouflage;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
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
 * [EN] Network Cable menu: connection status and the camouflage slot. Put a block in the slot and
 * the cable takes its look; the block is kept inside the cable, not consumed. Take it back and the
 * cable is glass again.
 *
 * [ES] Menú del Cable de red: estado de la conexión y el hueco de camuflaje. Pon un bloque en el
 * hueco y el cable toma su aspecto; el bloque se guarda dentro del cable, no se consume. Recupéralo
 * y el cable vuelve a ser de cristal.
 */
public class CableCamouflageMenu extends MenuHolder {

    public static final int SIZE = 27;
    public static final int STATUS_SLOT = 4;
    public static final int HELP_SLOT = 11;
    public static final int CAMO_SLOT = 13;
    public static final int REMOVE_SLOT = 15;
    public static final int CLOSE_SLOT = 22;

    private final Block block;

    public CableCamouflageMenu(MultiverseNets plugin, Player player, Block block) {
        super(plugin, player);
        this.block = block;
    }

    public void openMenu() {
        open(SIZE, Component.text("Network Cable", NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false));
    }

    @Override
    protected void draw() {
        NodeBlob cable = NodeStore.get(block);
        ItemStack header = MenuIcons.pane(Material.CYAN_STAINED_GLASS_PANE);
        ItemStack filler = MenuIcons.pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, i < 9 ? header : filler);
        }
        Network net = plugin.networks().networkAt(block);
        inv.setItem(STATUS_SLOT, net == null
                ? MenuIcons.icon(Material.RED_STAINED_GLASS, "Disconnected", NamedTextColor.RED, List.of(
                        "No controller is reached from here.",
                        "Check that the cables reach a Controller."))
                : MenuIcons.icon(Material.LIME_STAINED_GLASS, "Connected", NamedTextColor.GREEN, List.of(
                        "Network of " + net.size() + " node(s).")));
        inv.setItem(HELP_SLOT, MenuIcons.icon(Material.KNOWLEDGE_BOOK, "Camouflage", NamedTextColor.GOLD, List.of(
                "Hide this cable inside your build: put",
                "a block in the slot and the cable looks",
                "exactly like it. It still works the same.",
                "",
                "The block is kept, not used up. Take it",
                "back and the cable is glass again.",
                "Breaking the cable drops both.",
                "",
                "Solid, full blocks only: nothing with an",
                "inventory, nothing that falls or changes.")));
        ItemStack camo = cable == null ? null : cable.camouflage;
        if (camo == null) {
            inv.setItem(CAMO_SLOT, MenuIcons.icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "Camouflage slot",
                    NamedTextColor.GRAY, List.of(
                            "» Click with a block on the cursor,",
                            "  or Shift-Click one in your inventory.")));
            inv.setItem(REMOVE_SLOT, filler);
        } else {
            ItemStack shown = camo.clone();
            var meta = shown.getItemMeta();
            List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            lore.add(MenuIcons.line("Camouflage in use", NamedTextColor.AQUA));
            lore.add(MenuIcons.line("» Click to take it back.", NamedTextColor.YELLOW));
            lore.add(MenuIcons.line("» Click with another block to swap.", NamedTextColor.YELLOW));
            meta.lore(lore);
            shown.setItemMeta(meta);
            inv.setItem(CAMO_SLOT, shown);
            inv.setItem(REMOVE_SLOT, MenuIcons.icon(Material.GLASS, "Remove camouflage", NamedTextColor.WHITE, List.of(
                    "The block goes back to your inventory",
                    "and the cable is glass again.",
                    "» Click to remove.")));
        }
        inv.setItem(CLOSE_SLOT, MenuIcons.icon(Material.OAK_DOOR, "Close", NamedTextColor.WHITE, List.of()));
    }

    @Override
    protected void click(InventoryClickEvent event) {
        // Siempre el blob recien leido: dos jugadores con el menu abierto nunca sacan el mismo bloque.
        NodeBlob cable = NodeStore.get(block);
        if (cable == null || DeviceType.parse(cable.typeName) != DeviceType.MVN_CABLE) {
            player.closeInventory();
            return;
        }
        int raw = event.getRawSlot();
        if (raw >= inv.getSize()) {
            int playerSlot = playerInventorySlot(event);
            ItemStack mover = event.getCurrentItem() != null ? event.getCurrentItem()
                    : player.getInventory().getItem(playerSlot);
            if (!Camouflage.allowed(mover)) {
                refuse(mover);
                return;
            }
            ItemStack previous = Camouflage.apply(block, cable, mover);
            if (mover.getAmount() > 1) {
                mover.setAmount(mover.getAmount() - 1);
                player.getInventory().setItem(playerSlot, mover);
            } else {
                player.getInventory().setItem(playerSlot, null);
            }
            giveOrDrop(previous);
            player.updateInventory();
            done();
            return;
        }
        switch (raw) {
            case CLOSE_SLOT -> player.closeInventory();
            case REMOVE_SLOT -> {
                ItemStack back = Camouflage.remove(block, cable);
                if (back != null) {
                    giveOrDrop(back);
                    done();
                }
            }
            case CAMO_SLOT -> {
                ItemStack cursor = event.getView().getCursor();
                if (cursor == null || cursor.getType().isAir()) {
                    ItemStack back = Camouflage.remove(block, cable);
                    if (back != null) {
                        event.getView().setCursor(back);
                        done();
                    }
                    return;
                }
                if (!Camouflage.allowed(cursor)) {
                    refuse(cursor);
                    return;
                }
                ItemStack previous = Camouflage.apply(block, cable, cursor);
                if (cursor.getAmount() > 1) {
                    cursor.setAmount(cursor.getAmount() - 1);
                    event.getView().setCursor(cursor);
                    giveOrDrop(previous);
                } else {
                    // El cursor se queda con el bloque que habia antes (o vacio).
                    event.getView().setCursor(previous);
                }
                done();
            }
            default -> {
            }
        }
    }

    private void refuse(ItemStack item) {
        if (item != null && !item.getType().isAir()) {
            player.sendMessage(Text.msg("That block can not camouflage a cable: use a solid, full block "
                    + "without an inventory that does not fall or change by itself.", NamedTextColor.RED));
        }
    }

    private void done() {
        player.playSound(block.getLocation(), org.bukkit.Sound.BLOCK_STONE_PLACE, 0.8f, 1.2f);
        draw();
    }
}

package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.StackUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * EN: The Terminal's custom pages of a player: favourite items pinned to each page, plus the page
 * names. Kept in the player's PDC, so they follow the player to any Terminal and any network.
 *
 * ES: Las páginas personalizadas de la Terminal de un jugador: los ítems favoritos fijados en cada
 * página y los nombres de página. Se guardan en el PDC del jugador, así lo acompañan a cualquier
 * Terminal y a cualquier red.
 */
public final class TerminalPages {

    public static final int COUNT = 5;
    private static final int MAX_NAME = 24;

    private final Plugin plugin;
    private final Player player;
    private final List<List<ItemStack>> pages = new ArrayList<>();

    public TerminalPages(Plugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        for (int i = 0; i < COUNT; i++) {
            List<ItemStack> items = new ArrayList<>();
            String data = pdc.get(itemsKey(i), PersistentDataType.STRING);
            if (data != null) {
                // Mismo transporte que las plantillas de la llave: un NodeBlob con filterItems.
                NodeBlob carrier = NodeStore.decode(data);
                if (carrier != null && carrier.filterItems != null) {
                    for (ItemStack it : carrier.filterItems) {
                        if (it != null && !it.getType().isAir()) {
                            items.add(it);
                        }
                    }
                }
            }
            pages.add(items);
        }
    }

    /** Pinned samples of a page, in pin order / Muestras fijadas de una página, en orden. */
    public List<ItemStack> items(int page) {
        return pages.get(page);
    }

    public boolean contains(int page, ItemStack item) {
        return indexOf(page, item) >= 0;
    }

    /** Pins or unpins; true when the item is now on the page / Fija o quita; true si ahora está. */
    public boolean toggle(int page, ItemStack item) {
        int index = indexOf(page, item);
        if (index >= 0) {
            pages.get(page).remove(index);
            save(page);
            return false;
        }
        pages.get(page).add(StackUtils.getAsQuantity(item, 1));
        save(page);
        return true;
    }

    public void clear(int page) {
        pages.get(page).clear();
        save(page);
    }

    public String name(int page) {
        String name = player.getPersistentDataContainer().get(nameKey(page), PersistentDataType.STRING);
        return name == null || name.isBlank() ? "Page " + (page + 1) : name;
    }

    public void rename(int page, String name) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        String clean = name == null ? "" : name.strip();
        if (clean.isEmpty()) {
            pdc.remove(nameKey(page));
            return;
        }
        pdc.set(nameKey(page), PersistentDataType.STRING,
                clean.length() > MAX_NAME ? clean.substring(0, MAX_NAME) : clean);
    }

    private int indexOf(int page, ItemStack item) {
        List<ItemStack> items = pages.get(page);
        for (int i = 0; i < items.size(); i++) {
            if (StackUtils.itemsMatch(items.get(i), item)) {
                return i;
            }
        }
        return -1;
    }

    private void save(int page) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        List<ItemStack> items = pages.get(page);
        if (items.isEmpty()) {
            pdc.remove(itemsKey(page));
            return;
        }
        NodeBlob carrier = NodeBlob.create("MVN_TERMINAL_PAGE");
        carrier.filterItems = new ArrayList<>(items);
        pdc.set(itemsKey(page), PersistentDataType.STRING, NodeStore.encode(carrier));
    }

    private NamespacedKey itemsKey(int page) {
        return new NamespacedKey(plugin, "terminal_page_" + (page + 1));
    }

    private NamespacedKey nameKey(int page) {
        return new NamespacedKey(plugin, "terminal_page_" + (page + 1) + "_name");
    }
}

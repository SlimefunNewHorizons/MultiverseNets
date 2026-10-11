package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.ModuleIO;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
import com.chagui68.multiversenets.util.StackUtils;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [EN] Quota Manager menu: every Quota Limiter of the network on one screen.
 * <pre>
 *  row 0     header   summary (limiters used / free) · help
 *  rows 1-4  36 limiters per page: configured ones first (item, limit, stock, usage bar), then the free ones
 *  row 5     footer   previous · add item · next
 * </pre>
 * An item is added to a free limiter (click Add with the item on the cursor, click a free limiter,
 * or shift-click the item in the inventory). On a configured limiter: left-click types a new limit,
 * shift+right-click toggles it, right-click removes the item (the limiter becomes free), and a
 * click with an item on the cursor replaces its item. Without a free limiter nothing can be added:
 * place another Quota Limiter in the network.
 *
 * [ES] Menú del Quota Manager: todos los Quota Limiters de la red en una pantalla. Los configurados
 * primero (ítem, límite, stock y barra de uso), después los libres. Un ítem se añade a un limitador
 * libre (Add con el ítem en el cursor, clic en un limitador libre o shift+clic al ítem del
 * inventario). En uno configurado: clic izquierdo escribe un límite nuevo, shift+clic derecho lo
 * activa o desactiva, clic derecho quita el ítem (el limitador queda libre) y un clic con un ítem en
 * el cursor lo sustituye. Sin un limitador libre no se puede añadir nada.
 */
public class QuotaManagerMenu extends MenuHolder {

    public static final int SIZE = 54;
    public static final int SUMMARY_SLOT = 4;
    public static final int HELP_SLOT = 8;
    public static final int FIRST_ENTRY = 9;
    public static final int PAGE_SIZE = 36;
    public static final int PREV_SLOT = 45;
    public static final int ADD_SLOT = 49;
    public static final int NEXT_SLOT = 53;
    /** Limit given to a limiter that gets its first item / Límite de un limitador recién asignado. */
    public static final long DEFAULT_LIMIT = 1000;

    private final Network network;
    /** Grid slot → limiter position shown there / Hueco → posición del limitador. */
    private final Map<Integer, Long> shown = new HashMap<>();
    private int page;
    private BukkitTask task;

    public QuotaManagerMenu(MultiverseNets plugin, Player player, Network network) {
        super(plugin, player);
        this.network = network;
    }

    public void openMenu() {
        open(SIZE, Component.text("Quota Manager", NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false));
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (inv == null || player.getOpenInventory().getTopInventory().getHolder() != this) {
                stopTask();
                return;
            }
            draw();
        }, 40L, 40L);
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

    // ------------------------------------------------------------------ datos

    /** A limiter of the network as the menu shows it / Un limitador tal como lo muestra el menú. */
    private record Entry(long pos, NodeBlob blob) {
        boolean free() {
            return blob.quotaSample == null || blob.quotaSample.getType().isAir();
        }
    }

    /** Configured limiters (by item name) then free ones; unloaded chunks are skipped. */
    private List<Entry> entries() {
        List<Entry> configured = new ArrayList<>();
        List<Entry> free = new ArrayList<>();
        network.forEach(DeviceType.MVN_LIMITER, (pos, type) -> {
            Block block = network.block(pos);
            if (!block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) {
                return;
            }
            NodeBlob blob = NodeStore.get(block);
            if (blob == null) {
                return;
            }
            Entry entry = new Entry(pos, blob);
            (entry.free() ? free : configured).add(entry);
        });
        configured.sort((a, b) -> name(a.blob.quotaSample).compareToIgnoreCase(name(b.blob.quotaSample)));
        free.sort((a, b) -> Long.compare(a.pos, b.pos));
        configured.addAll(free);
        return configured;
    }

    private static String name(ItemStack item) {
        if (item == null) {
            return "";
        }
        var meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(meta.displayName());
        }
        return item.getType().name();
    }

    private long stock(ItemStack sample) {
        return network.storage().count(i -> StackUtils.itemsMatch(i, sample));
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    protected void draw() {
        List<Entry> entries = entries();
        int used = 0;
        for (Entry entry : entries) {
            if (!entry.free()) {
                used++;
            }
        }
        int free = entries.size() - used;
        int pages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.min(page, pages - 1);

        ItemStack header = MenuIcons.pane(Material.CYAN_STAINED_GLASS_PANE);
        ItemStack footer = MenuIcons.pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, header);
            inv.setItem(45 + i, footer);
        }
        ItemStack summary = MenuIcons.icon(Material.VERDANT_FROGLIGHT, "Quota Manager", NamedTextColor.AQUA, List.of(
                "Quota Limiters in this network: " + entries.size(),
                "In use: " + used + "   Free: " + free,
                "",
                free > 0 ? "You can add " + free + " more item(s)." : "Place another Quota Limiter",
                free > 0 ? "" : "in the network to add more items."));
        summary.setAmount(Math.max(1, Math.min(64, entries.size())));
        inv.setItem(SUMMARY_SLOT, summary);
        inv.setItem(HELP_SLOT, MenuIcons.icon(Material.KNOWLEDGE_BOOK, "How it works", NamedTextColor.GOLD, List.of(
                "Every Quota Limiter of the network, with",
                "how much of its item the network holds.",
                "",
                "» Add: click Add with an item on the cursor,",
                "  click a free limiter with it, or",
                "  Shift-Click the item in your inventory.",
                "  It uses one free Quota Limiter.",
                "» Left-Click a limiter: type a new limit.",
                "» Shift+Right-Click: turn it on / off.",
                "» Right-Click: remove its item (it is free).",
                "» Click with an item: replace its item.")));

        shown.clear();
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int slot = FIRST_ENTRY + i;
            int index = start + i;
            if (index < entries.size()) {
                Entry entry = entries.get(index);
                shown.put(slot, entry.pos);
                inv.setItem(slot, entry.free() ? freeIcon(entry) : limiterIcon(entry));
            } else {
                inv.setItem(slot, null);
            }
        }

        inv.setItem(PREV_SLOT, page > 0
                ? MenuIcons.icon(Material.ARROW, "◀ Previous Page", NamedTextColor.YELLOW, List.of())
                : footer);
        inv.setItem(NEXT_SLOT, page < pages - 1
                ? MenuIcons.icon(Material.ARROW, "Next Page ▶", NamedTextColor.YELLOW, List.of())
                : footer);
        inv.setItem(ADD_SLOT, free > 0
                ? MenuIcons.icon(Material.LIME_DYE, "Add item (" + free + " free)", NamedTextColor.GREEN, List.of(
                        "» Click with an item on the cursor,",
                        "  or Shift-Click one in your inventory.",
                        "New limit: " + Items.formatAmount(DEFAULT_LIMIT) + " (click the limiter to change it)."))
                : MenuIcons.icon(Material.GRAY_DYE, "Add item (no free limiter)", NamedTextColor.DARK_GRAY, List.of(
                        "Every Quota Limiter already has an item.",
                        "Place another Quota Limiter in the network,",
                        "or remove an item with Right-Click.")));
    }

    private ItemStack limiterIcon(Entry entry) {
        NodeBlob blob = entry.blob;
        ItemStack icon = StackUtils.getAsQuantity(blob.quotaSample, 1);
        var meta = icon.getItemMeta();
        long stock = stock(blob.quotaSample);
        long limit = blob.quotaLimit;
        double ratio = limit <= 0 ? (stock > 0 ? 1.0 : 0.0) : Math.min(1.0, (double) stock / limit);
        List<Component> lore = new ArrayList<>();
        lore.add(MenuIcons.bar(ratio, 10));
        lore.add(MenuIcons.line("Stock: " + exact(stock) + " / " + exact(limit), NamedTextColor.WHITE));
        lore.add(MenuIcons.line(stock >= limit ? "Limit reached: imports of this item stop"
                : "Room before the limit: " + exact(limit - stock), stock >= limit ? NamedTextColor.RED : NamedTextColor.GREEN));
        lore.add(MenuIcons.line(blob.quotaActive ? "● Enforcing" : "○ Disabled (not enforced)",
                blob.quotaActive ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        lore.add(MenuIcons.line("Limiter at " + coords(entry.pos), NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());
        lore.add(MenuIcons.line("» Left-Click: set the limit", NamedTextColor.YELLOW));
        lore.add(MenuIcons.line("» Shift+Right-Click: turn on / off", NamedTextColor.YELLOW));
        lore.add(MenuIcons.line("» Right-Click: remove this item", NamedTextColor.RED));
        lore.add(MenuIcons.line("» Click with an item: replace it", NamedTextColor.YELLOW));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(blob.quotaActive && stock >= limit);
        icon.setItemMeta(meta);
        return icon;
    }

    private ItemStack freeIcon(Entry entry) {
        return MenuIcons.icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "Free Quota Limiter", NamedTextColor.GRAY, List.of(
                "No item yet.",
                "Limiter at " + coords(entry.pos),
                "",
                "» Click with an item on the cursor",
                "  to limit that item here."));
    }

    private static String coords(long pos) {
        return PosUtil.unpackX(pos) + ", " + PosUtil.unpackY(pos) + ", " + PosUtil.unpackZ(pos);
    }

    private static String exact(long amount) {
        return String.format(java.util.Locale.ROOT, "%,d", amount);
    }

    // ------------------------------------------------------------------ clics

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        ClickType click = event.getClick();
        if (raw >= inv.getSize()) {
            ItemStack item = event.getCurrentItem() != null ? event.getCurrentItem()
                    : player.getInventory().getItem(playerInventorySlot(event));
            if (item != null && !item.getType().isAir()) {
                addToFreeLimiter(item);
            }
            return;
        }
        ItemStack cursor = event.getView().getCursor();
        boolean hasCursor = cursor != null && !cursor.getType().isAir();
        if (raw == PREV_SLOT && page > 0) {
            page--;
            draw();
            return;
        }
        if (raw == NEXT_SLOT) {
            page++;
            draw();
            return;
        }
        if (raw == ADD_SLOT) {
            if (hasCursor) {
                addToFreeLimiter(cursor);
            }
            return;
        }
        Long pos = shown.get(raw);
        if (pos == null) {
            return;
        }
        // Siempre el blob recien leido: dos jugadores con el menu abierto no pisan el mismo limitador.
        Block block = network.block(pos);
        NodeBlob blob = NodeStore.get(block);
        if (blob == null || DeviceType.parse(blob.typeName) != DeviceType.MVN_LIMITER) {
            draw();
            return;
        }
        boolean free = blob.quotaSample == null || blob.quotaSample.getType().isAir();
        if (hasCursor) {
            if (alreadyLimited(cursor, pos)) {
                player.sendMessage(Text.msg(name(cursor) + " already has a Quota Limiter.", NamedTextColor.RED));
                return;
            }
            assign(block, blob, cursor);
            return;
        }
        if (free) {
            return;
        }
        if (click == ClickType.SHIFT_RIGHT) {
            blob.quotaActive = !blob.quotaActive;
            NodeStore.put(block, blob);
            sound(blob.quotaActive ? 1.4f : 0.8f);
            draw();
        } else if (click.isRightClick()) {
            String removed = name(blob.quotaSample);
            blob.quotaSample = null;
            NodeStore.put(block, blob);
            player.sendMessage(Text.msg("Removed " + removed + ": that Quota Limiter is free again.", NamedTextColor.YELLOW));
            sound(0.8f);
            draw();
        } else if (click.isLeftClick()) {
            String item = name(blob.quotaSample);
            player.closeInventory();
            ChatPrompts.ask(player, "Type the limit for " + item + " (e.g. 500, 64k, 1m):", text -> {
                long value = ModuleIO.parseAmount(text);
                NodeBlob fresh = NodeStore.get(block);
                if (value < 0 || "max".equalsIgnoreCase(text == null ? "" : text.trim())) {
                    player.sendMessage(Text.msg("That is not a limit.", NamedTextColor.RED));
                } else if (fresh != null && DeviceType.parse(fresh.typeName) == DeviceType.MVN_LIMITER) {
                    fresh.quotaLimit = value;
                    NodeStore.put(block, fresh);
                    player.sendMessage(Text.msg("Limit for " + item + ": " + exact(value) + ".", NamedTextColor.GREEN));
                }
                openMenu();
            });
        }
    }

    /** True when another limiter of the network already limits this item. */
    private boolean alreadyLimited(ItemStack item, long exceptPos) {
        for (Entry entry : entries()) {
            if (entry.pos != exceptPos && !entry.free() && StackUtils.itemsMatch(entry.blob.quotaSample, item)) {
                return true;
            }
        }
        return false;
    }

    private void addToFreeLimiter(ItemStack item) {
        if (Items.typeOf(item) != null && Items.typeOf(item) == DeviceType.MVN_QUOTA_MANAGER) {
            return;
        }
        if (alreadyLimited(item, Long.MIN_VALUE)) {
            player.sendMessage(Text.msg(name(item) + " already has a Quota Limiter: click it to change its limit.",
                    NamedTextColor.RED));
            return;
        }
        for (Entry entry : entries()) {
            if (entry.free()) {
                Block block = network.block(entry.pos);
                NodeBlob blob = NodeStore.get(block);
                if (blob != null && (blob.quotaSample == null || blob.quotaSample.getType().isAir())) {
                    assign(block, blob, item);
                    return;
                }
            }
        }
        player.sendMessage(Text.msg("No free Quota Limiter: place another one in the network, or remove an item.",
                NamedTextColor.RED));
    }

    /** Puts the item on a limiter; the item itself is only copied, never taken. */
    private void assign(Block block, NodeBlob blob, ItemStack item) {
        boolean wasFree = blob.quotaSample == null || blob.quotaSample.getType().isAir();
        blob.quotaSample = StackUtils.getAsQuantity(item, 1);
        if (wasFree) {
            if (blob.quotaLimit <= 0) {
                blob.quotaLimit = DEFAULT_LIMIT;
            }
            blob.quotaActive = true;
        }
        NodeStore.put(block, blob);
        player.sendMessage(Text.msg("Limiting " + name(item) + " to " + exact(blob.quotaLimit)
                + ". Click it to change the limit.", NamedTextColor.GREEN));
        sound(1.2f);
        draw();
    }

    private void sound(float pitch) {
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.6f, pitch);
    }
}

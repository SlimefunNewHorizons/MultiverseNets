package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.net.NetworkManager;
import com.chagui68.multiversenets.net.NetworkStorage;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Keys;
import com.chagui68.multiversenets.util.StackUtils;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * EN: Main interactive digital storage terminal GUI for MultiverseNets networks.
 * <pre>
 *  row 0  [Normal][Purge][Fluids][Page 1][Page 2][Page 3][Page 4][Page 5][Input]
 *  rows 1-4  36-slot grid
 *  row 5  [Prev][Page x/y][Sort][Search][Network][Pin mode][Deposit all][Help][Next]
 * </pre>
 * Custom pages hold the items a player pinned (favourites), so a job only shows what it needs.
 *
 * ES: Menú principal interactivo de la terminal de almacenamiento digital para redes MultiverseNets.
 * Fila superior de pestañas (vista normal, modo purga, fluidos y 5 páginas personalizadas), una
 * cuadrícula de 36 ranuras y una barra de herramientas abajo. Las páginas personalizadas muestran
 * solo los ítems que el jugador fijó en ellas (favoritos).
 */
public class TerminalMenu extends MenuHolder {

    // Fila superior: pestañas.
    public static final int TAB_NORMAL = 0;
    public static final int TAB_PURGE = 1;
    public static final int TAB_FLUIDS = 2;
    public static final int TAB_FIRST_PAGE = 3;
    public static final int INPUT_SLOT = 8;
    // Fila inferior: herramientas.
    public static final int PREV_SLOT = 45;
    public static final int PAGE_INFO_SLOT = 46;
    public static final int SORT_SLOT = 47;
    public static final int SEARCH_SLOT = 48;
    public static final int NETWORK_INFO_SLOT = 49;
    public static final int PIN_MODE_SLOT = 50;
    public static final int DEPOSIT_ALL_SLOT = 51;
    public static final int HELP_SLOT = 52;
    public static final int NEXT_SLOT = 53;

    public static final int GRID_START = 9;
    private static final int GRID_SIZE = 36;

    private static final Material[] PAGE_DYES = {
            Material.RED_DYE, Material.ORANGE_DYE, Material.YELLOW_DYE, Material.LIME_DYE, Material.LIGHT_BLUE_DYE
    };
    private static final NamedTextColor[] PAGE_COLORS = {
            NamedTextColor.RED, NamedTextColor.GOLD, NamedTextColor.YELLOW, NamedTextColor.GREEN, NamedTextColor.AQUA
    };

    private static final String AMOUNT_PREFIX = "Amount: ";

    /** What the grid shows / Lo que muestra la cuadrícula. */
    public enum View {NORMAL, PURGE, FLUIDS, CUSTOM}

    private enum SortOrder {MVN_ALPHABETIC, MVN_AMOUNT}

    private final Network network;
    private final TerminalPages pages;
    private final ItemStack[] displayedSamples = new ItemStack[54];
    private final java.util.Map<Integer, String> displayedFluids = new java.util.HashMap<>();
    /** Grid slot → recovered memory module shown there (temporary items from an old Controller). */
    private final java.util.Map<Integer, ItemStack> displayedRecovered = new java.util.HashMap<>();
    private View view = View.NORMAL;
    /** Custom page shown when {@link #view} is CUSTOM / Página personalizada abierta. */
    private int customPage = 0;
    /** Page that grid clicks pin to, or -1 when pin mode is off / Página donde fija el modo pin, -1 apagado. */
    private int pinTarget = -1;
    private int page = 0;
    private int pageCount = 1;
    private String query = "";
    private SortOrder sortOrder = SortOrder.MVN_ALPHABETIC;
    private BukkitTask tickTask;

    public TerminalMenu(MultiverseNets plugin, Player player, Network network) {
        super(plugin, player);
        this.network = network;
        this.pages = new TerminalPages(plugin, player);
    }

    /** Storage + fluid stamps the open grid shows, and live ticks since the last full draw. */
    private long drawnStamp = Long.MIN_VALUE;
    private int ticksSinceDraw;

    public void openMenu() {
        cancelTask();
        open(54, Component.text("✦ ", NamedTextColor.DARK_AQUA)
                .append(Component.text("Network Terminal", NamedTextColor.DARK_GRAY))
                .decoration(TextDecoration.ITALIC, false));
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::liveTick, 10L, 10L);
    }

    public View view() {
        return view;
    }

    @Override
    protected java.util.Set<Integer> vanillaSlots() {
        return java.util.Set.of(INPUT_SLOT);
    }

    @Override
    protected boolean acceptsKeyClicks() {
        return true;
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    protected void draw() {
        drawnStamp = network.storage().changeStamp() * 31 + network.fluidStorage().changeStamp();
        ticksSinceDraw = 0;

        java.util.Arrays.fill(displayedSamples, null);
        displayedFluids.clear();
        displayedRecovered.clear();

        int entries = switch (view) {
            case FLUIDS -> drawFluids();
            case CUSTOM -> drawCustom();
            default -> drawItems();
        };
        drawTabs();
        drawToolbar(entries);
    }

    private void drawTabs() {
        inv.setItem(TAB_NORMAL, tab(Material.CHEST, "Network Storage", NamedTextColor.AQUA, view == View.NORMAL, List.of(
                line(network.storage().view().size() + " item types stored", NamedTextColor.GRAY),
                Component.empty(),
                line("Every item in the network.", NamedTextColor.DARK_GRAY))));
        inv.setItem(TAB_PURGE, tab(Material.LAVA_BUCKET, "Purge Mode", NamedTextColor.RED, view == View.PURGE, List.of(
                line("Active purgers: " + network.storage().countActivePurgers(), NamedTextColor.GRAY),
                line("Active greedy cells: " + network.storage().countActiveGreedyCells(), NamedTextColor.GRAY),
                Component.empty(),
                line("Items targeted for destruction", NamedTextColor.DARK_GRAY),
                line("by the active purgers.", NamedTextColor.DARK_GRAY))));
        long totalMb = network.fluidStorage().getTotalAmountMb();
        inv.setItem(TAB_FLUIDS, tab(Material.WATER_BUCKET, "Fluids", NamedTextColor.DARK_AQUA, view == View.FLUIDS, List.of(
                line("Stored liquids: " + network.fluidStorage().getFluids().size() + " types", NamedTextColor.GRAY),
                line("Total volume: " + Items.formatAmount(totalMb) + " mB (" + (totalMb / 1000) + " Buckets)", NamedTextColor.GRAY))));
        for (int i = 0; i < TerminalPages.COUNT; i++) {
            inv.setItem(TAB_FIRST_PAGE + i, pageTab(i));
        }
    }

    private ItemStack pageTab(int i) {
        int pinned = pages.items(i).size();
        List<Component> lore = new ArrayList<>();
        lore.add(line(pinned + (pinned == 1 ? " pinned item" : " pinned items"), NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(line("Click: open this page", NamedTextColor.YELLOW));
        lore.add(line("Right-click: rename", NamedTextColor.YELLOW));
        lore.add(line("Shift+Right-click: clear page", NamedTextColor.RED));
        lore.add(Component.empty());
        lore.add(line("Tip: hover an item and press " + (i + 1), NamedTextColor.DARK_GRAY));
        lore.add(line("to pin it to this page.", NamedTextColor.DARK_GRAY));
        boolean active = view == View.CUSTOM && customPage == i;
        ItemStack item = tab(PAGE_DYES[i], pages.name(i), PAGE_COLORS[i], active, lore);
        item.setAmount(Math.max(1, Math.min(64, pinned)));
        return item;
    }

    private void drawToolbar(int entries) {
        ItemStack filler = filler();
        for (int slot = PREV_SLOT; slot <= NEXT_SLOT; slot++) {
            inv.setItem(slot, filler);
        }
        inv.setItem(PREV_SLOT, page > 0
                ? button(Material.ARROW, "◀ Previous Page", NamedTextColor.YELLOW, List.of())
                : button(Material.GRAY_STAINED_GLASS_PANE, "◀ Previous Page", NamedTextColor.DARK_GRAY, List.of()));
        inv.setItem(NEXT_SLOT, page < pageCount - 1
                ? button(Material.ARROW, "Next Page ▶", NamedTextColor.YELLOW, List.of())
                : button(Material.GRAY_STAINED_GLASS_PANE, "Next Page ▶", NamedTextColor.DARK_GRAY, List.of()));
        ItemStack pageInfo = button(Material.PAPER, "Page " + (page + 1) + " / " + pageCount, NamedTextColor.WHITE, List.of(
                line("Showing: " + viewName(), NamedTextColor.GRAY),
                line(entries + (entries == 1 ? " entry" : " entries"), NamedTextColor.GRAY)));
        pageInfo.setAmount(Math.max(1, Math.min(64, page + 1)));
        inv.setItem(PAGE_INFO_SLOT, pageInfo);

        boolean byAmount = sortOrder == SortOrder.MVN_AMOUNT;
        inv.setItem(SORT_SLOT, button(byAmount ? Material.HOPPER : Material.COMPARATOR,
                "Sort: " + (byAmount ? "Amount" : "A-Z"), NamedTextColor.GOLD, List.of(
                        line((byAmount ? "  " : "▶ ") + "A-Z", byAmount ? NamedTextColor.DARK_GRAY : NamedTextColor.WHITE),
                        line((byAmount ? "▶ " : "  ") + "Amount", byAmount ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY),
                        Component.empty(),
                        line("Click: switch order", NamedTextColor.YELLOW),
                        line("Custom pages keep their pin order.", NamedTextColor.DARK_GRAY))));

        List<Component> searchLore = new ArrayList<>();
        searchLore.add(query.isBlank()
                ? line("No filter active", NamedTextColor.GRAY)
                : line("Filter: \"" + query + "\"", NamedTextColor.WHITE));
        searchLore.add(Component.empty());
        searchLore.add(line("Click: type a search", NamedTextColor.YELLOW));
        searchLore.add(line("Right-click: clear search", NamedTextColor.YELLOW));
        searchLore.add(Component.empty());
        searchLore.add(line("@mod  name or namespace", NamedTextColor.DARK_GRAY));
        searchLore.add(line("#text  lore", NamedTextColor.DARK_GRAY));
        searchLore.add(line(">100  <=64  =1  amount", NamedTextColor.DARK_GRAY));
        ItemStack search = button(query.isBlank() ? Material.SPYGLASS : Material.NAME_TAG,
                query.isBlank() ? "Search" : "Search: " + query, NamedTextColor.AQUA, searchLore);
        glint(search, !query.isBlank());
        inv.setItem(SEARCH_SLOT, search);

        inv.setItem(NETWORK_INFO_SLOT, networkInfo());
        inv.setItem(PIN_MODE_SLOT, pinModeButton());
        inv.setItem(DEPOSIT_ALL_SLOT, button(Material.CHEST_MINECART, "Deposit Inventory", NamedTextColor.GREEN, List.of(
                line("Moves your whole inventory", NamedTextColor.GRAY),
                line("(not the hotbar) into the network.", NamedTextColor.GRAY),
                Component.empty(),
                line("Click: deposit inventory", NamedTextColor.YELLOW),
                line("Shift+Click: include the hotbar", NamedTextColor.YELLOW))));
        inv.setItem(HELP_SLOT, button(Material.KNOWLEDGE_BOOK, "Controls", NamedTextColor.LIGHT_PURPLE, List.of(
                line("Click item: take 1", NamedTextColor.GRAY),
                line("Right-click item: take a stack", NamedTextColor.GRAY),
                line("Shift+Click item: stack to inventory", NamedTextColor.GRAY),
                line("Click with item on cursor: deposit it", NamedTextColor.GRAY),
                line("Shift+Click in your inventory: deposit", NamedTextColor.GRAY),
                Component.empty(),
                line("Keys 1-5 over an item: pin / unpin", NamedTextColor.GRAY),
                line("  it on custom page 1-5", NamedTextColor.GRAY),
                line("Q over an item on a custom page: unpin", NamedTextColor.GRAY),
                Component.empty(),
                line("Top-right slot: drop items or", NamedTextColor.GRAY),
                line("  fluid buckets to store them", NamedTextColor.GRAY))));
    }

    private ItemStack networkInfo() {
        List<NetworkStorage.View> all = network.storage().view();
        long total = 0;
        for (NetworkStorage.View v : all) {
            total += v.amount();
        }
        return button(Material.LODESTONE, "Network Overview", NamedTextColor.DARK_AQUA, List.of(
                line("Item types: " + all.size(), NamedTextColor.GRAY),
                line("Total items: " + exact(total), NamedTextColor.GRAY),
                line("Fluids: " + Items.formatAmount(network.fluidStorage().getTotalAmountMb()) + " mB", NamedTextColor.GRAY),
                line("Purgers: " + network.storage().countActivePurgers()
                        + "  Greedy cells: " + network.storage().countActiveGreedyCells(), NamedTextColor.GRAY)));
    }

    private ItemStack pinModeButton() {
        if (pinTarget < 0) {
            return button(Material.WRITABLE_BOOK, "Pin Mode: OFF", NamedTextColor.GRAY, List.of(
                    line("While on, clicking an item pins or", NamedTextColor.GRAY),
                    line("unpins it on the chosen page", NamedTextColor.GRAY),
                    line("instead of taking it.", NamedTextColor.GRAY),
                    Component.empty(),
                    line("Click: next page", NamedTextColor.YELLOW),
                    line("Right-click: previous page / off", NamedTextColor.YELLOW)));
        }
        ItemStack item = button(PAGE_DYES[pinTarget], "Pin Mode: " + pages.name(pinTarget), PAGE_COLORS[pinTarget], List.of(
                line("Click an item to pin or unpin it", NamedTextColor.GRAY),
                line("on " + pages.name(pinTarget) + ".", NamedTextColor.GRAY),
                Component.empty(),
                line("Click: next page", NamedTextColor.YELLOW),
                line("Right-click: previous page / off", NamedTextColor.YELLOW)));
        glint(item, true);
        return item;
    }

    private int drawItems() {
        List<NetworkStorage.View> list = filteredItems();
        // Los modulos recuperados de un Controlador antiguo van primero, aparte del stock.
        List<ItemStack> recovered = view == View.PURGE ? List.of() : recoveredModules();
        int total = recovered.size() + list.size();
        int start = clampPage(total);
        for (int i = 0; i < GRID_SIZE; i++) {
            int slot = GRID_START + i;
            int index = start + i;
            if (index < recovered.size()) {
                displayedRecovered.put(slot, recovered.get(index));
                inv.setItem(slot, recoveredIcon(recovered.get(index)));
            } else if (index - recovered.size() < list.size()) {
                NetworkStorage.View entry = list.get(index - recovered.size());
                displayedSamples[slot] = entry.sample();
                inv.setItem(slot, gridIcon(entry.sample(), entry.amount()));
            } else {
                inv.setItem(slot, null);
            }
        }
        if (total == 0) {
            inv.setItem(GRID_START + 13, emptyNotice(view == View.PURGE
                    ? "No items targeted by purgers"
                    : query.isBlank() ? "The network is empty" : "Nothing matches \"" + query + "\""));
        }
        return total;
    }

    /** A custom page: the pinned items in pin order, also when the network holds none of them. */
    private int drawCustom() {
        List<NetworkStorage.View> stock = network.storage().view();
        List<ItemStack> list = new ArrayList<>();
        List<Long> amounts = new ArrayList<>();
        String q = query.toLowerCase();
        for (ItemStack pinned : pages.items(customPage)) {
            long amount = 0;
            for (NetworkStorage.View v : stock) {
                if (StackUtils.itemsMatch(v.sample(), pinned)) {
                    amount = v.amount();
                    break;
                }
            }
            if (!query.isBlank() && !matchesSearch(pinned, amount, q)) {
                continue;
            }
            list.add(pinned);
            amounts.add(amount);
        }
        int start = clampPage(list.size());
        for (int i = 0; i < GRID_SIZE; i++) {
            int slot = GRID_START + i;
            int index = start + i;
            if (index < list.size()) {
                displayedSamples[slot] = list.get(index);
                inv.setItem(slot, gridIcon(list.get(index), amounts.get(index)));
            } else {
                inv.setItem(slot, null);
            }
        }
        if (list.isEmpty()) {
            inv.setItem(GRID_START + 13, emptyNotice(pages.items(customPage).isEmpty()
                    ? "Nothing pinned yet: hover an item in any view and press " + (customPage + 1)
                    : "Nothing matches \"" + query + "\""));
        }
        return list.size();
    }

    private int drawFluids() {
        List<java.util.Map.Entry<String, Long>> list = new ArrayList<>();
        for (var entry : network.fluidStorage().getFluids().entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                list.add(entry);
            }
        }
        int start = clampPage(list.size());
        for (int i = 0; i < GRID_SIZE; i++) {
            int slot = GRID_START + i;
            int index = start + i;
            if (index < list.size()) {
                var entry = list.get(index);
                displayedFluids.put(slot, entry.getKey());
                inv.setItem(slot, fluidGridIcon(entry.getKey(), entry.getValue()));
            } else {
                inv.setItem(slot, null);
            }
        }
        if (list.isEmpty()) {
            inv.setItem(GRID_START + 13, emptyNotice("No liquids currently stored"));
        }
        return list.size();
    }

    /** Fixes the page against the entry count and returns the first index shown. */
    private int clampPage(int total) {
        pageCount = Math.max(1, (total + GRID_SIZE - 1) / GRID_SIZE);
        if (page >= pageCount) {
            page = pageCount - 1;
        }
        return page * GRID_SIZE;
    }

    private String viewName() {
        return switch (view) {
            case NORMAL -> "Network Storage";
            case PURGE -> "Purge Mode";
            case FLUIDS -> "Fluids";
            case CUSTOM -> pages.name(customPage);
        };
    }

    private List<ItemStack> recoveredModules() {
        NodeBlob ctrl = NodeStore.get(network.block(network.controllerPos()));
        if (ctrl == null || ctrl.recoveredModules == null) {
            return List.of();
        }
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack module : ctrl.recoveredModules) {
            if (module != null && !module.getType().isAir()) {
                out.add(module);
            }
        }
        return out;
    }

    private ItemStack recoveredIcon(ItemStack module) {
        ItemStack icon = module.clone();
        var meta = icon.getItemMeta();
        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(line("⏳ TEMPORARY ITEM", NamedTextColor.GOLD));
        lore.add(line("This module was inside the Controller.", NamedTextColor.GRAY));
        lore.add(line("Its items are kept inside the module.", NamedTextColor.GRAY));
        lore.add(line("Click to take it, then install it", NamedTextColor.YELLOW));
        lore.add(line("in a DRAM Bay.", NamedTextColor.YELLOW));
        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    /**
     * EN: Hands a recovered module to the player (cursor if empty, else inventory). It is looked up
     * again in the Controller's list, so two viewers can never take the same module.
     * ES: Entrega un módulo recuperado al jugador (al cursor si está vacío, si no al inventario).
     * Se busca de nuevo en la lista del Controlador, así dos jugadores nunca se llevan el mismo.
     */
    private void takeRecovered(InventoryClickEvent event, ItemStack shown) {
        org.bukkit.block.Block ctrlBlock = network.block(network.controllerPos());
        NodeBlob ctrl = NodeStore.get(ctrlBlock);
        if (ctrl == null || ctrl.recoveredModules == null) {
            draw();
            return;
        }
        int index = -1;
        for (int i = 0; i < ctrl.recoveredModules.size(); i++) {
            ItemStack candidate = ctrl.recoveredModules.get(i);
            if (candidate != null && candidate.equals(shown)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            draw();
            return;
        }
        ItemStack module = ctrl.recoveredModules.remove(index);
        NodeStore.put(ctrlBlock, ctrl);
        ItemStack cursor = event.getView().getCursor();
        if (cursor == null || cursor.getType().isAir()) {
            event.getView().setCursor(module);
        } else {
            giveOrDrop(module);
        }
        player.sendMessage(Text.msg("Module taken. Install it in a DRAM Bay to bring its items back.", NamedTextColor.GREEN));
        draw();
    }

    private List<NetworkStorage.View> filteredItems() {
        List<NetworkStorage.View> all = view == View.PURGE
                ? network.storage().getPurgedItemsView()
                : network.storage().view();
        List<NetworkStorage.View> out = new ArrayList<>(all);
        if (sortOrder == SortOrder.MVN_AMOUNT) {
            out.sort(Comparator.comparingLong(NetworkStorage.View::amount).reversed());
        } else {
            // El nombre se calcula una vez por item: como clave del comparador se recalculaba (con
            // dos copias de ItemMeta) en cada comparacion, n·log n veces por redibujado.
            java.util.Map<NetworkStorage.View, String> names = new java.util.IdentityHashMap<>(out.size() * 2);
            for (NetworkStorage.View v : out) {
                names.put(v, readableName(v.sample()));
            }
            out.sort(Comparator.comparing(names::get));
        }
        if (query.isBlank()) {
            return out;
        }
        String q = query.toLowerCase();
        out.removeIf(v -> !matchesSearch(v.sample(), v.amount(), q));
        return out;
    }

    private boolean matchesSearch(ItemStack item, long amount, String q) {
        if (q.startsWith(">=") || q.startsWith("<=") || q.startsWith(">") || q.startsWith("<") || q.startsWith("=")) {
            try {
                if (q.startsWith(">=")) {
                    return amount >= Long.parseLong(q.substring(2).trim());
                } else if (q.startsWith("<=")) {
                    return amount <= Long.parseLong(q.substring(2).trim());
                } else if (q.startsWith(">")) {
                    return amount > Long.parseLong(q.substring(1).trim());
                } else if (q.startsWith("<")) {
                    return amount < Long.parseLong(q.substring(1).trim());
                } else {
                    return amount == Long.parseLong(q.substring(1).trim());
                }
            } catch (NumberFormatException ignored) {
            }
        }
        if (q.startsWith("@")) {
            String filter = q.substring(1).trim();
            if (filter.isEmpty()) return true;
            String typeName = item.getType().name().toLowerCase();
            if (typeName.contains(filter)) return true;
            if (item.hasItemMeta()) {
                var pdc = item.getItemMeta().getPersistentDataContainer();
                for (org.bukkit.NamespacedKey key : pdc.getKeys()) {
                    if (key.getNamespace().toLowerCase().contains(filter) || key.getKey().toLowerCase().contains(filter)) {
                        return true;
                    }
                }
            }
            return false;
        }
        if (q.startsWith("#")) {
            String filter = q.substring(1).trim();
            if (filter.isEmpty()) return true;
            if (item.hasItemMeta() && item.getItemMeta().hasLore() && item.getItemMeta().lore() != null) {
                for (Component line : item.getItemMeta().lore()) {
                    if (plain(line).toLowerCase().contains(filter)) {
                        return true;
                    }
                }
            }
            return false;
        }
        if (readableName(item).toLowerCase().contains(q)) {
            return true;
        }
        if (item.getType().name().toLowerCase().contains(q)) {
            return true;
        }
        if (item.hasItemMeta()) {
            var meta = item.getItemMeta();
            if (meta.hasLore() && meta.lore() != null) {
                for (Component line : meta.lore()) {
                    if (plain(line).toLowerCase().contains(q)) {
                        return true;
                    }
                }
            }
            var pdc = meta.getPersistentDataContainer();
            for (org.bukkit.NamespacedKey key : pdc.getKeys()) {
                if (key.getKey().toLowerCase().contains(q) || key.toString().toLowerCase().contains(q)) {
                    return true;
                }
                try {
                    String val = pdc.get(key, PersistentDataType.STRING);
                    if (val != null && val.toLowerCase().contains(q)) {
                        return true;
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return false;
    }

    private String readableName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return plain(item.getItemMeta().displayName());
        }
        return item.getType().name();
    }

    private ItemStack gridIcon(ItemStack sample, long amount) {
        ItemStack icon = sample.clone();
        icon.setAmount(1);
        var meta = icon.getItemMeta();
        List<Component> lore = meta != null && meta.hasLore() && meta.lore() != null
                ? new ArrayList<>(meta.lore())
                : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(AMOUNT_PREFIX + exact(amount) + " in the network",
                amount > 0 ? NamedTextColor.WHITE : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));

        if (view == View.PURGE) {
            lore.add(line("⚠ Targeted by Purger", NamedTextColor.RED));
        } else if (amount > 0) {
            // Desglose por almacenamiento: las partes suman el total, nada se cuenta dos veces.
            NetworkStorage.Breakdown parts = network.storage().breakdown(sample);
            lore.add(line("Stored in:", NamedTextColor.GRAY));
            addPart(lore, "▣ DRAM", parts.memory(), NamedTextColor.AQUA);
            addPart(lore, "▦ Quantum Cells", parts.cells(), NamedTextColor.YELLOW);
            addPart(lore, "▤ Infinity Barrels", parts.barrels(), NamedTextColor.LIGHT_PURPLE);
            addPart(lore, "⚡ Greedy Buffer (reserved)", parts.greedy(), NamedTextColor.GREEN);
            addPart(lore, "◆ Slimefun Barrels", parts.slimefunBarrels(), NamedTextColor.GOLD);
        } else {
            lore.add(line("Out of stock", NamedTextColor.DARK_GRAY));
        }

        // Paginas en las que esta fijado: un punto de color por pagina.
        Component pins = Component.empty();
        boolean any = false;
        for (int i = 0; i < TerminalPages.COUNT; i++) {
            if (pages.contains(i, sample)) {
                pins = pins.append(Component.text("● ", PAGE_COLORS[i]));
                any = true;
            }
        }
        if (any) {
            lore.add(Component.text("Pinned: ", NamedTextColor.GRAY).append(pins).decoration(TextDecoration.ITALIC, false));
        }

        if (meta != null) {
            meta.lore(lore);
            meta.getPersistentDataContainer().set(Keys.TERMINAL_DISPLAY, PersistentDataType.BYTE, (byte) 1);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private static void addPart(List<Component> lore, String label, long amount, NamedTextColor color) {
        if (amount <= 0) {
            return;
        }
        lore.add(Component.text(" " + label + ": ", color).append(Component.text(exact(amount), NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
    }

    /** 2500 → "2,500"; large amounts also get the short form: "1,250,000 (1.3M)". */
    private static String exact(long amount) {
        String full = String.format(java.util.Locale.ROOT, "%,d", amount);
        return amount >= 10_000 ? full + " (" + Items.formatAmount(amount) + ")" : full;
    }

    private ItemStack fluidGridIcon(String fluidType, long amountMb) {
        ItemStack item = new ItemStack(filledContainer(fluidType));
        var meta = item.getItemMeta();
        if (meta != null) {
            NamedTextColor col = switch (fluidType.toUpperCase(java.util.Locale.ROOT)) {
                case "LAVA" -> NamedTextColor.GOLD;
                case "MILK" -> NamedTextColor.WHITE;
                case "POWDER_SNOW" -> NamedTextColor.AQUA;
                case "HONEY" -> NamedTextColor.YELLOW;
                default -> NamedTextColor.DARK_AQUA;
            };
            meta.displayName(Component.text(fluidType, col).decoration(TextDecoration.ITALIC, false));
            boolean honey = "HONEY".equalsIgnoreCase(fluidType);
            String reqContainer = honey ? "Glass Bottle" : "Bucket";
            int units = honey ? (int) (amountMb / 250) : (int) (amountMb / 1000);
            String unitName = honey ? "Bottles" : "Buckets";
            meta.lore(List.of(
                    line("Stored: " + Items.formatAmount(amountMb) + " mB (" + units + " " + unitName + ")", NamedTextColor.WHITE),
                    Component.empty(),
                    line("Click: fill 1 " + reqContainer, NamedTextColor.YELLOW),
                    line("Shift+Click: fill every " + reqContainer, NamedTextColor.YELLOW)));
            meta.getPersistentDataContainer().set(Keys.TERMINAL_DISPLAY, PersistentDataType.BYTE, (byte) 2);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ------------------------------------------------------------------ piezas

    private static Component line(String text, TextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static ItemStack button(Material material, String name, TextColor color, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, false));
            if (!lore.isEmpty()) {
                meta.lore(lore);
            }
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack tab(Material material, String name, TextColor color, boolean active, List<Component> lore) {
        List<Component> full = new ArrayList<>(lore);
        full.add(Component.empty());
        full.add(active ? line("▶ Viewing now", NamedTextColor.GREEN) : line("Click to open", NamedTextColor.DARK_GRAY));
        ItemStack item = button(material, (active ? "▶ " : "") + name, color, full);
        glint(item, active);
        return item;
    }

    private static void glint(ItemStack item, boolean on) {
        var meta = item.getItemMeta();
        if (meta != null && on) {
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
        }
    }

    private static ItemStack filler() {
        return button(Material.BLACK_STAINED_GLASS_PANE, " ", NamedTextColor.DARK_GRAY, List.of());
    }

    private static ItemStack emptyNotice(String text) {
        return button(Material.STRUCTURE_VOID, text, NamedTextColor.GRAY, List.of());
    }

    // ------------------------------------------------------------------ clics

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        ClickType click = event.getClick();

        if (click == ClickType.NUMBER_KEY || click == ClickType.DROP) {
            keyOnGrid(event, raw);
            return;
        }

        if (raw >= TAB_FIRST_PAGE && raw < TAB_FIRST_PAGE + TerminalPages.COUNT) {
            pageTabClick(raw - TAB_FIRST_PAGE, click);
            return;
        }
        switch (raw) {
            case TAB_NORMAL -> switchView(View.NORMAL);
            case TAB_PURGE -> switchView(View.PURGE);
            case TAB_FLUIDS -> switchView(View.FLUIDS);
            case PREV_SLOT -> {
                if (page > 0) {
                    page--;
                    clickSound();
                    refresh();
                }
            }
            case NEXT_SLOT -> {
                if (page < pageCount - 1) {
                    page++;
                    clickSound();
                    refresh();
                }
            }
            case SORT_SLOT -> {
                sortOrder = sortOrder == SortOrder.MVN_ALPHABETIC ? SortOrder.MVN_AMOUNT : SortOrder.MVN_ALPHABETIC;
                page = 0;
                clickSound();
                refresh();
            }
            case SEARCH_SLOT -> {
                if (click.isRightClick()) {
                    query = "";
                    page = 0;
                    clickSound();
                    refresh();
                } else {
                    player.closeInventory();
                    ChatPrompts.ask(player, "Type your search term:", text -> {
                        query = text == null ? "" : text;
                        page = 0;
                        openMenu();
                    });
                }
            }
            case PIN_MODE_SLOT -> {
                // Apagado → pagina 1 → ... → pagina 5 → apagado; clic derecho al reves.
                int states = TerminalPages.COUNT + 1;
                int current = pinTarget + 1;
                int next = click.isRightClick() ? (current + states - 1) % states : (current + 1) % states;
                pinTarget = next - 1;
                clickSound();
                refresh();
            }
            case DEPOSIT_ALL_SLOT -> depositInventory(click.isShiftClick());
            default -> {
                if (raw >= GRID_START && raw < GRID_START + GRID_SIZE) {
                    gridClick(event, raw);
                } else if (raw >= event.getView().getTopInventory().getSize()) {
                    insertPlayerStack(event);
                }
            }
        }
    }

    private void switchView(View next) {
        if (view == next) {
            return;
        }
        view = next;
        page = 0;
        clickSound();
        refresh();
    }

    private void pageTabClick(int index, ClickType click) {
        if (click == ClickType.SHIFT_RIGHT) {
            pages.clear(index);
            player.sendMessage(Text.msg(pages.name(index) + " cleared.", NamedTextColor.YELLOW));
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 0.8f);
            refresh();
        } else if (click.isRightClick()) {
            player.closeInventory();
            ChatPrompts.ask(player, "Type a name for " + pages.name(index) + " (empty to reset):", text -> {
                pages.rename(index, text);
                openMenu();
            });
        } else if (view != View.CUSTOM || customPage != index) {
            view = View.CUSTOM;
            customPage = index;
            page = 0;
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
            refresh();
        }
    }

    private void gridClick(InventoryClickEvent event, int raw) {
        if (view == View.FLUIDS) {
            withdrawFluid(event, displayedFluids.get(raw));
            return;
        }
        if (displayedRecovered.containsKey(raw)) {
            takeRecovered(event, displayedRecovered.get(raw));
            return;
        }
        ItemStack cursor = event.getView().getCursor();
        boolean cursorEmpty = cursor == null || cursor.getType().isAir();
        ItemStack sample = displayedSamples[raw];
        if (pinTarget >= 0 && cursorEmpty) {
            if (sample != null) {
                pin(pinTarget, sample);
            }
            return;
        }
        // Con algo en el cursor que no es este item: se guarda en la red.
        if (!cursorEmpty && (sample == null || !StackUtils.itemsMatch(sample, cursor))) {
            depositCursor(event, cursor);
            return;
        }
        withdrawFromDisplay(event);
    }

    /** Number keys 1-5 pin/unpin the hovered item; Q unpins it from the open custom page. */
    private void keyOnGrid(InventoryClickEvent event, int raw) {
        if (raw < GRID_START || raw >= GRID_START + GRID_SIZE) {
            return;
        }
        ItemStack sample = displayedSamples[raw];
        if (sample == null) {
            return;
        }
        if (event.getClick() == ClickType.DROP) {
            if (view == View.CUSTOM) {
                pin(customPage, sample);
            }
            return;
        }
        int key = event.getHotbarButton();
        if (key >= 0 && key < TerminalPages.COUNT) {
            pin(key, sample);
        }
    }

    private void pin(int pageIndex, ItemStack sample) {
        boolean added = pages.toggle(pageIndex, sample);
        String name = readableName(sample);
        player.sendActionBar(Component.text(added ? "Pinned " + name + " to " : "Unpinned " + name + " from ",
                added ? NamedTextColor.GREEN : NamedTextColor.YELLOW)
                .append(Component.text(pages.name(pageIndex), PAGE_COLORS[pageIndex])));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, added ? 1.4f : 0.8f);
        refresh();
    }

    private void clickSound() {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1f);
    }

    private void depositCursor(InventoryClickEvent event, ItemStack cursor) {
        if (isFluidContainer(cursor)) {
            return;
        }
        ItemStack actual = cursor.clone();
        int leftover = network.storage().depositManual(actual);
        if (leftover >= actual.getAmount()) {
            player.sendMessage(Text.msg(refusalReason(actual), NamedTextColor.RED));
            return;
        }
        if (leftover <= 0) {
            event.getView().setCursor(null);
        } else {
            actual.setAmount(leftover);
            event.getView().setCursor(actual);
        }
        draw();
        player.updateInventory();
    }

    private void depositInventory(boolean includeHotbar) {
        int moved = 0;
        boolean refused = false;
        for (int i = includeHotbar ? 0 : 9; i < 36; i++) {
            ItemStack it = player.getInventory().getItem(i);
            if (it == null || it.getType().isAir() || isFluidContainer(it)) {
                continue;
            }
            int amount = it.getAmount();
            int leftover = network.storage().depositManual(it.clone());
            moved += amount - leftover;
            if (leftover <= 0) {
                player.getInventory().setItem(i, null);
            } else {
                refused = true;
                ItemStack back = it.clone();
                back.setAmount(leftover);
                player.getInventory().setItem(i, back);
            }
        }
        if (moved > 0) {
            player.sendMessage(Text.msg("Deposited " + Items.formatAmount(moved) + " items"
                    + (refused ? " (some did not fit)." : "."), NamedTextColor.GREEN));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.2f);
        } else if (refused) {
            player.sendMessage(Text.msg("Nothing fit in the network.", NamedTextColor.RED));
        }
        draw();
        player.updateInventory();
    }

    private static Material filledContainer(String fluidType) {
        return switch (fluidType.toUpperCase(java.util.Locale.ROOT)) {
            case "LAVA" -> Material.LAVA_BUCKET;
            case "MILK" -> Material.MILK_BUCKET;
            case "POWDER_SNOW" -> Material.POWDER_SNOW_BUCKET;
            case "HONEY" -> Material.HONEY_BOTTLE;
            default -> Material.WATER_BUCKET;
        };
    }

    private void withdrawFluid(InventoryClickEvent event, String fluidType) {
        if (fluidType == null) return;
        long stored = network.fluidStorage().count(fluidType);
        Material reqContainer = "HONEY".equalsIgnoreCase(fluidType) ? Material.GLASS_BOTTLE : Material.BUCKET;
        Material filledItem = filledContainer(fluidType);
        int mbCost = "HONEY".equalsIgnoreCase(fluidType) ? 250 : 1000;
        String containerName = "HONEY".equalsIgnoreCase(fluidType) ? "Glass Bottle" : "Bucket";

        // 1. Check if network has at least 1 unit of fluid
        if (stored < mbCost) {
            player.sendMessage(Text.msg("Not enough " + fluidType + " in network! (Need at least " + mbCost + " mB)", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // 2. Check if player has the required container on cursor
        ItemStack cursor = event.getView().getCursor();
        boolean shift = event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT;

        if (cursor != null && cursor.getType() == reqContainer) {
            if (network.fluidStorage().withdraw(fluidType, mbCost) >= mbCost) {
                cursor.setAmount(cursor.getAmount() - 1);
                if (cursor.getAmount() <= 0) {
                    event.getView().setCursor(new ItemStack(filledItem));
                } else {
                    event.getView().setCursor(cursor);
                    giveOrDrop(new ItemStack(filledItem));
                }
                player.playSound(player.getLocation(), "HONEY".equalsIgnoreCase(fluidType) ? Sound.ITEM_BOTTLE_FILL : Sound.ITEM_BUCKET_FILL, 1f, 1f);
                player.sendMessage(Text.msg("Withdrew 1x " + filledItem.name() + " from network fluids.", NamedTextColor.GREEN));
                draw();
                return;
            }
        }

        // 3. Check player inventory for the required container
        int containerSlot = -1;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack it = player.getInventory().getItem(i);
            if (it != null && it.getType() == reqContainer) {
                containerSlot = i;
                break;
            }
        }

        if (containerSlot == -1) {
            player.sendMessage(Text.msg("You need an empty " + containerName + " to withdraw " + fluidType + "!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        if (shift) {
            int filledCount = 0;
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack it = player.getInventory().getItem(i);
                if (it != null && it.getType() == reqContainer) {
                    while (it.getAmount() > 0 && network.fluidStorage().count(fluidType) >= mbCost) {
                        if (network.fluidStorage().withdraw(fluidType, mbCost) >= mbCost) {
                            it.setAmount(it.getAmount() - 1);
                            giveOrDrop(new ItemStack(filledItem));
                            filledCount++;
                        } else {
                            break;
                        }
                    }
                    if (it.getAmount() <= 0) {
                        player.getInventory().setItem(i, null);
                    }
                    if (network.fluidStorage().count(fluidType) < mbCost) break;
                }
            }
            if (filledCount > 0) {
                player.playSound(player.getLocation(), "HONEY".equalsIgnoreCase(fluidType) ? Sound.ITEM_BOTTLE_FILL : Sound.ITEM_BUCKET_FILL, 1f, 1f);
                player.sendMessage(Text.msg("Withdrew " + filledCount + "x " + filledItem.name() + " from network.", NamedTextColor.GREEN));
            }
        } else {
            ItemStack it = player.getInventory().getItem(containerSlot);
            if (it != null && network.fluidStorage().withdraw(fluidType, mbCost) >= mbCost) {
                it.setAmount(it.getAmount() - 1);
                if (it.getAmount() <= 0) {
                    player.getInventory().setItem(containerSlot, null);
                }
                giveOrDrop(new ItemStack(filledItem));
                player.playSound(player.getLocation(), "HONEY".equalsIgnoreCase(fluidType) ? Sound.ITEM_BOTTLE_FILL : Sound.ITEM_BUCKET_FILL, 1f, 1f);
                player.sendMessage(Text.msg("Withdrew 1x " + filledItem.name() + " from network.", NamedTextColor.GREEN));
            }
        }
        draw();
        player.updateInventory();
    }

    private void withdrawFromDisplay(InventoryClickEvent event) {
        ItemStack icon = event.getCurrentItem();
        if (!isGridStack(icon)) {
            return;
        }
        int raw = event.getRawSlot();
        ItemStack sample = (raw >= 0 && raw < displayedSamples.length) ? displayedSamples[raw] : null;
        ItemStack target = sample != null ? sample : cleanStack(icon);
        Predicate<ItemStack> matches = item -> StackUtils.itemsMatch(item, target);
        ClickType click = event.getClick();
        boolean shift = click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT;

        if (shift) {
            int want = Math.min(target.getMaxStackSize(), 64);
            ItemStack withdrawn = network.storage().withdraw(matches, want);
            if (withdrawn != null) {
                int leftover = NetworkManager.insertInto(player.getInventory(), withdrawn);
                if (leftover > 0) {
                    withdrawn.setAmount(leftover);
                    int unreturned = network.storage().depositManual(withdrawn);
                    if (unreturned > 0) {
                        withdrawn.setAmount(unreturned);
                        giveOrDrop(withdrawn);
                    }
                }
            }
            draw();
            player.updateInventory();
            return;
        }

        var view = event.getView();
        ItemStack cursor = view.getCursor();
        boolean right = click == ClickType.RIGHT;

        if (cursor == null || cursor.getType().isAir()) {
            int want = right ? Math.min(target.getMaxStackSize(), 64) : 1;
            ItemStack withdrawn = network.storage().withdraw(matches, want);
            if (withdrawn != null) {
                view.setCursor(withdrawn);
            }
        } else if (!right && StackUtils.itemsMatch(target, cursor) && cursor.getAmount() < cursor.getMaxStackSize()) {
            ItemStack single = network.storage().withdraw(matches, 1);
            if (single != null) {
                cursor.setAmount(Math.min(cursor.getMaxStackSize(), cursor.getAmount() + 1));
                view.setCursor(cursor);
            }
        }
        draw();
        player.updateInventory();
    }

    private boolean isFluidContainer(ItemStack item) {
        if (item == null) return false;
        Material m = item.getType();
        return m == Material.WATER_BUCKET || m == Material.LAVA_BUCKET || m == Material.MILK_BUCKET
                || m == Material.POWDER_SNOW_BUCKET || m == Material.HONEY_BOTTLE;
    }

    private static String fluidOf(Material m) {
        return switch (m) {
            case WATER_BUCKET -> "WATER";
            case LAVA_BUCKET -> "LAVA";
            case MILK_BUCKET -> "MILK";
            case POWDER_SNOW_BUCKET -> "POWDER_SNOW";
            case HONEY_BOTTLE -> "HONEY";
            default -> null;
        };
    }

    private void depositFluidContainer(InventoryClickEvent event, ItemStack item) {
        String fluid = fluidOf(item.getType());
        if (fluid == null) return;
        int mb = "HONEY".equals(fluid) ? 250 : 1000;
        Material emptyContainer = "HONEY".equals(fluid) ? Material.GLASS_BOTTLE : Material.BUCKET;

        long remainder = network.fluidStorage().deposit(fluid, mb);
        if (remainder == 0) {
            int playerSlot = playerInventorySlot(event);
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
                giveOrDrop(new ItemStack(emptyContainer));
            } else {
                player.getInventory().setItem(playerSlot, new ItemStack(emptyContainer));
            }
            player.playSound(player.getLocation(), "HONEY".equals(fluid) ? Sound.ITEM_BOTTLE_EMPTY : Sound.ITEM_BUCKET_EMPTY, 1f, 1f);
            player.sendMessage(Text.msg("Deposited " + mb + " mB of " + fluid + " into network fluids.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Text.msg("Cannot deposit " + fluid + ": network has no Quantum Fluid Cell with capacity!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
        draw();
        player.updateInventory();
    }

    private void insertPlayerStack(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType().isAir()) {
            return;
        }
        if (isFluidContainer(item)) {
            depositFluidContainer(event, item);
            return;
        }
        ItemStack actual = item.clone();
        int initialAmount = actual.getAmount();
        int playerSlot = playerInventorySlot(event);
        player.getInventory().setItem(playerSlot, null);
        int leftover = network.storage().depositManual(actual);
        if (leftover <= 0) {
            player.sendMessage(Text.msg("Deposited " + Items.formatAmount(initialAmount) + " items.", NamedTextColor.GREEN));
        } else {
            if (leftover < initialAmount) {
                player.sendMessage(Text.msg("Deposited " + Items.formatAmount(initialAmount - leftover) + " items.", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Text.msg(refusalReason(actual), NamedTextColor.RED));
            }
            ItemStack returned = actual.clone();
            returned.setAmount(leftover);
            player.getInventory().setItem(playerSlot, returned);
        }
        draw();
        player.updateInventory();
    }

    /**
     * Por qué la red no aceptó nada. Las celdas guardan un solo tipo de ítem: uno con otro nombre,
     * lore o datos (p. ej. Blistering Ingot 33% frente al final) es otro ítem y necesita una celda vacía.
     */
    public String refusalReason(ItemStack item) {
        for (NetworkStorage.View entry : network.storage().view()) {
            if (entry.sample().getType() == item.getType() && !StackUtils.itemsMatch(entry.sample(), item)) {
                return "Nothing deposited: this is not the same item as the stored "
                        + readableName(entry.sample()) + " (different name, lore or data), and there is "
                        + "no empty Quantum Cell for a new item type.";
            }
        }
        return "Nothing deposited: no Quantum Cell holds this item and there is no empty Quantum Cell.";
    }

    private void liveTick() {
        if (inv == null || player.getOpenInventory().getTopInventory().getHolder() != this) {
            cancelTask();
            return;
        }
        ItemStack input = inv.getItem(INPUT_SLOT);
        if (input != null && !input.getType().isAir()) {
            if (isFluidContainer(input)) {
                String fluid = fluidOf(input.getType());
                int mb = "HONEY".equals(fluid) ? 250 : 1000;
                Material empty = "HONEY".equals(fluid) ? Material.GLASS_BOTTLE : Material.BUCKET;
                if (fluid != null && network.fluidStorage().deposit(fluid, mb) == 0) {
                    // Las botellas de miel apilan hasta 16: solo se vacia una por ciclo, el resto
                    // del stack se queda en la ranura. Antes el stack entero se cambiaba por una
                    // sola botella vacia y se perdian las demas.
                    if (input.getAmount() > 1) {
                        input.setAmount(input.getAmount() - 1);
                        giveOrDrop(new ItemStack(empty));
                    } else {
                        inv.setItem(INPUT_SLOT, new ItemStack(empty));
                    }
                }
            } else {
                int leftover = network.storage().depositManual(input);
                if (leftover <= 0) {
                    inv.setItem(INPUT_SLOT, null);
                } else {
                    input.setAmount(leftover);
                }
            }
        }
        // Redibujar la cuadricula entera (filtrar, ordenar, rehacer cada icono con su lore) cada
        // medio segundo con la red quieta era el coste de tener un Terminal abierto. Ahora solo se
        // redibuja si cambio lo guardado, o cada 2 s para lo que cambia por fuera (un barril de
        // Slimefun vaciado a mano, una celda editada desde su menu).
        long stamp = network.storage().changeStamp() * 31 + network.fluidStorage().changeStamp();
        if (stamp != drawnStamp || ++ticksSinceDraw >= 4) {
            draw();
        }
    }

    @Override
    protected void onClose(InventoryCloseEvent event) {
        cancelTask();
        ItemStack input = inv.getItem(INPUT_SLOT);
        if (input == null || input.getType().isAir()) {
            return;
        }
        if (isFluidContainer(input)) {
            giveOrDrop(input);
            inv.setItem(INPUT_SLOT, null);
            return;
        }
        int leftover = network.storage().depositManual(input);
        inv.setItem(INPUT_SLOT, null);
        if (leftover > 0) {
            input.setAmount(leftover);
            giveOrDrop(input);
        }
    }

    private void cancelTask() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    private boolean isGridStack(ItemStack icon) {
        if (icon == null || icon.getType().isAir() || !icon.hasItemMeta()) {
            return false;
        }
        Byte mark = icon.getItemMeta().getPersistentDataContainer()
                .get(Keys.TERMINAL_DISPLAY, PersistentDataType.BYTE);
        return mark != null && mark == (byte) 1;
    }

    private ItemStack cleanStack(ItemStack icon) {
        ItemStack clean = icon.clone();
        var meta = clean.getItemMeta();
        if (meta != null) {
            if (meta.hasLore() && meta.lore() != null) {
                List<Component> lore = new ArrayList<>(meta.lore());
                // Todo lo que anadio la cuadricula va desde la linea en blanco anterior a la ultima
                // linea "Amount:" hasta el final; se corta entero.
                for (int i = lore.size() - 1; i >= 0; i--) {
                    if (plain(lore.get(i)).startsWith(AMOUNT_PREFIX)) {
                        int cut = i > 0 && plain(lore.get(i - 1)).isBlank() ? i - 1 : i;
                        lore = new ArrayList<>(lore.subList(0, cut));
                        break;
                    }
                }
                meta.lore(lore.isEmpty() ? null : lore);
            }
            meta.getPersistentDataContainer().remove(Keys.TERMINAL_DISPLAY);
            clean.setItemMeta(meta);
        }
        return clean;
    }
}

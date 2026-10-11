package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.MemoryModules;
import com.chagui68.multiversenets.net.ModuleIO;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.StackUtils;
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
 * [EN] DRAM Loader menu:
 * <pre>
 *  row 0  header       summary · help
 *  row 1  [card] [target] [start/stop]
 *  row 2  amount       Fill · -10k -1k -64 · [amount] · +64 +1k +10k · Reset
 *  row 3  progress     9 panes
 *  row 4  footer       close
 * </pre>
 * Put an empty card in, pick the item (or a fluid bucket for a Fluid DRAM), set the amount and press
 * start: the card is filled from the network and the Loader stops once it holds that amount.
 *
 * [ES] Menú del DRAM Loader: tarjeta, objetivo e iniciar/parar en la segunda fila, cantidad en la
 * tercera y el progreso en la cuarta. Mete una tarjeta vacía, elige el ítem (o un cubo de fluido para
 * un Fluid DRAM), fija la cantidad y pulsa iniciar: la tarjeta se llena desde la red y el Loader se
 * para cuando la tiene.
 */
public class ModuleLoaderMenu extends ModuleHolderMenu {

    public static final int SIZE = 45;
    public static final int STATS_SLOT = 4;
    public static final int HELP_SLOT = 8;
    public static final int CARD_SLOT = 11;
    public static final int TARGET_SLOT = 13;
    public static final int START_SLOT = 15;
    public static final int FILL_SLOT = 18;
    public static final int AMOUNT_SLOT = 22;
    public static final int RESET_SLOT = 26;
    public static final int PROGRESS_ROW = 27;
    public static final int CLOSE_SLOT = 40;
    /** Amount buttons: slot → step / Botones de cantidad: hueco → paso. */
    private static final int[] STEP_SLOTS = {19, 20, 21, 23, 24, 25};
    private static final long[] STEPS = {-10_000, -1_000, -64, 64, 1_000, 10_000};

    public ModuleLoaderMenu(MultiverseNets plugin, Player player, Block block) {
        super(plugin, player, block);
    }

    public void openMenu() {
        openMenu(SIZE, Component.text("DRAM Loader", NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false));
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    protected void draw() {
        NodeBlob blob = NodeStore.get(block);
        if (blob == null) {
            return;
        }
        NodeBlob card = ModuleIO.loaderModule(blob);
        DeviceType cardType = MemoryModules.typeOf(card);
        boolean fluidCard = cardType != null && !cardType.isCacheModule();
        String unit = fluidCard ? " mB" : "";
        long loaded = ModuleIO.loadedAmount(blob);
        long goal = ModuleIO.loaderGoal(blob);

        ItemStack header = MenuIcons.pane(Material.CYAN_STAINED_GLASS_PANE);
        ItemStack filler = MenuIcons.pane(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack footer = MenuIcons.pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, header);
            inv.setItem(9 + i, filler);
            inv.setItem(36 + i, footer);
        }

        List<String> summary = new ArrayList<>();
        summary.add("Card: " + (cardType == null ? "none" : cardType.display()));
        summary.add("Target: " + ModuleIO.targetName(blob));
        summary.add("Loaded: " + Items.formatAmount(loaded) + " / " + Items.formatAmount(goal) + unit);
        summary.add("Status: " + status(blob, cardType, loaded, goal));
        inv.setItem(STATS_SLOT, MenuIcons.icon(Material.WAXED_OXIDIZED_COPPER_BULB, "DRAM Loader", NamedTextColor.AQUA, summary));
        inv.setItem(HELP_SLOT, MenuIcons.icon(Material.KNOWLEDGE_BOOK, "How it works", NamedTextColor.GOLD, List.of(
                "Fills one memory module (card) with a",
                "single item from the network.",
                "",
                "1. Put a card in the card slot.",
                "2. Click the target slot with the item",
                "   (a fluid bucket for a Fluid DRAM).",
                "3. Set the amount, or Fill for a full card.",
                "4. Press Start. It stops by itself.",
                "",
                "Example: an empty Quantum card, cobblestone,",
                "500k, Start: a card ready to sell.")));

        inv.setItem(CARD_SLOT, card == null ? MenuIcons.icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "Card slot",
                NamedTextColor.GRAY, List.of(
                        "» Click with a memory module on the cursor,",
                        "  or Shift-Click one in your inventory."))
                : cardIcon(card, cardType));
        inv.setItem(TARGET_SLOT, targetIcon(blob, fluidCard));
        inv.setItem(START_SLOT, startIcon(blob, cardType, loaded, goal));

        long capacity = cardType == null ? 0 : ModuleIO.capacity(cardType);
        inv.setItem(FILL_SLOT, MenuIcons.icon(Material.BUCKET, "Fill the card", NamedTextColor.AQUA, List.of(
                "Loads until the card is full.",
                "» Click to choose it.")));
        for (int i = 0; i < STEP_SLOTS.length; i++) {
            long step = STEPS[i];
            ItemStack button = MenuIcons.icon(step < 0 ? Material.RED_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE,
                    (step < 0 ? "-" : "+") + Items.formatAmount(Math.abs(step)) + unit,
                    step < 0 ? NamedTextColor.RED : NamedTextColor.GREEN,
                    List.of("» Click: " + (step < 0 ? "-" : "+") + Items.formatAmount(Math.abs(step)),
                            "» Shift-Click: x100"));
            inv.setItem(STEP_SLOTS[i], button);
        }
        List<String> amountLore = new ArrayList<>();
        amountLore.add(blob.loaderAmount <= 0 ? "Fill the whole card" : "Amount: " + exact(blob.loaderAmount) + unit);
        if (capacity > 0) {
            amountLore.add("This card holds at most " + exact(capacity) + unit);
            if (blob.loaderAmount > capacity) {
                amountLore.add("Above that: it stops when the card is full.");
            }
        }
        amountLore.add("");
        amountLore.add("» Click to type an amount (1m, 500k, 2500, max)");
        ItemStack amount = MenuIcons.icon(Material.NAME_TAG,
                blob.loaderAmount <= 0 ? "Amount: Fill" : "Amount: " + Items.formatAmount(blob.loaderAmount) + unit,
                NamedTextColor.WHITE, amountLore);
        inv.setItem(AMOUNT_SLOT, amount);
        inv.setItem(RESET_SLOT, MenuIcons.icon(Material.BARRIER, "Reset", NamedTextColor.GRAY, List.of(
                "Clears the target and the amount.", "» Click to reset.")));

        double ratio = goal <= 0 ? 0 : Math.min(1.0, (double) loaded / goal);
        int filled = loaded <= 0 ? 0 : Math.max(1, (int) Math.round(ratio * 9));
        String progressName = "Progress: " + Math.round(ratio * 100) + "%";
        List<String> progressLore = List.of(exact(loaded) + " / " + exact(goal) + unit);
        for (int i = 0; i < 9; i++) {
            inv.setItem(PROGRESS_ROW + i, i < filled
                    ? MenuIcons.icon(Material.LIME_STAINED_GLASS_PANE, progressName, NamedTextColor.GREEN, progressLore)
                    : MenuIcons.icon(Material.GRAY_STAINED_GLASS_PANE, progressName, NamedTextColor.DARK_GRAY, progressLore));
        }
        inv.setItem(CLOSE_SLOT, MenuIcons.icon(Material.OAK_DOOR, "Close", NamedTextColor.WHITE, List.of()));
    }

    private static String exact(long amount) {
        return String.format(java.util.Locale.ROOT, "%,d", amount);
    }

    private static String status(NodeBlob blob, DeviceType cardType, long loaded, long goal) {
        if (cardType == null) {
            return "waiting for a card";
        }
        if (!ModuleIO.hasTarget(blob, cardType)) {
            return "choose a target";
        }
        if (blob.loaderActive) {
            return "loading…";
        }
        return goal > 0 && loaded >= goal ? "done ✔" : "stopped";
    }

    private ItemStack cardIcon(NodeBlob card, DeviceType type) {
        ItemStack shown = Items.create(type);
        var meta = shown.getItemMeta();
        long stored = ModuleIO.stored(card);
        long cap = ModuleIO.capacity(type);
        List<Component> lore = new ArrayList<>();
        lore.add(MenuIcons.bar(cap <= 0 ? 0 : Math.min(1.0, (double) stored / cap), 10));
        lore.add(MenuIcons.line(Items.formatAmount(stored) + " / " + Items.formatAmount(cap)
                + (type.isCacheModule() ? " items" : " mB"), NamedTextColor.WHITE));
        lore.add(Component.empty());
        lore.add(MenuIcons.line("» Click to take it out.", NamedTextColor.YELLOW));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(stored > 0);
        shown.setItemMeta(meta);
        return shown;
    }

    private ItemStack targetIcon(NodeBlob blob, boolean fluidCard) {
        List<String> help = new ArrayList<>();
        help.add(fluidCard
                ? "» Click with a fluid bucket to choose the fluid."
                : "» Click with an item to choose it.");
        help.add("» Shift-Click an item in your inventory too.");
        help.add("» Right-click to clear.");
        if (blob.loaderFluid != null) {
            ItemStack icon = MenuIcons.icon(bucketOf(blob.loaderFluid), "Target: " + blob.loaderFluid, NamedTextColor.AQUA, help);
            return icon;
        }
        if (blob.loaderTarget != null) {
            ItemStack icon = StackUtils.getAsQuantity(blob.loaderTarget, 1);
            var meta = icon.getItemMeta();
            List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            lore.add(MenuIcons.line("Target item", NamedTextColor.AQUA));
            for (String text : help) {
                lore.add(MenuIcons.line(text, NamedTextColor.YELLOW));
            }
            meta.lore(lore);
            icon.setItemMeta(meta);
            return icon;
        }
        return MenuIcons.icon(Material.ITEM_FRAME, "Target: none", NamedTextColor.GRAY, help);
    }

    private ItemStack startIcon(NodeBlob blob, DeviceType cardType, long loaded, long goal) {
        if (blob.loaderActive) {
            ItemStack item = MenuIcons.icon(Material.REDSTONE_BLOCK, "Loading… (click to stop)", NamedTextColor.GOLD, List.of(
                    exact(loaded) + " / " + exact(goal),
                    "» Click to stop."));
            var meta = item.getItemMeta();
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
            return item;
        }
        boolean ready = cardType != null && ModuleIO.hasTarget(blob, cardType) && goal > loaded;
        return MenuIcons.icon(ready ? Material.EMERALD_BLOCK : Material.COAL_BLOCK, ready ? "Start" : "Start (not ready)",
                ready ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY, List.of(
                        "Status: " + status(blob, cardType, loaded, goal),
                        "» Click to start loading."));
    }

    // ------------------------------------------------------------------ clics

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        NodeBlob blob = NodeStore.get(block);
        if (blob == null) {
            player.closeInventory();
            return;
        }
        if (raw >= inv.getSize()) {
            // Una tarjeta se mete; cualquier otro item pasa a ser el objetivo.
            if (!shiftInsert(event, blob)) {
                setTarget(blob, event.getCurrentItem() != null ? event.getCurrentItem()
                        : player.getInventory().getItem(playerInventorySlot(event)));
            }
            return;
        }
        switch (raw) {
            case CLOSE_SLOT -> player.closeInventory();
            case CARD_SLOT -> clickSlot(event, blob, 0);
            case TARGET_SLOT -> {
                ItemStack cursor = event.getView().getCursor();
                if (event.getClick().isRightClick() && (cursor == null || cursor.getType().isAir())) {
                    blob.loaderTarget = null;
                    blob.loaderFluid = null;
                    blob.loaderActive = false;
                    save(blob);
                } else {
                    setTarget(blob, cursor);
                }
            }
            case START_SLOT -> toggle(blob);
            case FILL_SLOT -> {
                blob.loaderAmount = 0;
                save(blob);
            }
            case RESET_SLOT -> {
                blob.loaderTarget = null;
                blob.loaderFluid = null;
                blob.loaderAmount = 0;
                blob.loaderActive = false;
                save(blob);
            }
            case AMOUNT_SLOT -> {
                player.closeInventory();
                ChatPrompts.ask(player, "Type the amount to load (e.g. 1m, 500k, 2500 or max):", text -> {
                    long parsed = ModuleIO.parseAmount(text);
                    NodeBlob fresh = NodeStore.get(block);
                    if (fresh != null && parsed >= 0) {
                        fresh.loaderAmount = parsed;
                        fresh.loaderActive = false;
                        NodeStore.put(block, fresh);
                    } else if (parsed < 0) {
                        player.sendMessage(Text.msg("That is not an amount.", NamedTextColor.RED));
                    }
                    openMenu();
                });
            }
            default -> {
                for (int i = 0; i < STEP_SLOTS.length; i++) {
                    if (raw == STEP_SLOTS[i]) {
                        long step = STEPS[i] * (event.getClick().isShiftClick() ? 100 : 1);
                        blob.loaderAmount = Math.max(0, blob.loaderAmount + step);
                        save(blob);
                        return;
                    }
                }
            }
        }
    }

    private void setTarget(NodeBlob blob, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        DeviceType cardType = MemoryModules.typeOf(ModuleIO.loaderModule(blob));
        String fluid = fluidOf(item.getType());
        if (cardType != null && !cardType.isCacheModule()) {
            if (fluid == null) {
                player.sendMessage(Text.msg("A Fluid DRAM loads fluids: click with a fluid bucket.", NamedTextColor.RED));
                return;
            }
            blob.loaderFluid = fluid;
            blob.loaderTarget = null;
        } else {
            if (Items.typeOf(item) != null && Items.typeOf(item).isMemoryModule()) {
                return;
            }
            blob.loaderTarget = StackUtils.getAsQuantity(item, 1);
            blob.loaderFluid = null;
        }
        blob.loaderActive = false;
        save(blob);
    }

    private void toggle(NodeBlob blob) {
        if (blob.loaderActive) {
            blob.loaderActive = false;
            save(blob);
            return;
        }
        DeviceType cardType = MemoryModules.typeOf(ModuleIO.loaderModule(blob));
        if (cardType == null) {
            player.sendMessage(Text.msg("Put a card in first.", NamedTextColor.RED));
            return;
        }
        if (!ModuleIO.hasTarget(blob, cardType)) {
            player.sendMessage(Text.msg("Choose what to load first.", NamedTextColor.RED));
            return;
        }
        if (ModuleIO.loaderGoal(blob) <= ModuleIO.loadedAmount(blob)) {
            player.sendMessage(Text.msg("The card already holds that amount, or it is full.", NamedTextColor.YELLOW));
            return;
        }
        if (plugin.networks().networkAt(block) == null) {
            player.sendMessage(Text.msg("This DRAM Loader is not connected to a network.", NamedTextColor.RED));
            return;
        }
        blob.loaderActive = true;
        player.playSound(block.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.6f);
        save(blob);
    }

    @Override
    protected void afterTakeOut(NodeBlob blob) {
        blob.loaderActive = false;
    }

    private void save(NodeBlob blob) {
        NodeStore.put(block, blob);
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
        draw();
    }

    static String fluidOf(Material m) {
        return switch (m) {
            case WATER_BUCKET -> "WATER";
            case LAVA_BUCKET -> "LAVA";
            case MILK_BUCKET -> "MILK";
            case POWDER_SNOW_BUCKET -> "POWDER_SNOW";
            case HONEY_BOTTLE -> "HONEY";
            default -> null;
        };
    }

    private static Material bucketOf(String fluid) {
        return switch (fluid.toUpperCase(java.util.Locale.ROOT)) {
            case "LAVA" -> Material.LAVA_BUCKET;
            case "MILK" -> Material.MILK_BUCKET;
            case "POWDER_SNOW" -> Material.POWDER_SNOW_BUCKET;
            case "HONEY" -> Material.HONEY_BOTTLE;
            default -> Material.WATER_BUCKET;
        };
    }
}

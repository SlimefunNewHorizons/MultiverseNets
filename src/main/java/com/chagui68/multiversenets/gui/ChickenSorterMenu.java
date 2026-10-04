package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.compat.ChickenGenetics;
import com.chagui68.multiversenets.item.DeviceType;
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
import java.util.Locale;
import java.util.Map;

/**
 * [EN] Genetic Chicken Sorter menu, laid out top to bottom in the order a player sets it up:
 * <pre>
 *  row 0  control bar   status · direction · summary · side · help
 *  row 1  accepted products (18 slots, the real product icon in each)
 *  row 2
 *  row 3  divider
 *  row 4  gene rules    [tier: min · max] [genes: strength · pure] [identity: DNA · age]
 *  row 5  actions       clear products · reset rules · close
 * </pre>
 * Every rule shows its value in its name and as the stack size, glows while it is active (not at
 * its default) and takes the same clicks: left +1, right −1, shift resets it. The summary book
 * spells out in plain words which chickens pass right now.
 *
 * [ES] Menú del Genetic Chicken Sorter, ordenado de arriba abajo como lo configura un jugador:
 * barra de control, productos aceptados (18 huecos con el icono real del producto), reglas de genes
 * agrupadas (nivel, genes, identidad) y acciones. Cada regla muestra su valor en el nombre y como
 * tamaño del stack, brilla mientras está activa y responde igual: izquierdo +1, derecho −1, shift la
 * reinicia. El libro de resumen dice en palabras simples qué pollos pasan ahora mismo.
 */
public class ChickenSorterMenu extends MenuHolder {

    public static final int SIZE = 54;
    public static final int MAX_PRODUCTS = 18;

    public static final int ACTIVE_SLOT = 1;
    public static final int MODE_SLOT = 3;
    public static final int SUMMARY_SLOT = 4;
    public static final int FACE_SLOT = 5;
    public static final int HELP_SLOT = 7;
    /** Products fill slots 9-26 / Los productos ocupan los huecos 9-26. */
    public static final int FIRST_PRODUCT_SLOT = 9;
    public static final int MIN_TIER_SLOT = 37;
    public static final int MAX_TIER_SLOT = 38;
    public static final int STRENGTH_SLOT = 40;
    public static final int PURE_SLOT = 41;
    public static final int KNOWN_SLOT = 43;
    public static final int AGE_SLOT = 44;
    public static final int CLEAR_SLOT = 46;
    public static final int RESET_SLOT = 49;
    public static final int CLOSE_SLOT = 52;

    private static final String[] FACES = {"ALL", "NORTH", "SOUTH", "EAST", "WEST", "UP", "DOWN"};
    private static final int MAX_TIER = 9;
    private static final int MAX_STRENGTH = 6;

    /** Icons for products whose id is not a vanilla item / Iconos de productos sin ítem vanilla. */
    private static final Map<String, Material> PRODUCT_ICONS = Map.ofEntries(
            Map.entry("IRON", Material.IRON_INGOT), Map.entry("GOLD", Material.GOLD_INGOT),
            Map.entry("LAPIS", Material.LAPIS_LAZULI), Map.entry("WATER", Material.WATER_BUCKET),
            Map.entry("LAVA", Material.LAVA_BUCKET), Map.entry("EXPERIENCE", Material.EXPERIENCE_BOTTLE),
            Map.entry("NETHERITE", Material.NETHERITE_INGOT), Map.entry("GOLD_DUST", Material.GLOWSTONE_DUST),
            Map.entry("SULFATE", Material.GUNPOWDER), Map.entry("SHULKER", Material.SHULKER_SHELL),
            Map.entry("MAGIC_LUMP", Material.AMETHYST_SHARD), Map.entry("URANIUM", Material.LIME_DYE),
            Map.entry("NEPTUNIUM", Material.GREEN_DYE), Map.entry("PLUTONIUM", Material.CYAN_DYE),
            Map.entry("BOOSTED_URANIUM", Material.SLIME_BALL), Map.entry("CARBONADO", Material.COAL_BLOCK),
            Map.entry("SYNTHETIC_DIAMOND", Material.DIAMOND), Map.entry("GOLD_24K", Material.GOLD_BLOCK),
            Map.entry("BRONZE", Material.COPPER_INGOT), Map.entry("CORINTHIAN_BRONZE", Material.COPPER_BLOCK),
            Map.entry("REDSTONE_ALLOY", Material.REDSTONE_BLOCK), Map.entry("BLISTERING", Material.BLAZE_POWDER));

    private final Block block;

    public ChickenSorterMenu(MultiverseNets plugin, Player player, Block block) {
        super(plugin, player);
        this.block = block;
    }

    public void openMenu() {
        open(SIZE, Component.text("Genetic Chicken Sorter", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
    }

    private NodeBlob blob() {
        NodeBlob blob = NodeStore.get(block);
        if (blob == null) {
            blob = NodeBlob.create(DeviceType.MVN_CHICKEN_SORTER.name());
        }
        if (blob.chickenProducts == null) {
            blob.chickenProducts = new ArrayList<>();
        }
        return blob;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    protected void draw() {
        NodeBlob blob = blob();
        ItemStack bar = pane(Material.ORANGE_STAINED_GLASS_PANE, " ");
        ItemStack background = pane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, i < 9 ? bar : background);
        }
        drawControlBar(blob);
        drawProducts(blob);
        drawDivider();
        drawRules(blob);
        drawActions(blob);
    }

    private void drawControlBar(NodeBlob blob) {
        inv.setItem(ACTIVE_SLOT, blob.chickenActive
                ? icon(Material.LIME_CONCRETE, "● Running", NamedTextColor.GREEN,
                        "Matching chickens are being moved.", "", "» Click to stop.")
                : icon(Material.RED_CONCRETE, "● Stopped", NamedTextColor.RED,
                        "Nothing moves while it is stopped.",
                        "A new sorter starts stopped, so it never",
                        "empties a network before it is set up.", "", "» Click to start."));

        inv.setItem(MODE_SLOT, blob.chickenPull
                ? icon(Material.STICKY_PISTON, "Direction: Pull in", NamedTextColor.AQUA,
                        "Takes matching chickens from the block", "on the selected side into the network.",
                        "", "» Click to switch to Push out.")
                : icon(Material.PISTON, "Direction: Push out", NamedTextColor.AQUA,
                        "Sends matching chickens from the network", "to the block on the selected side",
                        "(for example a Roost or a chest).", "", "» Click to switch to Pull in."));

        inv.setItem(SUMMARY_SLOT, summary(blob));

        List<String> sideLore = new ArrayList<>();
        sideLore.add("Which neighbour the sorter works with.");
        sideLore.add("");
        String current = face(blob);
        for (String option : FACES) {
            sideLore.add((option.equals(current) ? "▶ " : "   ") + faceName(option));
        }
        sideLore.add("");
        sideLore.add("» Click to cycle.");
        inv.setItem(FACE_SLOT, icon(Material.COMPASS, "Side: " + faceName(current), NamedTextColor.AQUA,
                sideLore.toArray(new String[0])));

        inv.setItem(HELP_SLOT, icon(Material.KNOWLEDGE_BOOK, "How it works", NamedTextColor.GOLD,
                "1. Pick the direction and the side.",
                "2. Optional: add the products you want",
                "   (empty list = every product).",
                "3. Optional: tighten the gene rules.",
                "4. Press Start.",
                "",
                "A chicken moves only if it passes EVERY",
                "rule. Only GeneticChickengineering",
                "pocket chickens; other items are ignored.",
                "Up to 16 chickens per cycle."));
    }

    private ItemStack summary(NodeBlob blob) {
        List<String> lines = new ArrayList<>();
        String side = faceName(face(blob)).toLowerCase(Locale.ROOT);
        lines.add("Status: " + (blob.chickenActive ? "running" : "stopped"));
        lines.add(blob.chickenPull ? "Moves: " + side + " → network" : "Moves: network → " + side);
        lines.add("");
        lines.add("Products: " + productsSummary(blob));
        lines.add("Tier: " + tierSummary(blob));
        lines.add("DNA strength: " + (blob.chickenMinStrength <= 0 ? "any" : "at least " + blob.chickenMinStrength));
        lines.add("Pure genes: " + (blob.chickenPureOnly ? "required" : "not required"));
        lines.add("DNA: " + switch (orAny(blob.chickenKnown)) {
            case "KNOWN" -> "sequenced only";
            case "UNKNOWN" -> "unsequenced only";
            default -> "any";
        });
        lines.add("Age: " + switch (orAny(blob.chickenAge)) {
            case "ADULT" -> "adults only";
            case "BABY" -> "babies only";
            default -> "any";
        });
        lines.add("");
        int active = activeRules(blob);
        lines.add(active == 0 ? "No rules: every pocket chicken passes." : active + " rule(s) active.");
        return icon(Material.WRITABLE_BOOK, "What passes right now", NamedTextColor.YELLOW, lines.toArray(new String[0]));
    }

    private void drawProducts(NodeBlob blob) {
        List<String> products = blob.chickenProducts;
        for (int i = 0; i < MAX_PRODUCTS; i++) {
            int slot = FIRST_PRODUCT_SLOT + i;
            if (i < products.size()) {
                String key = products.get(i);
                int tier = ChickenGenetics.productTier(key);
                inv.setItem(slot, icon(productIcon(key), ChickenGenetics.productName(key), NamedTextColor.YELLOW,
                        tier >= 0 ? "Laid by tier " + tier + " chickens." : "Accepted product.",
                        "", "» Click to remove it."));
            } else if (products.isEmpty() && i == 0) {
                inv.setItem(slot, icon(Material.LIME_STAINED_GLASS_PANE, "Every product passes", NamedTextColor.GREEN,
                        "The list is empty, so the product is",
                        "not checked. Add one to accept only",
                        "the products in the list:",
                        "",
                        "» Click here with a pocket chicken",
                        "  on the cursor, or Shift-click one",
                        "  in your inventory."));
            } else {
                inv.setItem(slot, icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "Free product slot", NamedTextColor.GRAY,
                        "» Click with a pocket chicken on the",
                        "  cursor, or Shift-click one in your",
                        "  inventory, to accept its product."));
            }
        }
    }

    private void drawDivider() {
        ItemStack divider = icon(Material.YELLOW_STAINED_GLASS_PANE, "▼ Gene rules", NamedTextColor.GOLD,
                "A chicken must pass every rule below.",
                "Glowing rule = active. Left +1, right −1,",
                "Shift-click resets it.",
                "",
                "▲ Above: accepted products.");
        for (int slot = 27; slot < 36; slot++) {
            inv.setItem(slot, divider);
        }
    }

    private void drawRules(NodeBlob blob) {
        int min = minTier(blob);
        inv.setItem(MIN_TIER_SLOT, rule(Material.IRON_INGOT, "Minimum tier", String.valueOf(min), min, min > 0,
                "Tier = number of recessive genes (0-6).",
                "Special species are tier 7-9.",
                "Default: 0 (any)."));
        Integer max = blob.chickenMaxTier;
        inv.setItem(MAX_TIER_SLOT, rule(Material.GOLD_INGOT, "Maximum tier", max == null ? "no limit" : String.valueOf(max),
                max == null ? MAX_TIER + 1 : max, max != null,
                "Rejects chickens above this tier.",
                "Go above 9 for no limit.",
                "Default: no limit."));
        int strength = blob.chickenMinStrength;
        inv.setItem(STRENGTH_SLOT, rule(Material.REDSTONE, "Minimum DNA strength", strength + " / " + MAX_STRENGTH,
                strength, strength > 0,
                "Strength drops with every recessive",
                "and every mixed (Aa) gene. 6 = flawless.",
                "Default: 0 (any)."));
        inv.setItem(PURE_SLOT, toggleRule(blob.chickenPureOnly ? Material.DIAMOND : Material.COAL, "Pure genes only",
                blob.chickenPureOnly,
                "ON: rejects chickens with any mixed",
                "(Aa) gene. Pure chickens breed true.",
                "Default: OFF."));
        String known = orAny(blob.chickenKnown);
        inv.setItem(KNOWN_SLOT, cycleRule(Material.BOOK, "DNA", known, !"ANY".equals(known),
                new String[]{"ANY", "KNOWN", "UNKNOWN"},
                new String[]{"Any", "Sequenced only", "Unsequenced only"},
                "Sequenced = DNA already read with",
                "the addon's tools."));
        String age = orAny(blob.chickenAge);
        inv.setItem(AGE_SLOT, cycleRule(Material.EGG, "Age", age, !"ANY".equals(age),
                new String[]{"ANY", "ADULT", "BABY"},
                new String[]{"Any", "Adults only", "Babies only"},
                "Babies grow up inside the pocket", "over time."));
    }

    private void drawActions(NodeBlob blob) {
        int count = blob.chickenProducts.size();
        inv.setItem(CLEAR_SLOT, count == 0
                ? icon(Material.STRUCTURE_VOID, "Clear product list", NamedTextColor.DARK_GRAY, "The list is already empty.")
                : icon(Material.BARRIER, "Clear product list (" + count + ")", NamedTextColor.RED,
                        "Removes every product from the list,", "so every product passes again.", "",
                        "» Click to clear."));
        int active = activeRules(blob);
        inv.setItem(RESET_SLOT, active == 0
                ? icon(Material.BUCKET, "Reset gene rules", NamedTextColor.DARK_GRAY, "Every rule is at its default.")
                : icon(Material.MILK_BUCKET, "Reset gene rules (" + active + " active)", NamedTextColor.RED,
                        "Puts tier, strength, pure genes, DNA", "and age back to their defaults.",
                        "Products, side and direction stay.", "", "» Click to reset."));
        inv.setItem(CLOSE_SLOT, icon(Material.OAK_DOOR, "Close", NamedTextColor.WHITE, "Changes are saved as you make them."));
    }

    // ------------------------------------------------------------------ clicks

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        NodeBlob blob = blob();
        if (raw >= inv.getSize()) {
            // Solo llegan aqui los shift-clic del inventario del jugador (ver GuiListener).
            addProduct(blob, event.getCurrentItem());
            return;
        }
        if (raw >= FIRST_PRODUCT_SLOT && raw < FIRST_PRODUCT_SLOT + MAX_PRODUCTS) {
            int index = raw - FIRST_PRODUCT_SLOT;
            if (index < blob.chickenProducts.size()) {
                String removed = blob.chickenProducts.remove(index);
                player.sendMessage(Text.msg("No longer accepting: " + ChickenGenetics.productName(removed), NamedTextColor.YELLOW));
                save(blob);
            } else {
                addProduct(blob, event.getView().getCursor());
            }
            return;
        }
        boolean right = event.isRightClick();
        boolean reset = event.isShiftClick();
        switch (raw) {
            case ACTIVE_SLOT -> {
                blob.chickenActive = !blob.chickenActive;
                if (blob.chickenActive && blob.chickenProducts.isEmpty() && activeRules(blob) == 0) {
                    player.sendMessage(Text.msg("Started with no rules: every pocket chicken will move.", NamedTextColor.YELLOW));
                }
            }
            case MODE_SLOT -> blob.chickenPull = !blob.chickenPull;
            case FACE_SLOT -> blob.targetFace = nextFace(face(blob), right);
            case MIN_TIER_SLOT -> {
                int next = reset ? 0 : clamp(minTier(blob) + (right ? -1 : 1), 0, MAX_TIER);
                blob.chickenMinTier = next == 0 ? null : next;
                if (blob.chickenMaxTier != null && blob.chickenMaxTier < next) {
                    blob.chickenMaxTier = next;
                }
            }
            case MAX_TIER_SLOT -> {
                if (reset) {
                    blob.chickenMaxTier = null;
                } else {
                    int current = blob.chickenMaxTier == null ? MAX_TIER + 1 : blob.chickenMaxTier;
                    int next = clamp(current + (right ? -1 : 1), 0, MAX_TIER + 1);
                    blob.chickenMaxTier = next > MAX_TIER ? null : next;
                    if (blob.chickenMaxTier != null && minTier(blob) > blob.chickenMaxTier) {
                        blob.chickenMinTier = blob.chickenMaxTier == 0 ? null : blob.chickenMaxTier;
                    }
                }
            }
            case STRENGTH_SLOT -> blob.chickenMinStrength = reset ? 0
                    : clamp(blob.chickenMinStrength + (right ? -1 : 1), 0, MAX_STRENGTH);
            case PURE_SLOT -> blob.chickenPureOnly = !reset && !blob.chickenPureOnly;
            case KNOWN_SLOT -> blob.chickenKnown = reset ? null : cycle(blob.chickenKnown, right, "KNOWN", "UNKNOWN");
            case AGE_SLOT -> blob.chickenAge = reset ? null : cycle(blob.chickenAge, right, "ADULT", "BABY");
            case CLEAR_SLOT -> {
                if (blob.chickenProducts.isEmpty()) {
                    return;
                }
                blob.chickenProducts.clear();
                player.sendMessage(Text.msg("Product list cleared: every product passes.", NamedTextColor.YELLOW));
            }
            case RESET_SLOT -> {
                if (activeRules(blob) == 0) {
                    return;
                }
                resetRules(blob);
                player.sendMessage(Text.msg("Gene rules reset to their defaults.", NamedTextColor.YELLOW));
            }
            case CLOSE_SLOT -> {
                player.closeInventory();
                return;
            }
            default -> {
                return;
            }
        }
        save(blob);
    }

    private void addProduct(NodeBlob blob, ItemStack item) {
        ChickenGenetics.Chicken chicken = ChickenGenetics.read(item);
        if (chicken == null) {
            if (item != null && !item.getType().isAir()) {
                player.sendMessage(Text.msg("Only GeneticChickengineering pocket chickens can be added.", NamedTextColor.RED));
            }
            return;
        }
        if (blob.chickenProducts.contains(chicken.product())) {
            player.sendMessage(Text.msg("That product is already in the list.", NamedTextColor.YELLOW));
            return;
        }
        if (blob.chickenProducts.size() >= MAX_PRODUCTS) {
            player.sendMessage(Text.msg("The product list is full (" + MAX_PRODUCTS + ").", NamedTextColor.RED));
            return;
        }
        blob.chickenProducts.add(chicken.product());
        player.sendMessage(Text.msg("Accepting: " + ChickenGenetics.productName(chicken.product()), NamedTextColor.GREEN));
        save(blob);
    }

    private void save(NodeBlob blob) {
        NodeStore.put(block, blob);
        draw();
    }

    // ------------------------------------------------------------------ rules

    /** Gene rules away from their default; products, side and direction are not rules / Reglas activas. */
    static int activeRules(NodeBlob blob) {
        int active = 0;
        if (minTier(blob) > 0) {
            active++;
        }
        if (blob.chickenMaxTier != null) {
            active++;
        }
        if (blob.chickenMinStrength > 0) {
            active++;
        }
        if (blob.chickenPureOnly) {
            active++;
        }
        if (blob.chickenKnown != null) {
            active++;
        }
        if (blob.chickenAge != null) {
            active++;
        }
        return active;
    }

    private static void resetRules(NodeBlob blob) {
        blob.chickenMinTier = null;
        blob.chickenMaxTier = null;
        blob.chickenMinStrength = 0;
        blob.chickenPureOnly = false;
        blob.chickenKnown = null;
        blob.chickenAge = null;
    }

    private static String productsSummary(NodeBlob blob) {
        List<String> products = blob.chickenProducts;
        if (products.isEmpty()) {
            return "any";
        }
        StringBuilder out = new StringBuilder();
        int shown = Math.min(3, products.size());
        for (int i = 0; i < shown; i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(ChickenGenetics.productName(products.get(i)));
        }
        if (products.size() > shown) {
            out.append(" +").append(products.size() - shown).append(" more");
        }
        return out.toString();
    }

    private static String tierSummary(NodeBlob blob) {
        int min = minTier(blob);
        Integer max = blob.chickenMaxTier;
        if (min == 0 && max == null) {
            return "any";
        }
        if (max == null) {
            return min + " or higher";
        }
        if (min == 0) {
            return "up to " + max;
        }
        return min == max ? "exactly " + min : min + " to " + max;
    }

    private static String face(NodeBlob blob) {
        return blob.targetFace == null ? "ALL" : blob.targetFace.toUpperCase(Locale.ROOT);
    }

    private static String faceName(String face) {
        return switch (face) {
            case "NORTH" -> "North";
            case "SOUTH" -> "South";
            case "EAST" -> "East";
            case "WEST" -> "West";
            case "UP" -> "Up";
            case "DOWN" -> "Down";
            default -> "All sides";
        };
    }

    private static String nextFace(String current, boolean backwards) {
        for (int i = 0; i < FACES.length; i++) {
            if (FACES[i].equals(current)) {
                return FACES[Math.floorMod(i + (backwards ? -1 : 1), FACES.length)];
            }
        }
        return "ALL";
    }

    private static int minTier(NodeBlob blob) {
        return blob.chickenMinTier == null ? 0 : blob.chickenMinTier;
    }

    private static String orAny(String value) {
        return value == null ? "ANY" : value;
    }

    /** null → first → second → null; backwards walks the other way / Recorre los tres estados. */
    private static String cycle(String current, boolean backwards, String first, String second) {
        String[] order = {null, first, second};
        int index = 0;
        for (int i = 0; i < order.length; i++) {
            if (java.util.Objects.equals(order[i], current)) {
                index = i;
            }
        }
        return order[Math.floorMod(index + (backwards ? -1 : 1), order.length)];
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // ------------------------------------------------------------------ icons

    static Material productIcon(String key) {
        String id = ChickenGenetics.productId(key);
        if (id == null) {
            return Material.EGG;
        }
        Material mapped = PRODUCT_ICONS.get(id);
        if (mapped != null) {
            return mapped;
        }
        Material direct = Material.matchMaterial(id);
        if (direct != null && direct.isItem() && !direct.isAir()) {
            return direct;
        }
        if (id.endsWith("_DUST")) {
            return Material.SUGAR;
        }
        int tier = ChickenGenetics.productTier(key);
        return tier >= 9 ? Material.NETHER_STAR : tier >= 7 ? Material.IRON_BLOCK : Material.EGG;
    }

    private static ItemStack rule(Material material, String name, String value, int amount, boolean active, String... help) {
        List<String> lore = new ArrayList<>(List.of(help));
        lore.add("");
        lore.add("» Left-click: +1   Right-click: −1");
        lore.add("» Shift-click: reset");
        ItemStack item = icon(material, name + ": " + value, active ? NamedTextColor.GREEN : NamedTextColor.WHITE,
                lore.toArray(new String[0]));
        item.setAmount(clamp(amount, 1, 64));
        glow(item, active);
        return item;
    }

    private static ItemStack toggleRule(Material material, String name, boolean on, String... help) {
        List<String> lore = new ArrayList<>(List.of(help));
        lore.add("");
        lore.add("» Click to turn " + (on ? "OFF" : "ON") + ".");
        ItemStack item = icon(material, name + ": " + (on ? "ON" : "OFF"), on ? NamedTextColor.GREEN : NamedTextColor.WHITE,
                lore.toArray(new String[0]));
        glow(item, on);
        return item;
    }

    private static ItemStack cycleRule(Material material, String name, String current, boolean active,
                                       String[] values, String[] labels, String... help) {
        List<String> lore = new ArrayList<>(List.of(help));
        lore.add("");
        String label = labels[0];
        for (int i = 0; i < values.length; i++) {
            boolean selected = values[i].equals(current);
            if (selected) {
                label = labels[i];
            }
            lore.add((selected ? "▶ " : "   ") + labels[i]);
        }
        lore.add("");
        lore.add("» Left-click: next   Right-click: back");
        lore.add("» Shift-click: reset");
        ItemStack item = icon(material, name + ": " + label, active ? NamedTextColor.GREEN : NamedTextColor.WHITE,
                lore.toArray(new String[0]));
        glow(item, active);
        return item;
    }

    private static void glow(ItemStack item, boolean on) {
        if (!on) {
            return;
        }
        var meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
    }

    private static ItemStack pane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack icon(Material material, String name, NamedTextColor color, String... lore) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        List<Component> lines = new ArrayList<>();
        for (String line : lore) {
            NamedTextColor lineColor = line.startsWith("»") ? NamedTextColor.YELLOW
                    : line.startsWith("▶") ? NamedTextColor.GREEN : NamedTextColor.GRAY;
            lines.add(Component.text(line, lineColor).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lines);
        item.setItemMeta(meta);
        return item;
    }
}

package com.chagui68.multiversenets.item;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.util.Keys;
import com.chagui68.multiversenets.util.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Factory and registry for plugin items, custom tools, PDC metadata, and crafting recipes.
 *
 * Fábrica y registro de ítems del plugin, herramientas personalizadas, metadatos PDC y recetas de crafteo.
 */
public final class Items {

    private Items() {
    }

    /**
     * Creates a newly instantiated ItemStack for a device type with standard metadata and lore.
 *
     * Crea un nuevo ItemStack para un tipo de dispositivo con metadatos y lore estándar.
     *
     * @param type Device type / Tipo de dispositivo
     * @return Prepared ItemStack / ItemStack preparado
     */
    public static ItemStack create(DeviceType type) {
        ItemStack item = new ItemStack(type.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(type.display(), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();

        switch (type) {
            case MVN_CONTROLLER -> {
                lore.add(Component.text("Central brain powering and coordinating the network.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Memory modules go in a DRAM Bay connected to the network.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_CABLE -> {
                lore.add(Component.text("Digital conduit connecting devices across the network.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Right-click it to camouflage it as any solid block.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("4 cables + a dye: coloured cables to tell areas apart.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_TERMINAL -> {
                lore.add(Component.text("Interactive console to view, store, and withdraw items and liquids.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Includes search filtering, sorting, and fluid storage access.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_MONITOR -> {
                lore.add(Component.text("Diagnostic panel: nodes by type, storage usage and network health.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Right-click to open; refreshes live while open.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_ROUTER -> {
                lore.add(Component.text("Antenna that lifts the Wireless Terminal range and world limits.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Without it, Wireless Terminals only work nearby, in the same world.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_CELL_T1, MVN_CELL_T2, MVN_CELL_T3, MVN_CELL_T4, MVN_CELL_T5, MVN_CELL_T6 -> {
                lore.add(Component.text("High-capacity digital storage cell for a single item type.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Capacity: " + formatAmount(capacityOf(type)) + " items", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_GREEDY_CELL -> {
                lore.add(Component.text("Dedicated buffer cell continuously pulling target items from network.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Capacity: " + formatAmount(capacityOf(type)) + " items", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_INFINITY_BARREL -> {
                lore.add(Component.text("Deep-storage barrel for a single item type.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Capacity: " + formatAmount(Settings.barrelCapacity()) + " items", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_GRABBER -> {
                lore.add(Component.text("Omnidirectional node importing items from adjacent containers.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_GRABBER_HT -> {
                lore.add(Component.text("Directional high-speed importer with side and filter controls.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_PUSHER -> {
                lore.add(Component.text("Omnidirectional node exporting items into adjacent containers.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_PUSHER_HT -> {
                lore.add(Component.text("Directional high-speed exporter with side and filter controls.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_VACUUM -> {
                lore.add(Component.text("Absorbs dropped item entities in the world into the network.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_PURGER -> {
                lore.add(Component.text("Safely voids and deletes unwanted overflow items matching filter.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_LIMITER -> {
                lore.add(Component.text("Regulates max storage stock for a target item in the network.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Stops incoming imports once ceiling limit is met.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_PROBE -> {
                lore.add(Component.text("Diagnostic tool inspecting network throughput, nodes, and status.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_CRAFTER -> {
                lore.add(Component.text("Automated crafting machine driven by installed recipe Blueprints.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_SF_CRAFTER -> {
                lore.add(Component.text("Automated crafting machine driven by recipe Blueprints.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Accepts Slimefun AND vanilla Blueprints.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_STOCK_CRAFTER -> {
                lore.add(Component.text("Auto-Crafter that stops each output at 64 in the network.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_SF_STOCK_CRAFTER -> {
                lore.add(Component.text("Auto-Crafter that stops each output at 64 in the network.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Accepts Slimefun AND vanilla Blueprints.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_ENCODER -> {
                lore.add(Component.text("Encodes standard crafting recipes onto blank Blueprints.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_SF_ENCODER -> {
                lore.add(Component.text("Encodes Slimefun item recipes onto blank Blueprints.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_CRAFTING_GRID -> {
                lore.add(Component.text("Interactive 3x3 crafting grid directly connected to network storage.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_QUANTUM_WORKBENCH -> {
                lore.add(Component.text("Upgrades a Quantum Cell one tier (cell + 8 diamonds), keeping its cargo.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_TRANSMITTER -> {
                lore.add(Component.text("Wireless bridge end. Link it to a Receiver to send filtered items.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_RECEIVER -> {
                lore.add(Component.text("Wireless bridge end. Link it to a Transmitter to pull filtered items.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_WIRELESS_TERMINAL -> {
                lore.add(Component.text("Handheld device granting remote access to network storage.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Status: ", NamedTextColor.GRAY)
                        .append(Component.text("Unbound", NamedTextColor.RED))
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Shift+Right Click a Controller or Terminal to bind.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }
            case MVN_BLUEPRINT -> {
                lore.add(Component.text("Recipe pattern blueprint for Auto-Crafter machines.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_CONFIGURATOR -> {
                lore.add(Component.text("Shift+right-click a device to copy its filter; right-click to paste it.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_RAKE -> {
                lore.add(Component.text("Instantly dismantles and recovers network nodes without damage.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_FLUID_CELL -> {
                lore.add(Component.text("Digital storage tank for water, lava, milk, honey, and powder snow.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Capacity: " + formatAmount(Settings.fluidCellCapacity()) + " mB ("
                        + (Settings.fluidCellCapacity() / 1000) + " Buckets)", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Right-click with a bucket to deposit/extract directly.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_LIQUID_PUMP -> {
                lore.add(Component.text("Pumps water and lava sources directly from the block underneath.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Deposits pumped liquids directly into network fluid cells.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_REQUEST_TERMINAL -> {
                lore.add(Component.text("On-demand crafting ordering console discovered across network.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Order custom quantities to player inventory or storage.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_REQUEST_CRAFTER -> {
                lore.add(Component.text("On-demand crafting unit managed via the Request Terminal.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Holds blueprints for batch crafting jobs (does not auto-craft).", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_SF_REQUEST_CRAFTER -> {
                lore.add(Component.text("On-demand crafting unit managed via the Request Terminal.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Holds Slimefun and vanilla blueprints for batch requests.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_DRAM_BAY -> {
                lore.add(Component.text("Holds one memory module: its stock becomes network storage.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Eject the module to carry its whole stock to another network.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_FLUID_DRAM -> {
                lore.add(Component.text("Fluid-only memory module for a DRAM Bay; several fluids at once.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Capacity: " + formatAmount(Settings.fluidDramCapacity()) + " mB ("
                        + (Settings.fluidDramCapacity() / 1000) + " Buckets)", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Keeps its fluids when taken out of the bay.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_CHICKEN_SORTER -> {
                lore.add(Component.text("Sorts GeneticChickengineering pocket chickens by their genes.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Filter by product, tier, DNA strength, purity, DNA and age.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_SILICON_WAFER, MVN_NETWORK_CIRCUIT, MVN_LOGIC_PROCESSOR, MVN_STORAGE_MATRIX,
                 MVN_TRANSFER_COIL, MVN_QUANTUM_CORE -> {
                lore.add(Component.text(switch (type) {
                    case MVN_SILICON_WAFER -> "Base of every circuit.";
                    case MVN_NETWORK_CIRCUIT -> "Basic logic for cables, terminals and devices.";
                    case MVN_LOGIC_PROCESSOR -> "Brain of controllers, crafters and terminals.";
                    case MVN_STORAGE_MATRIX -> "Core of every storage block and module.";
                    case MVN_TRANSFER_COIL -> "Moves items: Grabbers, Pushers, Vacuum, Pump.";
                    default -> "Links far places: wireless, routers, workbench.";
                }, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Crafting component: only used in MultiverseNets recipes.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_QUOTA_MANAGER -> {
                lore.add(Component.text("Shows and edits every Quota Limiter of its network.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Add, set or remove limited items; each uses a free limiter.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_CONTAINER_CONNECTOR -> {
                lore.add(Component.text("Chests, barrels and shulker boxes touching it become network storage.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Used last: the network fills its own storage first.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_MODULE_UNLOADER -> {
                lore.add(Component.text("Empties memory modules (cards) into the network.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Holds 9 cards; what does not fit stays on the card.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_MODULE_LOADER -> {
                lore.add(Component.text("Fills a memory module with one item or fluid from the network.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Choose the item and the amount, press Start.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            case MVN_STATUS_LIGHT -> {
                lore.add(Component.text("Place it on any side of a network block.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Green: connected to a Controller. Red: no connection.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            default -> {
                if (type.isCacheModule()) {
                    lore.add(Component.text("Item memory module for a DRAM Bay.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                    lore.add(Component.text("Capacity: " + formatAmount(capacityOf(type)) + " items", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                    lore.add(Component.text("Keeps its items when taken out of the bay.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
                }
            }
        }

        // MultiverseNets distinction footer
        lore.add(Component.empty());
        lore.add(Component.text("▪ MultiverseNets", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(Keys.DEVICE_TYPE, PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
        return item;
    }

    // ---------------------------------------------------------------- Coloured cables / Cables de color

    /** Glass for a plain cable, the matching stained glass for a coloured one / Material del cable. */
    public static Material cableMaterial(org.bukkit.DyeColor color) {
        return color == null ? DeviceType.MVN_CABLE.material() : Material.valueOf(color.name() + "_STAINED_GLASS");
    }

    /** "Light Blue" for LIGHT_BLUE / Nombre legible del color. */
    public static String colorName(org.bukkit.DyeColor color) {
        StringBuilder out = new StringBuilder();
        for (String part : color.name().toLowerCase(java.util.Locale.ROOT).split("_")) {
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    /**
     * EN: A Network Cable of a colour (null = plain glass). Colour is only a look: every colour
     * connects like the plain cable; it helps telling areas of a build apart.
     * ES: Un Cable de red de un color (null = cristal). El color es solo aspecto: todos conectan
     * igual que el cable normal; sirve para distinguir zonas.
     */
    public static ItemStack cable(org.bukkit.DyeColor color) {
        ItemStack plain = create(DeviceType.MVN_CABLE);
        if (color == null) {
            return plain;
        }
        ItemStack item = plain.withType(cableMaterial(color));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(DeviceType.MVN_CABLE.display() + " ", NamedTextColor.AQUA)
                .append(Component.text("(" + colorName(color) + ")",
                        net.kyori.adventure.text.format.TextColor.color(color.getColor().asRGB())))
                .decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(Keys.CABLE_COLOR, PersistentDataType.STRING, color.name());
        item.setItemMeta(meta);
        return item;
    }

    /** The colour of a cable item, or null for a plain one / El color de un cable, o null. */
    public static org.bukkit.DyeColor cableColor(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        return parseColor(item.getPersistentDataContainer().get(Keys.CABLE_COLOR, PersistentDataType.STRING));
    }

    public static org.bukkit.DyeColor parseColor(String name) {
        if (name == null) {
            return null;
        }
        try {
            return org.bukkit.DyeColor.valueOf(name);
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    /**
     * Resolves the DeviceType of an ItemStack from its PersistentDataContainer.
 *
     * Resuelve el DeviceType de un ItemStack desde su PersistentDataContainer.
     *
     * @param item Target ItemStack / ItemStack objetivo
     * @return Resolved DeviceType or null / DeviceType resuelto o null
     */
    public static DeviceType typeOf(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        // Read-only PDC view: no ItemMeta clone on the network hot path (#87).
        String name = item.getPersistentDataContainer()
                .get(Keys.DEVICE_TYPE, PersistentDataType.STRING);
        return name == null ? null : DeviceType.parse(name);
    }

    // ---------------------------------------------------------------- Tools / Herramientas

    /**
     * Creates a fresh network rake tool with configured durability uses.
 *
     * Crea una herramienta de rastrillo de red nueva con los usos de durabilidad configurados.
     *
     * @return Rake ItemStack / ItemStack del rastrillo
     */
    public static ItemStack rake() {
        ItemStack item = create(DeviceType.MVN_RAKE);
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.RAKE_USES, PersistentDataType.INTEGER, Settings.rakeUses());
        meta.lore(java.util.List.of(
                Component.text("Right click a network node to remove it instantly.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text(Settings.rakeUses() + " uses left", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Retrieves remaining uses on a rake tool item.
 *
     * Obtiene los usos restantes de un rastrillo.
     *
     * @param item Rake ItemStack / ItemStack del rastrillo
     * @return Remaining uses / Usos restantes
     */
    public static int rakeUses(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        Integer uses = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.RAKE_USES, PersistentDataType.INTEGER);
        return uses == null ? 0 : uses;
    }

    /**
     * Decrements a use from a rake tool. Returns false if durability is depleted and the item should break.
 *
     * Gasta un uso de un rastrillo. Devuelve false cuando la herramienta se agota y debe romperse.
     *
     * @param item Rake ItemStack / ItemStack del rastrillo
     * @return true if uses remain, false if broken / true si quedan usos, false si se rompió
     */
    public static boolean spendRakeUse(ItemStack item) {
        int uses = rakeUses(item) - 1;
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.RAKE_USES, PersistentDataType.INTEGER, uses);
        meta.lore(java.util.List.of(
                Component.text("Right click a network node to remove it instantly.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text(Math.max(0, uses) + " uses left", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return uses > 0;
    }

    /**
     * Saves copied filter settings onto a configurator wrench item ("WL:MAT1,MAT2" or "BL:MAT1,MAT2").
 *
     * Guarda la configuración de filtros copiada en una llave de configuración ("WL:MAT1,MAT2" o "BL:MAT1,MAT2").
     *
     * @param item Configurator ItemStack / ItemStack de la llave
     * @param mats List of material names / Lista de nombres de materiales
     * @param blacklist true if blacklist mode, false if whitelist / true si es lista negra, false si es blanca
     */
    public static void saveConfig(ItemStack item, java.util.List<String> mats, boolean blacklist) {
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.CONFIG_DATA, PersistentDataType.STRING,
                (blacklist ? "BL:" : "WL:") + String.join(",", mats));
        meta.lore(java.util.List.of(
                Component.text((blacklist ? "Blacklist: " : "Whitelist: ")
                                + (mats.isEmpty() ? "(empty)" : String.join(", ", mats)),
                        NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
    }

    /**
     * EN: Stores the exact filter templates (custom names, Slimefun items, MultiverseNets devices)
     * next to the material list, so a pasted filter matches what the source device matched.
     *
     * ES: Guarda las plantillas exactas del filtro (nombres, ítems de Slimefun, dispositivos) junto
     * a la lista de materiales, para que el filtro pegado coincida con el del dispositivo origen.
     */
    public static void saveConfigItems(ItemStack item, java.util.List<ItemStack> templates) {
        var meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        if (templates == null || templates.isEmpty()) {
            pdc.remove(Keys.CONFIG_ITEMS);
        } else {
            com.chagui68.multiversenets.persist.NodeBlob carrier =
                    com.chagui68.multiversenets.persist.NodeBlob.create("MVN_CONFIGURATOR");
            carrier.filterItems = new ArrayList<>(templates);
            pdc.set(Keys.CONFIG_ITEMS, PersistentDataType.STRING,
                    com.chagui68.multiversenets.persist.NodeStore.encode(carrier));
        }
        item.setItemMeta(meta);
    }

    /**
     * EN: Exact filter templates copied by the wrench; empty for wrenches copied before this existed.
     *
     * ES: Plantillas exactas copiadas por la llave; vacío en llaves copiadas antes de que existiera.
     */
    public static java.util.List<ItemStack> readConfigItems(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return new ArrayList<>();
        }
        String data = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.CONFIG_ITEMS, PersistentDataType.STRING);
        if (data == null) {
            return new ArrayList<>();
        }
        com.chagui68.multiversenets.persist.NodeBlob carrier =
                com.chagui68.multiversenets.persist.NodeStore.decode(data);
        if (carrier == null || carrier.filterItems == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(carrier.filterItems);
    }

    /**
     * Reads copied filter configuration from a configurator wrench item.
     * Returns array [mats..., "bl"/"wl"] or null if empty.
 *
     * Lee la configuración de filtros de una llave.
     * Devuelve el array [mats..., "bl"/"wl"] o null si está vacía.
     *
     * @param item Configurator ItemStack / ItemStack de la llave
     * @return Parsed configuration array or null / Array de configuración parseado o null
     */
    public static String[] readConfig(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String data = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.CONFIG_DATA, PersistentDataType.STRING);
        if (data == null || data.length() < 3) {
            return null;
        }
        String mode = data.substring(0, 3);
        String body = data.substring(3);
        if (body.isEmpty()) {
            return new String[]{mode.equals("BL:") ? "bl" : "wl"};
        }
        String[] mats = body.split(",");
        String[] out = new String[mats.length + 1];
        System.arraycopy(mats, 0, out, 0, mats.length);
        out[mats.length] = mode.equals("BL:") ? "bl" : "wl";
        return out;
    }

    /**
     * Creates an encoded recipe blueprint item.
 *
     * Crea un ítem de plano de receta codificado.
     *
     * @param recipeKey Namespaced key string / Clave de receta namespaced
     * @param resultName Display name of recipe output / Nombre legible del resultado de la receta
     * @return Encoded blueprint ItemStack / ItemStack del plano codificado
     */
    public static ItemStack blueprint(String recipeKey, String resultName) {
        ItemStack item = new ItemStack(DeviceType.MVN_BLUEPRINT.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Blueprint: " + resultName, NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Recipe: " + recipeKey, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Click an Auto-Crafter to install", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(Keys.DEVICE_TYPE, PersistentDataType.STRING, DeviceType.MVN_BLUEPRINT.name());
        meta.getPersistentDataContainer().set(Keys.BLUEPRINT_RECIPE, PersistentDataType.STRING, recipeKey);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * @param item Target item / Ítem objetivo
     * @return true if item is an encoded blueprint / true si el ítem es un plano codificado
     */
    public static boolean isBlueprint(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .has(Keys.BLUEPRINT_RECIPE, PersistentDataType.STRING);
    }

    /**
     * @param item Target item / Ítem objetivo
     * @return Encoded recipe key or null / Clave de receta codificada o null
     */
    public static String readBlueprint(ItemStack item) {
        if (!isBlueprint(item)) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .get(Keys.BLUEPRINT_RECIPE, PersistentDataType.STRING);
    }

    /**
     * Links a receiver item to a transmitter block location.
 *
     * Vincula un ítem receptor a la ubicación del bloque transmisor.
     *
     * @param item Receiver ItemStack / ItemStack del receptor
     * @param transmitter Location of target transmitter / Ubicación del transmisor objetivo
     */
    public static void linkReceiver(ItemStack item, Location transmitter) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.RECEIVER_BIND, PersistentDataType.STRING,
                transmitter.getWorld().getUID() + ";" + transmitter.getBlockX() + ";"
                        + transmitter.getBlockY() + ";" + transmitter.getBlockZ());
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("MultiverseNets", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Status: ", NamedTextColor.GRAY)
                .append(Component.text("Linked", NamedTextColor.GREEN))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Transmitter: ", NamedTextColor.GRAY)
                .append(Component.text(transmitter.getBlockX() + ", " + transmitter.getBlockY() + ", " + transmitter.getBlockZ(), NamedTextColor.YELLOW))
                .decoration(TextDecoration.ITALIC, false));
        if (transmitter.getWorld() != null) {
            lore.add(Component.text("World: ", NamedTextColor.GRAY)
                    .append(Component.text(transmitter.getWorld().getName(), NamedTextColor.AQUA))
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    /**
     * Reads bound transmitter location from a receiver item.
 *
     * Lee la ubicación del transmisor vinculado desde un ítem receptor.
     *
     * @param item Receiver ItemStack / ItemStack del receptor
     * @return Bound transmitter Location or null / Ubicación del transmisor vinculado o null
     */
    public static Location readReceiverBind(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String data = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.RECEIVER_BIND, PersistentDataType.STRING);
        return parseLocation(data);
    }

    private static Location parseLocation(String data) {
        if (data == null) {
            return null;
        }
        String[] parts = data.split(";");
        var world = Bukkit.getWorld(java.util.UUID.fromString(parts[0]));
        if (world == null) {
            return null;
        }
        return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }

    /**
     * Binds a wireless terminal item to a controller/terminal block location.
 *
     * Vincula una terminal inalámbrica a la ubicación de un controlador o terminal.
     *
     * @param item Wireless Terminal ItemStack / ItemStack de la terminal inalámbrica
     * @param loc Target Controller/Terminal Location / Ubicación del controlador/terminal objetivo
     */
    public static void bindWireless(ItemStack item, Location loc) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.WIRELESS_BIND, PersistentDataType.STRING,
                loc.getWorld().getUID() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ());
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("MultiverseNets", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Status: ", NamedTextColor.GRAY)
                .append(Component.text("Linked", NamedTextColor.GREEN))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Controller: ", NamedTextColor.GRAY)
                .append(Component.text(loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(), NamedTextColor.YELLOW))
                .decoration(TextDecoration.ITALIC, false));
        if (loc.getWorld() != null) {
            lore.add(Component.text("World: ", NamedTextColor.GRAY)
                    .append(Component.text(loc.getWorld().getName(), NamedTextColor.AQUA))
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Right click to open terminal", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    /**
     * Reads bound controller location from a wireless terminal item.
 *
     * Lee la ubicación del controlador vinculado desde una terminal inalámbrica.
     *
     * @param item Wireless Terminal ItemStack / ItemStack de la terminal inalámbrica
     * @return Bound Location or null / Ubicación vinculada o null
     */
    public static Location readWirelessBind(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String data = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.WIRELESS_BIND, PersistentDataType.STRING);
        return parseLocation(data);
    }

    /**
     * Retrieves storage capacity for a given device type.
 *
     * Obtiene la capacidad de almacenamiento para un tipo de dispositivo dado.
     *
     * @param type Target device type / Tipo de dispositivo objetivo
     * @return Total capacity in items / Capacidad total en ítems
     */
    public static long capacityOf(DeviceType type) {
        if (type == null) {
            return 0L;
        }
        if (type == DeviceType.MVN_INFINITY_BARREL) {
            return com.chagui68.multiversenets.util.Settings.barrelCapacity();
        }
        if (type == DeviceType.MVN_GREEDY_CELL) {
            return com.chagui68.multiversenets.util.Settings.greedyCapacity();
        }
        if (type.isCacheModule()) {
            return com.chagui68.multiversenets.util.Settings.virtualCacheCapacity(type.cacheTier());
        }
        return com.chagui68.multiversenets.util.Settings.cellCapacity(type.cellTier());
    }

    /**
     * Formats large item counts into human-readable shorthand strings (e.g. 1.5k, 2.3M, 1B).
 *
     * Formatea grandes cantidades de ítems en texto abreviado legible (ej. 1.5k, 2.3M, 1B).
     *
     * @param amount Item count / Cantidad de ítems
     * @return Formatted string / Cadena formateada
     */
    public static String formatAmount(long amount) {
        if (amount >= 1_000_000_000L) {
            return trim(amount / 1_000_000_000.0) + "B";
        }
        if (amount >= 1_000_000L) {
            return trim(amount / 1_000_000.0) + "M";
        }
        if (amount >= 1_000L) {
            return trim(amount / 1_000.0) + "k";
        }
        return String.valueOf(amount);
    }

    private static String trim(double v) {
        return v >= 100 ? String.valueOf((long) v) : String.valueOf(Math.round(v * 10.0) / 10.0);
    }

    /**
     * Registers all plugin crafting recipes with the Bukkit server recipe manager.
 *
     * Registra todas las recetas de crafteo del plugin en el gestor de recetas del servidor Bukkit.
     *
     * @param plugin Main plugin instance / Instancia principal del plugin
     */
    public static synchronized void registerRecipes(MultiverseNets plugin) {
        synchronized (RECIPE_KEYS) {
            // Se quitan primero las registradas: si /mvnets reload desactivo una maquina (p. ej. las
            // de Slimefun), su receta no debe quedarse viva en Bukkit.
            for (org.bukkit.NamespacedKey key : RECIPE_KEYS) {
                try {
                    Bukkit.removeRecipe(key);
                } catch (Throwable ignored) {
                }
            }
            RECIPE_KEYS.clear();
        }
        // Componentes intermedios: solo existen para las recetas del plugin, asi ningun
        // dispositivo depende de una forma que una receta vanilla pueda pisar.
        shaped(plugin, "silicon_wafer", stackOf(create(DeviceType.MVN_SILICON_WAFER), 2), r -> {
            r.shape(" Q ", "QSQ", " Q ");
            r.setIngredient('Q', Material.QUARTZ);
            r.setIngredient('S', Material.SAND);
        });
        shaped(plugin, "network_circuit", stackOf(create(DeviceType.MVN_NETWORK_CIRCUIT), 2), r -> {
            r.shape("RGR", "WIW", "RGR");
            r.setIngredient('R', Material.REDSTONE);
            r.setIngredient('G', Material.GOLD_NUGGET);
            device(r, 'W', DeviceType.MVN_SILICON_WAFER);
            r.setIngredient('I', Material.IRON_INGOT);
        });
        shaped(plugin, "logic_processor", create(DeviceType.MVN_LOGIC_PROCESSOR), r -> {
            r.shape("QCQ", "CRC", "QCQ");
            r.setIngredient('Q', Material.QUARTZ);
            device(r, 'C', DeviceType.MVN_NETWORK_CIRCUIT);
            r.setIngredient('R', Material.REDSTONE_BLOCK);
        });
        shaped(plugin, "storage_matrix", create(DeviceType.MVN_STORAGE_MATRIX), r -> {
            r.shape("IWI", "CDC", "IWI");
            r.setIngredient('I', Material.IRON_INGOT);
            device(r, 'W', DeviceType.MVN_SILICON_WAFER);
            device(r, 'C', DeviceType.MVN_NETWORK_CIRCUIT);
            r.setIngredient('D', Material.DIAMOND);
        });
        shaped(plugin, "transfer_coil", stackOf(create(DeviceType.MVN_TRANSFER_COIL), 2), r -> {
            r.shape("KRK", "RIR", "KRK");
            r.setIngredient('K', Material.COPPER_INGOT);
            r.setIngredient('R', Material.REDSTONE);
            r.setIngredient('I', Material.IRON_INGOT);
        });
        shaped(plugin, "quantum_core", create(DeviceType.MVN_QUANTUM_CORE), r -> {
            r.shape("ADA", "PEP", "ADA");
            r.setIngredient('A', Material.AMETHYST_SHARD);
            r.setIngredient('D', Material.DIAMOND);
            device(r, 'P', DeviceType.MVN_LOGIC_PROCESSOR);
            r.setIngredient('E', Material.ENDER_PEARL);
        });
        shaped(plugin, "controller", create(DeviceType.MVN_CONTROLLER), r -> {
            r.shape("IPI", "CNC", "IPI");
            r.setIngredient('I', Material.IRON_BLOCK);
            device(r, 'P', DeviceType.MVN_LOGIC_PROCESSOR);
            device(r, 'C', DeviceType.MVN_NETWORK_CIRCUIT);
            r.setIngredient('N', Material.NETHER_STAR);
        });
        shaped(plugin, "cable", stackOf(create(DeviceType.MVN_CABLE), 16), r -> {
            r.shape("GGG", "GCG", "GGG");
            r.setIngredient('G', Material.GLASS);
            device(r, 'C', DeviceType.MVN_NETWORK_CIRCUIT);
        });
        // Cables de color: 4 cables de cualquier color + un tinte = 4 cables de ese color. En cruz y
        // no 8 + 1 para no chocar con la receta vanilla de cristal tintado.
        List<Material> anyCable = new ArrayList<>();
        anyCable.add(DeviceType.MVN_CABLE.material());
        for (org.bukkit.DyeColor color : org.bukkit.DyeColor.values()) {
            anyCable.add(cableMaterial(color));
        }
        for (org.bukkit.DyeColor color : org.bukkit.DyeColor.values()) {
            shaped(plugin, "cable_" + color.name().toLowerCase(java.util.Locale.ROOT), stackOf(cable(color), 4), r -> {
                r.shape(" C ", "CDC", " C ");
                r.setIngredient('C', new org.bukkit.inventory.RecipeChoice.MaterialChoice(anyCable));
                PENDING_DEVICES.put('C', DeviceType.MVN_CABLE);
                r.setIngredient('D', Material.valueOf(color.name() + "_DYE"));
            });
        }
        shaped(plugin, "terminal", create(DeviceType.MVN_TERMINAL), r -> {
            r.shape("GCG", "EBE", "GCG");
            r.setIngredient('G', Material.GLASS);
            device(r, 'C', DeviceType.MVN_NETWORK_CIRCUIT);
            r.setIngredient('E', Material.ENDER_PEARL);
            r.setIngredient('B', Material.BEACON);
        });
        shaped(plugin, "cell_t1", create(DeviceType.MVN_CELL_T1), r -> {
            r.shape("GGG", "GMG", "GGG");
            r.setIngredient('G', Material.GLASS);
            device(r, 'M', DeviceType.MVN_STORAGE_MATRIX);
        });
        for (int tier = 2; tier <= 6; tier++) {
            DeviceType prev = DeviceType.parse("MVN_CELL_T" + (tier - 1));
            DeviceType cur = DeviceType.parse("MVN_CELL_T" + tier);
            final DeviceType prevFinal = prev;
            shaped(plugin, "cell_t" + tier, create(cur), r -> {
                r.shape("DDD", "DPD", "DDD");
                r.setIngredient('D', Material.DIAMOND);
                device(r, 'P', prevFinal);
            });
        }
        shaped(plugin, "grabber", create(DeviceType.MVN_GRABBER), r -> {
            r.shape("ITI", "OCO", "ITI");
            r.setIngredient('I', Material.IRON_INGOT);
            device(r, 'T', DeviceType.MVN_TRANSFER_COIL);
            r.setIngredient('O', Material.OBSERVER);
            device(r, 'C', DeviceType.MVN_NETWORK_CIRCUIT);
        });
        shaped(plugin, "pusher", create(DeviceType.MVN_PUSHER), r -> {
            r.shape("ITI", "DCD", "ITI");
            r.setIngredient('I', Material.IRON_INGOT);
            device(r, 'T', DeviceType.MVN_TRANSFER_COIL);
            r.setIngredient('D', Material.DROPPER);
            device(r, 'C', DeviceType.MVN_NETWORK_CIRCUIT);
        });
        shaped(plugin, "vacuum", create(DeviceType.MVN_VACUUM), r -> {
            r.shape("STS", "THT", "SCS");
            r.setIngredient('S', Material.STRING);
            device(r, 'T', DeviceType.MVN_TRANSFER_COIL);
            r.setIngredient('H', Material.HOPPER);
            device(r, 'C', DeviceType.MVN_NETWORK_CIRCUIT);
        });
        shaped(plugin, "purger", create(DeviceType.MVN_PURGER), r -> {
            r.shape("ILI", "LHL", "ICI");
            r.setIngredient('I', Material.IRON_INGOT);
            r.setIngredient('L', Material.MAGMA_BLOCK);
            r.setIngredient('H', Material.HOPPER);
            device(r, 'C', DeviceType.MVN_NETWORK_CIRCUIT);
        });
        shaped(plugin, "probe", create(DeviceType.MVN_PROBE), r -> {
            r.shape(" A ", "ASA", " C ");
            r.setIngredient('A', Material.AMETHYST_SHARD);
            r.setIngredient('S', Material.SPYGLASS);
            device(r, 'C', DeviceType.MVN_NETWORK_CIRCUIT);
        });
        shaped(plugin, "crafter", create(DeviceType.MVN_CRAFTER), r -> {
            r.shape("RCR", "IPI", "RCR");
            r.setIngredient('R', Material.REDSTONE);
            r.setIngredient('C', Material.CRAFTING_TABLE);
            r.setIngredient('I', Material.IRON_INGOT);
            device(r, 'P', DeviceType.MVN_LOGIC_PROCESSOR);
        });
        // El Crafter de Stock: un Auto-Crafter con comparadores que leen el stock de la red.
        shaped(plugin, "stock_crafter", create(DeviceType.MVN_STOCK_CRAFTER), r -> {
            r.shape("RKR", "KAK", "RLR");
            r.setIngredient('R', Material.REDSTONE);
            r.setIngredient('K', Material.COMPARATOR);
            device(r, 'A', DeviceType.MVN_CRAFTER);
            r.setIngredient('L', Material.LOOM);
        });
        shaped(plugin, "wireless_terminal", create(DeviceType.MVN_WIRELESS_TERMINAL), r -> {
            r.shape(" Q ", "PNP", " C ");
            device(r, 'Q', DeviceType.MVN_QUANTUM_CORE);
            r.setIngredient('P', Material.ENDER_PEARL);
            r.setIngredient('N', Material.NETHER_STAR);
            r.setIngredient('C', Material.COMPASS);
        });
        shaped(plugin, "monitor", create(DeviceType.MVN_MONITOR), r -> {
            r.shape("GGG", "KCK", "GGG");
            r.setIngredient('G', Material.GLASS_PANE);
            r.setIngredient('K', Material.COMPARATOR);
            device(r, 'C', DeviceType.MVN_NETWORK_CIRCUIT);
        });
        shaped(plugin, "transmitter", create(DeviceType.MVN_TRANSMITTER), r -> {
            r.shape("IRI", "RCR", "IQI");
            r.setIngredient('I', Material.IRON_INGOT);
            r.setIngredient('R', Material.REDSTONE_BLOCK);
            r.setIngredient('C', Material.CONDUIT);
            device(r, 'Q', DeviceType.MVN_QUANTUM_CORE);
        });
        shaped(plugin, "receiver", create(DeviceType.MVN_RECEIVER), r -> {
            r.shape("IPI", "PLP", "IQI");
            r.setIngredient('I', Material.IRON_INGOT);
            r.setIngredient('P', Material.ENDER_PEARL);
            r.setIngredient('L', Material.REDSTONE_LAMP);
            device(r, 'Q', DeviceType.MVN_QUANTUM_CORE);
        });
        shaped(plugin, "greedy_cell", create(DeviceType.MVN_GREEDY_CELL), r -> {
            r.shape("GHG", "HSH", "GMG");
            r.setIngredient('G', Material.GOLD_INGOT);
            r.setIngredient('H', Material.HOPPER);
            r.setIngredient('S', Material.SLIME_BLOCK);
            device(r, 'M', DeviceType.MVN_STORAGE_MATRIX);
        });
        shaped(plugin, "grabber_ht", create(DeviceType.MVN_GRABBER_HT), r -> {
            r.shape("TPT");
            device(r, 'T', DeviceType.MVN_TRANSFER_COIL);
            r.setIngredient('P', Material.STICKY_PISTON);
        });
        shaped(plugin, "pusher_ht", create(DeviceType.MVN_PUSHER_HT), r -> {
            r.shape("TPT");
            device(r, 'T', DeviceType.MVN_TRANSFER_COIL);
            r.setIngredient('P', Material.PISTON);
        });
        shaped(plugin, "encoder", create(DeviceType.MVN_ENCODER), r -> {
            r.shape("KPK", "CSC", "KPK");
            r.setIngredient('K', Material.INK_SAC);
            r.setIngredient('P', Material.PAPER);
            device(r, 'C', DeviceType.MVN_NETWORK_CIRCUIT);
            r.setIngredient('S', Material.SMITHING_TABLE);
        });
        if (Settings.sfEncoderEnabled()) {
            shaped(plugin, "sf_encoder", create(DeviceType.MVN_SF_ENCODER), r -> {
                r.shape("EPE", "CBC", "EPE");
                r.setIngredient('E', Material.ENDER_PEARL);
                r.setIngredient('P', Material.PAPER);
                device(r, 'C', DeviceType.MVN_NETWORK_CIRCUIT);
                r.setIngredient('B', Material.ENCHANTING_TABLE);
            });
        }
        shaped(plugin, "router", create(DeviceType.MVN_ROUTER), r -> {
            r.shape(" L ", " C ", " Q ");
            r.setIngredient('L', Material.LIGHTNING_ROD);
            device(r, 'C', DeviceType.MVN_CABLE);
            device(r, 'Q', DeviceType.MVN_QUANTUM_CORE);
        });
        shaped(plugin, "cache_l1", create(DeviceType.MVN_CACHE_L1), r -> {
            r.shape("CRC", "RMR", "CRC");
            r.setIngredient('C', Material.COPPER_INGOT);
            r.setIngredient('R', Material.REDSTONE);
            device(r, 'M', DeviceType.MVN_STORAGE_MATRIX);
        });
        shaped(plugin, "cache_l2", create(DeviceType.MVN_CACHE_L2), r -> {
            r.shape("GLG", "LPL", "GLG");
            r.setIngredient('G', Material.GOLD_INGOT);
            r.setIngredient('L', Material.LAPIS_LAZULI);
            device(r, 'P', DeviceType.MVN_CACHE_L1);
        });
        shaped(plugin, "cache_l3", create(DeviceType.MVN_CACHE_L3), r -> {
            r.shape("DAD", "APA", "DAD");
            r.setIngredient('D', Material.DIAMOND);
            r.setIngredient('A', Material.AMETHYST_SHARD);
            device(r, 'P', DeviceType.MVN_CACHE_L2);
        });
        shaped(plugin, "cache_dram", create(DeviceType.MVN_CACHE_DRAM), r -> {
            r.shape("NEN", "EPE", "NEN");
            r.setIngredient('N', Material.NETHERITE_INGOT);
            r.setIngredient('E', Material.ENDER_EYE);
            device(r, 'P', DeviceType.MVN_CACHE_L3);
        });
        shaped(plugin, "cache_quantum", create(DeviceType.MVN_CACHE_QUANTUM), r -> {
            r.shape("NSN", "SPS", "NSN");
            r.setIngredient('N', Material.NETHERITE_BLOCK);
            r.setIngredient('S', Material.NETHER_STAR);
            device(r, 'P', DeviceType.MVN_CACHE_DRAM);
        });
        shaped(plugin, "crafting_grid", create(DeviceType.MVN_CRAFTING_GRID), r -> {
            r.shape("CRC", "RGR", "CPC");
            r.setIngredient('C', Material.CRAFTING_TABLE);
            r.setIngredient('R', Material.REDSTONE);
            r.setIngredient('G', Material.CARTOGRAPHY_TABLE);
            device(r, 'P', DeviceType.MVN_LOGIC_PROCESSOR);
        });
        // Blueprint en blanco: el Encoder lo rellena con la receta de la matriz.
        shaped(plugin, "blueprint", stackOf(create(DeviceType.MVN_BLUEPRINT), 4), r -> {
            r.shape("PPP", "PWP", "PBP");
            r.setIngredient('P', Material.PAPER);
            device(r, 'W', DeviceType.MVN_SILICON_WAFER);
            r.setIngredient('B', Material.BLUE_DYE);
        });
        shaped(plugin, "configurator", create(DeviceType.MVN_CONFIGURATOR), r -> {
            r.shape("I I", " C ", " I ");
            r.setIngredient('I', Material.IRON_INGOT);
            device(r, 'C', DeviceType.MVN_NETWORK_CIRCUIT);
        });
        shaped(plugin, "rake", rake(), r -> {
            r.shape("D D", " S ", " S ");
            r.setIngredient('D', Material.DEAD_BUSH);
            r.setIngredient('S', Material.STICK);
        });
        shaped(plugin, "quantum_workbench", create(DeviceType.MVN_QUANTUM_WORKBENCH), r -> {
            r.shape("DQD", "DCD", "DDD");
            r.setIngredient('D', Material.DIAMOND);
            device(r, 'Q', DeviceType.MVN_QUANTUM_CORE);
            r.setIngredient('C', Material.CRAFTING_TABLE);
        });
        shaped(plugin, "infinity_barrel", create(DeviceType.MVN_INFINITY_BARREL), r -> {
            r.shape("NMN", "DBD", "NDN");
            r.setIngredient('N', Material.NETHERITE_INGOT);
            device(r, 'M', DeviceType.MVN_STORAGE_MATRIX);
            r.setIngredient('D', Material.DIAMOND_BLOCK);
            r.setIngredient('B', Material.BARREL);
        });
        shaped(plugin, "limiter", create(DeviceType.MVN_LIMITER), r -> {
            r.shape("RKR", "TCT", "RKR");
            r.setIngredient('R', Material.REDSTONE);
            r.setIngredient('K', Material.COMPARATOR);
            r.setIngredient('T', Material.TARGET);
            device(r, 'C', DeviceType.MVN_NETWORK_CIRCUIT);
        });
        shaped(plugin, "fluid_cell", create(DeviceType.MVN_FLUID_CELL), r -> {
            r.shape("GBG", "GMG", "GLG");
            r.setIngredient('G', Material.GLASS);
            r.setIngredient('B', Material.BUCKET);
            device(r, 'M', DeviceType.MVN_STORAGE_MATRIX);
            r.setIngredient('L', Material.LAPIS_BLOCK);
        });
        shaped(plugin, "liquid_pump", create(DeviceType.MVN_LIQUID_PUMP), r -> {
            r.shape(" G ", "PBP", " T ");
            r.setIngredient('G', Material.BLUE_STAINED_GLASS);
            r.setIngredient('P', Material.PISTON);
            r.setIngredient('B', Material.BUCKET);
            device(r, 'T', DeviceType.MVN_TRANSFER_COIL);
        });
        shaped(plugin, "request_terminal", create(DeviceType.MVN_REQUEST_TERMINAL), r -> {
            r.shape("GLG", "CPC", "GGG");
            r.setIngredient('G', Material.GLASS);
            r.setIngredient('L', Material.LECTERN);
            r.setIngredient('C', Material.CRAFTING_TABLE);
            device(r, 'P', DeviceType.MVN_LOGIC_PROCESSOR);
        });
        shaped(plugin, "request_crafter", create(DeviceType.MVN_REQUEST_CRAFTER), r -> {
            r.shape("RCR", "LPL", "RCR");
            r.setIngredient('R', Material.REDSTONE);
            r.setIngredient('C', Material.CRAFTING_TABLE);
            r.setIngredient('L', Material.LECTERN);
            device(r, 'P', DeviceType.MVN_LOGIC_PROCESSOR);
        });
        if (Settings.sfCrafterEnabled()) {
            shaped(plugin, "sf_crafter", create(DeviceType.MVN_SF_CRAFTER), r -> {
                r.shape("ECE", "IPI", "ECE");
                r.setIngredient('E', Material.ENDER_PEARL);
                r.setIngredient('C', Material.CRYING_OBSIDIAN);
                r.setIngredient('I', Material.IRON_INGOT);
                device(r, 'P', DeviceType.MVN_LOGIC_PROCESSOR);
            });
            shaped(plugin, "sf_stock_crafter", create(DeviceType.MVN_SF_STOCK_CRAFTER), r -> {
                r.shape("EKE", "KAK", "EBE");
                r.setIngredient('E', Material.ENDER_PEARL);
                r.setIngredient('K', Material.COMPARATOR);
                device(r, 'A', DeviceType.MVN_SF_CRAFTER);
                r.setIngredient('B', Material.AMETHYST_BLOCK);
            });
            shaped(plugin, "sf_request_crafter", create(DeviceType.MVN_SF_REQUEST_CRAFTER), r -> {
                r.shape("ECE", "LPL", "ECE");
                r.setIngredient('E', Material.ENDER_PEARL);
                r.setIngredient('C', Material.PURPUR_PILLAR);
                r.setIngredient('L', Material.LECTERN);
                device(r, 'P', DeviceType.MVN_LOGIC_PROCESSOR);
            });
        }
        // El bloque que aloja los modulos de memoria: hierro, cuarzo y redstone alrededor de un
        // bloque de cobre (el material del propio bay).
        shaped(plugin, "dram_bay", create(DeviceType.MVN_DRAM_BAY), r -> {
            r.shape("IMI", "RCR", "IQI");
            r.setIngredient('I', Material.IRON_INGOT);
            device(r, 'M', DeviceType.MVN_STORAGE_MATRIX);
            r.setIngredient('R', Material.REDSTONE);
            r.setIngredient('C', Material.COPPER_BLOCK);
            r.setIngredient('Q', Material.QUARTZ);
        });
        // El panel de cuotas: un Network Monitor y un Quota Limiter, con papel y comparadores.
        shaped(plugin, "quota_manager", create(DeviceType.MVN_QUOTA_MANAGER), r -> {
            r.shape("RKR", "PMP", "RLR");
            r.setIngredient('R', Material.REDSTONE);
            r.setIngredient('K', Material.COMPARATOR);
            r.setIngredient('P', Material.PAPER);
            device(r, 'M', DeviceType.MVN_MONITOR);
            device(r, 'L', DeviceType.MVN_LIMITER);
        });
        // El conector de contenedores: dos cofres, la bobina que mueve los items y el circuito de red.
        shaped(plugin, "container_connector", create(DeviceType.MVN_CONTAINER_CONNECTOR), r -> {
            r.shape("INI", "CTC", "INI");
            r.setIngredient('I', Material.IRON_INGOT);
            device(r, 'N', DeviceType.MVN_NETWORK_CIRCUIT);
            r.setIngredient('C', Material.CHEST);
            device(r, 'T', DeviceType.MVN_TRANSFER_COIL);
        });
        // Descargador y cargador: un DRAM Bay mas lo que mueve los items (tolva para sacar, soltador y
        // comparador para la cantidad al cargar).
        shaped(plugin, "module_unloader", create(DeviceType.MVN_MODULE_UNLOADER), r -> {
            r.shape("IHI", "RBR", "ICI");
            r.setIngredient('I', Material.IRON_INGOT);
            r.setIngredient('H', Material.HOPPER);
            r.setIngredient('R', Material.REDSTONE);
            device(r, 'B', DeviceType.MVN_DRAM_BAY);
            device(r, 'C', DeviceType.MVN_CABLE);
        });
        shaped(plugin, "module_loader", create(DeviceType.MVN_MODULE_LOADER), r -> {
            r.shape("IDI", "KBK", "IRI");
            r.setIngredient('I', Material.IRON_INGOT);
            r.setIngredient('D', Material.DROPPER);
            r.setIngredient('K', Material.COMPARATOR);
            r.setIngredient('R', Material.REDSTONE_BLOCK);
            device(r, 'B', DeviceType.MVN_DRAM_BAY);
        });
        // Dos celdas de fluidos vacias (64 cubos cada una) mas diamantes y cubos dan un modulo de
        // 512 cubos: compensa frente a 8 celdas, pero no es gratis.
        shaped(plugin, "fluid_dram", create(DeviceType.MVN_FLUID_DRAM), r -> {
            r.shape("DBD", "FEF", "DBD");
            r.setIngredient('D', Material.DIAMOND);
            r.setIngredient('B', Material.BUCKET);
            r.setIngredient('E', Material.ENDER_EYE);
            device(r, 'F', DeviceType.MVN_FLUID_CELL);
        });
        shaped(plugin, "chicken_sorter", create(DeviceType.MVN_CHICKEN_SORTER), r -> {
            r.shape("FEF", "CPC", "FEF");
            r.setIngredient('F', Material.FEATHER);
            r.setIngredient('E', Material.EGG);
            r.setIngredient('C', Material.COMPARATOR);
            device(r, 'P', DeviceType.MVN_PUSHER_HT);
        });
        // Una sonda (la que comprueba la conexion), un cable, los dos tintes de los dos estados,
        // redstone y glowstone para la luz. Sin cristal suelto: el cable tambien es GLASS.
        shaped(plugin, "status_light", create(DeviceType.MVN_STATUS_LIGHT), r -> {
            r.shape("RGR", "LPD", "RCR");
            r.setIngredient('R', Material.REDSTONE);
            r.setIngredient('G', Material.GLOWSTONE);
            r.setIngredient('L', Material.LIME_DYE);
            r.setIngredient('D', Material.RED_DYE);
            device(r, 'P', DeviceType.MVN_PROBE);
            device(r, 'C', DeviceType.MVN_CABLE);
        });
        // Al final y no al principio: una resolucion hecha a mitad del registro tambien se olvida.
        com.chagui68.multiversenets.craft.Blueprints.clearRecipeCache();
        com.chagui68.multiversenets.craft.CraftingSupport.clearResultCache();
        plugin.getLogger().info("Registered " + recipeCount() + " crafting recipes with Bukkit.");
    }

    private static final List<NamespacedKey> RECIPE_KEYS = new ArrayList<>();

    /**
     * EN: MultiverseNets devices a recipe takes as ingredients, with how many of each. The
     * ingredient is registered as its plain material so the server always matches the recipe
     * (a cell that carries cargo, or a module whose lore changed, no longer matched an exact-item
     * ingredient), and {@code CraftingListener} then checks that every such slot really holds
     * that device.
     *
     * ES: Dispositivos de MultiverseNets que una receta usa como ingredientes, con cuántos de cada
     * uno. El ingrediente se registra como su material simple para que el servidor siempre case la
     * receta (una celda con carga, o un módulo cuyo lore cambió, ya no casaba con un ingrediente de
     * ítem exacto), y {@code CraftingListener} comprueba que cada ranura tenga de verdad ese
     * dispositivo.
     */
    private static final java.util.Map<NamespacedKey, java.util.Map<DeviceType, Integer>> DEVICE_INGREDIENTS =
            new java.util.HashMap<>();
    private static final java.util.Map<Character, DeviceType> PENDING_DEVICES = new java.util.HashMap<>();
    /** Which grid character of a recipe is a device, for the in-game guide. */
    private static final java.util.Map<NamespacedKey, java.util.Map<Character, DeviceType>> DEVICE_CHARS =
            new java.util.HashMap<>();
    /**
     * The recipes as this plugin built them. Paper's {@code Bukkit.getRecipe} rebuilds them with new grid
     * characters and a trimmed shape, so {@link #DEVICE_CHARS} only matches these originals.
     */
    private static final java.util.Map<NamespacedKey, ShapedRecipe> ORIGINAL_RECIPES = new java.util.HashMap<>();
    /** Recipes that upgrade a cell or a memory module: the ingredient's stored cargo moves to the result. */
    private static final java.util.Set<String> UPGRADE_RECIPES = java.util.Set.of(
            "cell_t2", "cell_t3", "cell_t4", "cell_t5", "cell_t6",
            "cache_l2", "cache_l3", "cache_dram", "cache_quantum");

    private static void device(ShapedRecipe recipe, char key, DeviceType type) {
        recipe.setIngredient(key, new org.bukkit.inventory.RecipeChoice.MaterialChoice(type.material()));
        PENDING_DEVICES.put(key, type);
    }

    /** Devices (and how many) a recipe of this plugin needs; empty when it takes none. */
    public static java.util.Map<DeviceType, Integer> deviceIngredients(NamespacedKey key) {
        synchronized (RECIPE_KEYS) {
            return DEVICE_INGREDIENTS.getOrDefault(key, java.util.Map.of());
        }
    }

    /** The recipe as this plugin built it (grid characters match {@link #deviceIngredientAt}), or null. */
    public static ShapedRecipe originalRecipe(NamespacedKey key) {
        synchronized (RECIPE_KEYS) {
            return ORIGINAL_RECIPES.get(key);
        }
    }

    /** The device a recipe's grid character stands for, or null when it is a plain material. */
    public static DeviceType deviceIngredientAt(NamespacedKey key, char c) {
        synchronized (RECIPE_KEYS) {
            java.util.Map<Character, DeviceType> chars = DEVICE_CHARS.get(key);
            return chars == null ? null : chars.get(c);
        }
    }

    /** True for the cell and memory-module tier upgrades, which keep the ingredient's cargo. */
    public static boolean isUpgradeRecipe(NamespacedKey key) {
        return key != null && UPGRADE_RECIPES.contains(key.getKey());
    }

    /**
     * @return Total count of registered recipe keys.
     */
    public static int recipeCount() {
        synchronized (RECIPE_KEYS) {
            return RECIPE_KEYS.size();
        }
    }

    /**
     * @return Unmodifiable snapshot of registered recipe keys.
     */
    public static List<NamespacedKey> recipeKeys() {
        synchronized (RECIPE_KEYS) {
            return Collections.unmodifiableList(new ArrayList<>(RECIPE_KEYS));
        }
    }

    /**
     * Unlocks all MultiverseNets recipes in the player's recipe book.
     */
    public static void discoverRecipes(Player player) {
        if (player == null) {
            return;
        }
        List<NamespacedKey> keys = recipeKeys();
        for (NamespacedKey key : keys) {
            try {
                if (!player.hasDiscoveredRecipe(key)) {
                    player.discoverRecipe(key);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private interface RecipeDef {
        void define(ShapedRecipe recipe);
    }

    private static ShapedRecipe shaped(MultiverseNets plugin, String key, ItemStack result, RecipeDef def) {
        NamespacedKey nk = new NamespacedKey(plugin, key);
        try {
            Bukkit.removeRecipe(nk);
        } catch (Throwable ignored) {
        }
        ShapedRecipe recipe = new ShapedRecipe(nk, result);
        java.util.Map<DeviceType, Integer> devices = new java.util.EnumMap<>(DeviceType.class);
        synchronized (RECIPE_KEYS) {
            PENDING_DEVICES.clear();
            def.define(recipe);
            for (String row : recipe.getShape()) {
                for (char c : row.toCharArray()) {
                    DeviceType type = PENDING_DEVICES.get(c);
                    if (type != null) {
                        devices.merge(type, 1, Integer::sum);
                    }
                }
            }
            if (devices.isEmpty()) {
                DEVICE_INGREDIENTS.remove(nk);
                DEVICE_CHARS.remove(nk);
            } else {
                DEVICE_INGREDIENTS.put(nk, devices);
                DEVICE_CHARS.put(nk, new java.util.HashMap<>(PENDING_DEVICES));
            }
            PENDING_DEVICES.clear();
            ORIGINAL_RECIPES.put(nk, recipe);
        }
        try {
            boolean ok = Bukkit.addRecipe(recipe);
            if (!ok) {
                plugin.getLogger().warning("Bukkit.addRecipe returned false for: " + key);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to register recipe " + key + ": " + t.getMessage());
        }
        synchronized (RECIPE_KEYS) {
            if (!RECIPE_KEYS.contains(nk)) {
                RECIPE_KEYS.add(nk);
            }
        }
        return recipe;
    }

    private static ItemStack stackOf(ItemStack item, int amount) {
        item.setAmount(amount);
        return item;
    }
}

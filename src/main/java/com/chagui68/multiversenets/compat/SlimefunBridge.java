package com.chagui68.multiversenets.compat;

import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.util.StackUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * [EN] Slimefun Compatibility Bridge
 * Allows MultiverseNets to interact with Slimefun machines without compile-time dependencies.
 *
 * Why it's needed: Grabbers and Pushers normally look for an {@code InventoryHolder}
 * in adjacent blocks. Slimefun machines do not store items in {@code BlockState}, but rather
 * in a separate {@code BlockMenu} in Slimefun's registry. Without this bridge, MultiverseNets
 * would treat Slimefun machines as plain decorative blocks.
 *
 * Why via reflection: The plugin remains completely standalone. If Slimefun is absent,
 * the bridge stays dormant and vanilla container operations continue normally.
 *
 * Multi-package support: Supports both legacy upstream packages ({@code io.github.thebusybiscuit})
 * and relocated forks ({@code com.github.drakescraft_labs}).
 *
 * Slot safety: Only accesses slots declared in {@code getSlotsAccessedByItemTransport} to respect
 * machine input/output slot design.
 *
 * [ES] Puente de Compatibilidad con Slimefun
 * Permite que MultiverseNets interactúe con máquinas de Slimefun sin dependencias en tiempo de compilación.
 *
 * Por qué es necesario: Grabbers y Pushers buscan un {@code InventoryHolder} en bloques vecinos.
 * Las máquinas de Slimefun guardan su inventario en un {@code BlockMenu} separado. Este puente permite
 * transferir ítems respetando los slots de entrada/salida de cada máquina.
 *
 * Por reflexión: Mantiene el plugin autónomo (standalone). Si Slimefun no está instalado, queda inerte.
 */
public final class SlimefunBridge {

    /**
     * Prefijos completos del paquete de la API legacy. El jar vivo de Slimefun-Drake en DrakesCraft
     * (1.2.DEV v11.0-Drake-1.21.11) expone {@code ...slimefun4.legacy.api.BlockStorage}, sin el
     * segmento {@code .Slimefun}; el arbol de fuentes actual del fork lo tiene con el. Se prueban
     * ambos y el upstream original, y el primero que resuelva gana.
     */
    private static final String[] API_ROOTS = {
            "com.github.drakescraft_labs.slimefun4.legacy.api",
            "com.github.drakescraft_labs.slimefun4.legacy.Slimefun.api",
            "io.github.thebusybiscuit.slimefun4.legacy.api",
            "io.github.thebusybiscuit.slimefun4.legacy.Slimefun.api",
            "me.mrCookieSlime.Slimefun.api",
    };

    private static boolean available;
    private static Method mGetInventory;
    private static Method mCheckId;
    private static Method mGetPreset;
    private static Method mSlotsForTransport;
    private static Method mSlotsForTransportSimple;
    private static Method mGetItemInSlot;
    private static Method mPushItem;
    private static Method mReplaceExistingItem;
    private static Object flowInsert;
    private static Object flowWithdraw;
    private static Method mGetLocationInfo;
    private static Method mAddBlockInfo;
    private static Method mCheckItem;
    private static Method mGetByItem;
    private static final int BARREL_DISPLAY_SLOT = 31;
    private static final NamespacedKey SLIMEFUN_ITEM_ID = new NamespacedKey("slimefun", "slimefun_item");

    // Advanced BlockMenu reflection hooks for Quantum Cell coexistence
    private static Method mGetStorage;
    private static Method mClearBlockInfo;
    private static Field fInventories;
    private static Class<?> cBlockMenu;
    private static Class<?> cBlockMenuPreset;
    private static Constructor<?> ctorBlockMenu;

    public record SlimefunRecipeDetails(ItemStack[] inputs, ItemStack output, String sfId, String recipeType) {}
    private static final List<SlimefunRecipeDetails> SF_RECIPES = new ArrayList<>();
    private static boolean recipesLoaded = false;

    private SlimefunBridge() {
    }

    /**
     * EN: Initializes the Slimefun reflection hooks. If Slimefun is missing, it stays dormant.
     *
     * ES: Inicializa los enlaces por reflexión con Slimefun. Si Slimefun no está instalado, queda inerte.
     *
     * @param log Logger instance for diagnostic notices / ES: Instancia del logger para mensajes.
     */
    public static void registerSerializationAliases() {
        try {
            org.bukkit.configuration.serialization.ConfigurationSerialization.registerClass(
                    ItemStack.class, "com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack");
            org.bukkit.configuration.serialization.ConfigurationSerialization.registerClass(
                    ItemStack.class, "io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack");
            org.bukkit.configuration.serialization.ConfigurationSerialization.registerClass(
                    ItemStack.class, "me.mrCookieSlime.Slimefun.api.SlimefunItemStack");
        } catch (Throwable ignored) {
        }
    }

    public static void init(Logger log) {
        registerSerializationAliases();
        if (!com.chagui68.multiversenets.util.Settings.compatSlimefun()) {
            log.info("[Compat] Slimefun integration disabled in config (compat.slimefun).");
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("Slimefun") == null) {
            log.info("[Compat] Slimefun is not installed; the network will work only with vanilla containers.");
            return;
        }
        for (String root : API_ROOTS) {
            if (tryHook(root)) {
                available = true;
                loadSlimefunRecipes();
                log.info("[Compat] Slimefun detected (" + root + "). Grabbers, pushers, and crafters can use machines.");
                return;
            }
        }
        log.warning("[Compat] Slimefun is installed but its API does not match any known package root. Integration disabled.");
    }

    private static boolean tryHook(String root) {
        try {
            Class<?> blockStorage = Class.forName(root + ".BlockStorage");
            Class<?> dirtyMenu = Class.forName(root + ".inventory.DirtyChestMenu");
            Class<?> preset = Class.forName(root + ".inventory.BlockMenuPreset");
            Class<?> flow = Class.forName(root + ".item_transport.ItemTransportFlow");

            mGetInventory = blockStorage.getMethod("getInventory", Block.class);
            mCheckId = blockStorage.getMethod("checkID", Block.class);
            mGetPreset = dirtyMenu.getMethod("getPreset");
            mSlotsForTransport = preset.getMethod(
                    "getSlotsAccessedByItemTransport", dirtyMenu, flow, ItemStack.class);
            try {
                mSlotsForTransportSimple = preset.getMethod("getSlotsAccessedByItemTransport", flow);
            } catch (NoSuchMethodException ignored) {}
            mGetItemInSlot = dirtyMenu.getMethod("getItemInSlot", int.class);
            mPushItem = dirtyMenu.getMethod("pushItem", ItemStack.class, int[].class);
            mReplaceExistingItem = dirtyMenu.getMethod("replaceExistingItem", int.class, ItemStack.class);

            try {
                mGetStorage = blockStorage.getMethod("getStorage", org.bukkit.World.class);
            } catch (NoSuchMethodException ignored) {}
            try {
                mClearBlockInfo = blockStorage.getMethod("clearBlockInfo", org.bukkit.Location.class);
            } catch (NoSuchMethodException ignored) {}
            try {
                fInventories = blockStorage.getDeclaredField("inventories");
                fInventories.setAccessible(true);
            } catch (Exception ignored) {}
            try {
                cBlockMenu = Class.forName(root + ".inventory.BlockMenu");
                cBlockMenuPreset = Class.forName(root + ".inventory.BlockMenuPreset");
                ctorBlockMenu = cBlockMenu.getConstructor(cBlockMenuPreset, org.bukkit.Location.class);
            } catch (Exception ignored) {}
            try {
                for (String prefix : new String[]{"com.github.drakescraft_labs.slimefun4", "io.github.thebusybiscuit.slimefun4", "me.mrCookieSlime.Slimefun"}) {
                    try {
                        Class<?> itemClass = Class.forName(prefix + ".api.items.SlimefunItem");
                        mGetByItem = itemClass.getMethod("getByItem", ItemStack.class);
                        break;
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}

            Object[] values = flow.getEnumConstants();
            for (Object val : values) {
                String name = ((Enum<?>) val).name();
                if ("INSERT".equals(name)) flowInsert = val;
                if ("WITHDRAW".equals(name)) flowWithdraw = val;
            }
            return flowInsert != null && flowWithdraw != null;
        } catch (ReflectiveOperationException | RuntimeException error) {
            return false;
        }
    }

    /**
     * EN: Returns true if Slimefun is present and its reflection API was successfully resolved.
 *
     * ES: Devuelve true si Slimefun está presente y su API se resolvió por reflexión.
     */
    public static boolean isAvailable() {
        return available;
    }

    /**
     * EN: Checks if a block is an active Slimefun machine with a custom menu.
 *
     * ES: Comprueba si un bloque es una máquina activa de Slimefun con menú propio.
     */
    public static boolean isMachine(Block block) {
        return menuOf(block) != null;
    }

    /**
     * EN: Returns the Slimefun item ID for a given block, or null if not a Slimefun block.
 *
     * ES: Obtiene el ID de Slimefun de un bloque, o null si no es de Slimefun.
     */
    public static String getId(Block block) {
        if (!available || block == null) return null;
        try {
            Object id = mCheckId.invoke(null, block);
            return id == null ? null : id.toString();
        } catch (ReflectiveOperationException | RuntimeException error) {
            return null;
        }
    }

    /**
     * EN: Returns the Slimefun ID from an ItemStack's PersistentDataContainer, or null if vanilla.
 *
     * ES: Obtiene el ID de Slimefun del PersistentDataContainer de un ItemStack, o null si es vanilla.
     */
    public static String getId(ItemStack item) {
        if (item == null) {
            return null;
        }
        if (!item.getType().isAir()) {
            // Read-only PDC view: no ItemMeta clone on the network hot path (#87).
            var pdc = item.getPersistentDataContainer();
            String id = pdc.get(SLIMEFUN_ITEM_ID, org.bukkit.persistence.PersistentDataType.STRING);
            if (id != null && !id.isBlank()) {
                return id;
            }
            // Retain compatibility with historical forks that used a different namespace.
            for (org.bukkit.NamespacedKey key : pdc.getKeys()) {
                if ("slimefun_item".equalsIgnoreCase(key.getKey())) {
                    String legacyId = pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
                    if (legacyId != null && !legacyId.isBlank()) {
                        return legacyId;
                    }
                }
            }
        }
        // Sin ItemMeta no hay id de Slimefun posible: ni PDC ni SlimefunItemStack. Esto ahorra la
        // llamada reflectiva a getByItem por cada item vanilla que pasa por un filtro.
        if (mGetByItem != null && item.hasItemMeta()) {
            try {
                Object sfItem = mGetByItem.invoke(null, item);
                if (sfItem != null) {
                    Method mId = ID_GETTERS.computeIfAbsent(sfItem.getClass(), SlimefunBridge::idGetter);
                    Object id = mId == null ? null : mId.invoke(sfItem);
                    if (id != null) return id.toString();
                }
            } catch (Throwable ignored) {}
        }
        if (item.getClass().getSimpleName().equals("SlimefunItemStack")) {
            try {
                Method mId = item.getClass().getMethod("getItemId");
                Object id = mId.invoke(item);
                if (id != null) return id.toString();
            } catch (Throwable ignored) {}
        }
        return null;
    }

    /**
     * EN: Returns true if the ItemStack is a registered Slimefun item.
 *
     * ES: Devuelve true si el ItemStack es un ítem registrado de Slimefun.
     */
    public static boolean isSlimefunItem(ItemStack item) {
        return getId(item) != null;
    }

    /** getId() per SlimefunItem class, looked up once / getId() por clase, resuelto una vez. */
    private static final java.util.Map<Class<?>, Method> ID_GETTERS = new java.util.concurrent.ConcurrentHashMap<>();

    private static Method idGetter(Class<?> type) {
        try {
            return type.getMethod("getId");
        } catch (NoSuchMethodException missing) {
            return null;
        }
    }

    private static Object menuOf(Block block) {
        if (!available || block == null) return null;
        try {
            return mGetInventory.invoke(null, block);
        } catch (ReflectiveOperationException | RuntimeException error) {
            return null;
        }
    }

    private static int[] getTransportSlots(Object menu, Object flow, ItemStack reference) {
        try {
            Object preset = mGetPreset.invoke(menu);
            if (preset == null) return new int[0];
            Object result = mSlotsForTransport.invoke(preset, menu, flow, reference);
            return result instanceof int[] array ? array : new int[0];
        } catch (ReflectiveOperationException | RuntimeException error) {
            return new int[0];
        }
    }

    /**
     * EN: Extracts up to {@code max} items matching {@code filter} from the machine's output slots.
     * Every output slot holding an equivalent stack is drained into the result, so the per-cycle
     * quota is fully honoured. Slots holding a different item are left untouched.
     *
     * ES: Extrae hasta {@code max} ítems que cumplan {@code filter} de los huecos de salida de la máquina.
     * Vacia en el resultado todos los huecos de salida que tengan un stack equivalente, de modo que la
     * cuota por ciclo se respeta por completo. Los huecos con otro ítem no se tocan.
     *
     * @param block  The block containing the Slimefun machine / ES: Bloque de la máquina.
     * @param filter Predicate filtering allowed items / ES: Predicado que filtra los ítems válidos.
     * @param max    Maximum amount to extract / ES: Cantidad máxima a extraer.
     * @return Extracted ItemStack, or null if none found / ES: ItemStack extraído o null si no había nada.
     */
    public static ItemStack extract(Block block, Predicate<ItemStack> filter, int max) {
        Object menu = menuOf(block);
        if (menu == null || max <= 0) return null;
        try {
            ItemStack result = null;
            int got = 0;
            for (int slot : getTransportSlots(menu, flowWithdraw, null)) {
                if (got >= max) break;
                Object raw = mGetItemInSlot.invoke(menu, slot);
                if (!(raw instanceof ItemStack current) || current.getType().isAir()) continue;
                if (filter != null && !filter.test(current)) continue;
                if (result == null) {
                    result = current.clone();
                } else if (!StackUtils.itemsMatch(result, current)) {
                    continue;
                }

                int amount = Math.min(max - got, current.getAmount());
                int remaining = current.getAmount() - amount;
                ItemStack remainingStack = null;
                if (remaining > 0) {
                    remainingStack = current.clone();
                    remainingStack.setAmount(remaining);
                }
                mReplaceExistingItem.invoke(menu, slot, remainingStack);
                got += amount;
            }
            if (result == null || got <= 0) {
                return null;
            }
            result.setAmount(got);
            return result;
        } catch (ReflectiveOperationException | RuntimeException error) {
            logError(block, error);
        }
        return null;
    }

    /**
     * EN: Inserts an ItemStack into the machine's valid input slots.
 *
     * ES: Inserta un ItemStack en los huecos de entrada declarados por la máquina.
     *
     * @param block The block containing the Slimefun machine / ES: Bloque de la máquina.
     * @param stack The items to insert / ES: Ítems a insertar.
     * @return Number of items that did not fit (0 if all inserted) / ES: Cantidad que no cupo (0 si entró todo).
     */
    /**
     * EN: Returns true if the block is a Slimefun Networks bridge or cable.
     * ES: Devuelve true si el bloque es un puente o cable del addon Networks de Slimefun.
     */
    public static boolean isNetworkCable(Block block) {
        if (!available || block == null) return false;
        return isNetworkCableId(getId(block));
    }

    /** {@link #isNetworkCable} for an id the caller already has / Con un id ya leído. */
    public static boolean isNetworkCableId(String id) {
        if (id == null) return false;
        String upper = id.toUpperCase(java.util.Locale.ROOT);
        return upper.contains("BRIDGE") || upper.contains("CABLE") || upper.equals("NTW_BRIDGE");
    }

    /**
     * EN: Returns true if the block is a Slimefun Barrel or storage unit.
     * ES: Devuelve true si el bloque es un barril de Slimefun o unidad de almacenamiento.
     */
    public static boolean isBarrel(Block block) {
        return storedField(block) != null;
    }

    /** {@link #isBarrel(Block)} for an id the caller already has / Con un id ya leído. */
    public static boolean isBarrel(Block block, String id) {
        return storedField(block, id) != null;
    }

    /**
     * The barrel's raw "stored" field, or null when the block is not a barrel. One reflective read
     * answers both "is it a barrel?" and "how much is in it?"; before, every amount read re-ran
     * the whole barrel check and then read the field a second time.
     */
    private static Object storedField(Block block) {
        if (!available || block == null || mGetLocationInfo == null) return null;
        try {
            Object stored = mGetLocationInfo.invoke(null, block.getLocation(), "stored");
            if (stored == null) return null;
            return isBarrelId(block, getId(block)) ? stored : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Object storedField(Block block, String id) {
        if (!available || block == null || mGetLocationInfo == null) return null;
        try {
            Object stored = mGetLocationInfo.invoke(null, block.getLocation(), "stored");
            return stored != null && isBarrelId(block, id) ? stored : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static boolean isBarrelId(Block block, String id) {
        if (id == null) return false;
        String upper = id.toUpperCase(java.util.Locale.ROOT);
        return upper.contains("BARREL") || upper.contains("STORAGE") || isMachine(block);
    }

    /**
     * EN: Gets the total stored item count from a Slimefun Barrel.
     * ES: Obtiene la cantidad total almacenada en un barril de Slimefun.
     */
    public static long getBarrelStoredAmount(Block block) {
        Object raw = storedField(block);
        return raw == null ? 0 : parseStored(raw);
    }

    private static long parseStored(Object raw) {
        try {
            return Math.max(0, Long.parseLong(raw.toString().trim()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    /**
     * EN: Gets the template item stored in a Slimefun Barrel (slot 31).
     * ES: Obtiene la muestra de ítem almacenada en un barril de Slimefun (slot 31).
     */
    public static ItemStack getBarrelStoredItem(Block block) {
        if (!isBarrel(block)) return null;
        return barrelSample(block);
    }

    /** {@link #getBarrelStoredItem} for a block already known to be a barrel / Sin repetir la comprobación. */
    private static ItemStack barrelSample(Block block) {
        Object menu = menuOf(block);
        if (menu == null) return null;
        try {
            Object raw = mGetItemInSlot.invoke(menu, BARREL_DISPLAY_SLOT);
            if (raw instanceof ItemStack item && !item.getType().isAir() && item.getType() != Material.BARRIER) {
                ItemStack clone = item.clone();
                clone.setAmount(1);
                // Clean unclickable / fluffy PDC tags from sample clone. The read-only PDC view says
                // whether there is anything to clean; the ItemMeta copy is only paid when there is.
                if (clone.hasItemMeta() && hasUnclickableKey(clone)) {
                    var meta = clone.getItemMeta();
                    var pdc = meta.getPersistentDataContainer();
                    for (var key : pdc.getKeys()) {
                        if ("unclickable".equalsIgnoreCase(key.getKey())) {
                            pdc.remove(key);
                        }
                    }
                    clone.setItemMeta(meta);
                }
                return clone;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return null;
    }

    private static boolean hasUnclickableKey(ItemStack item) {
        for (NamespacedKey key : item.getPersistentDataContainer().getKeys()) {
            if ("unclickable".equalsIgnoreCase(key.getKey())) {
                return true;
            }
        }
        return false;
    }

    /**
     * EN: Gets capacity of a Slimefun Barrel.
     * ES: Obtiene la capacidad máxima de un barril de Slimefun.
     */
    public static long getBarrelCapacity(Block block) {
        if (!isBarrel(block)) return 0;
        return barrelCapacity(block);
    }

    /** Capacity of a block already known to be a barrel / Capacidad de un bloque que ya es barril. */
    private static long barrelCapacity(Block block) {
        try {
            if (mCheckItem != null) {
                Object sfItem = mCheckItem.invoke(null, block);
                if (sfItem != null) {
                    Method mCap = optionalMethod(sfItem.getClass(), "getCapacity", Block.class);
                    if (mCap != null && mCap.invoke(sfItem, block) instanceof Number num) {
                        return num.longValue();
                    }
                    Method mMaxCap = optionalMethod(sfItem.getClass(), "getMaxCapacity");
                    if (mMaxCap != null && mMaxCap.invoke(sfItem) instanceof Number num) {
                        return num.longValue();
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return 1_000_000L;
    }

    private static final Method NO_METHOD;

    static {
        try {
            NO_METHOD = Object.class.getMethod("hashCode");
        } catch (NoSuchMethodException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    /**
     * Method lookups by class, cached including the misses. getMethod() on every barrel deposit
     * (and a NoSuchMethodException for each miss) was pure reflection overhead.
     */
    private static final java.util.Map<String, Method> METHODS = new java.util.concurrent.ConcurrentHashMap<>();

    private static Method optionalMethod(Class<?> type, String name, Class<?>... params) {
        String key = type.getName() + '#' + name + java.util.Arrays.toString(params);
        Method found = METHODS.computeIfAbsent(key, ignored -> {
            try {
                return type.getMethod(name, params);
            } catch (NoSuchMethodException missing) {
                return NO_METHOD;
            }
        });
        return found == NO_METHOD ? null : found;
    }

    /**
     * EN: Deposits items directly into a Slimefun Barrel.
     * ES: Deposita ítems directamente en un barril de Slimefun.
     * @return Number of items that did not fit (0 if all deposited).
     */
    public static int depositBarrel(Block block, ItemStack stack) {
        if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
            return stack == null ? 0 : stack.getAmount();
        }
        // Una sola comprobacion de barril; antes isBarrel, la cantidad y la capacidad la repetian.
        Object raw = storedField(block);
        if (raw == null) {
            return stack.getAmount();
        }
        long stored = parseStored(raw);
        long capacity = barrelCapacity(block);
        long space = capacity - stored;
        if (space <= 0) {
            return stack.getAmount();
        }

        ItemStack currentSample = barrelSample(block);
        if (stored == 0 || currentSample == null) {
            // Adopt new item type
            long take = Math.min(space, stack.getAmount());
            setBarrelState(block, stack, take, capacity);
            return (int) (stack.getAmount() - take);
        } else if (com.chagui68.multiversenets.util.StackUtils.itemsMatch(currentSample, stack)) {
            long take = Math.min(space, stack.getAmount());
            setBarrelState(block, currentSample, stored + take, capacity);
            return (int) (stack.getAmount() - take);
        }
        return stack.getAmount();
    }

    /**
     * EN: Extracts up to {@code want} items matching {@code matcher} from a Slimefun Barrel.
     * ES: Extrae hasta {@code want} ítems de un barril de Slimefun.
     */
    public static ItemStack withdrawBarrel(Block block, Predicate<ItemStack> matcher, int want) {
        if (want <= 0) return null;
        Object raw = storedField(block);
        if (raw == null) return null;
        long stored = parseStored(raw);
        if (stored <= 0) return null;
        ItemStack sample = barrelSample(block);
        if (sample == null || (matcher != null && !matcher.test(sample))) return null;

        long take = Math.min(want, stored);
        long remaining = stored - take;
        long capacity = barrelCapacity(block);
        setBarrelState(block, remaining > 0 ? sample : null, remaining, capacity);

        ItemStack out = sample.clone();
        out.setAmount((int) take);
        return out;
    }

    private static void setBarrelState(Block block, ItemStack sample, long newAmount, long capacity) {
        try {
            if (mAddBlockInfo != null) {
                mAddBlockInfo.invoke(null, block.getLocation(), "stored", String.valueOf(newAmount));
            }
            Object menu = menuOf(block);
            if (menu != null) {
                if (newAmount > 0 && sample != null) {
                    ItemStack display = sample.clone();
                    display.setAmount(1);
                    mReplaceExistingItem.invoke(menu, BARREL_DISPLAY_SLOT, display);
                } else {
                    mReplaceExistingItem.invoke(menu, BARREL_DISPLAY_SLOT, new ItemStack(Material.BARRIER));
                }
                // Try to trigger menu update
                if (mCheckItem != null) {
                    Object sfItem = mCheckItem.invoke(null, block);
                    if (sfItem != null) {
                        Method upd = optionalMethod(sfItem.getClass(), "updateMenu", Block.class, menu.getClass(), boolean.class, int.class);
                        if (upd != null) {
                            upd.invoke(sfItem, block, menu, false, (int) Math.min(capacity, Integer.MAX_VALUE));
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    public static int insert(Block block, ItemStack stack) {
        return insert(block, stack, 0);
    }

    /**
     * EN: Inserts into the machine's input slots, one stack per slot at most. Slimefun's own
     * {@code pushItem} drops the whole stack into the first empty slot, so a Pusher's 128 or 1,024
     * units ended up as one over-sized slot and everything above a stack was lost. With
     * {@code kinds} &gt; 1 (a whitelist of several ingredients), one item type never takes more
     * than its share of the input slots, so the other ingredients still fit. See
     * {@link #fillInputSlots} for the one-new-slot-per-insert rule.
     *
     * ES: Inserta en las ranuras de entrada de la máquina, como mucho un stack por ranura. El
     * {@code pushItem} de Slimefun mete el stack entero en la primera ranura vacía, así que las
     * 128 o 1.024 unidades de un Pusher quedaban en una ranura sobredimensionada y todo lo que
     * pasaba de un stack se perdía. Con {@code kinds} &gt; 1 (whitelist de varios ingredientes), un
     * tipo nunca ocupa más que su parte de las ranuras de entrada. Ver {@link #fillInputSlots} para
     * la regla de un hueco nuevo por inserción.
     *
     * @return units that did not fit / unidades que no cupieron
     */
    public static int insert(Block block, ItemStack stack, int kinds) {
        Object menu = menuOf(block);
        if (menu == null || stack == null || stack.getAmount() <= 0) {
            return stack == null ? 0 : stack.getAmount();
        }
        try {
            ItemStack sample = stack.clone();
            sample.setAmount(1);
            int[] slots = getTransportSlots(menu, flowInsert, sample);
            return fillInputSlots(new SlotAccess() {
                @Override
                public ItemStack get(int slot) {
                    try {
                        return mGetItemInSlot.invoke(menu, slot) instanceof ItemStack item ? item : null;
                    } catch (ReflectiveOperationException error) {
                        throw new IllegalStateException(error);
                    }
                }

                @Override
                public void set(int slot, ItemStack item) {
                    try {
                        mReplaceExistingItem.invoke(menu, slot, item);
                    } catch (ReflectiveOperationException error) {
                        throw new IllegalStateException(error);
                    }
                }
            }, slots, stack, kinds, error -> logError(block, error));
        } catch (RuntimeException error) {
            // Fallo antes de tocar ningun hueco: no entro nada.
            logError(block, error);
            return stack.getAmount();
        }
    }

    /** The slots of a machine's menu, as the insert logic sees them / Los huecos del menú de una máquina. */
    interface SlotAccess {
        ItemStack get(int slot);

        void set(int slot, ItemStack item);
    }

    /**
     * EN: The insert rule, apart from Slimefun's reflection so it can be tested.
     * <ol>
     *   <li>Top up every slot (among the ones the machine offered for this item) that already holds
     *       it, up to one stack each.</li>
     *   <li>Then open <b>at most one</b> new slot, as NetworksV6 does (it moves one stack per
     *       cycle). The machine decides which slots it offers: the Electric Smeltery, the Heated
     *       Pressure Chamber and addon machines with the same rule only offer the slot that already
     *       holds the item (none once it is full), so each ingredient ends up in a single stack and
     *       the other slots stay free for the other ingredients. Ordinary machines offer every input
     *       slot and still fill them all, one per cycle.</li>
     * </ol>
     * A whitelist of several ingredients ({@code kinds} &gt; 1) still caps one item type at its
     * share of the slots.
     *
     * ES: La regla de inserción, aparte de la reflexión de Slimefun para poder probarla. Primero
     * rellena hasta un stack los huecos (de los que la máquina ofrece para este ítem) que ya lo
     * tienen; después abre <b>como mucho un</b> hueco nuevo, como NetworksV6 (un stack por ciclo). La
     * máquina decide qué huecos ofrece: la Electric Smeltery, la Heated Pressure Chamber y las
     * máquinas de addons con la misma regla solo ofrecen el hueco que ya tiene el ítem (ninguno si
     * está lleno), así cada ingrediente queda en un solo stack y los demás huecos quedan libres. Las
     * máquinas normales ofrecen todos los huecos de entrada y los siguen llenando, uno por ciclo.
     *
     * A failing slot access stops the insert and reports it to {@code onError}; the return value
     * still counts exactly what went in, so nothing is duplicated.
     * Un fallo al acceder a un hueco detiene la inserción y se informa a {@code onError}; el
     * resultado sigue contando exactamente lo que entró, así no se duplica nada.
     *
     * @return units that did not fit / unidades que no cupieron
     */
    static int fillInputSlots(SlotAccess access, int[] slots, ItemStack stack, int kinds,
                              java.util.function.Consumer<RuntimeException> onError) {
        int remaining = stack.getAmount();
        if (slots == null || slots.length == 0 || remaining <= 0) {
            return remaining;
        }
        ItemStack sample = stack.clone();
        sample.setAmount(1);
        int perSlot = Math.max(1, Math.min(stack.getMaxStackSize(), 64));
        int share = kinds > 1 ? Math.max(1, slots.length / kinds) : Integer.MAX_VALUE;
        int occupied = 0;
        try {
            for (int slot : slots) {
                ItemStack current = access.get(slot);
                if (current == null || current.getType().isAir() || !StackUtils.itemsMatch(current, sample)) {
                    continue;
                }
                occupied++;
                int room = perSlot - current.getAmount();
                if (room <= 0 || remaining <= 0) {
                    continue;
                }
                int add = Math.min(room, remaining);
                ItemStack merged = current.clone();
                merged.setAmount(current.getAmount() + add);
                access.set(slot, merged);
                remaining -= add;
            }
            if (remaining <= 0 || occupied >= share) {
                return remaining;
            }
            for (int slot : slots) {
                ItemStack current = access.get(slot);
                if (current != null && !current.getType().isAir()) {
                    continue;
                }
                int add = Math.min(perSlot, remaining);
                ItemStack placed = sample.clone();
                placed.setAmount(add);
                access.set(slot, placed);
                remaining -= add;
                // Un solo hueco nuevo por insercion: el siguiente ciclo vuelve a preguntar a la maquina.
                break;
            }
        } catch (RuntimeException error) {
            onError.accept(error);
        }
        return remaining;
    }

    /**
     * EN: Opens the Slimefun machine GUI for a player if the block is an active Slimefun machine.
     * ES: Abre la GUI de la máquina de Slimefun para un jugador si el bloque es una máquina activa.
     *
     * @param block Target Slimefun machine block / Bloque de la máquina de Slimefun.
     * @param player Target player / Jugador objetivo.
     * @return true if successfully opened, false otherwise / true si se abrió con éxito, false en caso contrario.
     */
    public static boolean openSlimefunMenu(Block block, org.bukkit.entity.Player player) {
        if (!available || block == null || player == null) return false;
        try {
            Object menu = menuOf(block);
            if (menu == null) return false;

            // Check permission / canOpen if available
            try {
                Method mCanOpen = menu.getClass().getMethod("canOpen", Block.class, org.bukkit.entity.Player.class);
                Object allowed = mCanOpen.invoke(menu, block, player);
                if (allowed instanceof Boolean b && !b) {
                    return false;
                }
            } catch (NoSuchMethodException ignored) {}

            try {
                Method mOpen = menu.getClass().getMethod("open", org.bukkit.entity.Player[].class);
                mOpen.invoke(menu, (Object) new org.bukkit.entity.Player[]{player});
                return true;
            } catch (NoSuchMethodException e) {
                Method mOpen = menu.getClass().getMethod("open", org.bukkit.entity.Player.class);
                mOpen.invoke(menu, player);
                return true;
            }
        } catch (ReflectiveOperationException | RuntimeException error) {
            return false;
        }
    }

    // ----------------------------------------------------------------
    // Slimefun Recipe Discovery & Auto-Crafting Support
    // ----------------------------------------------------------------


    /**
     * EN: Caches all 3x3 recipes from enabled Slimefun items in the registry.
     * ES: Cachea todas las recetas 3x3 de ítems habilitados de Slimefun en el registro.
     */
    public static synchronized void loadSlimefunRecipes() {
        if (!available) return;
        SF_RECIPES.clear();
        try {
            Class<?> sfClass = null;
            for (String prefix : new String[]{"com.github.drakescraft_labs.slimefun4", "io.github.thebusybiscuit.slimefun4"}) {
                try {
                    sfClass = Class.forName(prefix + ".implementation.Slimefun");
                    break;
                } catch (ClassNotFoundException ignored) {}
            }
            if (sfClass == null) return;

            Method mGetRegistry = sfClass.getMethod("getRegistry");
            Object registry = mGetRegistry.invoke(null);
            Method mGetEnabled = registry.getClass().getMethod("getEnabledSlimefunItems");
            Object itemsObj = mGetEnabled.invoke(registry);

            if (itemsObj instanceof Iterable<?> iterable) {
                for (Object item : iterable) {
                    if (item == null) continue;
                    Class<?> itemClass = item.getClass();

                    Method mGetId = itemClass.getMethod("getId");
                    String id = (String) mGetId.invoke(item);
                    if (id == null || id.toUpperCase(Locale.ROOT).contains("BACKPACK")) {
                        continue;
                    }

                    Method mGetRecipe = itemClass.getMethod("getRecipe");
                    ItemStack[] recipe = (ItemStack[]) mGetRecipe.invoke(item);
                    if (recipe == null || recipe.length != 9) {
                        continue;
                    }

                    Method mGetRecipeOutput = itemClass.getMethod("getRecipeOutput");
                    ItemStack output = (ItemStack) mGetRecipeOutput.invoke(item);
                    if (output == null || output.getType().isAir()) {
                        continue;
                    }

                    String typeName = "Slimefun Recipe";
                    try {
                        Method mGetRecipeType = itemClass.getMethod("getRecipeType");
                        Object recipeType = mGetRecipeType.invoke(item);
                        if (recipeType != null) {
                            try {
                                Method mToItem = recipeType.getClass().getMethod("toItem");
                                ItemStack rtItem = (ItemStack) mToItem.invoke(recipeType);
                                if (rtItem != null && rtItem.hasItemMeta() && rtItem.getItemMeta().hasDisplayName()) {
                                    typeName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                                            .serialize(rtItem.getItemMeta().displayName());
                                }
                            } catch (Exception ignored) {
                                typeName = recipeType.toString();
                            }
                        }
                    } catch (Exception ignored) {}

                    ItemStack[] inputs = new ItemStack[9];
                    for (int i = 0; i < 9; i++) {
                        inputs[i] = recipe[i] != null ? recipe[i].clone() : null;
                    }
                    SF_RECIPES.add(new SlimefunRecipeDetails(inputs, output.clone(), id, typeName));
                }
            }
            recipesLoaded = true;
        } catch (Throwable t) {
            Logger.getLogger("MultiverseNets").log(Level.WARNING, "Failed to load Slimefun recipes", t);
        }
    }

    /**
     * EN: Returns unmodifiable list of all discovered Slimefun 3x3 recipes.
     * ES: Devuelve lista inmodificable de todas las recetas 3x3 de Slimefun.
     */
    public static List<SlimefunRecipeDetails> getSlimefunRecipes() {
        if (!recipesLoaded && available) loadSlimefunRecipes();
        return Collections.unmodifiableList(SF_RECIPES);
    }

    /**
     * EN: Matches a 3x3 matrix against cached Slimefun recipes, returning recipe details if found.
     * ES: Compara una matriz 3x3 con las recetas de Slimefun, devolviendo los detalles si coincide.
     */
    public static SlimefunRecipeDetails findSlimefunRecipeDetails(ItemStack[] matrix) {
        if (!available || matrix == null || matrix.length != 9) return null;
        if (!recipesLoaded) loadSlimefunRecipes();
        for (SlimefunRecipeDetails details : SF_RECIPES) {
            if (matchesSlimefunMatrix(matrix, details.inputs())) {
                return details;
            }
        }
        return null;
    }

    /**
     * EN: Resolves the crafted output ItemStack for a 3x3 Slimefun recipe pattern.
     * ES: Resuelve el ItemStack de salida para un patrón de receta 3x3 de Slimefun.
     */
    public static ItemStack findSlimefunRecipe(ItemStack[] matrix) {
        SlimefunRecipeDetails details = findSlimefunRecipeDetails(matrix);
        return details != null ? details.output().clone() : null;
    }

    private static boolean matchesSlimefunMatrix(ItemStack[] a, ItemStack[] b) {
        if (a == null || b == null || a.length != 9 || b.length != 9) return false;
        for (int i = 0; i < 9; i++) {
            boolean aAir = a[i] == null || a[i].getType().isAir();
            boolean bAir = b[i] == null || b[i].getType().isAir();
            if (aAir && bAir) {
                continue;
            }
            if (aAir != bAir) {
                return false;
            }
            String sfA = getId(a[i]);
            String sfB = getId(b[i]);
            if (sfA != null && sfB != null) {
                if (!sfA.equalsIgnoreCase(sfB)) {
                    return false;
                }
            } else if (sfA != null || sfB != null) {
                return false;
            } else if (!StackUtils.itemsMatch(a[i], b[i], false)) {
                return false;
            }
        }
        return true;
    }
    // ----------------------------------------------------------------
    // Quantum Cell Slimefun Cleanup
    // ----------------------------------------------------------------


    /**
     * EN: Cleans up registered BlockMenu and block info when a Quantum Cell is broken.
     * ES: Limpia el BlockMenu y la información de bloque registrados al romper una Celda Cuántica.
     */
    public static void unregisterCell(Block block) {
        if (!available || block == null) return;
        try {
            if (mGetStorage != null && fInventories != null) {
                Object storage = mGetStorage.invoke(null, block.getWorld());
                if (storage != null) {
                    Map<?, ?> invs = (Map<?, ?>) fInventories.get(storage);
                    if (invs != null) {
                        invs.remove(block.getLocation());
                    }
                }
            }
            if (mClearBlockInfo != null) {
                mClearBlockInfo.invoke(null, block.getLocation());
            }
        } catch (Throwable ignored) {
        }
    }

    // ----------------------------------------------------------------
    // Legacy backward-compatibility aliases / Alias de compatibilidad
    // ----------------------------------------------------------------

    /** @deprecated Use {@link #isAvailable()} */
    @Deprecated
    public static boolean disponible() {
        return isAvailable();
    }

    /** @deprecated Use {@link #isMachine(Block)} */
    @Deprecated
    public static boolean esMaquina(Block block) {
        return isMachine(block);
    }

    /** @deprecated Use {@link #getId(Block)} */
    @Deprecated
    public static String idDe(Block block) {
        return getId(block);
    }

    /** @deprecated Use {@link #getId(ItemStack)} */
    @Deprecated
    public static String idDe(ItemStack item) {
        return getId(item);
    }

    /** @deprecated Use {@link #isSlimefunItem(ItemStack)} */
    @Deprecated
    public static boolean esItemSlimefun(ItemStack item) {
        return isSlimefunItem(item);
    }

    /** @deprecated Use {@link #extract(Block, Predicate, int)} */
    @Deprecated
    public static ItemStack extraer(Block block, Predicate<ItemStack> filter, int max) {
        return extract(block, filter, max);
    }

    /** @deprecated Use {@link #insert(Block, ItemStack)} */
    @Deprecated
    public static int insertar(Block block, ItemStack stack) {
        return insert(block, stack);
    }

    private static void logError(Block block, Throwable error) {
        Logger.getLogger("MultiverseNets").log(Level.FINE,
                "Failed communicating with Slimefun machine at " + block.getX() + "," + block.getY()
                        + "," + block.getZ(), error);
    }
}

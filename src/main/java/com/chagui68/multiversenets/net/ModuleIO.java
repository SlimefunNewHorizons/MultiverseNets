package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.util.Settings;
import com.chagui68.multiversenets.util.StackUtils;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * EN: DRAM Unloader and DRAM Loader. Unlike a DRAM Bay, the modules inside them are NOT network
 * storage: they are cards being emptied into the network (Unloader) or filled from it (Loader).
 * <ul>
 *   <li>Unloader: up to {@link #UNLOADER_SLOTS} cards. Every cycle it moves their stock into the
 *   network; what the network can not take stays on the card and the next item is tried, so the
 *   card ends up as empty as the network allows.</li>
 *   <li>Loader: one card, one target item (or fluid for a Fluid DRAM) and an amount. Once started
 *   it pulls that item from the network until the card holds the amount, or is full, then stops.
 *   A card with 1,000,000 cobblestone, ready to sell.</li>
 * </ul>
 *
 * ES: DRAM Unloader y DRAM Loader. A diferencia de un DRAM Bay, los módulos que tienen dentro NO
 * son almacenamiento de la red: son tarjetas que se vacían en la red (Unloader) o se llenan desde
 * ella (Loader). El Unloader admite hasta 9 tarjetas y deja cada una lo más vacía que la red
 * permita; lo que no cabe se queda en la tarjeta y se prueba el siguiente ítem. El Loader tiene una
 * tarjeta, un ítem (o fluido) objetivo y una cantidad: al iniciarlo saca ese ítem de la red hasta
 * que la tarjeta tenga esa cantidad o esté llena, y se para.
 */
public final class ModuleIO {

    public static final int UNLOADER_SLOTS = 9;
    public static final int LOADER_SLOTS = 1;

    private ModuleIO() {
    }

    public static int slots(DeviceType holder) {
        return switch (holder) {
            case MVN_MODULE_UNLOADER -> UNLOADER_SLOTS;
            case MVN_MODULE_LOADER -> LOADER_SLOTS;
            default -> MemoryModules.BAY_SLOTS;
        };
    }

    public static int freeSlots(NodeBlob holder) {
        DeviceType type = holder == null ? null : DeviceType.parse(holder.typeName);
        return type == null ? 0 : Math.max(0, slots(type) - MemoryModules.modules(holder).size());
    }

    /** Installs one module item if there is room / Instala un módulo si hay sitio. */
    public static boolean install(NodeBlob holder, ItemStack moduleItem) {
        if (freeSlots(holder) <= 0) {
            return false;
        }
        boolean installed = MemoryModules.install(holder, moduleItem);
        if (installed && DeviceType.parse(holder.typeName) == DeviceType.MVN_MODULE_LOADER) {
            // Una tarjeta nueva no arranca sola: el jugador elige y pulsa iniciar.
            holder.loaderActive = false;
        }
        return installed;
    }

    /** Capacity of a module: items for a cache module, mB for a Fluid DRAM / Capacidad de un módulo. */
    public static long capacity(DeviceType module) {
        if (module == null) {
            return 0;
        }
        return module.isCacheModule() ? MemoryModules.itemCapacity(module) : Settings.fluidDramCapacity();
    }

    /** What a module holds: items or mB / Lo que guarda un módulo: ítems o mB. */
    public static long stored(NodeBlob module) {
        DeviceType type = MemoryModules.typeOf(module);
        if (type == null) {
            return 0;
        }
        return type.isCacheModule() ? module.totalVirtualAmount() : module.totalDramFluid();
    }

    // ------------------------------------------------------------------ Unloader

    /**
     * EN: One Unloader cycle. Moves up to the configured amount out of the cards into the network
     * and returns how many items plus mB moved.
     *
     * ES: Un ciclo del Unloader. Mueve de las tarjetas a la red hasta lo configurado y devuelve
     * cuántos ítems más mB se movieron.
     */
    public static long unload(Network net, NodeBlob unloader) {
        long itemBudget = Settings.moduleItemsPerOp();
        long fluidBudget = Settings.moduleFluidPerOp();
        long moved = 0;
        for (NodeBlob module : MemoryModules.modules(unloader)) {
            DeviceType type = MemoryModules.typeOf(module);
            if (type == null) {
                continue;
            }
            if (type.isCacheModule()) {
                // De atras adelante: removeVirtualItem quita la entrada cuando se vacia.
                for (int i = module.virtualSamples.size() - 1; i >= 0 && itemBudget > 0; i--) {
                    long amount = module.virtualAmounts.get(i);
                    int take = (int) Math.min(Math.min(amount, itemBudget), Integer.MAX_VALUE);
                    if (take <= 0) {
                        continue;
                    }
                    ItemStack stack = StackUtils.getAsQuantity(module.virtualSamples.get(i), take);
                    int leftover = net.storage().deposit(stack);
                    int done = take - Math.max(0, leftover);
                    if (done > 0) {
                        module.removeVirtualItem(i, done);
                        itemBudget -= done;
                        moved += done;
                    }
                    // Si la red no admite este item, se queda en la tarjeta y se sigue con el siguiente.
                }
            } else {
                for (int i = module.dramFluids.size() - 1; i >= 0 && fluidBudget > 0; i--) {
                    String fluid = module.dramFluids.get(i);
                    long take = Math.min(module.dramFluidAmounts.get(i), fluidBudget);
                    long done = take - net.fluidStorage().deposit(fluid, take);
                    if (done > 0) {
                        module.removeDramFluid(fluid, done);
                        fluidBudget -= done;
                        moved += done;
                    }
                }
            }
            if (itemBudget <= 0 && fluidBudget <= 0) {
                break;
            }
        }
        return moved;
    }

    // ------------------------------------------------------------------ Loader

    /** The module in a Loader, or null / El módulo del Loader, o null. */
    public static NodeBlob loaderModule(NodeBlob loader) {
        List<NodeBlob> modules = MemoryModules.modules(loader);
        return modules.isEmpty() ? null : modules.get(0);
    }

    /** How much of the target the Loader's card holds / Cuánto del objetivo tiene la tarjeta. */
    public static long loadedAmount(NodeBlob loader) {
        NodeBlob module = loaderModule(loader);
        DeviceType type = MemoryModules.typeOf(module);
        if (type == null) {
            return 0;
        }
        if (type.isCacheModule()) {
            int idx = loader.loaderTarget == null ? -1 : module.indexOfVirtualSample(loader.loaderTarget);
            return idx < 0 ? 0 : module.virtualAmounts.get(idx);
        }
        return loader.loaderFluid == null ? 0 : module.dramFluidAmount(loader.loaderFluid);
    }

    /**
     * EN: The amount the card will end up holding: the chosen amount, never above what fits next
     * to what the card already stores. 0 when there is no card or no target.
     *
     * ES: La cantidad con la que acabará la tarjeta: la elegida, nunca por encima de lo que cabe junto
     * a lo que la tarjeta ya guarda. 0 sin tarjeta u objetivo.
     */
    public static long loaderGoal(NodeBlob loader) {
        NodeBlob module = loaderModule(loader);
        DeviceType type = MemoryModules.typeOf(module);
        if (type == null || !hasTarget(loader, type)) {
            return 0;
        }
        long current = loadedAmount(loader);
        long max = current + Math.max(0, capacity(type) - stored(module));
        return loader.loaderAmount <= 0 ? max : Math.min(loader.loaderAmount, max);
    }

    public static boolean hasTarget(NodeBlob loader, DeviceType moduleType) {
        if (moduleType == null) {
            return false;
        }
        return moduleType.isCacheModule() ? loader.loaderTarget != null : loader.loaderFluid != null;
    }

    /**
     * EN: One Loader cycle. Pulls the target from the network into the card; stops the Loader when
     * the goal is reached. Returns the units moved.
     *
     * ES: Un ciclo del Loader. Saca el objetivo de la red y lo mete en la tarjeta; para el Loader al
     * llegar a la meta. Devuelve las unidades movidas.
     */
    public static long load(Network net, NodeBlob loader) {
        if (!loader.loaderActive) {
            return 0;
        }
        NodeBlob module = loaderModule(loader);
        DeviceType type = MemoryModules.typeOf(module);
        if (type == null || !hasTarget(loader, type)) {
            loader.loaderActive = false;
            return 0;
        }
        long need = loaderGoal(loader) - loadedAmount(loader);
        if (need <= 0) {
            loader.loaderActive = false;
            return 0;
        }
        long moved;
        if (type.isCacheModule()) {
            ItemStack target = loader.loaderTarget;
            int want = (int) Math.min(need, Settings.moduleItemsPerOp());
            ItemStack got = net.storage().withdraw(item -> StackUtils.itemsMatch(item, target), want);
            moved = got == null ? 0 : got.getAmount();
            if (moved > 0) {
                module.addVirtualItem(target, moved);
            }
        } else {
            moved = net.fluidStorage().withdraw(loader.loaderFluid, Math.min(need, Settings.moduleFluidPerOp()));
            if (moved > 0) {
                module.addDramFluid(loader.loaderFluid, moved);
            }
        }
        if (loadedAmount(loader) >= loaderGoal(loader)) {
            loader.loaderActive = false;
        }
        return moved;
    }

    /** "1m", "500k", "2.5M", "1,000,000" → units; -1 when unreadable / Lee una cantidad. */
    public static long parseAmount(String text) {
        if (text == null) {
            return -1;
        }
        String clean = text.trim().toLowerCase(java.util.Locale.ROOT).replace(",", "").replace("_", "").replace(" ", "");
        if (clean.isEmpty()) {
            return -1;
        }
        if (clean.equals("max") || clean.equals("full")) {
            return 0;
        }
        long factor = 1;
        char last = clean.charAt(clean.length() - 1);
        if (last == 'k' || last == 'm' || last == 'b') {
            factor = last == 'k' ? 1_000L : last == 'm' ? 1_000_000L : 1_000_000_000L;
            clean = clean.substring(0, clean.length() - 1);
        }
        try {
            double value = Double.parseDouble(clean);
            if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) {
                return -1;
            }
            return Math.round(value * factor);
        } catch (NumberFormatException error) {
            return -1;
        }
    }

    /** Readable name of the Loader's target / Nombre legible del objetivo del Loader. */
    public static String targetName(NodeBlob loader) {
        if (loader.loaderFluid != null) {
            return loader.loaderFluid;
        }
        if (loader.loaderTarget == null) {
            return "nothing";
        }
        var meta = loader.loaderTarget.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(meta.displayName());
        }
        return loader.loaderTarget.getType().name();
    }
}

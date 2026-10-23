package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.compat.SlimefunBridge;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
import com.chagui68.multiversenets.util.Settings;
import com.chagui68.multiversenets.util.StackUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * [EN] Vanilla containers linked by a Container Connector. A chest, trapped chest, copper chest,
 * barrel or shulker box touching a Connector is storage of the Connector's network: the Terminal
 * shows its items, deposits fill it and withdrawals empty it. It is the last storage in both
 * directions: the network fills its own storage first and takes from it first.
 * <p>
 * Items are moved exactly as they are (merged only with {@link ItemStack#isSimilar}), so a shulker
 * box keeps its contents and a tool its stats. A shulker box never goes into a shulker box.
 *
 * [ES] Contenedores vanilla enlazados por un Container Connector. Un cofre, cofre trampa, cofre de
 * cobre, barril o shulker que toca un Connector es almacenamiento de su red: el Terminal muestra
 * sus ítems, los depósitos lo llenan y las retiradas lo vacían. Es el último almacenamiento en los
 * dos sentidos: la red llena antes el suyo propio y saca antes de él.
 * <p>
 * Los ítems se mueven tal cual (solo se juntan con {@link ItemStack#isSimilar}), así un shulker
 * conserva su contenido y una herramienta sus datos. Un shulker nunca entra en otro shulker.
 */
public final class VanillaStorage {

    private VanillaStorage() {
    }

    /**
     * One linked container. Valid while its blocks keep their type: a chest broken in the middle of
     * a tick is skipped instead of receiving items into a block that no longer exists.
     */
    static final class Linked {
        final Inventory inventory;
        final Block block;
        /** The other half of a double chest, or null / La otra mitad de un cofre doble, o null. */
        final Block partner;
        private final Material type;
        private final Material partnerType;
        private final boolean shulker;

        private Linked(Inventory inventory, Block block, Block partner) {
            this.inventory = inventory;
            this.block = block;
            this.partner = partner;
            this.type = block.getType();
            this.partnerType = partner == null ? null : partner.getType();
            this.shulker = inventory.getType() == InventoryType.SHULKER_BOX;
        }

        boolean valid() {
            return block.getType() == type && (partner == null || partner.getType() == partnerType);
        }

        boolean accepts(ItemStack item) {
            return !shulker || !Tag.SHULKER_BOXES.isTagged(item.getType());
        }
    }

    /**
     * EN: Blocks a Container Connector links. By name for copper chests: they only exist since
     * 1.21.9 and the plugin runs on every 1.21 server.
     * ES: Bloques que enlaza un Container Connector. Los cofres de cobre por nombre: solo existen
     * desde la 1.21.9 y el plugin funciona en cualquier servidor 1.21.
     */
    public static boolean isStorageMaterial(Material type) {
        if (type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL) {
            return true;
        }
        String name = type.name();
        return name.endsWith("COPPER_CHEST") || name.endsWith("SHULKER_BOX");
    }

    /**
     * EN: Side where the other half of a double chest is, the same rule as vanilla's
     * ChestBlock.getConnectedDirection: clockwise of the facing for LEFT, counter-clockwise for RIGHT.
     * ES: Lado donde está la otra mitad de un cofre doble, la misma regla que vanilla: a la derecha
     * del frente (sentido horario) para LEFT, a la izquierda para RIGHT.
     */
    static BlockFace partnerFace(BlockFace facing, org.bukkit.block.data.type.Chest.Type type) {
        BlockFace clockwise = switch (facing) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            default -> BlockFace.NORTH;
        };
        return type == org.bukkit.block.data.type.Chest.Type.LEFT ? clockwise : clockwise.getOppositeFace();
    }

    /**
     * EN: The container at {@code block} if a Connector may link it: a storage block that is neither a
     * network device nor a Slimefun block. Never loads a chunk: a double chest whose other half is in
     * an unloaded chunk is skipped.
     * ES: El contenedor de {@code block} si un Connector puede enlazarlo: un bloque de almacén que no
     * es dispositivo de la red ni bloque de Slimefun. Nunca carga un chunk.
     */
    static Linked at(Block block) {
        if (!isStorageMaterial(block.getType()) || NodeStore.hasNode(block) || slimefunBlock(block)) {
            return null;
        }
        Block partner = null;
        if (block.getBlockData() instanceof org.bukkit.block.data.type.Chest data
                && data.getType() != org.bukkit.block.data.type.Chest.Type.SINGLE) {
            BlockFace side = partnerFace(data.getFacing(), data.getType());
            // getInventory() de un cofre doble lee la otra mitad: con su chunk sin cargar, lo cargaria.
            if (!block.getWorld().isChunkLoaded((block.getX() + side.getModX()) >> 4,
                    (block.getZ() + side.getModZ()) >> 4)) {
                return null;
            }
            Block other = block.getRelative(side);
            if (other.getBlockData() instanceof org.bukkit.block.data.type.Chest otherData
                    && otherData.getFacing() == data.getFacing()
                    && otherData.getType() != org.bukkit.block.data.type.Chest.Type.SINGLE
                    && otherData.getType() != data.getType()
                    && !NodeStore.hasNode(other) && !slimefunBlock(other)) {
                partner = other;
            }
        }
        if (!(block.getState(false) instanceof Container container)) {
            return null;
        }
        Inventory inventory = container.getInventory();
        if (inventory instanceof DoubleChestInventory) {
            // Sin saber cual es la otra mitad no se puede comprobar su proteccion: no se toca.
            return partner == null ? null : new Linked(inventory, block, partner);
        }
        return new Linked(inventory, block, null);
    }

    private static boolean slimefunBlock(Block block) {
        return Settings.compatSlimefun() && SlimefunBridge.isAvailable() && SlimefunBridge.getId(block) != null;
    }

    /**
     * EN: The network's linked containers that are loaded and still valid, each once (both halves of a
     * double chest are one container). A half the last scan did not check is skipped until the next.
     * ES: Los contenedores enlazados cargados y válidos, cada uno una vez (las dos mitades de un cofre
     * doble son uno). Una mitad que el último escaneo no comprobó se salta hasta el siguiente.
     */
    static List<Linked> load(Network network) {
        LongOpenHashSet positions = network.vanillaContainers();
        if (positions.isEmpty()) {
            return List.of();
        }
        List<Linked> out = new ArrayList<>();
        LongOpenHashSet seen = new LongOpenHashSet();
        for (long pos : positions.toLongArray()) {
            if (seen.contains(pos)
                    || !network.world().isChunkLoaded(PosUtil.unpackX(pos) >> 4, PosUtil.unpackZ(pos) >> 4)) {
                continue;
            }
            Linked linked = at(network.block(pos));
            if (linked == null) {
                continue;
            }
            if (linked.partner != null) {
                long other = PosUtil.pack(linked.partner.getX(), linked.partner.getY(), linked.partner.getZ());
                if (!positions.contains(other)) {
                    continue;
                }
                seen.add(other);
            }
            seen.add(pos);
            out.add(linked);
        }
        return out;
    }

    private static boolean admits(Set<Material> materials, ItemStack item) {
        return materials == null || materials.contains(item.getType());
    }

    private static boolean empty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    static long count(List<Linked> containers, Predicate<ItemStack> matcher, Set<Material> materials) {
        long total = 0;
        for (Linked linked : containers) {
            if (!linked.valid()) {
                continue;
            }
            for (ItemStack it : linked.inventory.getStorageContents()) {
                if (!empty(it) && admits(materials, it) && matcher.test(it)) {
                    total += it.getAmount();
                }
            }
        }
        return total;
    }

    static ItemStack peek(List<Linked> containers, Predicate<ItemStack> matcher, Set<Material> materials) {
        for (Linked linked : containers) {
            if (!linked.valid()) {
                continue;
            }
            for (ItemStack it : linked.inventory.getStorageContents()) {
                if (!empty(it) && admits(materials, it) && matcher.test(it)) {
                    return StackUtils.getAsQuantity(it, 1);
                }
            }
        }
        return null;
    }

    /**
     * EN: Takes up to {@code max} units of one item that passes {@code matcher}: the first matching
     * stack, or {@code like} when given, and only stacks similar to it. Null when nothing was taken.
     * ES: Saca hasta {@code max} unidades de un ítem que pasa {@code matcher}: el del primer stack que
     * coincide, o {@code like} si se da, y solo stacks similares a él. Null si no sacó nada.
     */
    static ItemStack withdraw(List<Linked> containers, Predicate<ItemStack> matcher, Set<Material> materials,
                              ItemStack like, int max) {
        ItemStack taken = null;
        int got = 0;
        for (Linked linked : containers) {
            if (got >= max) {
                break;
            }
            if (!linked.valid()) {
                continue;
            }
            Inventory inv = linked.inventory;
            int size = inv.getStorageContents().length;
            for (int slot = 0; slot < size && got < max; slot++) {
                ItemStack it = inv.getItem(slot);
                if (empty(it) || !admits(materials, it) || !matcher.test(it)) {
                    continue;
                }
                ItemStack model = taken != null ? taken : like;
                if (model != null && !it.isSimilar(model)) {
                    continue;
                }
                if (taken == null) {
                    taken = it.clone();
                }
                int take = Math.min(it.getAmount(), max - got);
                int left = it.getAmount() - take;
                if (left <= 0) {
                    inv.setItem(slot, null);
                } else {
                    it.setAmount(left);
                    inv.setItem(slot, it);
                }
                got += take;
            }
        }
        if (taken == null) {
            return null;
        }
        taken.setAmount(got);
        return taken;
    }

    /**
     * EN: Puts {@code amount} units of {@code item} into the containers: first on top of stacks of the
     * same item, then into empty slots, never more than a stack per slot. Returns what did not fit.
     * ES: Mete {@code amount} unidades de {@code item}: primero sobre stacks del mismo ítem, luego en
     * ranuras vacías, nunca más de un stack por ranura. Devuelve lo que no cupo.
     */
    static long deposit(List<Linked> containers, ItemStack item, long amount) {
        long remaining = amount;
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (Linked linked : containers) {
                if (remaining <= 0) {
                    break;
                }
                if (!linked.valid() || !linked.accepts(item)) {
                    continue;
                }
                Inventory inv = linked.inventory;
                int max = Math.max(1, Math.min(item.getMaxStackSize(), inv.getMaxStackSize()));
                int size = inv.getStorageContents().length;
                for (int slot = 0; slot < size && remaining > 0; slot++) {
                    ItemStack current = inv.getItem(slot);
                    boolean free = empty(current);
                    // Pasada 0: stacks del mismo item. Pasada 1: ranuras vacias.
                    if (pass == 0 ? free || !current.isSimilar(item) : !free) {
                        continue;
                    }
                    int have = free ? 0 : current.getAmount();
                    int put = (int) Math.min(max - have, remaining);
                    if (put <= 0) {
                        continue;
                    }
                    inv.setItem(slot, StackUtils.getAsQuantity(item, have + put));
                    remaining -= put;
                }
            }
        }
        return remaining;
    }

    /**
     * EN: Changes when the contents of a linked container change, even by hand or with a hopper, so a
     * Terminal open on the network redraws.
     * ES: Cambia cuando cambia el contenido de un contenedor enlazado, también a mano o con una tolva,
     * para que un Terminal abierto se redibuje.
     */
    static long fingerprint(List<Linked> containers) {
        long fp = 1;
        for (Linked linked : containers) {
            if (!linked.valid()) {
                continue;
            }
            for (ItemStack it : linked.inventory.getStorageContents()) {
                fp = fp * 31 + (empty(it) ? 0 : it.getType().ordinal() * 128L + it.getAmount());
            }
        }
        return fp;
    }
}

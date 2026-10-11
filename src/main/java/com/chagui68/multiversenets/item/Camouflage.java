package com.chagui68.multiversenets.item;

import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.StackUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

/**
 * EN: Cable camouflage. A Network Cable can hold one block (taken from the player, not consumed)
 * and look exactly like it, so a line of cables disappears inside a quartz wall. Taking the block
 * back turns the cable into glass again; breaking the cable drops both. Only stable, solid, full
 * blocks are accepted: nothing with an inventory or a right-click action, nothing that falls, and
 * nothing that changes by itself (grass, unwaxed copper, live coral), since the block in the world
 * must stay what it shows.
 *
 * ES: Camuflaje de cables. Un Cable de red puede guardar un bloque (del jugador, sin consumirlo) y
 * tener exactamente su aspecto, así una línea de cables desaparece dentro de una pared de cuarzo.
 * Recuperar el bloque vuelve a dejar el cable de cristal; romper el cable suelta los dos. Solo se
 * aceptan bloques sólidos, completos y estables: nada con inventario o acción de clic derecho,
 * nada que caiga y nada que cambie solo (césped, cobre sin encerar, coral vivo).
 */
public final class Camouflage {

    private Camouflage() {
    }

    /** True when {@code item} can camouflage a cable / True si el ítem sirve de camuflaje. */
    public static boolean allowed(ItemStack item) {
        if (item == null || item.getType().isAir() || Items.typeOf(item) != null) {
            return false;
        }
        return allowed(item.getType());
    }

    @SuppressWarnings("deprecation")
    public static boolean allowed(Material material) {
        if (material == null || !material.isBlock() || material.isAir() || !material.isSolid()
                || !material.isOccluding() || material.hasGravity() || material.isInteractable()) {
            return false;
        }
        String name = material.name();
        if (name.contains("COPPER") && !name.startsWith("WAXED_") && !name.startsWith("RAW_")
                && !name.contains("ORE")) {
            return false;
        }
        if (name.endsWith("CORAL_BLOCK") && !name.startsWith("DEAD_")) {
            return false;
        }
        return switch (material) {
            case GRASS_BLOCK, MYCELIUM, TNT, SPAWNER, BEEHIVE, BEE_NEST, SCULK_CATALYST, SCULK_SHRIEKER,
                 BUDDING_AMETHYST, REINFORCED_DEEPSLATE, BEDROCK, BARRIER, COMMAND_BLOCK,
                 CHAIN_COMMAND_BLOCK, REPEATING_COMMAND_BLOCK, STRUCTURE_BLOCK, JIGSAW,
                 INFESTED_STONE, INFESTED_COBBLESTONE, INFESTED_STONE_BRICKS, INFESTED_MOSSY_STONE_BRICKS,
                 INFESTED_CRACKED_STONE_BRICKS, INFESTED_CHISELED_STONE_BRICKS, INFESTED_DEEPSLATE -> false;
            default -> true;
        };
    }

    /** The material a cable should show / El material que debe mostrar un cable. */
    public static Material look(NodeBlob cable) {
        return cable != null && cable.camouflage != null && allowed(cable.camouflage.getType())
                ? cable.camouflage.getType()
                : plainLook(cable);
    }

    /** The cable's own look: its colour's stained glass, or glass / El aspecto propio del cable. */
    public static Material plainLook(NodeBlob cable) {
        return Items.cableMaterial(cable == null ? null : Items.parseColor(cable.cableColor));
    }

    /**
     * EN: Stores one unit of {@code item} in the cable and shows it. Returns the block that was there
     * before (to give back), or null.
     * ES: Guarda una unidad de {@code item} en el cable y la muestra. Devuelve el bloque que había
     * antes (para devolverlo), o null.
     */
    public static ItemStack apply(Block block, NodeBlob cable, ItemStack item) {
        ItemStack previous = cable.camouflage;
        cable.camouflage = StackUtils.getAsQuantity(item, 1);
        NodeStore.put(block, cable);
        // Sin fisica: el cable solo cambia de aspecto, los vecinos no reaccionan.
        block.setType(look(cable), false);
        return previous;
    }

    /** Takes the camouflage back and turns the cable into glass again / Quita el camuflaje. */
    public static ItemStack remove(Block block, NodeBlob cable) {
        ItemStack previous = cable.camouflage;
        if (previous == null) {
            return null;
        }
        cable.camouflage = null;
        NodeStore.put(block, cable);
        block.setType(plainLook(cable), false);
        return previous;
    }
}

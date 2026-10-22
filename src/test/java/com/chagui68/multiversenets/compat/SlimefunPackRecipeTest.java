package com.chagui68.multiversenets.compat;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Las mejoras de packs de DankTech2 no entran en las recetas de la red: perderían el contenido. */
class SlimefunPackRecipeTest {

    private static final NamespacedKey SF_ID = new NamespacedKey("slimefun", "slimefun_item");

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static ItemStack sf(String id) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(SF_ID, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack[] around(ItemStack core) {
        ItemStack cell = sf("DK2_CELL_2");
        return new ItemStack[]{cell, cell, cell, cell, core, cell, cell, cell, cell};
    }

    @Test
    void packUpgradesAreSkipped() {
        assertTrue(SlimefunBridge.hasPackInput(around(sf("DK2_PACK_1"))));
        assertTrue(SlimefunBridge.hasPackInput(around(sf("DK2_TRASH_3"))));
    }

    @Test
    void ordinaryDankTechRecipesStay() {
        assertFalse(SlimefunBridge.hasPackInput(around(sf("DK2_PACK_CORE"))));
        assertFalse(SlimefunBridge.hasPackInput(around(new ItemStack(Material.HOPPER))));
        assertFalse(SlimefunBridge.hasPackInput(new ItemStack[9]));
    }
}

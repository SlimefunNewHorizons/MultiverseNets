package com.chagui68.multiversenets;

import com.chagui68.multiversenets.util.StackUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] The shortcuts {@link StackUtils#itemsMatch} takes before copying any ItemMeta must give the
 * same answers as the full comparison.
 *
 * [ES] Los atajos de {@link StackUtils#itemsMatch} antes de copiar ItemMeta deben dar las mismas
 * respuestas que la comparación completa.
 */
class StackUtilsFastPathTest {

    private static final NamespacedKey SF_ID = new NamespacedKey("slimefun", "slimefun_item");

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static ItemStack slimefun(String id, String name) {
        ItemStack item = new ItemStack(Material.IRON_INGOT);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.getPersistentDataContainer().set(SF_ID, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    @Test
    @DisplayName("identical items match, whatever their amount")
    void identicalItemsMatch() {
        ItemStack a = slimefun("STEEL_INGOT", "Steel Ingot");
        ItemStack b = a.clone();
        b.setAmount(37);
        assertTrue(StackUtils.itemsMatch(a, b));
    }

    @Test
    @DisplayName("two Slimefun items with different ids never match, even with the same name")
    void differentSlimefunIdsNeverMatch() {
        assertFalse(StackUtils.itemsMatch(slimefun("STEEL_INGOT", "Ingot"), slimefun("DAMASCUS_STEEL_INGOT", "Ingot")));
    }

    @Test
    @DisplayName("an item with meta never matches the same material without meta")
    void metaAgainstPlainNeverMatches() {
        ItemStack plain = new ItemStack(Material.DIAMOND_SWORD);
        ItemStack enchanted = new ItemStack(Material.DIAMOND_SWORD);
        enchanted.addUnsafeEnchantment(Enchantment.SHARPNESS, 3);
        assertFalse(StackUtils.itemsMatch(plain, enchanted));
        assertFalse(StackUtils.itemsMatch(enchanted, plain));
        assertTrue(StackUtils.itemsMatch(plain, new ItemStack(Material.DIAMOND_SWORD, 1)));
    }

    @Test
    @DisplayName("lore is still ignored when asked to, for the same Slimefun item")
    void loreIgnoredWhenRequested() {
        ItemStack a = slimefun("STEEL_INGOT", "Steel Ingot");
        ItemStack b = a.clone();
        var meta = b.getItemMeta();
        meta.lore(java.util.List.of(Component.text("extra line")));
        b.setItemMeta(meta);
        assertTrue(StackUtils.itemsMatch(a, b, false));
        assertFalse(StackUtils.itemsMatch(a, b, true));
    }
}

package com.chagui68.multiversenets;

import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Settings;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] The {@code slimefun-machines} section: on by default, a master switch that turns off every
 * Slimefun machine (recipe, placing) and per-machine keys, with the old {@code sf-*} keys still read.
 *
 * [ES] La sección {@code slimefun-machines}: activa por defecto, un interruptor general que apaga
 * todas las máquinas de Slimefun (receta, colocación) y claves por máquina, leyendo aún las antiguas
 * {@code sf-*}.
 */
class SlimefunMachinesSwitchTest {

    private ServerMock server;
    private MultiverseNets plugin;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(MultiverseNets.class);
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private boolean place(PlayerMock player, int x, DeviceType type) {
        Block target = world.getBlockAt(x, 64, 0);
        BlockState previous = target.getState();
        target.setType(type.material());
        BlockPlaceEvent event = new BlockPlaceEvent(target, previous, target.getRelative(BlockFace.DOWN),
                Items.create(type), player, true, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);
        return !event.isCancelled() && NodeStore.hasNode(target);
    }

    @Test
    void everythingIsOnByDefault() {
        assertTrue(Settings.sfMachinesEnabled());
        assertTrue(Settings.sfEncoderEnabled());
        assertTrue(Settings.sfCrafterEnabled());
        assertNotNull(Bukkit.getRecipe(new NamespacedKey(plugin, "sf_crafter")));
        assertTrue(place(server.addPlayer(), 0, DeviceType.MVN_SF_CRAFTER));
    }

    @Test
    void theMasterSwitchTurnsOffEverySlimefunMachineAndOnlyThose() {
        plugin.getConfig().set("slimefun-machines.enabled", false);
        Items.registerRecipes(plugin);
        PlayerMock player = server.addPlayer();

        assertFalse(Settings.sfEncoderEnabled(), "the master switch wins over the per-machine key");
        assertFalse(Settings.sfCrafterEnabled());
        assertNull(Bukkit.getRecipe(new NamespacedKey(plugin, "sf_crafter")),
                "a reload removes the recipe that was registered at startup");
        assertNull(Bukkit.getRecipe(new NamespacedKey(plugin, "sf_encoder")));
        assertFalse(place(player, 0, DeviceType.MVN_SF_CRAFTER), "a disabled machine cannot be placed");
        assertFalse(place(player, 1, DeviceType.MVN_SF_ENCODER));
        assertFalse(place(player, 2, DeviceType.MVN_SF_REQUEST_CRAFTER));
        assertTrue(place(player, 3, DeviceType.MVN_CRAFTER), "vanilla machines are untouched");
        assertNotNull(Bukkit.getRecipe(new NamespacedKey(plugin, "crafter")));
    }

    @Test
    void perMachineKeysAndLegacyKeys() {
        plugin.getConfig().set("slimefun-machines.encoder", false);
        assertFalse(Settings.sfEncoderEnabled());
        assertTrue(Settings.sfCrafterEnabled(), "turning off the encoder leaves the crafters on");

        // Una config antigua sin la clave nueva sigue mandando con sf-crafter.enabled.
        plugin.getConfig().set("slimefun-machines.crafters", null);
        plugin.getConfig().set("sf-crafter.enabled", false);
        assertFalse(Settings.sfCrafterEnabled());
    }
}

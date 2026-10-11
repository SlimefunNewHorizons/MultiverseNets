package com.chagui68.multiversenets;

import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.net.StatusLights;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * [EN] The Network Status Light turns green on a connected network block and red otherwise, and
 * never becomes part of a network.
 * [ES] La Luz de Estado se pone verde sobre un bloque de red conectado y roja si no, y nunca forma
 * parte de una red.
 */
class StatusLightTest {

    private ServerMock server;
    private MultiverseNets plugin;
    private WorldMock world;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(MultiverseNets.class);
        world = server.addSimpleWorld("world");
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Block place(int x, int y, int z, DeviceType type) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(type.material());
        NodeStore.put(block, NodeBlob.create(type.name()));
        plugin.networks().invalidateNear(block);
        return block;
    }

    private Block placeLight(int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        ItemStack item = Items.create(DeviceType.MVN_STATUS_LIGHT);
        block.setType(item.getType());
        BlockPlaceEvent event = new BlockPlaceEvent(block, block.getState(), block.getRelative(0, -1, 0),
                item, player, true, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);
        return block;
    }

    @Test
    void greenOnAConnectedBlockAndRedWhenTheLinkIsCut() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        plugin.networks().registerController(controller);
        Block cable = place(1, 64, 0, DeviceType.MVN_CABLE);
        Network net = plugin.networks().networkByController(controller.getLocation());
        net.scan();

        Block light = placeLight(1, 65, 0);
        assertEquals(StatusLights.ON, light.getType(), "on a connected cable the light is green");
        assertNull(plugin.networks().networkAt(light), "the light itself never joins the network");

        cable.setType(Material.AIR);
        NodeStore.remove(cable);
        net.scan();
        server.getScheduler().performTicks(25);
        assertEquals(StatusLights.OFF, light.getType(), "with the cable gone it turns red");
    }

    @Test
    void redOnABlockWithoutNetwork() {
        Block cable = place(5, 64, 5, DeviceType.MVN_CABLE);
        Block light = placeLight(5, 65, 5);
        assertNotNull(cable);
        assertEquals(StatusLights.OFF, light.getType(), "a cable with no controller is red");
    }

    @Test
    void lightsNeverBridgeTwoNetworks() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        plugin.networks().registerController(controller);
        Network net = plugin.networks().networkByController(controller.getLocation());
        placeLight(1, 64, 0);
        Block beyond = place(2, 64, 0, DeviceType.MVN_CABLE);
        net.scan();
        assertFalse(net.contains(com.chagui68.multiversenets.util.PosUtil.pack(2, 64, 0)),
                "a cable reached only through a light is not in the network");
        assertNotNull(beyond);
    }

    @Test
    void breakingTheLightDropsItAndForgetsIt() {
        Block light = placeLight(3, 65, 3);
        BlockBreakEvent event = new BlockBreakEvent(light, player);
        server.getPluginManager().callEvent(event);
        assertNull(NodeStore.get(light), "the node is gone");
        light.setType(Material.AIR);
        server.getScheduler().performTicks(25);
        assertEquals(Material.AIR, light.getType(), "a forgotten light is never recoloured");
    }
}

package com.chagui68.multiversenets;

import com.chagui68.multiversenets.gui.CableCamouflageMenu;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Coloured Network Cables: one device in 16 colours, they all connect, keep their colour when
 * broken and after camouflage, and are crafted from 4 cables and a dye.
 * [ES] Cables de red de color: un mismo dispositivo en 16 colores, todos conectan, conservan su color
 * al romperlos y tras el camuflaje, y se craftean con 4 cables y un tinte.
 */
class ColoredCableTest {

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

    private Block placeItem(int x, int y, int z, ItemStack item) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(item.getType());
        server.getPluginManager().callEvent(new BlockPlaceEvent(block, block.getState(), block.getRelative(0, -1, 0),
                item, player, true, EquipmentSlot.HAND));
        return block;
    }

    @Test
    void aColouredCableIsStillANetworkCable() {
        ItemStack red = Items.cable(DyeColor.RED);
        assertEquals(DeviceType.MVN_CABLE, Items.typeOf(red));
        assertEquals(Material.RED_STAINED_GLASS, red.getType());
        assertEquals(DyeColor.RED, Items.cableColor(red));
        assertNull(Items.cableColor(Items.create(DeviceType.MVN_CABLE)), "the plain cable has no colour");
    }

    @Test
    void everyColourConnectsAndKeepsItsColourWhenBroken() {
        Block controller = world.getBlockAt(0, 64, 0);
        controller.setType(Material.LODESTONE);
        NodeStore.put(controller, NodeBlob.create(DeviceType.MVN_CONTROLLER.name()));
        plugin.networks().registerController(controller);
        Block red = placeItem(1, 64, 0, Items.cable(DyeColor.RED));
        placeItem(2, 64, 0, Items.cable(DyeColor.BLUE));
        Block cell = world.getBlockAt(3, 64, 0);
        cell.setType(DeviceType.MVN_CELL_T1.material());
        NodeStore.put(cell, NodeBlob.create(DeviceType.MVN_CELL_T1.name()));
        Network net = plugin.networks().networkByController(controller.getLocation());
        net.scan();

        assertEquals("RED", NodeStore.get(red).cableColor);
        assertTrue(net.contains(PosUtil.pack(3, 64, 0)), "red and blue cables connect like plain ones");

        server.getPluginManager().callEvent(new BlockBreakEvent(red, player));
        ItemStack dropped = null;
        for (Item item : world.getEntitiesByClass(Item.class)) {
            dropped = item.getItemStack();
        }
        assertNotNull(dropped);
        assertEquals(DyeColor.RED, Items.cableColor(dropped), "it drops a red cable");
        assertEquals(DeviceType.MVN_CABLE, Items.typeOf(dropped));
    }

    @Test
    void takingTheCamouflageBackRestoresTheColour() {
        Block blue = placeItem(5, 64, 5, Items.cable(DyeColor.BLUE));
        new CableCamouflageMenu(plugin, player, blue).openMenu();
        player.setItemOnCursor(new ItemStack(Material.QUARTZ_BLOCK));
        click(CableCamouflageMenu.CAMO_SLOT);
        assertEquals(Material.QUARTZ_BLOCK, blue.getType());
        player.setItemOnCursor(null);
        click(CableCamouflageMenu.CAMO_SLOT);
        assertEquals(Material.BLUE_STAINED_GLASS, blue.getType(), "back to blue, not plain glass");
    }

    @Test
    void sixteenColourRecipesAreRegistered() {
        for (DyeColor color : DyeColor.values()) {
            Recipe recipe = Bukkit.getRecipe(new NamespacedKey(plugin, "cable_" + color.name().toLowerCase(java.util.Locale.ROOT)));
            assertNotNull(recipe, color + " cable recipe");
            assertEquals(4, recipe.getResult().getAmount());
            assertEquals(color, Items.cableColor(recipe.getResult()));
        }
    }

    private void click(int raw) {
        server.getPluginManager().callEvent(new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, raw, ClickType.LEFT, InventoryAction.PICKUP_ALL));
    }
}

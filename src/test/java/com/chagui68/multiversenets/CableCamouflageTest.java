package com.chagui68.multiversenets;

import com.chagui68.multiversenets.gui.CableCamouflageMenu;
import com.chagui68.multiversenets.item.Camouflage;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Cable camouflage: a cable holds a block without consuming it and looks like it, still works,
 * turns back to glass when the block is taken back, and drops the block when broken.
 * [ES] Camuflaje de cables: un cable guarda un bloque sin consumirlo y toma su aspecto, sigue
 * funcionando, vuelve a ser de cristal al recuperar el bloque y lo suelta al romperse.
 */
class CableCamouflageTest {

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
        if (type == DeviceType.MVN_CONTROLLER) {
            plugin.networks().registerController(block);
        } else {
            plugin.networks().invalidateNear(block);
        }
        return block;
    }

    private void click(int raw, ClickType type) {
        server.getPluginManager().callEvent(new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, raw, type,
                type.isShiftClick() ? InventoryAction.MOVE_TO_OTHER_INVENTORY : InventoryAction.PICKUP_ALL));
    }

    private Network network(Block controller) {
        Network net = plugin.networks().networkByController(controller.getLocation());
        net.scan();
        return net;
    }

    @Test
    void rightClickOpensTheCableMenu() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Block cable = place(1, 64, 0, DeviceType.MVN_CABLE);
        network(controller);
        player.getInventory().setItemInMainHand(null);
        server.getPluginManager().callEvent(new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, cable,
                BlockFace.NORTH, EquipmentSlot.HAND, null));
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof CableCamouflageMenu);
    }

    @Test
    void theBlockIsKeptNotConsumedAndComesBack() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Block cable = place(1, 64, 0, DeviceType.MVN_CABLE);
        Block beyond = place(2, 64, 0, DeviceType.MVN_CELL_T1);
        Network net = network(controller);
        new CableCamouflageMenu(plugin, player, cable).openMenu();

        player.setItemOnCursor(new ItemStack(Material.QUARTZ_BLOCK, 3));
        click(CableCamouflageMenu.CAMO_SLOT, ClickType.LEFT);

        assertEquals(Material.QUARTZ_BLOCK, cable.getType(), "the cable looks like quartz");
        assertEquals(2, player.getItemOnCursor().getAmount(), "one block went into the cable");
        assertEquals(Material.QUARTZ_BLOCK, NodeStore.get(cable).camouflage.getType());
        net.scan();
        assertTrue(net.contains(PosUtil.pack(beyond.getX(), beyond.getY(), beyond.getZ())),
                "a camouflaged cable still connects");

        player.setItemOnCursor(null);
        click(CableCamouflageMenu.CAMO_SLOT, ClickType.LEFT);
        assertEquals(Material.QUARTZ_BLOCK, player.getItemOnCursor().getType(), "the block comes back");
        assertEquals(1, player.getItemOnCursor().getAmount());
        assertEquals(Material.GLASS, cable.getType(), "and the cable is glass again");
        assertNull(NodeStore.get(cable).camouflage);
    }

    @Test
    void shiftClickFromTheInventorySwapsAndReturnsTheOldBlock() {
        Block cable = place(5, 64, 5, DeviceType.MVN_CABLE);
        new CableCamouflageMenu(plugin, player, cable).openMenu();
        int bricks = shiftFromInventory(0, new ItemStack(Material.STONE_BRICKS, 5));
        assertEquals(Material.STONE_BRICKS, cable.getType());
        assertEquals(4, player.getInventory().getItem(bricks).getAmount());

        shiftFromInventory(1, new ItemStack(Material.OAK_PLANKS, 1));
        assertEquals(Material.OAK_PLANKS, cable.getType(), "swapped");
        int bricksBack = 0;
        for (ItemStack it : player.getInventory().getContents()) {
            if (it != null && it.getType() == Material.STONE_BRICKS) bricksBack += it.getAmount();
        }
        assertEquals(5, bricksBack, "the stone bricks came back");
    }

    /** Shift-clicks the n-th slot of the player's inventory under the menu; returns its inventory index. */
    private int shiftFromInventory(int offset, ItemStack item) {
        InventoryClickEvent event = new InventoryClickEvent(player.getOpenInventory(), InventoryType.SlotType.CONTAINER,
                CableCamouflageMenu.SIZE + offset, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        player.getInventory().setItem(event.getSlot(), item);
        server.getPluginManager().callEvent(event);
        return event.getSlot();
    }

    @Test
    void unstableOrInteractiveBlocksAreRefused() {
        assertTrue(Camouflage.allowed(Material.QUARTZ_BLOCK));
        assertTrue(Camouflage.allowed(Material.STONE_BRICKS));
        assertTrue(Camouflage.allowed(Material.OAK_PLANKS));
        assertTrue(Camouflage.allowed(Material.WAXED_COPPER_BLOCK));
        assertFalse(Camouflage.allowed(Material.SAND), "falls");
        assertFalse(Camouflage.allowed(Material.CHEST), "has an inventory");
        assertFalse(Camouflage.allowed(Material.FURNACE), "has an inventory");
        assertFalse(Camouflage.allowed(Material.GRASS_BLOCK), "changes by itself");
        assertFalse(Camouflage.allowed(Material.COPPER_BLOCK), "oxidizes");
        assertFalse(Camouflage.allowed(Material.TNT));
        assertFalse(Camouflage.allowed(Material.TORCH), "not a full block");
        assertFalse(Camouflage.allowed(Items.create(DeviceType.MVN_CELL_T1)), "a network device is not camouflage");
    }

    @Test
    void breakingTheCableDropsTheCableAndTheBlock() {
        Block cable = place(5, 64, 5, DeviceType.MVN_CABLE);
        new CableCamouflageMenu(plugin, player, cable).openMenu();
        player.setItemOnCursor(new ItemStack(Material.QUARTZ_BLOCK));
        click(CableCamouflageMenu.CAMO_SLOT, ClickType.LEFT);
        player.closeInventory();

        server.getPluginManager().callEvent(new BlockBreakEvent(cable, player));

        List<Material> drops = new ArrayList<>();
        for (Item item : world.getEntitiesByClass(Item.class)) {
            drops.add(item.getItemStack().getType());
        }
        assertTrue(drops.contains(Material.QUARTZ_BLOCK), "the camouflage block drops");
        assertTrue(drops.contains(Material.GLASS), "and the cable item");
        assertNull(NodeStore.get(cable));
    }
}

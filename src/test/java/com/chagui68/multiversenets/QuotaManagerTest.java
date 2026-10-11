package com.chagui68.multiversenets;

import com.chagui68.multiversenets.gui.QuotaManagerMenu;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Quota Manager: lists the network's Quota Limiters, adds an item to a free one (never without
 * one), edits and removes; and a Grabber empties an Infinity Barrel that is not part of the network.
 * [ES] Quota Manager: lista los Quota Limiters de la red, añade un ítem a uno libre (nunca sin uno
 * libre), edita y quita; y un Grabber vacía un Infinity Barrel que no es de la red.
 */
class QuotaManagerTest {

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

    private Network network(Block controller) {
        Network net = plugin.networks().networkByController(controller.getLocation());
        net.scan();
        return net;
    }

    private void rightClick(Block block, ItemStack held) {
        player.getInventory().setItemInMainHand(held);
        server.getPluginManager().callEvent(new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, held, block,
                BlockFace.NORTH, EquipmentSlot.HAND, null));
    }

    private void click(int raw, ClickType type) {
        server.getPluginManager().callEvent(new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, raw, type, InventoryAction.PICKUP_ALL));
    }

    @Test
    void itemsGoToFreeLimitersOnlyAndCanBeEditedAndRemoved() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Block bay = place(1, 64, 0, DeviceType.MVN_DRAM_BAY);
        Block diamonds = place(0, 64, 1, DeviceType.MVN_LIMITER);
        Block spare = place(0, 64, -1, DeviceType.MVN_LIMITER);
        Block manager = place(-1, 64, 0, DeviceType.MVN_QUOTA_MANAGER);
        NodeBlob diamondBlob = NodeStore.get(diamonds);
        diamondBlob.quotaSample = new ItemStack(Material.DIAMOND);
        diamondBlob.quotaLimit = 100;
        NodeStore.put(diamonds, diamondBlob);
        Network net = network(controller);
        rightClick(bay, Items.create(DeviceType.MVN_CACHE_L3));

        player.getInventory().setItemInMainHand(null);
        rightClick(manager, null);
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof QuotaManagerMenu,
                "right-click opens the Quota Manager");
        assertEquals(Material.DIAMOND, player.getOpenInventory().getTopInventory()
                .getItem(QuotaManagerMenu.FIRST_ENTRY).getType(), "the configured limiter is listed");

        // Anadir: ocupa el limitador libre y el item del cursor no se gasta.
        player.setItemOnCursor(new ItemStack(Material.COBBLESTONE, 5));
        click(QuotaManagerMenu.ADD_SLOT, ClickType.LEFT);
        NodeBlob spareBlob = NodeStore.get(spare);
        assertEquals(Material.COBBLESTONE, spareBlob.quotaSample.getType());
        assertEquals(QuotaManagerMenu.DEFAULT_LIMIT, spareBlob.quotaLimit);
        assertEquals(5, player.getItemOnCursor().getAmount(), "the item is only copied");

        // Sin limitador libre no se anade nada.
        player.setItemOnCursor(new ItemStack(Material.DIRT));
        click(QuotaManagerMenu.ADD_SLOT, ClickType.LEFT);
        assertEquals(Material.COBBLESTONE, NodeStore.get(spare).quotaSample.getType());
        assertEquals(Material.DIAMOND, NodeStore.get(diamonds).quotaSample.getType(), "nothing was overwritten");
        player.setItemOnCursor(null);

        // A-Z: COBBLESTONE primero. Clic derecho lo quita y el limitador queda libre.
        assertEquals(Material.COBBLESTONE, player.getOpenInventory().getTopInventory()
                .getItem(QuotaManagerMenu.FIRST_ENTRY).getType());
        click(QuotaManagerMenu.FIRST_ENTRY, ClickType.RIGHT);
        assertNull(NodeStore.get(spare).quotaSample, "removed: the limiter is free again");

        // Shift+clic derecho desactiva el de diamantes.
        click(QuotaManagerMenu.FIRST_ENTRY, ClickType.SHIFT_RIGHT);
        assertFalse(NodeStore.get(diamonds).quotaActive);
        assertEquals(0, net.storage().deposit(new ItemStack(Material.DIAMOND, 200)), "a disabled limiter does not limit");
    }

    @Test
    void aGrabberEmptiesABarrelThatIsNotPartOfTheNetwork() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Block bay = place(1, 64, 0, DeviceType.MVN_DRAM_BAY);
        place(-1, 64, 0, DeviceType.MVN_GRABBER);
        Block barrel = place(-2, 64, 0, DeviceType.MVN_INFINITY_BARREL);
        NodeBlob barrelBlob = NodeStore.get(barrel);
        barrelBlob.cellSample = new ItemStack(Material.COBBLESTONE);
        barrelBlob.cellAmount = 500;
        NodeStore.put(barrel, barrelBlob);
        Network net = network(controller);
        rightClick(bay, Items.create(DeviceType.MVN_CACHE_L3));

        assertFalse(net.contains(PosUtil.pack(-2, 64, 0)), "the grabber does not pull the barrel into the network");
        server.getScheduler().performTicks(200);

        assertEquals(500, net.storage().count(i -> i.getType() == Material.COBBLESTONE), "the grabber emptied it");
        assertEquals(0, NodeStore.get(barrel).cellAmount);
        assertEquals(Material.COBBLESTONE, NodeStore.get(barrel).cellSample.getType(), "a barrel keeps its item");
    }

    @Test
    void aBarrelWiredByCableStaysNetworkStorage() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(-1, 64, 0, DeviceType.MVN_GRABBER);
        Block barrel = place(-2, 64, 0, DeviceType.MVN_INFINITY_BARREL);
        place(0, 64, 1, DeviceType.MVN_CABLE);
        place(-1, 64, 1, DeviceType.MVN_CABLE);
        place(-2, 64, 1, DeviceType.MVN_CABLE);
        NodeBlob barrelBlob = NodeStore.get(barrel);
        barrelBlob.cellSample = new ItemStack(Material.COBBLESTONE);
        barrelBlob.cellAmount = 500;
        NodeStore.put(barrel, barrelBlob);
        Network net = network(controller);

        assertTrue(net.contains(PosUtil.pack(-2, 64, 0)), "reached by cable, the barrel is storage");
        server.getScheduler().performTicks(200);
        assertEquals(500, NodeStore.get(barrel).cellAmount, "the grabber never pulls from its own network");
        assertEquals(500, net.storage().count(i -> i.getType() == Material.COBBLESTONE));
    }
}

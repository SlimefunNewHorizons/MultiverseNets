package com.chagui68.multiversenets;

import com.chagui68.multiversenets.gui.BarrelMenu;
import com.chagui68.multiversenets.gui.TerminalMenu;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [EN] A Quota Limiter caps automatic imports, never a player depositing by hand (Terminal or a storage's menu).
 * [ES] Un Quota Limiter limita la importación automática, nunca al jugador que deposita a mano.
 */
class QuotaLimiterMenusTest {

    private static final int LIMIT = 100;

    private ServerMock server;
    private MultiverseNets plugin;
    private WorldMock world;
    private PlayerMock player;
    private Network net;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(MultiverseNets.class);
        world = server.addSimpleWorld("world");
        player = server.addPlayer();

        Block ctrl = world.getBlockAt(0, 64, 0);
        ctrl.setType(Material.LODESTONE);
        NodeStore.put(ctrl, NodeBlob.create(DeviceType.MVN_CONTROLLER.name()));
        plugin.networks().registerController(ctrl);

        Block limiter = world.getBlockAt(-1, 64, 0);
        limiter.setType(Material.TARGET);
        NodeBlob quota = NodeBlob.create(DeviceType.MVN_LIMITER.name());
        quota.quotaSample = new ItemStack(Material.DIAMOND);
        quota.quotaLimit = LIMIT;
        quota.quotaActive = true;
        NodeStore.put(limiter, quota);

        net = plugin.networks().networkAt(ctrl);
        net.scan();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Block place(int x, DeviceType type, Material material) {
        Block block = world.getBlockAt(x, 64, 0);
        block.setType(material);
        NodeStore.put(block, NodeBlob.create(type.name()));
        net.scan();
        return block;
    }

    private void click(int rawSlot, ClickType click) {
        server.getPluginManager().callEvent(new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, rawSlot, click, InventoryAction.MOVE_TO_OTHER_INVENTORY));
    }

    private long diamonds() {
        return net.storage().count(i -> i.getType() == Material.DIAMOND);
    }

    @Test
    void automaticImportsStopAtTheQuota() {
        place(1, DeviceType.MVN_CELL_T1, Material.TERRACOTTA);
        assertEquals(28, net.storage().deposit(new ItemStack(Material.DIAMOND, 128)));
        assertEquals(LIMIT, diamonds());
    }

    @Test
    void terminalShiftClickIgnoresTheQuota() {
        place(1, DeviceType.MVN_CELL_T1, Material.TERRACOTTA);
        new TerminalMenu(plugin, player, net).openMenu();

        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 64));
        click(54, ClickType.SHIFT_LEFT);
        player.getInventory().setItem(1, new ItemStack(Material.DIAMOND, 64));
        click(55, ClickType.SHIFT_LEFT);

        assertEquals(128, diamonds());
        assertNull(player.getInventory().getItem(1));
    }

    @Test
    void storageMenuIgnoresTheQuota() {
        Block barrel = place(1, DeviceType.MVN_INFINITY_BARREL, Material.BARREL);
        NodeBlob blob = NodeStore.get(barrel);
        blob.cellSample = new ItemStack(Material.DIAMOND);
        NodeStore.put(barrel, blob);
        new BarrelMenu(plugin, player, barrel).openMenu();

        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 64));
        player.getInventory().setItem(1, new ItemStack(Material.DIAMOND, 64));
        click(BarrelMenu.DEPOSIT_ALL_SLOT, ClickType.LEFT);

        assertEquals(128, NodeStore.get(barrel).cellAmount);
    }
}

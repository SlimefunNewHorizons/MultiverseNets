package com.chagui68.multiversenets;

import com.chagui68.multiversenets.gui.ModuleLoaderMenu;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.MemoryModules;
import com.chagui68.multiversenets.net.ModuleIO;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] DRAM Unloader empties cards into the network as far as it allows; DRAM Loader fills a card
 * with one item up to a chosen amount and stops.
 * [ES] El DRAM Unloader vacía tarjetas en la red hasta donde esta admite; el DRAM Loader llena una
 * tarjeta con un ítem hasta la cantidad elegida y se para.
 */
class ModuleIOTest {

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

    private static ItemStack card(DeviceType type, Material material, long amount) {
        NodeBlob cargo = NodeBlob.create(type.name());
        cargo.addVirtualItem(new ItemStack(material), amount);
        return MemoryModules.moduleItem(type, cargo);
    }

    private static long count(Network net, Material material) {
        return net.storage().count(i -> i.getType() == material);
    }

    @Test
    void theUnloaderEmptiesACardAndItsStockIsNotNetworkStorage() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Block bay = place(1, 64, 0, DeviceType.MVN_DRAM_BAY);
        Block unloader = place(-1, 64, 0, DeviceType.MVN_MODULE_UNLOADER);
        Network net = network(controller);
        rightClick(bay, Items.create(DeviceType.MVN_CACHE_L3));

        rightClick(unloader, card(DeviceType.MVN_CACHE_L2, Material.COBBLESTONE, 5000));
        assertEquals(1, MemoryModules.modules(NodeStore.get(unloader)).size(), "the card went in");
        assertEquals(0, count(net, Material.COBBLESTONE), "a card in the Unloader is not network storage");

        server.getScheduler().performTicks(200);

        assertEquals(5000, count(net, Material.COBBLESTONE), "the whole stock is in the network");
        assertEquals(0, ModuleIO.stored(MemoryModules.modules(NodeStore.get(unloader)).get(0)), "the card is empty");
    }

    @Test
    void whatTheNetworkCanNotTakeStaysOnTheCard() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Block bay = place(1, 64, 0, DeviceType.MVN_DRAM_BAY);
        Network net = network(controller);
        rightClick(bay, Items.create(DeviceType.MVN_CACHE_L1));
        long cap = MemoryModules.itemCapacity(DeviceType.MVN_CACHE_L1);

        NodeBlob unloader = NodeBlob.create(DeviceType.MVN_MODULE_UNLOADER.name());
        ModuleIO.install(unloader, card(DeviceType.MVN_CACHE_L3, Material.DIRT, cap + 500));
        ModuleIO.unload(net, unloader);
        ModuleIO.unload(net, unloader);

        assertEquals(cap, count(net, Material.DIRT), "the network is full");
        assertEquals(500, ModuleIO.stored(MemoryModules.modules(unloader).get(0)), "the rest waits on the card");
    }

    @Test
    void theLoaderFillsACardUpToTheAmountAndStops() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Block bay = place(1, 64, 0, DeviceType.MVN_DRAM_BAY);
        Network net = network(controller);
        rightClick(bay, Items.create(DeviceType.MVN_CACHE_L3));
        assertEquals(0, net.storage().deposit(new ItemStack(Material.COBBLESTONE, 10_000)));

        NodeBlob loader = NodeBlob.create(DeviceType.MVN_MODULE_LOADER.name());
        ModuleIO.install(loader, Items.create(DeviceType.MVN_CACHE_L2));
        loader.loaderTarget = new ItemStack(Material.COBBLESTONE);
        loader.loaderAmount = 6_000;
        loader.loaderActive = true;
        for (int i = 0; i < 5 && loader.loaderActive; i++) {
            ModuleIO.load(net, loader);
        }

        assertFalse(loader.loaderActive, "it stops by itself");
        assertEquals(6_000, ModuleIO.loadedAmount(loader));
        assertEquals(4_000, count(net, Material.COBBLESTONE), "taken from the network");

        // Mas de lo que cabe: se para con la tarjeta llena.
        loader.loaderAmount = 1_000_000;
        loader.loaderActive = true;
        for (int i = 0; i < 5 && loader.loaderActive; i++) {
            ModuleIO.load(net, loader);
        }
        assertEquals(MemoryModules.itemCapacity(DeviceType.MVN_CACHE_L2), ModuleIO.loadedAmount(loader), "full card");
    }

    @Test
    void theLoaderMenuSetsTheTargetAndStarts() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Block bay = place(1, 64, 0, DeviceType.MVN_DRAM_BAY);
        Block loader = place(-1, 64, 0, DeviceType.MVN_MODULE_LOADER);
        Network net = network(controller);
        rightClick(bay, Items.create(DeviceType.MVN_CACHE_L3));
        assertEquals(0, net.storage().deposit(new ItemStack(Material.COBBLESTONE, 3_000)));

        rightClick(loader, Items.create(DeviceType.MVN_CACHE_L1));
        assertEquals(1, MemoryModules.modules(NodeStore.get(loader)).size(), "the card went in");
        assertFalse(NodeStore.get(loader).loaderActive, "inserting a card does nothing by itself");

        player.getInventory().setItemInMainHand(null);
        new ModuleLoaderMenu(plugin, player, loader).openMenu();
        player.setItemOnCursor(new ItemStack(Material.COBBLESTONE));
        click(ModuleLoaderMenu.TARGET_SLOT);
        player.setItemOnCursor(null);
        click(ModuleLoaderMenu.START_SLOT);
        assertTrue(NodeStore.get(loader).loaderActive, "started");

        server.getScheduler().performTicks(200);
        NodeBlob after = NodeStore.get(loader);
        assertFalse(after.loaderActive);
        assertEquals(MemoryModules.itemCapacity(DeviceType.MVN_CACHE_L1), ModuleIO.loadedAmount(after),
                "no amount set: the card is filled");
    }

    @Test
    void amountsAreReadLikePeopleWriteThem() {
        assertEquals(1_000_000, ModuleIO.parseAmount("1m"));
        assertEquals(500_000, ModuleIO.parseAmount("500k"));
        assertEquals(2_500_000, ModuleIO.parseAmount("2.5M"));
        assertEquals(1_000_000, ModuleIO.parseAmount("1,000,000"));
        assertEquals(0, ModuleIO.parseAmount("max"));
        assertEquals(-1, ModuleIO.parseAmount("lots"));
    }

    private void click(int raw) {
        server.getPluginManager().callEvent(new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, raw, ClickType.LEFT, InventoryAction.PICKUP_ALL));
    }
}

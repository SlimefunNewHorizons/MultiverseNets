package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.StackUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.type.Chest;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Container Connector: vanilla containers touching it are network storage, used last, moved
 * exactly as they are, and never looped by the network's own Grabbers and Pushers.
 * [ES] Container Connector: los contenedores vanilla que lo tocan son almacenamiento de la red, el
 * último, se mueven tal cual y los Grabbers y Pushers de la propia red no hacen bucles con ellos.
 */
class ContainerConnectorTest {

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

    private Inventory container(int x, int y, int z, Material type) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(type);
        return ((Container) block.getState()).getInventory();
    }

    private Network network(Block controller) {
        Network net = plugin.networks().networkByController(controller.getLocation());
        net.scan();
        return net;
    }

    private static int countIn(Inventory inv, Material type) {
        int total = 0;
        for (ItemStack it : inv.getContents()) {
            if (it != null && it.getType() == type) {
                total += it.getAmount();
            }
        }
        return total;
    }

    @Test
    void aChestTouchingTheConnectorIsNetworkStorage() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 64, 0, DeviceType.MVN_CONTAINER_CONNECTOR);
        Inventory chest = container(2, 64, 0, Material.CHEST);
        chest.addItem(new ItemStack(Material.DIAMOND, 10));
        Inventory loose = container(0, 64, 5, Material.CHEST);
        loose.addItem(new ItemStack(Material.DIAMOND, 7));
        Network net = network(controller);

        assertEquals(10, net.storage().count(i -> i.getType() == Material.DIAMOND),
                "only the chest touching the connector counts");
        assertTrue(net.storage().view().stream().anyMatch(v -> v.sample().getType() == Material.DIAMOND
                && v.amount() == 10), "the Terminal sees it");

        ItemStack taken = net.storage().withdraw(i -> i.getType() == Material.DIAMOND, 4);
        assertNotNull(taken);
        assertEquals(4, taken.getAmount());
        assertEquals(6, countIn(chest, Material.DIAMOND), "taken out of the chest");
        assertEquals(6, net.storage().breakdown(new ItemStack(Material.DIAMOND)).containers());
        assertEquals(7, countIn(loose, Material.DIAMOND), "a chest that is not linked is never touched");
    }

    @Test
    void containersAreTheLastStorageAndKeepWhatDoesNotFit() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 64, 0, DeviceType.MVN_CONTAINER_CONNECTOR);
        Block cell = place(0, 64, 1, DeviceType.MVN_CELL_T1);
        Inventory chest = container(2, 64, 0, Material.CHEST);
        Network net = network(controller);

        assertEquals(0, net.storage().deposit(new ItemStack(Material.COBBLESTONE, 64)));
        assertEquals(64, NodeStore.get(cell).cellAmount, "the network's own storage first");
        assertEquals(0, countIn(chest, Material.COBBLESTONE));

        // Sin la celda, todo va al cofre; lo que no cabe se devuelve.
        world.getBlockAt(0, 64, 1).setType(Material.AIR);
        NodeStore.remove(world.getBlockAt(0, 64, 1));
        net.scan();
        int slots = chest.getSize();
        int left = net.storage().deposit(new ItemStack(Material.DIRT, 64 * slots + 10));
        assertEquals(10, left, "a full chest returns the rest");
        assertEquals(64 * slots, countIn(chest, Material.DIRT));
    }

    @Test
    void stacksOfTheSameItemAreToppedUpBeforeEmptySlots() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 64, 0, DeviceType.MVN_CONTAINER_CONNECTOR);
        Inventory chest = container(2, 64, 0, Material.CHEST);
        chest.setItem(5, new ItemStack(Material.IRON_INGOT, 60));
        Network net = network(controller);

        assertEquals(0, net.storage().deposit(new ItemStack(Material.IRON_INGOT, 10)));
        assertEquals(64, chest.getItem(5).getAmount(), "the partial stack is filled first");
        assertEquals(6, chest.getItem(0).getAmount(), "the rest goes to the first empty slot");
    }

    @Test
    void aShulkerBoxNeverGoesIntoAShulkerBox() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 64, 0, DeviceType.MVN_CONTAINER_CONNECTOR);
        Inventory shulker = container(2, 64, 0, Material.SHULKER_BOX);
        Network net = network(controller);

        assertEquals(1, net.storage().deposit(new ItemStack(Material.RED_SHULKER_BOX)), "refused");
        assertEquals(0, countIn(shulker, Material.RED_SHULKER_BOX));
        assertEquals(0, net.storage().deposit(new ItemStack(Material.COBBLESTONE, 5)));
        assertEquals(5, countIn(shulker, Material.COBBLESTONE), "anything else is stored");
    }

    @Test
    void itemsComeOutExactlyAsTheyWereStored() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 64, 0, DeviceType.MVN_CONTAINER_CONNECTOR);
        Inventory chest = container(2, 64, 0, Material.BARREL);
        ItemStack special = named(Material.DIAMOND_SWORD, true);
        ItemStack plain = named(Material.DIAMOND_SWORD, false);
        assertTrue(StackUtils.itemsMatch(special, plain) && !special.isSimilar(plain),
                "precondition: the network's loose match sees both swords as one item");
        chest.setItem(0, special);
        chest.setItem(1, plain);
        Network net = network(controller);

        ItemStack taken = net.storage().withdraw(i -> StackUtils.itemsMatch(i, plain), 2);
        assertNotNull(taken);
        assertEquals(1, taken.getAmount(), "two different swords are never merged into one stack");
        assertTrue(taken.isSimilar(special));
        assertTrue(chest.getItem(1).isSimilar(plain), "the other sword stays as it was");
    }

    private static ItemStack named(Material type, boolean unbreakable) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text("Blade"));
        meta.setUnbreakable(unbreakable);
        item.setItemMeta(meta);
        return item;
    }

    @Test
    void aBrokenChestLeavesTheStorageAtOnce() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 64, 0, DeviceType.MVN_CONTAINER_CONNECTOR);
        container(2, 64, 0, Material.CHEST).addItem(new ItemStack(Material.DIAMOND, 10));
        Network net = network(controller);
        assertEquals(10, net.storage().count(i -> i.getType() == Material.DIAMOND));

        world.getBlockAt(2, 64, 0).setType(Material.AIR);
        assertEquals(0, net.storage().count(i -> i.getType() == Material.DIAMOND), "same tick, no rescan");
        assertEquals(5, net.storage().deposit(new ItemStack(Material.DIAMOND, 5)), "nothing goes into a gone chest");
    }

    @Test
    void grabbersAndPushersOfTheNetworkLeaveLinkedContainersAlone() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 64, 0, DeviceType.MVN_CONTAINER_CONNECTOR);
        place(0, 64, 1, DeviceType.MVN_CELL_T1);
        // Una celda vacia: sin la regla, el Grabber vaciaria el cofre en ella.
        place(-1, 64, 0, DeviceType.MVN_CELL_T1);
        place(1, 64, 1, DeviceType.MVN_CABLE);
        place(1, 64, -1, DeviceType.MVN_CABLE);
        Block grabber = place(2, 64, 1, DeviceType.MVN_GRABBER);
        NodeBlob grabberBlob = NodeStore.get(grabber);
        grabberBlob.filterBlacklist = true; // blacklist vacia = importa todo
        NodeStore.put(grabber, grabberBlob);
        Block pusher = place(2, 64, -1, DeviceType.MVN_PUSHER);
        NodeBlob pusherBlob = NodeStore.get(pusher);
        pusherBlob.filterMaterials = new java.util.ArrayList<>(List.of("GOLD_INGOT"));
        NodeStore.put(pusher, pusherBlob);
        Inventory chest = container(2, 64, 0, Material.CHEST);
        chest.addItem(new ItemStack(Material.DIAMOND, 10));
        Network net = network(controller);
        assertTrue(net.linksContainer(world.getBlockAt(2, 64, 0)));
        assertEquals(0, net.storage().deposit(new ItemStack(Material.GOLD_INGOT, 20)));
        assertEquals(0, countIn(chest, Material.GOLD_INGOT), "the gold went to a cell");

        server.getScheduler().performTicks(200);

        assertEquals(10, countIn(chest, Material.DIAMOND), "the grabber does not pull from its own storage");
        assertEquals(0, net.storage().breakdown(new ItemStack(Material.DIAMOND)).cells(), "no diamonds moved into a cell");
        assertEquals(0, countIn(chest, Material.GOLD_INGOT), "the pusher does not push into its own storage");
        assertEquals(20, net.storage().count(i -> i.getType() == Material.GOLD_INGOT));
    }

    @Test
    void aChestPlacedNextToTheConnectorJoinsOnTheNextScan() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 64, 0, DeviceType.MVN_CONTAINER_CONNECTOR);
        Network net = network(controller);
        assertFalse(net.isStale());

        Block block = world.getBlockAt(1, 65, 0);
        org.bukkit.block.BlockState replaced = block.getState();
        Inventory chest = container(1, 65, 0, Material.CHEST);
        chest.addItem(new ItemStack(Material.EMERALD, 3));
        server.getPluginManager().callEvent(new BlockPlaceEvent(block, replaced, world.getBlockAt(1, 64, 0),
                new ItemStack(Material.CHEST), server.addPlayer(), true, EquipmentSlot.HAND));
        assertTrue(net.isStale(), "placing a chest next to the connector asks for a rescan");

        net.scan();
        assertEquals(3, net.storage().count(i -> i.getType() == Material.EMERALD));
    }

    @Test
    void handEditsOfALinkedChestChangeTheStamp() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 64, 0, DeviceType.MVN_CONTAINER_CONNECTOR);
        Inventory chest = container(2, 64, 0, Material.CHEST);
        Network net = network(controller);
        long before = net.storage().changeStamp();

        chest.addItem(new ItemStack(Material.DIAMOND, 1));
        server.getScheduler().performTicks(1);
        assertNotEquals(before, net.storage().changeStamp(), "an open Terminal redraws");
    }

    @Test
    void storageBlocksAndDoubleChestSides() {
        assertTrue(VanillaStorage.isStorageMaterial(Material.CHEST));
        assertTrue(VanillaStorage.isStorageMaterial(Material.TRAPPED_CHEST));
        assertTrue(VanillaStorage.isStorageMaterial(Material.BARREL));
        assertTrue(VanillaStorage.isStorageMaterial(Material.LIME_SHULKER_BOX));
        assertFalse(VanillaStorage.isStorageMaterial(Material.ENDER_CHEST), "per-player, not a block inventory");
        assertFalse(VanillaStorage.isStorageMaterial(Material.HOPPER));
        assertFalse(VanillaStorage.isStorageMaterial(Material.FURNACE));
        // ChestBlock.getConnectedDirection: LEFT a la derecha del frente, RIGHT a la izquierda.
        assertEquals(BlockFace.EAST, VanillaStorage.partnerFace(BlockFace.NORTH, Chest.Type.LEFT));
        assertEquals(BlockFace.WEST, VanillaStorage.partnerFace(BlockFace.NORTH, Chest.Type.RIGHT));
        assertEquals(BlockFace.SOUTH, VanillaStorage.partnerFace(BlockFace.EAST, Chest.Type.LEFT));
        assertEquals(BlockFace.WEST, VanillaStorage.partnerFace(BlockFace.SOUTH, Chest.Type.LEFT));
        assertEquals(BlockFace.NORTH, VanillaStorage.partnerFace(BlockFace.WEST, Chest.Type.LEFT));
    }

    @Test
    void aNetworkDeviceIsNeverLinkedAsAContainer() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 64, 0, DeviceType.MVN_CONTAINER_CONNECTOR);
        Block barrel = place(2, 64, 0, DeviceType.MVN_INFINITY_BARREL);
        Network net = network(controller);

        assertFalse(net.linksContainer(barrel), "an Infinity Barrel is a vanilla barrel inside, but a device");
        assertNull(VanillaStorage.at(barrel));
    }
}

package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] The caches added for the network loop must never serve something stale: the per-tick cell
 * list, the filter predicates, the position-to-network index and the throughput counters.
 *
 * [ES] Las cachés del bucle de red nunca deben servir algo viejo: la lista de celdas por tick, los
 * predicados de filtro, el índice posición-red y los contadores de flujo.
 */
class StorageCachingTest {

    private ServerMock server;
    private MultiverseNets plugin;
    private WorldMock world;
    private NetworkManager manager;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(MultiverseNets.class);
        world = server.addSimpleWorld("world");
        manager = plugin.networks();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Block place(int x, DeviceType type) {
        Block block = world.getBlockAt(x, 64, 0);
        block.setType(type.material());
        NodeStore.put(block, NodeBlob.create(type.name()));
        if (type == DeviceType.MVN_CONTROLLER) {
            manager.registerController(block);
        } else {
            manager.invalidateNear(block);
        }
        return block;
    }

    private Network network() {
        return manager.networkFor(world, PosUtil.pack(0, 64, 0));
    }

    @Test
    @DisplayName("a cell rewritten by a menu (a new copy) is seen by the very next operation of the tick")
    void menuWriteIsSeenWithinTheTick() {
        place(0, DeviceType.MVN_CONTROLLER);
        Block cell = place(1, DeviceType.MVN_CELL_T1);
        Network net = network();

        assertEquals(0, net.storage().deposit(new ItemStack(Material.STONE, 10)));
        assertEquals(10, net.storage().count(item -> item.getType() == Material.STONE));

        // Lo que hace un menu: copia, cambia, escribe la copia.
        NodeBlob copy = NodeStore.get(cell);
        copy.cellAmount = 3;
        NodeStore.put(cell, copy);

        assertEquals(3, net.storage().count(item -> item.getType() == Material.STONE),
                "the cached cell list must not hide a write made in the same tick");
        ItemStack out = net.storage().withdraw(item -> item.getType() == Material.STONE, 64);
        assertEquals(3, out.getAmount());
        assertNull(net.storage().withdraw(item -> item.getType() == Material.STONE, 1));
    }

    @Test
    @DisplayName("the storage change stamp moves on deposits and withdrawals, not on reads")
    void changeStampFollowsChanges() {
        place(0, DeviceType.MVN_CONTROLLER);
        place(1, DeviceType.MVN_CELL_T1);
        Network net = network();

        long before = net.storage().changeStamp();
        net.storage().count(item -> true);
        net.storage().view();
        assertEquals(before, net.storage().changeStamp(), "reading changes nothing");

        net.storage().deposit(new ItemStack(Material.DIRT, 5));
        long afterDeposit = net.storage().changeStamp();
        assertNotEquals(before, afterDeposit);

        net.storage().withdraw(item -> item.getType() == Material.DIRT, 2);
        assertNotEquals(afterDeposit, net.storage().changeStamp());
    }

    @Test
    @DisplayName("a filter predicate is reused for the same blob and rebuilt when the filter changes")
    void filterPredicateFollowsTheFilter() {
        NodeBlob blob = NodeBlob.create(DeviceType.MVN_PUSHER.name());
        blob.filterItems.add(new ItemStack(Material.STONE));

        Predicate<ItemStack> first = NetworkManager.filterPredicate(blob);
        assertSame(first, NetworkManager.filterPredicate(blob), "same blob, same filter: reused");
        assertTrue(first.test(new ItemStack(Material.STONE)));

        blob.filterItems.add(new ItemStack(Material.DIRT));
        Predicate<ItemStack> grown = NetworkManager.filterPredicate(blob);
        assertTrue(grown.test(new ItemStack(Material.DIRT)), "an entry added in place is honoured");

        blob.filterBlacklist = true;
        assertFalse(NetworkManager.filterPredicate(blob).test(new ItemStack(Material.STONE)),
                "switching to blacklist is honoured");
    }

    @Test
    @DisplayName("networkAt follows rescans and controller removal")
    void networkAtFollowsTopology() {
        Block controller = place(0, DeviceType.MVN_CONTROLLER);
        Block cable = place(1, DeviceType.MVN_CABLE);
        Network net = network();
        assertSame(net, manager.networkAt(cable));

        NodeStore.remove(cable);
        manager.invalidateNear(cable);
        assertNull(manager.networkAt(cable), "a removed cable belongs to no network");

        NodeStore.put(cable, NodeBlob.create(DeviceType.MVN_CABLE.name()));
        manager.invalidateNear(cable);
        assertSame(net, manager.networkAt(cable));

        manager.removeController(controller);
        assertNull(manager.networkAt(cable), "a removed network is never answered from the index");
    }

    @Test
    @DisplayName("throughput counters of positions that left the network are dropped on rescan")
    void throughputCountersArePruned() {
        place(0, DeviceType.MVN_CONTROLLER);
        Block grabber = place(1, DeviceType.MVN_GRABBER);
        Network net = network();
        long pos = PosUtil.pack(1, 64, 0);

        net.throughput().recordFlow(pos, 64);
        assertEquals(64, net.throughput().getNodeTotalTransferred(pos));

        NodeStore.remove(grabber);
        manager.invalidateNear(grabber);
        assertEquals(0, net.throughput().getNodeTotalTransferred(pos));
    }
}

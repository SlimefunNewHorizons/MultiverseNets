package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
import com.chagui68.multiversenets.util.Settings;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] When the ticker rescans a network. A full BFS of every network every second, whether or not
 * anything changed, was one of the plugin's heaviest recurring costs. Now a dirty network is
 * rescanned on the next cycle, a stale one after scan-interval-ticks, and an untouched one only
 * every full-rescan-ticks.
 *
 * [ES] Cuándo reescanea el ticker una red: sucia, en el siguiente ciclo; "quizá cambiada", tras
 * scan-interval-ticks; intacta, solo cada full-rescan-ticks.
 */
class ScanSchedulingTest {

    private ServerMock server;
    private MultiverseNets plugin;
    private WorldMock world;
    private NetworkManager manager;
    private Network net;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(MultiverseNets.class);
        world = server.addSimpleWorld("world");
        manager = plugin.networks();

        Block controller = world.getBlockAt(0, 64, 0);
        controller.setType(DeviceType.MVN_CONTROLLER.material());
        NodeStore.put(controller, NodeBlob.create(DeviceType.MVN_CONTROLLER.name()));
        manager.registerController(controller);
        net = manager.networkFor(world, PosUtil.pack(0, 64, 0));
        plugin.ticker().tick();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** A cable stored without any event: only a rescan can find it. */
    private void sneakCable(int x) {
        Block cable = world.getBlockAt(x, 64, 0);
        cable.setType(Material.GLASS);
        NodeStore.put(cable, NodeBlob.create(DeviceType.MVN_CABLE.name()));
    }

    private void tickFor(int ticks) {
        for (int t = 0; t < ticks; t += 5) {
            plugin.ticker().tick();
        }
    }

    @Test
    @DisplayName("an untouched network is not rescanned every scan-interval-ticks")
    void idleNetworkIsNotRescannedEverySecond() {
        sneakCable(1);
        tickFor(Settings.scanIntervalTicks() * 3);
        assertEquals(1, net.size(), "nothing marked the network, so the hidden cable must not be found yet");
    }

    @Test
    @DisplayName("the periodic full rescan still catches changes no event reported")
    void periodicFullRescanCatchesSilentChanges() {
        sneakCable(1);
        tickFor(Settings.fullRescanTicks() + Settings.fullRescanTicks() / 4 + 5);
        assertEquals(2, net.size(), "the full rescan must index the cable");
    }

    @Test
    @DisplayName("a dirty network is rescanned on the next cycle")
    void dirtyNetworkIsRescannedAtOnce() {
        sneakCable(1);
        net.markDirty();
        plugin.ticker().tick();
        assertEquals(2, net.size());
        assertFalse(net.isDirty());
    }

    @Test
    @DisplayName("a stale network waits scan-interval-ticks since its last scan, then rescans")
    void staleNetworkWaitsForTheInterval() {
        net.scan();
        sneakCable(1);
        net.markStale();
        plugin.ticker().tick();
        assertEquals(1, net.size(), "a stale mark right after a scan must wait for the interval");

        tickFor(Settings.scanIntervalTicks());
        assertEquals(2, net.size());
        assertFalse(net.isStale());
    }

    @Test
    @DisplayName("loading a chunk next to a network marks it stale; a far chunk does not")
    void chunkLoadMarksOnlyNearbyNetworks() {
        manager.chunkLoaded(world.getChunkAt(40, 40));
        assertFalse(net.isStale(), "a chunk far from the network cannot change it");

        manager.chunkLoaded(world.getChunkAt(1, 0));
        assertTrue(net.isStale(), "a bordering chunk can extend the network");
    }
}

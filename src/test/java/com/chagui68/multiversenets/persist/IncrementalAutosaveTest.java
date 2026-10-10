package com.chagui68.multiversenets.persist;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] The autosave spreads its main-thread work over ticks. These tests pin that it still saves
 * everything that was dirty when it started, and that a region the network keeps rewriting cannot
 * keep one autosave running forever.
 *
 * [ES] El autoguardado reparte su trabajo en ticks. Estos tests fijan que sigue guardando todo lo
 * que estaba sucio al empezar, y que una región que la red reescribe sin parar no puede mantener un
 * autoguardado vivo para siempre.
 */
class IncrementalAutosaveTest {

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
        NodeStore.autosaveBudgetNanos = 2_000_000L;
        MockBukkit.unmock();
    }

    private Block cellIn(int regionX, long amount) {
        Block block = world.getBlockAt(regionX * 512 + 3, 64, 3);
        block.setType(Material.GLASS);
        NodeBlob blob = NodeBlob.create(DeviceType.MVN_CELL_T2.name());
        blob.cellSample = new ItemStack(Material.IRON_INGOT);
        blob.cellAmount = amount;
        NodeStore.put(block, blob);
        return block;
    }

    private void runAutosaveToTheEnd() {
        NodeStore.autosave();
        for (int i = 0; i < 200 && NodeStore.autosaveRunning(); i++) {
            server.getScheduler().performOneTick();
        }
        assertFalse(NodeStore.autosaveRunning(), "the autosave must finish");
    }

    @Test
    @DisplayName("every region dirty at the start is saved, even across several ticks")
    void everyDirtyRegionIsSaved() {
        // Sin presupuesto: una region por tick, asi el guardado se reparte de verdad en 12 ticks.
        NodeStore.autosaveBudgetNanos = 0;
        Block[] cells = new Block[12];
        for (int r = 0; r < cells.length; r++) {
            cells[r] = cellIn(r, 1000L + r);
        }

        runAutosaveToTheEnd();
        NodeStore.init(plugin);

        for (int r = 0; r < cells.length; r++) {
            assertEquals(1000L + r, NodeStore.get(cells[r]).cellAmount, "region " + r + " must be on disk");
        }
    }

    @Test
    @DisplayName("a region rewritten every tick does not keep the autosave alive")
    void rewrittenRegionDoesNotKeepItAlive() {
        NodeStore.autosaveBudgetNanos = 0;
        Block busy = cellIn(0, 1);
        for (int r = 1; r < 6; r++) {
            cellIn(r, r);
        }
        NodeStore.autosave();
        assertTrue(NodeStore.autosaveRunning(), "one region per tick: six regions take several ticks");
        for (int i = 0; i < 200 && NodeStore.autosaveRunning(); i++) {
            NodeBlob live = NodeStore.canonical(busy);
            live.cellAmount++;
            NodeStore.put(busy, live);
            server.getScheduler().performOneTick();
        }
        assertFalse(NodeStore.autosaveRunning(), "changes made during the autosave wait for the next one");
        assertTrue(NodeStore.stats().unsavedRegions() >= 1, "the busy region is dirty again, for the next autosave");
    }
}

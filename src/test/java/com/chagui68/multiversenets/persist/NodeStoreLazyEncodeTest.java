package com.chagui68.multiversenets.persist;

import com.chagui68.multiversenets.MultiverseNets;
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * [EN] {@link NodeStore#put} no longer encodes on every write (ticket #83: storage cells paid a
 * GZIP + ObjectOutputStream per transfer). These tests pin that deferring the encode changes
 * nothing anyone can observe: the bytes, the default-blob shortcut, the region file and the
 * copies {@code get} hands out are the same as when {@code put} encoded eagerly.
 *
 * [ES] {@link NodeStore#put} ya no codifica en cada escritura (ticket #83). Estos tests fijan que
 * aplazar la codificación no cambia nada observable: bytes, atajo del blob por defecto, fichero de
 * región y copias de {@code get} son los mismos que cuando {@code put} codificaba al momento.
 */
class NodeStoreLazyEncodeTest {

    private ServerMock server;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockBukkit.load(MultiverseNets.class);
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static NodeBlob filledCell(long amount) {
        NodeBlob blob = NodeBlob.create("MVN_CELL_T1");
        blob.cellSample = new ItemStack(Material.DIAMOND);
        blob.cellAmount = amount;
        return blob;
    }

    @Test
    @DisplayName("stored bytes equal an eager encode of the last write")
    void bytesMatchEagerEncode() {
        Block block = world.getBlockAt(0, 64, 0);
        NodeStore.put(block, filledCell(1));
        NodeStore.put(block, filledCell(2));
        NodeBlob last = filledCell(3);
        NodeStore.put(block, last);

        assertArrayEquals(NodeStore.encodeBytes(last), NodeStore.rawData(block));
    }

    @Test
    @DisplayName("a default blob still stores no bytes")
    void defaultBlobStillStoresNothing() {
        Block block = world.getBlockAt(1, 64, 0);
        NodeStore.put(block, filledCell(5));
        NodeStore.put(block, NodeBlob.create("MVN_CELL_T1"));

        assertNull(NodeStore.rawData(block), "an untouched device costs a type name, nothing else");
    }

    @Test
    @DisplayName("the live instance mutated and re-put is what get() and the region file see")
    void canonicalMutationReachesCopiesAndDisk() throws java.io.IOException {
        Block block = world.getBlockAt(2, 64, 0);
        NodeStore.put(block, filledCell(10));

        // Exactly the shape of NetworkStorage: mutate the live instance, then put it back.
        NodeBlob live = NodeStore.canonical(block);
        live.cellAmount = 4096;
        NodeStore.put(block, live);

        assertEquals(4096L, NodeStore.get(block).cellAmount);

        NodeRegion region = NodeStore.regionForTest(block);
        assertNotNull(region);
        NodeRegion reread = RegionFile.deserialize(region.rx, region.rz, RegionFile.serialize(region));
        NodeRecord record = reread.get(block.getX(), block.getY(), block.getZ());
        assertNotNull(record);
        assertEquals(4096L, NodeStore.decodeBytes(record.data()).cellAmount,
                "the region writer must encode the newest live state, not a stale snapshot");
    }

    @Test
    @DisplayName("an unserializable blob keeps its last good bytes instead of failing the region")
    void unserializableBlobKeepsLastGoodBytes() {
        Block block = world.getBlockAt(3, 64, 0);
        NodeStore.put(block, filledCell(7));
        byte[] good = NodeStore.rawData(block);

        NodeBlob broken = filledCell(8);
        broken.cellSample = new UnserializableStack();
        NodeStore.put(block, broken);

        assertArrayEquals(good, NodeStore.rawData(block),
                "one bad blob must not sink the whole region write; the last saved state stays");
    }

    /** An ItemStack whose serialize() throws, the way Paper refuses stacks above 99 / Un ItemStack que no serializa. */
    private static final class UnserializableStack extends ItemStack {
        UnserializableStack() {
            super(Material.DIAMOND);
        }

        @Override
        public java.util.Map<String, Object> serialize() {
            throw new IllegalArgumentException("refusing to serialize");
        }
    }
}

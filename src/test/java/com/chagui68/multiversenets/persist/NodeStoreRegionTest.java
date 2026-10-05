package com.chagui68.multiversenets.persist;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.util.Keys;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Region-file storage: no per-chunk ceiling, default blobs cost only their type, data survives
 * a restart, broken files are kept aside, and chunks written by 5.2 and earlier migrate out of the
 * chunk PDC.
 *
 * [ES] Almacenamiento en archivos de región: sin techo por chunk, los blobs por defecto solo cuestan
 * su tipo, los datos sobreviven a un reinicio, los archivos rotos se apartan, y los chunks escritos
 * por la 5.2 y anteriores migran fuera del PDC del chunk.
 */
class NodeStoreRegionTest {

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

    /** Saves everything and reopens the storage, as a server restart would. */
    private void restart() {
        NodeStore.init(plugin);
    }

    @Test
    @DisplayName("a chunk takes thousands of nodes: there is no per-chunk ceiling")
    void noPerChunkCeiling() {
        int placed = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 60; y < 68; y++) {
                    NodeStore.put(world.getBlockAt(x, y, z), NodeBlob.create(DeviceType.MVN_CABLE.name()));
                    placed++;
                }
            }
        }
        Chunk chunk = world.getChunkAt(0, 0);
        assertEquals(placed, NodeStore.countNodesInChunk(chunk));

        restart();

        assertEquals(placed, NodeStore.countNodesInChunk(chunk), "every node is read back");
        assertEquals(DeviceType.MVN_CABLE, NodeStore.getType(world.getBlockAt(15, 67, 15)));
    }

    @Test
    @DisplayName("cables and untouched devices store only their type; configured ones keep their blob")
    void defaultBlobsCostOnlyTheirType() {
        Block cable = world.getBlockAt(0, 64, 0);
        NodeStore.put(cable, NodeBlob.create(DeviceType.MVN_CABLE.name()));
        Block grabber = world.getBlockAt(1, 64, 0);
        NodeBlob configured = NodeBlob.create(DeviceType.MVN_GRABBER.name());
        configured.filterMaterials.add("DIAMOND");
        NodeStore.put(grabber, configured);

        assertNull(NodeStore.rawData(cable), "a cable is a type and a position, nothing else");
        assertNotNull(NodeStore.rawData(grabber));
        assertEquals(DeviceType.MVN_CABLE.name(), NodeStore.get(cable).typeName,
                "a default node still reads as a full blob");

        // Volver a los valores por defecto vuelve a ahorrar el blob.
        NodeBlob cleared = NodeStore.get(grabber);
        cleared.filterMaterials.clear();
        NodeStore.put(grabber, cleared);
        assertNull(NodeStore.rawData(grabber));
    }

    @Test
    @DisplayName("blobs survive a restart byte for byte, in negative regions too")
    void dataSurvivesRestart() throws IOException {
        Block cell = world.getBlockAt(-5, 70, -40);
        NodeBlob blob = NodeBlob.create(DeviceType.MVN_CELL_T3.name());
        blob.cellSample = new ItemStack(Material.EMERALD);
        blob.cellAmount = 123_456;
        NodeStore.put(cell, blob);

        restart();

        NodeBlob back = NodeStore.get(cell);
        assertNotNull(back);
        assertEquals(Material.EMERALD, back.cellSample.getType());
        assertEquals(123_456L, back.cellAmount);
        assertTrue(Files.isRegularFile(NodeStore.folder(world).resolve("r.-1.-1.mvn")),
                "chunk -1,-3 lives in region -1,-1");
    }

    @Test
    @DisplayName("a region left empty deletes its file")
    void emptyRegionDeletesItsFile() {
        Block block = world.getBlockAt(3, 64, 3);
        NodeStore.put(block, NodeBlob.create(DeviceType.MVN_TERMINAL.name()));
        restart();
        Path file = NodeStore.folder(world).resolve("r.0.0.mvn");
        assertTrue(Files.isRegularFile(file));

        NodeStore.remove(block);
        restart();

        assertFalse(Files.exists(file));
        assertFalse(NodeStore.hasNode(block));
    }

    @Test
    @DisplayName("an unreadable region file is moved aside and the region starts empty")
    void corruptRegionIsQuarantined() throws IOException {
        Path folder = NodeStore.folder(world);
        Files.createDirectories(folder);
        Files.write(folder.resolve("r.0.0.mvn"), new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
                15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30});
        restart();
        world.getBlockAt(0, 64, 0).getChunk();

        assertNull(NodeStore.get(world.getBlockAt(0, 64, 0)), "nothing readable, nothing served");
        try (Stream<Path> files = Files.list(folder)) {
            assertTrue(files.anyMatch(p -> p.getFileName().toString().startsWith("r.0.0.mvn.corrupt-")),
                    "the broken file is kept for inspection");
        }
        NodeStore.put(world.getBlockAt(0, 64, 0), NodeBlob.create(DeviceType.MVN_CABLE.name()));
        assertTrue(NodeStore.hasNode(world.getBlockAt(0, 64, 0)), "and the region works again");
    }

    @Test
    @DisplayName("the ticking counter follows placements, type changes and removals")
    void tickingCounter() {
        Chunk chunk = world.getChunkAt(0, 0);
        Block a = world.getBlockAt(0, 64, 0);
        Block b = world.getBlockAt(1, 64, 0);
        NodeStore.put(a, NodeBlob.create(DeviceType.MVN_CABLE.name()));
        NodeStore.put(b, NodeBlob.create(DeviceType.MVN_GRABBER.name()));
        assertEquals(1, NodeStore.countTickingInChunk(chunk));

        NodeStore.put(a, NodeBlob.create(DeviceType.MVN_VACUUM.name()));
        assertEquals(2, NodeStore.countTickingInChunk(chunk));
        assertEquals(2, NodeStore.countNodesInChunk(chunk), "a type change is not a new node");

        NodeStore.remove(b);
        assertEquals(1, NodeStore.countTickingInChunk(chunk));
        NodeStore.remove(a);
        assertFalse(NodeStore.chunkHasNodes(chunk));
    }

    @Test
    @DisplayName("the optional cap blocks only active devices; passive blocks are never limited")
    void activeDeviceCap() {
        plugin.getConfig().set("network.max-active-devices-per-chunk", 2);
        PlayerMock player = server.addPlayer();

        assertTrue(place(player, 0, DeviceType.MVN_GRABBER));
        assertTrue(place(player, 1, DeviceType.MVN_PUSHER));
        assertFalse(place(player, 2, DeviceType.MVN_VACUUM), "third active device in the chunk is refused");
        for (int x = 3; x < 13; x++) {
            assertTrue(place(player, x, DeviceType.MVN_CABLE), "cables never count");
        }
        assertTrue(place(player, 13, DeviceType.MVN_CELL_T1), "cells never count");

        plugin.getConfig().set("network.max-active-devices-per-chunk", 0);
        assertTrue(place(player, 14, DeviceType.MVN_VACUUM), "0 disables the cap");
    }

    @Test
    @DisplayName("a placed Slimefun Request Crafter with heavy blueprints keeps its type after a restart")
    void sfRequestCrafterSurvivesRestart() {
        // #56: en la 5.2 el blob vivia en el PDC del chunk; un chunk con blueprints pesados superaba
        // el limite UTF de NBT (#35), no se guardaba y el crafter volvia como bloque vanilla.
        PlayerMock player = server.addPlayer();
        assertTrue(place(player, 0, DeviceType.MVN_SF_REQUEST_CRAFTER));
        Block crafter = world.getBlockAt(0, 64, 5);
        NodeBlob blob = NodeStore.get(crafter);
        String heavy = "x".repeat(70_000);
        for (int i = 0; i < 18; i++) {
            blob.blueprintData.add(heavy + i);
        }
        NodeStore.put(crafter, blob);

        restart();

        assertEquals(DeviceType.MVN_SF_REQUEST_CRAFTER, NodeStore.getType(crafter),
                "the crafter is still the Slimefun variant, not a vanilla block");
        NodeBlob back = NodeStore.get(crafter);
        assertEquals(18, back.blueprintData.size());
        assertEquals(heavy + 17, back.blueprintData.get(17));
    }

    private boolean place(PlayerMock player, int x, DeviceType type) {
        Block target = world.getBlockAt(x, 64, 5);
        BlockState previous = target.getState();
        target.setType(type.material());
        ItemStack item = Items.create(type);
        BlockPlaceEvent event = new BlockPlaceEvent(target, previous, target.getRelative(BlockFace.DOWN),
                item, player, true, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);
        return !event.isCancelled() && NodeStore.hasNode(target);
    }

    @Test
    @DisplayName("chunks written by 5.2 move out of the chunk PDC on load, data intact")
    void legacyChunkDataMigrates() {
        Block cell = world.getBlockAt(2, 64, 2);
        Block cable = world.getBlockAt(3, 64, 2);
        Block untyped = world.getBlockAt(4, 64, 2);
        Chunk chunk = cell.getChunk();
        NodeBlob cellBlob = NodeBlob.create(DeviceType.MVN_CELL_T2.name());
        cellBlob.cellSample = new ItemStack(Material.IRON_INGOT);
        cellBlob.cellAmount = 999;

        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        writeLegacy(pdc, cell, NodeStore.encode(cellBlob), DeviceType.MVN_CELL_T2.name());
        writeLegacy(pdc, cable, NodeStore.encode(NodeBlob.create(DeviceType.MVN_CABLE.name())), DeviceType.MVN_CABLE.name());
        // Nodos muy antiguos: solo el blob, sin la clave de tipo.
        writeLegacy(pdc, untyped, NodeStore.encode(NodeBlob.create(DeviceType.MVN_PUSHER.name())), null);
        pdc.set(Keys.CHUNK_HAS_NODES, PersistentDataType.BYTE, (byte) 1);

        assertEquals(3, NodeStore.migrateLegacy(chunk));

        assertTrue(pdc.getKeys().isEmpty(), "the chunk no longer carries node data");
        NodeBlob back = NodeStore.get(cell);
        assertEquals(999L, back.cellAmount);
        assertEquals(Material.IRON_INGOT, back.cellSample.getType());
        assertEquals(DeviceType.MVN_CABLE, NodeStore.getType(cable));
        assertNull(NodeStore.rawData(cable), "a default legacy blob is not kept");
        assertEquals(DeviceType.MVN_PUSHER, NodeStore.getType(untyped), "type recovered from the blob");
        assertEquals(0, NodeStore.migrateLegacy(chunk), "a migrated chunk is not migrated twice");

        restart();
        assertEquals(999L, NodeStore.get(cell).cellAmount, "and it is on disk");
    }

    private void writeLegacy(PersistentDataContainer pdc, Block block, String blob, String type) {
        String pos = block.getX() + "_" + block.getY() + "_" + block.getZ();
        pdc.set(new NamespacedKey(plugin, "n" + pos), PersistentDataType.STRING, blob);
        if (type != null) {
            pdc.set(new NamespacedKey(plugin, "t" + pos), PersistentDataType.STRING, type);
        }
    }

    @Test
    @DisplayName("an unloaded chunk answers 'no node' and is never read")
    void unloadedChunkAnswersNothing() {
        Block far = world.getBlockAt(10_000, 64, 10_000);
        NodeStore.put(far, NodeBlob.create(DeviceType.MVN_CABLE.name()));
        assertTrue(NodeStore.hasNode(far));
        world.getChunkAt(far).unload();
        if (!world.isChunkLoaded(far.getX() >> 4, far.getZ() >> 4)) {
            assertFalse(NodeStore.hasNode(far));
            assertNull(NodeStore.get(far));
        }
        assertEquals(Base64.getEncoder().encodeToString(NodeStore.encodeBytes(NodeBlob.create("MVN_CABLE"))),
                NodeStore.encode(NodeBlob.create("MVN_CABLE")), "the String API wraps the same bytes");
    }
}

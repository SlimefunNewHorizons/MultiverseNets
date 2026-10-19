package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * [EN] What the work-saving shortcuts of large networks must keep: an idle device still wakes up, a
 * purger that skipped a full scan still purges what arrives, and the cell index answers exactly what
 * a scan of every cell would.
 *
 * [ES] Lo que deben conservar los atajos para redes grandes: un dispositivo ocioso sigue
 * despertando, un purger que se saltó el recorrido sigue purgando lo que llega, y el índice de celdas
 * responde exactamente lo mismo que recorrer todas.
 */
class PerformanceGuardsTest {

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

    private Block place(int x, int y, int z, DeviceType type) {
        Block block = world.getBlockAt(x, y, z);
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
        Network net = manager.networkFor(world, PosUtil.pack(0, 64, 0));
        net.scan();
        return net;
    }

    private static int countIn(Inventory inv, Material material) {
        int total = 0;
        for (ItemStack it : inv.getContents()) {
            if (it != null && it.getType() == material) {
                total += it.getAmount();
            }
        }
        return total;
    }

    @Test
    @DisplayName("a long-idle grabber keeps sitting cycles out, and still wakes up")
    void longIdleGrabberKeepsBackingOff() {
        place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 64, 0, DeviceType.MVN_CELL_T1);
        Block grabber = place(0, 64, 1, DeviceType.MVN_GRABBER);
        NodeBlob grabberBlob = NodeStore.get(grabber);
        grabberBlob.filterBlacklist = true; // blacklist vacia = importa todo
        NodeStore.put(grabber, grabberBlob);
        Block chest = world.getBlockAt(0, 64, 2);
        chest.setType(Material.CHEST);
        Network net = network();
        NetworkTicker ticker = plugin.ticker();

        // 100 ciclos con el cofre vacio. Antes el contador se quedaba en 30 (30 % 3 == 0) y desde
        // ahi el Grabber miraba el cofre en cada ciclo.
        for (int i = 0; i < 100; i++) {
            ticker.tick();
        }
        ((Container) chest.getState()).getInventory().addItem(new ItemStack(Material.COBBLESTONE, 10));

        ticker.tick();
        assertEquals(10, countIn(((Container) chest.getState()).getInventory(), Material.COBBLESTONE),
                "a long-idle grabber sits this cycle out");

        for (int i = 0; i < 6; i++) {
            ticker.tick();
        }
        assertEquals(0, countIn(((Container) chest.getState()).getInventory(), Material.COBBLESTONE),
                "within six cycles the grabber looks again and imports");
        assertEquals(10, net.storage().count(item -> item.getType() == Material.COBBLESTONE));
    }

    @Test
    @DisplayName("an idle purger purges what arrives, and a filter edited by a menu, on the next cycle")
    void idlePurgerReactsToChanges() {
        place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 64, 0, DeviceType.MVN_CELL_T1);
        place(2, 64, 0, DeviceType.MVN_CELL_T1);
        Block purger = place(0, 64, 1, DeviceType.MVN_PURGER);
        NodeBlob blob = NodeStore.get(purger);
        blob.filterItems.add(new ItemStack(Material.DIRT));
        NodeStore.put(purger, blob);
        Network net = network();
        NetworkTicker ticker = plugin.ticker();

        net.storage().deposit(new ItemStack(Material.STONE, 32));
        for (int i = 0; i < 5; i++) {
            ticker.tick();
        }
        assertEquals(32, net.storage().count(item -> item.getType() == Material.STONE), "stone is not in the filter");

        net.storage().deposit(new ItemStack(Material.DIRT, 16));
        ticker.tick();
        assertEquals(0, net.storage().count(item -> item.getType() == Material.DIRT),
                "a deposit wakes the idle purger up at once");
        // Un ciclo sin nada que purgar: el purger se queda ocioso con el almacen tal como esta.
        ticker.tick();

        // Lo que hace un menu: copia, cambia, escribe la copia. El almacen no cambia.
        NodeBlob edited = NodeStore.get(purger);
        edited.filterItems.clear();
        edited.filterItems.add(new ItemStack(Material.STONE));
        NodeStore.put(purger, edited);
        ticker.tick();
        assertEquals(0, net.storage().count(item -> item.getType() == Material.STONE),
                "a filter written by a menu is purged on the next cycle");
    }

    @Test
    @DisplayName("cells emptied and refilled within one tick stay consistent with the index")
    void indexFollowsCellsWithinATick() {
        place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Block a = place(1, 64, 0, DeviceType.MVN_CELL_T1);
        Block b = place(2, 64, 0, DeviceType.MVN_CELL_T1);
        Block c = place(3, 64, 0, DeviceType.MVN_CELL_T1);
        Network net = network();
        NetworkStorage storage = net.storage();

        assertEquals(0, storage.deposit(new ItemStack(Material.STONE, 10)));
        assertEquals(0, storage.deposit(new ItemStack(Material.DIRT, 10)));
        ItemStack stone = storage.withdraw(item -> item.getType() == Material.STONE,
                NetworkStorage.only(Material.STONE), 64, -1L, true);
        assertNotNull(stone);
        assertEquals(10, stone.getAmount());

        // La celda de la piedra quedo libre en este mismo tick: la grava y luego la piedra tienen sitio.
        assertEquals(0, storage.deposit(new ItemStack(Material.GRAVEL, 5)));
        assertEquals(0, storage.deposit(new ItemStack(Material.STONE, 7)));
        assertEquals(64, storage.deposit(new ItemStack(Material.SAND, 64)), "three cells, three item types");

        assertEquals(5, storage.countOf(new ItemStack(Material.GRAVEL), true));
        assertEquals(7, storage.countOf(new ItemStack(Material.STONE), true));
        assertEquals(10, storage.countOf(new ItemStack(Material.DIRT), true));
        assertEquals(5, storage.count(item -> item.getType() == Material.GRAVEL, NetworkStorage.only(Material.GRAVEL)));
        assertEquals(22, storage.count(item -> true, Set.of(Material.GRAVEL, Material.STONE, Material.DIRT)));
        assertNull(storage.peek(item -> item.getType() == Material.SAND, NetworkStorage.only(Material.SAND), -1L, true));

        int gravel = 0;
        int stoneCells = 0;
        int dirt = 0;
        for (Block cell : List.of(a, b, c)) {
            ItemStack sample = NodeStore.get(cell).cellSample;
            assertNotNull(sample, "every cell holds something");
            if (sample.getType() == Material.GRAVEL) {
                gravel++;
            } else if (sample.getType() == Material.STONE) {
                stoneCells++;
            } else if (sample.getType() == Material.DIRT) {
                dirt++;
            }
        }
        assertEquals(1, gravel);
        assertEquals(1, stoneCells);
        assertEquals(1, dirt);
    }

    @Test
    @DisplayName("filter Material hints cover everything the filter lets through")
    void filterMaterialHints() {
        NodeBlob whitelist = NodeBlob.create(DeviceType.MVN_PUSHER.name());
        whitelist.filterItems.add(new ItemStack(Material.DIAMOND));
        whitelist.filterItems.add(new ItemStack(Material.IRON_INGOT));
        assertEquals(Set.of(Material.DIAMOND, Material.IRON_INGOT), NetworkManager.filterMaterials(whitelist));

        NodeBlob blacklist = NodeBlob.create(DeviceType.MVN_PUSHER.name());
        blacklist.filterItems.add(new ItemStack(Material.DIAMOND));
        blacklist.filterBlacklist = true;
        assertNull(NetworkManager.filterMaterials(blacklist), "a blacklist lets any Material through");

        NodeBlob empty = NodeBlob.create(DeviceType.MVN_PUSHER.name());
        assertNull(NetworkManager.filterMaterials(empty), "an empty filter lets any Material through");

        NodeBlob device = NodeBlob.create(DeviceType.MVN_PUSHER.name());
        device.filterItems.add(Items.create(DeviceType.MVN_CABLE));
        assertNull(NetworkManager.filterMaterials(device), "a cable can be any coloured glass");

        NodeBlob materials = NodeBlob.create(DeviceType.MVN_PUSHER.name());
        materials.filterMaterials.add("COAL");
        materials.filterMaterials.add("NOT_A_MATERIAL");
        assertEquals(Set.of(Material.COAL), NetworkManager.filterMaterials(materials));
        materials.filterMaterials.add("SLIMEFUN:COPPER_DUST");
        assertNull(NetworkManager.filterMaterials(materials), "a Slimefun id can be any Material");
    }
}

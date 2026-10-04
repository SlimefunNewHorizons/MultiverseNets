package com.chagui68.multiversenets;

import com.chagui68.multiversenets.compat.ChickenGenetics;
import com.chagui68.multiversenets.gui.ChickenSorterMenu;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.net.NetworkTicker;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Genetic Chicken Sorter: reads GeneticChickengineering pocket chickens from their data and
 * moves only the ones that meet every rule.
 *
 * [ES] Genetic Chicken Sorter: lee los pollos de bolsillo de GeneticChickengineering desde sus
 * datos y solo mueve los que cumplen todas las reglas.
 */
class ChickenSorterTest {

    private static final NamespacedKey DNA = new NamespacedKey("geneticchickengineering", "gce_pocket_chicken_dna");
    private static final NamespacedKey ADAPTER = new NamespacedKey("geneticchickengineering", "gce_pocket_chicken_adapter");
    private static final NamespacedKey SPECIES = new NamespacedKey("geneticchickengineering", "gce_expanded_species");

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

    /** A pocket chicken with the given six gene states (0 aa, 1 Aa, 3 AA) and DNA-known flag. */
    private static ItemStack chicken(int[] genes, boolean known, boolean baby, String species) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        var meta = item.getItemMeta();
        int[] state = new int[7];
        System.arraycopy(genes, 0, state, 0, 6);
        state[6] = known ? 1 : 0;
        meta.getPersistentDataContainer().set(DNA, PersistentDataType.INTEGER_ARRAY, state);
        meta.getPersistentDataContainer().set(ADAPTER, PersistentDataType.STRING, "{\"baby\":" + baby + ",\"_health\":4.0}");
        if (species != null) {
            meta.getPersistentDataContainer().set(SPECIES, PersistentDataType.STRING, species);
        }
        item.setItemMeta(meta);
        return item;
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

    @Test
    void genesAreReadLikeTheAddonDoes() {
        // Six dominant genes: typing 63 = Feather, tier 0, full strength, pure.
        ChickenGenetics.Chicken feather = ChickenGenetics.read(chicken(new int[]{3, 3, 3, 3, 3, 3}, true, false, null));
        assertNotNull(feather);
        assertEquals("TYPE:63", feather.product());
        assertEquals("Feather", ChickenGenetics.productName(feather.product()));
        assertEquals(0, feather.tier());
        assertEquals(6, feather.strength());
        assertTrue(feather.pure());

        // First gene recessive, second mixed: typing 31 = Bone, tier 1, strength 6-1-1 = 4, not pure.
        ChickenGenetics.Chicken bone = ChickenGenetics.read(chicken(new int[]{0, 1, 3, 3, 3, 3}, false, true, null));
        assertEquals("TYPE:31", bone.product());
        assertEquals(1, bone.tier());
        assertEquals(4, bone.strength());
        assertFalse(bone.pure());
        assertFalse(bone.known());
        assertFalse(bone.adult());

        ChickenGenetics.Chicken uranium = ChickenGenetics.read(chicken(new int[]{0, 0, 0, 0, 0, 0}, true, false, "URANIUM"));
        assertEquals("SPECIES:URANIUM", uranium.product());
        assertEquals(8, uranium.tier());

        assertNull(ChickenGenetics.read(new ItemStack(Material.PLAYER_HEAD)), "a plain head is not a chicken");
    }

    @Test
    void rulesMustAllPass() {
        NodeBlob rules = NodeBlob.create(DeviceType.MVN_CHICKEN_SORTER.name());
        ItemStack strongAdult = chicken(new int[]{3, 3, 3, 3, 3, 0}, true, false, null);
        ItemStack weakBaby = chicken(new int[]{1, 1, 3, 3, 3, 0}, false, true, null);
        assertTrue(ChickenGenetics.matches(rules, strongAdult), "default rules accept every chicken");
        assertFalse(ChickenGenetics.matches(rules, new ItemStack(Material.EGG)), "never anything that is not a chicken");

        rules.chickenMinStrength = 5;
        assertTrue(ChickenGenetics.matches(rules, strongAdult));
        assertFalse(ChickenGenetics.matches(rules, weakBaby));

        rules.chickenMinStrength = 0;
        rules.chickenAge = "ADULT";
        assertFalse(ChickenGenetics.matches(rules, weakBaby));
        rules.chickenAge = null;
        rules.chickenKnown = "KNOWN";
        assertFalse(ChickenGenetics.matches(rules, weakBaby));
        rules.chickenKnown = null;
        rules.chickenPureOnly = true;
        assertFalse(ChickenGenetics.matches(rules, weakBaby));
        assertTrue(ChickenGenetics.matches(rules, strongAdult));
        rules.chickenPureOnly = false;

        rules.chickenProducts.add(ChickenGenetics.read(strongAdult).product());
        assertTrue(ChickenGenetics.matches(rules, strongAdult));
        assertFalse(ChickenGenetics.matches(rules, chicken(new int[]{3, 3, 3, 3, 3, 3}, true, false, null)),
                "another product is rejected");

        rules.chickenProducts.clear();
        rules.chickenMinTier = 2;
        assertFalse(ChickenGenetics.matches(rules, strongAdult), "tier 1 is below the minimum");
    }

    @Test
    void theSorterPushesOnlyMatchingChickensAndOnlyWhenRunning() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 64, 0, DeviceType.MVN_DRAM_BAY);
        NodeBlob bay = NodeStore.get(world.getBlockAt(1, 64, 0));
        com.chagui68.multiversenets.net.MemoryModules.install(bay,
                com.chagui68.multiversenets.item.Items.create(DeviceType.MVN_CACHE_L1));
        NodeStore.put(world.getBlockAt(1, 64, 0), bay);
        Block sorter = place(0, 64, 1, DeviceType.MVN_CHICKEN_SORTER);
        Block chest = world.getBlockAt(0, 64, 2);
        chest.setType(Material.CHEST);
        Network net = plugin.networks().networkByController(controller.getLocation());
        net.scan();

        ItemStack pure = chicken(new int[]{3, 3, 3, 3, 3, 3}, true, false, null);
        ItemStack mixed = chicken(new int[]{1, 3, 3, 3, 3, 3}, true, false, null);
        assertEquals(0, net.storage().deposit(pure));
        assertEquals(0, net.storage().deposit(mixed));
        assertEquals(0, net.storage().deposit(new ItemStack(Material.DIRT, 5)));

        new NetworkTicker(plugin, plugin.networks()).tick();
        Inventory inv = ((Container) chest.getState()).getInventory();
        assertTrue(inv.isEmpty(), "a stopped sorter moves nothing");

        NodeBlob rules = NodeStore.get(sorter);
        rules.chickenActive = true;
        rules.chickenPureOnly = true;
        rules.targetFace = "SOUTH";
        NodeStore.put(sorter, rules);
        new NetworkTicker(plugin, plugin.networks()).tick();

        int chickens = 0;
        for (ItemStack it : inv.getContents()) {
            if (it != null) {
                assertTrue(ChickenGenetics.read(it).pure(), "only the pure chicken left");
                chickens += it.getAmount();
            }
        }
        assertEquals(1, chickens);
        assertEquals(5, net.storage().count(i -> i.getType() == Material.DIRT), "other items are never touched");
        assertEquals(1, net.storage().count(ChickenGenetics::isPocketChicken), "the mixed chicken stays");
    }

    // ------------------------------------------------------------------ menu

    private PlayerMock openSorterMenu(Block sorter) {
        PlayerMock player = server.addPlayer();
        new ChickenSorterMenu(plugin, player, sorter).openMenu();
        return player;
    }

    private void click(PlayerMock player, int raw, ClickType type) {
        InventoryAction action = type.isShiftClick() ? InventoryAction.MOVE_TO_OTHER_INVENTORY : InventoryAction.PICKUP_ALL;
        server.getPluginManager().callEvent(new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, raw, type, action));
    }

    private static String lore(ItemStack item) {
        StringBuilder out = new StringBuilder();
        for (var line : item.getItemMeta().lore()) {
            out.append(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line)).append(System.lineSeparator());
        }
        return out.toString();
    }

    private static String name(ItemStack item) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(item.getItemMeta().displayName());
    }

    @Test
    void theMenuSpellsOutWhatPassesAndShowsRealProductIcons() {
        Block sorter = place(0, 64, 0, DeviceType.MVN_CHICKEN_SORTER);
        PlayerMock player = openSorterMenu(sorter);
        var top = player.getOpenInventory().getTopInventory();

        assertEquals("● Stopped", name(top.getItem(ChickenSorterMenu.ACTIVE_SLOT)), "a fresh sorter is stopped");
        assertTrue(lore(top.getItem(ChickenSorterMenu.SUMMARY_SLOT)).contains("every pocket chicken passes"));
        assertEquals(Material.LIME_STAINED_GLASS_PANE, top.getItem(ChickenSorterMenu.FIRST_PRODUCT_SLOT).getType(),
                "an empty list says that every product passes");

        player.setItemOnCursor(chicken(new int[]{3, 3, 3, 3, 3, 3}, true, false, null));
        click(player, ChickenSorterMenu.FIRST_PRODUCT_SLOT + 4, ClickType.LEFT);

        assertEquals(java.util.List.of("TYPE:63"), NodeStore.get(sorter).chickenProducts);
        top = player.getOpenInventory().getTopInventory();
        assertEquals(Material.FEATHER, top.getItem(ChickenSorterMenu.FIRST_PRODUCT_SLOT).getType(),
                "the product shows its own item, not a generic egg");
        assertTrue(lore(top.getItem(ChickenSorterMenu.SUMMARY_SLOT)).contains("Products: Feather"));

        click(player, ChickenSorterMenu.FIRST_PRODUCT_SLOT, ClickType.LEFT);
        assertTrue(NodeStore.get(sorter).chickenProducts.isEmpty(), "clicking a product removes it");
    }

    @Test
    void ruleButtonsStepResetAndKeepTheTierRangeValid() {
        Block sorter = place(0, 64, 0, DeviceType.MVN_CHICKEN_SORTER);
        PlayerMock player = openSorterMenu(sorter);

        click(player, ChickenSorterMenu.MIN_TIER_SLOT, ClickType.LEFT);
        click(player, ChickenSorterMenu.MIN_TIER_SLOT, ClickType.LEFT);
        assertEquals(2, NodeStore.get(sorter).chickenMinTier);
        var minIcon = player.getOpenInventory().getTopInventory().getItem(ChickenSorterMenu.MIN_TIER_SLOT);
        assertEquals(2, minIcon.getAmount(), "the stack size shows the value");
        assertTrue(Boolean.TRUE.equals(minIcon.getItemMeta().getEnchantmentGlintOverride()), "an active rule glows");

        // Bajar el maximo por debajo del minimo arrastra el minimo: el rango nunca queda vacio.
        click(player, ChickenSorterMenu.MAX_TIER_SLOT, ClickType.RIGHT);
        assertEquals(9, NodeStore.get(sorter).chickenMaxTier, "from 'no limit' one step down is 9");
        for (int i = 0; i < 8; i++) {
            click(player, ChickenSorterMenu.MAX_TIER_SLOT, ClickType.RIGHT);
        }
        NodeBlob rules = NodeStore.get(sorter);
        assertEquals(1, rules.chickenMaxTier);
        assertEquals(1, rules.chickenMinTier, "min follows max down");

        click(player, ChickenSorterMenu.STRENGTH_SLOT, ClickType.LEFT);
        click(player, ChickenSorterMenu.KNOWN_SLOT, ClickType.RIGHT);
        assertEquals("UNKNOWN", NodeStore.get(sorter).chickenKnown, "right-click walks the options backwards");
        click(player, ChickenSorterMenu.STRENGTH_SLOT, ClickType.SHIFT_LEFT);
        assertEquals(0, NodeStore.get(sorter).chickenMinStrength, "shift-click resets one rule");

        click(player, ChickenSorterMenu.RESET_SLOT, ClickType.LEFT);
        rules = NodeStore.get(sorter);
        assertNull(rules.chickenMinTier);
        assertNull(rules.chickenMaxTier);
        assertNull(rules.chickenKnown);
        assertTrue(ChickenGenetics.matches(rules, chicken(new int[]{0, 0, 0, 0, 0, 0}, false, true, null)),
                "after a reset every chicken passes again");
    }

    @Test
    void controlBarTogglesStatusDirectionAndSide() {
        Block sorter = place(0, 64, 0, DeviceType.MVN_CHICKEN_SORTER);
        PlayerMock player = openSorterMenu(sorter);

        click(player, ChickenSorterMenu.ACTIVE_SLOT, ClickType.LEFT);
        click(player, ChickenSorterMenu.MODE_SLOT, ClickType.LEFT);
        click(player, ChickenSorterMenu.FACE_SLOT, ClickType.LEFT);
        NodeBlob blob = NodeStore.get(sorter);
        assertTrue(blob.chickenActive);
        assertTrue(blob.chickenPull);
        assertEquals("NORTH", blob.targetFace);

        click(player, ChickenSorterMenu.FACE_SLOT, ClickType.RIGHT);
        click(player, ChickenSorterMenu.FACE_SLOT, ClickType.RIGHT);
        assertEquals("DOWN", NodeStore.get(sorter).targetFace, "right-click cycles the sides backwards");
        assertTrue(lore(player.getOpenInventory().getTopInventory().getItem(ChickenSorterMenu.SUMMARY_SLOT))
                .contains("Moves: down → network"));
    }
}

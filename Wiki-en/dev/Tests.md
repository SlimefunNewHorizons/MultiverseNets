# 🧪 MultiverseNets plugin tests

This document explains how the project's tests work and what each one covers. Aimed at developers
who want to run them, understand them or extend them.

> Development area: [Structure](Structure.md) · [How the code works](Code.md) · **The tests**

---

## 1. Running the tests

The tests are **JUnit 5 (Jupiter)** and do not need a real Minecraft server:

```
mvn test
```

A full build (`mvn clean package`) runs them too. Requirements: JDK 21 and Maven.

The tests run on the 1.21.11 API (MockBukkit only exists for 1.21). For 26.1 and 26.2 the CI
(`.github/workflows/verify.yml`) compiles the same sources against those Paper APIs with JDK 25:
`mvn -P api-26.1 clean compile` and `mvn -P api-26.2 clean compile`. Running the suite with those
profiles is not supported: MockBukkit cannot start a 1.21 server on the 26.x API.

## 2. Test infrastructure: MockBukkit

Most tests use **[MockBukkit](https://github.com/MockBukkit/MockBukkit)**, an in-memory Bukkit server:

```java
@BeforeEach void setUp() {
    server = MockBukkit.mock();                     // fake server
    plugin = MockBukkit.load(MultiverseNets.class); // runs onEnable
    world = server.addSimpleWorld("world");
    player = server.addPlayer();
}

@AfterEach void tearDown() {
    MockBukkit.unmock();
}
```

That allows placing blocks, firing events (`server.getPluginManager().callEvent(...)`), simulating
inventory clicks, explosions and pistons, running the ticker
(`new NetworkTicker(plugin, plugin.networks()).tick()`) and reading node state back with `NodeStore`.
Region files go to the plugin data folder (MockBukkit does not expose world folders), one folder per
world UUID, so tests never see each other's files.

Things to know:
- A test that places a device by hand must also store its blob
  (`NodeStore.put(block, NodeBlob.create(type.name()))`); without it the block is not a node.
- MockBukkit does not implement `Display.setBillboard` or `HumanEntity.openWorkbench`. The ticker
  catches hologram failures, so ticker tests run normally; the one test that calls the hologram API
  directly (`FluidAndRequesterTest.testHologramRedesignNoFlowOrRouted`) is reported as **skipped**.
  Crafting tests open a `server.createInventory(player, InventoryType.WORKBENCH)` instead of
  `openWorkbench`.
- `src/test/java/dev/espi/protectionstones/PSRegion.java` is a stub of the ProtectionStones API that
  the provider tests load by reflection.

## 3. Overview: 52 classes, 357 tests

| Class (package `com.chagui68.multiversenets` unless stated) | Tests | Covers |
| --- | --- | --- |
| `BlockFlowsTest` | 24 | Break/place with embedded state, pistons, explosions, wireless binding, rake, wrench, sneaking guard, dimensions, bridge linking from the Transmitter item, cable status. |
| `BlueprintDupeTest` | 3 | Encoder Blueprints are never duplicated (two viewers, breaking with the menu open); installing a Blueprint consumes it and *Clear All* returns it. |
| `CableCamouflageTest` | 5 | Right-clicking a cable opens its menu; a block in the camouflage slot is kept (one unit), the cable looks like it and still connects, taking it back restores glass; shift-click swaps and returns the old block; unstable, interactive or non-full blocks and devices are refused; breaking drops the cable and the block. |
| `CellGuiTest` | 9 | Quantum Cell menu: template, quick deposit, withdrawal, capacity, no duplication when the ticker runs. |
| `ChickenSorterTest` | 6 | GeneticChickengineering genes read like the addon (product, tier, strength, purity, age, special species); every rule must pass; the sorter moves only matching chickens, only while running, and never other items. Menu: the summary spells out what passes, products show their own item, a cursor chicken adds its product and clicking removes it; rule buttons step, reset, glow and keep the tier range valid; the control bar toggles status, direction and side (backwards with right-click). |
| `ColoredCableTest` | 4 | A coloured cable is still `mvn_cable` (stained glass, colour in PDC); red and blue cables connect like plain ones and drop with their colour; taking the camouflage back restores the colour; the 16 colour recipes give 4 cables each. |
| `CrafterGuiTest` | 10 | Crafter menu: install/uninstall/clear; Slimefun crafters accept Slimefun and vanilla Blueprints, standard crafters refuse Slimefun ones. |
| `DeviceTypeTest` | 7 | `DeviceType` classification: filterable devices, Greedy Cell is not a cell, hand items, directional devices, request and Slimefun crafters. |
| `DramBayTest` | 13 | A module in a DRAM Bay stores items; a module taken out moves its stock to another network; breaking the bay drops every module with its stock; the Fluid DRAM holds several fluids and travels with them; a controller no longer takes modules; an old controller module waits in the Terminal; a bay holds 18 modules (a 19th stays in the hand) and the network fills all of them; clicking a module in the menu takes out only that one; a bay saved with a single module keeps it and its stock; the menu shows 18 slots and an item gauge lit in proportion to the fill. |
| `FilterGuiTest` | 16 | Filter menu: add/remove templates, whitelist/blacklist, shift-click, faces, clear; the face buttons (and, on a Simple Grabber, shift-click) open the neighbouring container. |
| `FluidAndRequesterTest` | 14 | Fluid storage and fluid cell quick interaction, Liquid Pump (drains a source; its menu has no mode or side buttons), terminal fluid page, Request Terminal (orders, chat amount, recursive chains, ignores Auto-Crafters, Slimefun Request Crafter), Slimefun Auto-Crafter, controller hologram text. |
| `GrabberQuotaTest` | 9 | `extractMatching`: honours the full per-cycle quota (including HT), merges slots of one item, leaves other items alone; a refused item goes back with `returnToSlots` without losing anything and the next item is reached. |
| `GreedyCellTest` | 8 | Greedy Cell multi-item storage, shared capacity, menu and terminal integration. |
| `GreedyReserveTest` | 3 | A Pusher takes from a Greedy Cell but leaves 1 of a defined item; any withdrawal leaves that unit; a Pusher whitelisting the item empties the Greedy Cell into the cells, new deposits skip it, and without that Pusher the Greedy Cell takes the item again. |
| `GuideMenuTest` | 6 | Every device is documented in both languages and its recipe is found; `/mvnets guide en|es` opens the menu (no book); navigation to a device shows its real recipe; the language toggle keeps the page; device ingredients open their own page; nothing can be taken out. |
| `GuiDupeGuardTest` | 3 | `GuiListener` cancels dangerous clicks (also in cell and barrel menus); shift-click deposits never duplicate. |
| `GuiFlowsTest` | 11 | Terminal, Encoder, Auto-Crafter (atomic crafting), Crafting Grid and Monitor flows; number key 1 pins an item to custom page 1, which then shows only that item and still withdraws. |
| `InfinityBarrelTest` | 7 | Barrel capacity, menu, network integration and break/place persistence; Void excess destroys the overflow instead of taking an empty cell (without it the overflow goes to one); the Terminal explains why a similar item is refused. |
| `ModuleIOTest` | 5 | DRAM Unloader empties a card into the network and its cards are not network storage; what the network can not take stays on the card; DRAM Loader fills a card up to the amount (and up to its capacity) and stops; the Loader menu sets the target and starts; amounts like `1m`/`500k`/`max` are read. |
| `NetworksCoexistenceTest` | 5 | Plugin name, main class, commands and permissions never collide with NetworksV6; Slimefun is a soft dependency. |
| `NewDevicesTest` | 6 | Purger and Probe properties; every `DeviceType` has a material and a name. |
| `PluginResourcesTest` | 3 | `plugin.yml` and `config.yml` on the classpath; version sanity check. |
| `PosUtilTest` | 2 | Coordinate packing round-trips, including world borders and negative Y. |
| `QuantumWorkbenchTest` | 2 | Cell upgrade keeps the cargo; ingredients are returned on close. |
| `QuotaLimiterMenusTest` | 3 | Automatic imports stop at the quota; a player depositing in the Terminal (shift-click) or a storage menu is not limited. |
| `QuotaManagerTest` | 3 | The Quota Manager lists the network's limiters, adds an item to a free limiter without taking it, adds nothing when none is free, removes and toggles; a Grabber empties an Infinity Barrel that is not wired to the network (and does not pull it in); a barrel wired by cable stays network storage and the Grabber leaves it alone. |
| `RecipeTest` | 11 | Every recipe registered once, also when registered again, which forgets every cached Blueprint resolution; cable and cell T1/T2 recipes craft; components are hand items used by device recipes; a cell with cargo matches its upgrade recipe and keeps the cargo; the 2×2 player grid is padded and odd matrix sizes are skipped. |
| `RecipeValidationTest` | 5 | Device ingredients are tracked per recipe; a plain material cannot stand in for a device; cell and module upgrades keep their cargo; a fluid cell that still holds fluid is refused; devices never feed vanilla recipes. |
| `ReportedIssuesTest` | 8 | Advanced Pusher never loses items (one stack per slot); a multi-item whitelist leaves room for every ingredient; blacklisted items stay in the network; nothing goes into an Infinity Barrel's block inventory; hoppers never touch a device; custom items match after their name is stored differently; the barrel takes back a custom item it handed out; the Slimefun Recipe Encoder keeps its Blueprints. |
| `SettingsCellCapacityTest` | 9 | `Settings` defaults and edge cases (capacities, clamps, null config). |
| `SlimefunBridgeTest` | 6 | The Slimefun bridge is inert and never throws without Slimefun; Slimefun items are identified by their PDC id, legacy namespace included. |
| `SlimefunMachinesSwitchTest` | 3 | The `slimefun-machines` section: on by default; the master switch removes the recipes (also on reload) and blocks placing every Slimefun machine but no vanilla one; per-machine keys and the old `sf-crafter.enabled` key still work. |
| `StackUtilsFastPathTest` | 4 | The fast item comparison: identical items match whatever their amount, different Slimefun ids never match, meta never matches plain, lore is ignored when asked. |
| `StatusLightTest` | 4 | The Status Light is green on a connected block and red once the link is cut; red without a network; it never bridges two networks; breaking it forgets it. |
| `ToolsTest` | 3 | Wrench and Rake are hand tools; the Receiver is filterable; filters default to whitelist. |
| `TransmissionFixesTest` | 13 | Item and fluid transmission: all-or-nothing fluid deposits, the pump never duplicates fluid, bridge with template-only filter, bridge never drains Greedy Cells, a device shared by two controllers works once per cycle, partial crafting results are undone, wrench pastes exact templates, rake returns the device, filters/face/transit buffer survive break and place, transit buffers above 99 units are saved without crashing. |
| `UpgradedFeaturesTest` | 6 | Memory module in a DRAM Bay, Router, per-chunk node counting, grabber transit buffer, cache kept on break, creative breaking drops nothing. |
| `compat.NetworkOwnershipTest` | 9 | A network runs inside its owner's claim; other networks and a null owner are strangers; public land stays open; broken or unwired providers grant nothing; owner answers never leak between networks. |
| `compat.ProtectionCacheTest` | 4 | Protection answers are cached: unclaimed land is asked once, owner and stranger keep their own answers, the sweep keeps live answers, dropping the cache asks again. |
| `compat.ProtectionStonesProviderTest` | 18 | The ProtectionStones provider against the real API shape (`PSRegion.fromLocation*`, exact signatures), owner/member certification, fail-closed behaviour; WorldGuard region lookup fails safe. |
| `compat.ProtectionWhitelistTest` | 15 | Protection defaults and `exempt-locations` parsing and geometry; the bridge is inert without providers. |
| `compat.SlimefunInsertRuleTest` | 4 | Inserting into Slimefun machines: a fake Electric Smeltery (same transport answer as Slimefun's) keeps each ingredient in one stack and leaves the other slots free; a stack is topped up without opening another slot; an ordinary machine fills every slot, one per cycle; a menu failing half-way never returns what already went in. |
| `listen.SneakingRightClickTest` | 3 | Sneaking + right-click never opens a device menu and still allows vanilla placement. |
| `net.PerformanceGuardsTest` | 4 | A long-idle Grabber keeps sitting cycles out and still wakes up within six; an idle Purger purges a new item, and a filter written by a menu, on the next cycle; cells emptied and refilled within one tick stay consistent with the Material index; filter Material hints cover everything the filter lets through. |
| `net.ScanCostTest` | 2 | The BFS neighbour walk allocates nothing per node and a large scan stays linear. |
| `net.ScanSchedulingTest` | 5 | Rescans: an untouched network is not rescanned every interval but the periodic full rescan still catches silent changes; a dirty network rescans at once; a stale one waits for the interval; a chunk load marks only nearby networks. |
| `net.StorageCachingTest` | 5 | The loop caches never serve stale data: a menu write is seen in the same tick, the change stamp follows deposits and withdrawals, filter predicates follow the filter, networkAt follows rescans, throughput counters are pruned. |
| `persist.IncrementalAutosaveTest` | 2 | The spread autosave saves every region dirty at its start, and a region rewritten every tick does not keep it running. |
| `persist.NodeStoreCanonicalTest` | 5 | The shared decoded blob is never older than the last write. |
| `persist.NodeStoreCorruptionTest` | 6 | Corrupt stored blobs read as missing, silently and cheaply, stay in place and can be overwritten; a large blob still fits the item PDC string limit. |
| `persist.NodeStoreLazyEncodeTest` | 4 | Lazy encoding: stored bytes equal an eager encode, default blobs store nothing, the live instance re-put reaches copies and disk, an unserializable blob keeps its last good bytes. |
| `persist.NodeStoreRegionTest` | 12 | Region storage: thousands of nodes and any number of active devices in one chunk (no ceiling, no cap), default blobs store only their type, data survives a restart (negative regions too) and a Slimefun Request Crafter with heavy Blueprints keeps its type, an emptied region deletes its file, a corrupt file is moved aside, the node counter, migration out of the 5.2 chunk PDC (a failed write keeps the old data; re-importing never overwrites a newer record), unloaded chunks answer "no node". |

## 4. Notes on some suites

### `TransmissionFixesTest` and `BlueprintDupeTest`
Each test reproduces a loss or duplication that existed in the code and was fixed. They were checked
to **fail** against the code before the fix, so they guard against regressions rather than restate
the implementation. Examples: a purger shared by two controllers deleted 256 items per cycle instead
of 128; a nearly full fluid network kept part of a bucket and the bucket; two players opening the
same Recipe Encoder turned 16 stored Blueprints into 32.

### `BlockFlowsTest`
The three dimension tests (`networkExtractsInsideTheNether/End/Overworld`) build a real network with
a grabber and a chest and run the scheduler. They prove the scan, ticker and storage carry no
dimension check of their own, so whatever blocks a dimension at runtime is the protection bridge.

### `compat.*`
Protection plugins are not on the test classpath. What is tested is everything that does not need
them (defaults, whitelist geometry, inert bridge) plus the ProtectionStones provider against a stub of
its API; each provider's live decision is verified on a real server.

---

To understand the functionality these tests cover, see [How the code works](Code.md).

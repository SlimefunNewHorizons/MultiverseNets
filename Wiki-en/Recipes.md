# 📜 Recipes and functions of MultiverseNets items

Every item in the plugin, its **crafting recipe** as you see it on the crafting table (3×3 grid),
and what it **does** in the network. Numbers are the `config.yml` defaults; "cycle" means one
transfer cycle (`network.op-interval-ticks.transfer`, 5 ticks).

> In the grids, `·` marks an empty slot. Every item can also be given with `/mvnets give <id>`.
> Back to the [wiki index](README.md).

> **Components and devices as ingredients.** When a recipe asks for a component or another device
> (Network Circuit, Storage Matrix, previous cell, previous module, cable, fluid cell, DRAM Bay,
> Monitor, Advanced Pusher...) it must be that item: the plain material underneath (prismarine shard,
> disc fragment, terracotta, copper ingot, glass, prismarine bricks, piston...) does not count. Cell
> and module upgrades keep the cargo of the ingredient; every other recipe refuses a device that still
> stores something. No component or device can be used as an ingredient of a vanilla recipe.

---

## 🔩 Components

The base devices are crafted from these intermediate parts, so no device recipe can clash with a
vanilla recipe. They are hand items: they cannot be placed or used, and no vanilla recipe accepts
them. Blueprints encoded with the old device recipes no longer work: encode them again.

### Silicon Wafer (×2) · `mvn_silicon_wafer`

```
· Q ·
Q S Q
· Q ·
```

> Q = **Quartz** · S = **Sand**

- **Result**: 2× Silicon Wafer (hand item, Prismarine Crystals)
- **Function**: The first component. Network Circuits, Storage Matrices and Blank Blueprints are
  built on it. Only an ingredient.

---

### Network Circuit (×2) · `mvn_network_circuit`

```
R G R
W I W
R G R
```

> R = **Redstone** · G = **Gold Nugget** · W = **Silicon Wafer** · I = **Iron Ingot**

- **Result**: 2× Network Circuit (hand item, Prismarine Shard)
- **Function**: Basic network logic. The Controller, cables, Terminal, Simple Grabber and Pusher,
  Vacuum, Purger, Quota Limiter, Monitor, Probe, both Encoders, the Configuration Wrench and the
  Container Connector use it,
  and it is the base of the Logic Processor and the Storage Matrix. Only an ingredient.

---

### Logic Processor · `mvn_logic_processor`

```
Q C Q
C R C
Q C Q
```

> Q = **Quartz** · C = **Network Circuit** · R = **Redstone Block**

- **Result**: 1× Logic Processor (hand item, Shulker Shell)
- **Function**: The brain of the machines that decide: Controller, Auto-Crafters, Request Crafters,
  Request Terminal and Crafting Grid. Two of them make a Quantum Core. Only an ingredient.

---

### Storage Matrix · `mvn_storage_matrix`

```
I W I
C D C
I W I
```

> I = **Iron Ingot** · W = **Silicon Wafer** · C = **Network Circuit** · D = **Diamond**

- **Result**: 1× Storage Matrix (hand item, Disc Fragment)
- **Function**: The core of every storage: Quantum Cell T1, Infinity Barrel, Greedy Cell, Quantum
  Fluid Cell, DRAM Bay and the L1 module. Only an ingredient.

---

### Transfer Coil (×2) · `mvn_transfer_coil`

```
K R K
R I R
K R K
```

> K = **Copper Ingot** · R = **Redstone** · I = **Iron Ingot**

- **Result**: 2× Transfer Coil (hand item, Tripwire Hook)
- **Function**: Moves things: Grabbers and Pushers (simple and advanced), Vacuum, Liquid Pump and
  Container Connector.
  Only an ingredient.

---

### Quantum Core · `mvn_quantum_core`

```
A D A
P E P
A D A
```

> A = **Amethyst Shard** · D = **Diamond** · P = **Logic Processor** · E = **Ender Pearl**

- **Result**: 1× Quantum Core (hand item, Echo Shard)
- **Function**: Links far places: Wireless Terminal, Wireless Transmitter and Receiver, Network
  Router and Quantum Workbench. Only an ingredient.

---

## 🖥️ Core and access

### Network Controller · `mvn_controller`

```
I P I
C N C
I P I
```

> I = **Iron Block** · P = **Logic Processor** · C = **Network Circuit** · N = **Nether Star**

- **Result**: 1× Network Controller (Lodestone)
- **Function**: Root of the network. Every scan starts here and walks through every connected
  MultiverseNets block. Whoever places it becomes the network's **owner** (used by land protection).
  Shows a floating hologram with status, node count and stored totals. Memory modules go in a
  **DRAM Bay**, not in the controller; a controller that still holds an old module keeps using it, and
  its menu can take that module out with its items. Right-click opens its status menu. Only one
  controller per network: a second one wired to the same cables is reported as `foreign controller`.

---

### Network Cable (×16) · `mvn_cable`

```
G G G
G C G
G G G
```

> G = **Glass** · C = **Network Circuit**

- **Result**: 16× Network Cable (Glass)
- **Function**: Connects devices. Every MultiverseNets block conducts, cables are just the cheap way
  to cover distance. Right-click a cable (empty hand) to see whether it reaches a controller and how
  big the network is, and to open its menu.
- **Colours**: 4 cables of any colour around a dye (a cross: cable on the top, left, right and bottom
  of the dye) give 4 cables of that colour, in all 16 dye colours. They look like stained glass and
  connect exactly like the plain cable, so colours only help to tell areas apart; a broken cable drops
  with its colour and camouflage keeps it.
- **Camouflage**: put a block in the cable menu's slot and the cable looks exactly like it (a quartz
  wall stays a quartz wall). The block is kept inside the cable, not used up: take it back and the
  cable is glass again; breaking the cable drops the cable and the block. Only solid, full blocks that
  do not fall, change by themselves or have an inventory.

---

### Network Terminal · `mvn_terminal`

```
G C G
E B E
G C G
```

> G = **Glass** · C = **Network Circuit** · E = **Ender Pearl** · B = **Beacon**

- **Result**: 1× Network Terminal (Beacon)
- **Function**: The storage grid. The top row holds the views: **Network Storage**, **Purge Mode**
  (items targeted by purgers), **Fluids** and **5 custom pages**, plus the input slot. The bottom row
  holds pages, sort, search, a network overview, **Pin Mode**, **Deposit Inventory** and a help book.
  Left-click takes 1, right-click a stack, shift+click sends to your inventory; click the grid with an
  item on the cursor (or shift+left-click your own items, or leave them in the input slot) to store
  them. Buckets and honey bottles go to fluid storage.
- **Custom pages (favourites)**: hover an item and press **1-5** to pin or unpin it on page 1-5 (or
  turn on Pin Mode and click). A custom page shows only its pinned items, also when out of stock;
  **Q** over an item there unpins it. Right-click a page tab to rename it, shift+right-click to clear
  it. Pages are saved per player and work on every Terminal.

---

### Wireless Terminal · `mvn_wireless_terminal`

```
· Q ·
P N P
· C ·
```

> Q = **Quantum Core** · P = **Ender Pearl** · N = **Nether Star** · C = **Compass**

- **Result**: 1× Wireless Terminal (hand item)
- **Function**: Opens a network's terminal remotely. Shift+right-click a Controller or a Terminal to
  bind it, then right-click in the air. Without a **Network Router** it only works in the same world
  and within 64 blocks (`wireless.local-range-without-router`). It is locked for 10 s after combat and
  checks that you may access the network's land.

---

### Network Router · `mvn_router`

```
· L ·
· C ·
· Q ·
```

> L = **Lightning Rod** · C = **Network Cable** · Q = **Quantum Core**

- **Result**: 1× Network Router (Lightning Rod)
- **Function**: Connected anywhere in a network, it lifts the Wireless Terminal limits for that
  network: any distance and any world.

---

### Network Monitor · `mvn_monitor`

```
G G G
K C K
G G G
```

> G = **Glass Pane** · K = **Comparator** · C = **Network Circuit**

- **Result**: 1× Network Monitor (Respawn Anchor)
- **Function**: Live diagnostic panel: node counts by type, storage usage and scan errors. It refreshes
  while open.

---

### Network Probe · `mvn_probe`

```
· A ·
A S A
· C ·
```

> A = **Amethyst Shard** · S = **Spyglass** · C = **Network Circuit**

- **Result**: 1× Network Probe (hand item)
- **Function**: Right-click any block (node or not) to see which network it belongs to, how many nodes
  it has, where its controller is, and any scan warning — including links cut by land protection.

---

### Network Status Light · `mvn_status_light`

```
R G R
L P D
R C R
```

> R = **Redstone** · G = **Glowstone** · L = **Lime Dye** · P = **Network Probe** · D = **Red Dye** ·
> C = **Network Cable**

- **Result**: 1× Network Status Light (Stained Glass)
- **Function**: Place it on top of any network block. It turns **green** when that block reaches a
  Controller and **red** when it does not, so you see the state of your build from afar. It checks once
  a second, is not part of the network and never links two networks.

---

### DRAM Bay · `mvn_dram_bay`

```
I M I
R C R
I Q I
```

> I = **Iron Ingot** · M = **Storage Matrix** · R = **Redstone** · C = **Copper Block** ·
> Q = **Quartz**

- **Result**: 1× DRAM Bay (Waxed Copper Bulb)
- **Function**: Network block that holds **up to 18 memory modules** (any mix of the item modules
  below and Fluid DRAMs), each with its own stock. While installed, their stock is part of the
  network. Right-click the bay with a module in hand to install it in the next free slot, or
  right-click to open its menu: the bay's summary, 18 module slots with
  their fill bars, and item and fluid gauges. Install from the
  cursor or with shift+click; **click an installed module to take it out**. A module taken out keeps
  its whole stock: install it in a DRAM Bay of another network and the stock appears there and is
  gone from the first one. Breaking the bay drops it and each module (with its stock) separately.

---

### Memory Modules · `mvn_cache_l1` … `mvn_cache_quantum`

Item modules for the **DRAM Bay**: multi-item storage, any mix of item types, capacity per tier in
`virtual-cache.tier-1` … `tier-5`. A module keeps its items when it is taken out of the bay. Each
tier is crafted from the previous one; upgrading a module that holds items keeps them.

#### L1 CPU Cache Module — 2,048 items

```
C R C
R M R
C R C
```

> C = **Copper Ingot** · R = **Redstone** · M = **Storage Matrix**

#### L2 CPU Cache Module — 8,192 items

```
G L G
L P L
G L G
```

> G = **Gold Ingot** · L = **Lapis Lazuli** · P = **L1 CPU Cache Module**

#### L3 CPU Cache Module — 32,768 items

```
D A D
A P A
D A D
```

> D = **Diamond** · A = **Amethyst Shard** · P = **L2 CPU Cache Module**

#### DRAM Memory Module — 131,072 items

```
N E N
E P E
N E N
```

> N = **Netherite Ingot** · E = **Eye of Ender** · P = **L3 CPU Cache Module**

#### Quantum Cache Matrix — 524,288 items

```
N S N
S P S
N S N
```

> N = **Netherite Block** · S = **Nether Star** · P = **DRAM Memory Module**

---

### Fluid DRAM Module · `mvn_fluid_dram`

```
D B D
F E F
D B D
```

> D = **Diamond** · B = **Bucket** · F = **Quantum Fluid Cell** (empty) · E = **Eye of Ender**

- **Result**: 1× Fluid DRAM Module (hand item, Heart of the Sea)
- **Function**: **Fluid-only** memory module for the DRAM Bay. It holds several fluids at once, up to
  `fluids.dram-capacity-mb` (512,000 mB = 512 buckets) in total, and adds that to the network's fluid
  storage. Like the item modules, taking it out of the bay carries its fluids to whichever network
  you install it in next.

---

### DRAM Unloader · `mvn_module_unloader`

```
I H I
R B R
I C I
```

> I = **Iron Ingot** · H = **Hopper** · R = **Redstone** · B = **DRAM Bay** · C = **Network Cable**

- **Result**: 1× DRAM Unloader (Waxed Exposed Copper Bulb)
- **Function**: **Empties cards** (memory modules) into the network. Holds up to 9 cards; every
  transfer cycle it moves up to 4,096 items and 64,000 mB (`transfer.module-items-per-op`,
  `transfer.module-fluid-per-op`). Unlike a DRAM Bay, the cards inside are not network storage. What the
  network can not store stays on the card and the next item is tried, so the card ends up as empty as
  possible. It needs free space in the network. Right-click it with a card in hand or open its menu;
  **Take empty cards** collects every finished card.

---

### DRAM Loader · `mvn_module_loader`

```
I D I
K B K
I R I
```

> I = **Iron Ingot** · D = **Dropper** · K = **Comparator** · B = **DRAM Bay** ·
> R = **Redstone Block**

- **Result**: 1× DRAM Loader (Waxed Oxidized Copper Bulb)
- **Function**: **Fills one card** with a single item (or a fluid, for a Fluid DRAM) taken from the
  network. Inserting a card does nothing: choose the target (click the target slot with the item, or a
  fluid bucket), set the **amount** (±64 / ±1k / ±10k buttons, shift ×100, or click it and type `1m`,
  `500k`, `max`) and press **Start**. It stops by itself when the card holds that amount or is full.
  Example: an empty Quantum card + cobblestone + 500k + Start = a card ready to sell. Its target and
  amount travel with the block when you break it.

---

## 📦 Item storage

### Quantum Cell T1 – T6 · `mvn_cell_t1` … `mvn_cell_t6`

**T1:**

```
G G G
G M G
G G G
```

> G = **Glass** · M = **Storage Matrix**

**Tn+1 (n ≥ 1):** the previous cell in the centre, surrounded by diamonds.

```
D D D
D P D
D D D
```

> D = **Diamond** · P = **Previous cell** (with or without cargo)

- **Result**: 1× cell of the next tier (terracotta: plain, orange, yellow, lime, cyan, purple)
- **Function**: Stores **one item type** each. An empty cell adopts the first item type that has
  nowhere else to go. Right-click to see or manage its contents; breaking it keeps the cargo inside
  the item. A cell **with cargo** is upgraded keeping that cargo (in the crafting table or the Quantum
  Workbench). Capacities (`cells.capacities`):

| Tier | Capacity |
|---|---|
| T1 | 65,536 |
| T2 | 262,144 |
| T3 | 1,048,576 |
| T4 | 16,777,216 |
| T5 | 268,435,456 |
| T6 | 2,000,000,000 |

---

### Infinity Barrel · `mvn_infinity_barrel`

```
N M N
D B D
N D N
```

> N = **Netherite Ingot** · M = **Storage Matrix** · D = **Diamond Block** · B = **Barrel**

- **Result**: 1× Infinity Barrel (Barrel)
- **Function**: Like a cell, one item type, with `barrel.capacity` (2,000,000,000). It **stays
  registered** to its item when it empties. In its menu, click *Set Item* with an item on the cursor
  to register it; right-click *Set Item* with an empty cursor clears the registration (only when it
  is empty). Items with their own id (Slimefun and other plugins) are recognised by that id and
  their visible text, so whatever you take out goes back in. Hoppers do not interact with it.

---

### Greedy Cell · `mvn_greedy_cell`

```
G H G
H S H
G M G
```

> G = **Gold Ingot** · H = **Hopper** · S = **Slime Block** · M = **Storage Matrix**

- **Result**: 1× Greedy Cell (Slime Block)
- **Function**: Multi-item buffer up to `greedy.capacity` (262,144) in total.
  - **With a filter** it is a priority sink: items entering the network that match go to it first;
    every cycle it pulls up to 512 more from the network and pushes up to 256 into adjacent
    **non-network** containers (chests, Slimefun machines). Ideal for feeding a machine line.
  - **Without a filter** it is general overflow storage, used only when everything else is full.
  - **Internal filter**: Pushers, terminals and crafting can take from it, but an item defined in
    its filter always keeps 1 unit inside. A Pusher with that item in its whitelist releases it: the
    Greedy Cell moves all of it to the cells and stops taking it. The wireless bridge never takes
    from it.

---

### Container Connector · `mvn_container_connector`

```
I N I
C T C
I N I
```

> I = **Iron Ingot** · N = **Network Circuit** · C = **Chest** · T = **Transfer Coil**

- **Result**: 1× Container Connector (Ochre Froglight)
- **Function**: Links the vanilla containers touching it (chests, trapped and copper chests,
  barrels, shulker boxes) to its network as storage. The Terminal shows their items, deposits fill
  them and withdrawals empty them. They are the last storage: the network fills its own storage first
  and takes from it first. Items move exactly as they are; a shulker box never goes into another one.
  Grabbers, Pushers and Greedy Cells of the same network leave linked containers alone.

---

### Quantum Workbench · `mvn_quantum_workbench`

```
D Q D
D C D
D D D
```

> D = **Diamond** · Q = **Quantum Core** · C = **Crafting Table**

- **Result**: 1× Quantum Workbench (Brain Coral Block)
- **Function**: Upgrades a Quantum Cell T1–T5 to the next tier keeping its cargo. Place the cell in
  the centre, 8 diamonds around it, press *Entangle & Upgrade* and take the result. Ingredients left
  in the grid are returned when you close the menu.

---

## 💧 Fluids

### Quantum Fluid Cell · `mvn_fluid_cell`

```
G B G
G M G
G L G
```

> G = **Glass** · B = **Bucket** · M = **Storage Matrix** · L = **Lapis Block**

- **Result**: 1× Quantum Fluid Cell (Prismarine Bricks)
- **Function**: Holds one fluid — Water, Lava, Milk, Powder Snow or Honey — up to
  `fluids.cell-capacity-mb` (64,000 mB = 64 buckets). All fluid cells of a network form its fluid
  storage. Right-click it with a filled bucket / honey bottle to pour, with an empty bucket to fill
  (Water, Lava, Milk, Powder Snow). Its menu shows the level, extracts one bucket, and has a *Void
  Fluid Tank* button (shift+right-click to confirm) that empties the cell permanently.

---

### Liquid Pump · `mvn_liquid_pump`

```
· G ·
P B P
· T ·
```

> G = **Blue Stained Glass** · P = **Piston** · B = **Bucket** · T = **Transfer Coil**

- **Result**: 1× Liquid Pump (Blue Stained Glass)
- **Function**: Each cycle drains one **source** block of water or lava directly **below** it
  (1,000 mB) into the network's fluid cells. The source is removed only if the whole 1,000 mB fit.
  Right-click to choose ANY / WATER / LAVA.

---

## 🔄 Item transport

### Simple Grabber (Importer) · `mvn_grabber`

```
I T I
O C O
I T I
```

> I = **Iron Ingot** · T = **Transfer Coil** · O = **Observer** · C = **Network Circuit**

- **Result**: 1× Simple Grabber (Observer)
- **Function**: Imports from adjacent containers into the network: up to 128 items of one type per
  cycle, from all six faces. Whitelist/blacklist filter; **an empty whitelist imports nothing**, an empty blacklist imports everything. If the
  network refuses part of it, the overflow goes first to Pushers that accept it, then back to the
  slots it came from, and the grabber moves on to the **next item**: one item the network does not
  accept never blocks the rest of the chest. A Quantum Cell or Infinity Barrel it touches
  that is not wired to the network is emptied the same way. Only if it can not go back does it wait in the grabber's
  transit buffer (kept even if you break it).

---

### Advanced Grabber · `mvn_grabber_ht`

```
T P T
```

> T = **Transfer Coil** · P = **Sticky Piston**

- **Result**: 1× Advanced Grabber (Sticky Piston)
- **Function**: Same as the Simple Grabber ×8 (`transfer.ht-multiplier`): 1,024 items per cycle. Its
  menu can restrict it to **one face**.

---

### Simple Pusher (Exporter) · `mvn_pusher`

```
I T I
D C D
I T I
```

> I = **Iron Ingot** · T = **Transfer Coil** · D = **Dropper** · C = **Network Circuit**

- **Result**: 1× Simple Pusher (Target)
- **Function**: Exports from the network into adjacent containers through **all six faces** (it has
  no face selector): up to 128 items per cycle, only when a container is next to it, never into
  another network block. **An empty whitelist does nothing** (a fresh pusher never empties the
  network); a **blacklist exports everything except what is listed** (the listed items stay in the
  network). At most one stack per slot; a whitelist of several items gives each item its share of
  the target's slots and serves them in turn, so one ingredient cannot fill a machine. Furnaces get
  fuel in the fuel slot and the rest in the input, never in the result slot. What does not fit goes
  back to the network.

---

### Advanced Pusher · `mvn_pusher_ht`

```
T P T
```

> T = **Transfer Coil** · P = **Piston**

- **Result**: 1× Advanced Pusher (Piston)
- **Function**: Same as the Simple Pusher ×8 (1,024 per cycle). With a face selected it delivers
  **only** to that side; use it to feed one machine.

---

### Network Vacuum · `mvn_vacuum`

```
S T S
T H T
S C S
```

> S = **String** · T = **Transfer Coil** · H = **Hopper** · C = **Network Circuit**

- **Result**: 1× Network Vacuum (Sponge)
- **Function**: Every 10 ticks picks up dropped items within `vacuum.radius` (4 blocks) into the
  network. Whitelist/blacklist filter: **an empty whitelist picks up nothing** (a freshly placed
  vacuum does not swallow the whole floor); an empty blacklist picks up everything. Items that do not
  fit stay on the ground.

---

### Network Purger · `mvn_purger`

```
I L I
L H L
I C I
```

> I = **Iron Ingot** · L = **Magma Block** · H = **Hopper** · C = **Network Circuit**

- **Result**: 1× Network Purger (Magma Block)
- **Function**: Deletes up to 128 items per cycle that match its filter, so waste (gravel, seeds…)
  never jams the network. **Without a filter it deletes nothing**, on purpose.

---

### Network Quota Limiter · `mvn_limiter`

```
R K R
T C T
R K R
```

> R = **Redstone** · K = **Comparator** · T = **Target** · C = **Network Circuit**

- **Result**: 1× Network Quota Limiter (Target)
- **Function**: Caps how much of one item the network may hold. Every deposit stops at the cap:
  grabbers, vacuum, terminal, crafting results and the wireless bridge. In its menu: click with an
  item to set the target, ±1/10/64/1,000 buttons or a chat prompt for the limit, and an on/off toggle.
  Several limiters on the same item: the lowest wins.

---

### Quota Manager · `mvn_quota_manager`

```
R K R
P M P
R L R
```

> R = **Redstone** · K = **Comparator** · P = **Paper** · M = **Network Monitor** ·
> L = **Network Quota Limiter**

- **Result**: 1× Quota Manager (Verdant Froglight)
- **Function**: Lists every Quota Limiter of its network: the limited item, its limit, the network's
  stock with a usage bar, whether it is enforced, and how many limiters are free. Add an item (Add
  button with the item on the cursor, a free limiter, or Shift-Click from your inventory): it takes
  one free Quota Limiter and the item is only copied. Without a free limiter nothing is added. On a
  limited item: left-click types the limit, Shift+right-click turns it on/off, right-click removes
  the item and frees the limiter.

---

### Wireless Transmitter · `mvn_transmitter`

```
I R I
R C R
I Q I
```

> I = **Iron Ingot** · R = **Redstone Block** · C = **Conduit** · Q = **Quantum Core**

- **Result**: 1× Wireless Transmitter (Conduit)
- **Function**: One end of a wireless bridge between two networks. Shift+right-click a placed
  Transmitter with a Receiver item to make that **receiver pull** from this network. Or link the other
  way: shift+right-click a placed Receiver with a Transmitter item, and this transmitter **pushes** up
  to 128 items per cycle that pass its filter into the receiver's network. Its menu is a filter menu
  with a button that opens its own network's terminal.

---

### Wireless Receiver · `mvn_receiver`

```
I P I
P L P
I Q I
```

> I = **Iron Ingot** · P = **Ender Pearl** · L = **Redstone Lamp** · Q = **Quantum Core**

- **Result**: 1× Wireless Receiver (Redstone Lamp)
- **Function**: The other end of the bridge. When linked to a Transmitter it **pulls** up to 128 items
  per cycle that pass **its** filter from the transmitter's network into its own, across any distance
  and world while the other end's chunk is loaded. **An empty whitelist moves nothing**, on purpose;
  an empty blacklist moves everything. Greedy Cells are never drained. Its menu has a button that
  opens the remote network's terminal (if you may access that land).

---

## 🛠️ Crafting

### Blank Blueprint (×4) · `mvn_blueprint`

```
P P P
P W P
P B P
```

> P = **Paper** · W = **Silicon Wafer** · B = **Blue Dye**

- **Result**: 4× Blueprint (Book)
- **Function**: Carries a recipe (3×3 grid + result) once written by a Recipe Encoder. Crafting never
  consumes it. Installing it moves it into the crafter; removing it, replacing it or *Clear All*
  gives it back.

---

### Recipe Encoder · `mvn_encoder`

```
K P K
C S C
K P K
```

> K = **Ink Sac** · P = **Paper** · C = **Network Circuit** · S = **Smithing Table**

- **Result**: 1× Recipe Encoder (Smithing Table)
- **Function**: Build the recipe in its persistent 3×3 template grid (clicking only marks slots, no
  items are spent), put a Blueprint (blank or already encoded) in the blue slot and press *Encode*.
  Clicking an encoded Blueprint loads its recipe back into the grid. Blueprints left in its slots
  stay stored in the block; only one player at a time sees them.

---

### Slimefun Recipe Encoder · `mvn_sf_encoder`

```
E P E
C B C
E P E
```

> E = **Ender Pearl** · P = **Paper** · C = **Network Circuit** · B = **Enchanting Table**

- **Result**: 1× Slimefun Recipe Encoder (Enchanting Table)
- **Function**: Same as the Recipe Encoder for Slimefun recipes. Blueprints left in its slots stay
  stored in the block when you close it. Needs Slimefun. Its recipe, placing and menu exist only
  while `slimefun-machines.enabled` and `slimefun-machines.encoder` are `true` (the default).

---

### Auto-Crafter · `mvn_crafter`

```
R C R
I P I
R C R
```

> R = **Redstone** · C = **Crafting Table** · I = **Iron Ingot** · P = **Logic Processor**

- **Result**: 1× Auto-Crafter (Crafting Table)
- **Function**: Holds up to 18 vanilla Blueprints (`crafter.max-recipes`) and every 20 ticks tries
  each one once with the network's stock. All-or-nothing: if any ingredient is missing nothing is
  taken, and if the result does not fit the whole craft is undone. Slimefun Blueprints are refused
  (use the Slimefun Auto-Crafter).

---

### Slimefun Auto-Crafter · `mvn_sf_crafter`

```
E C E
I P I
E C E
```

> E = **Ender Pearl** · C = **Crying Obsidian** · I = **Iron Ingot** · P = **Logic Processor**

- **Result**: 1× Slimefun Auto-Crafter (Crying Obsidian)
- **Function**: Same as the Auto-Crafter and accepts **both Slimefun and vanilla Blueprints**.
  `slimefun-machines.crafters: false` (or `slimefun-machines.enabled: false`) removes its recipe,
  its placing, its menu and its crafting.

---

### Request Crafter · `mvn_request_crafter`

```
R C R
L P L
R C R
```

> R = **Redstone** · C = **Crafting Table** · L = **Lectern** · P = **Logic Processor**

- **Result**: 1× Request Crafter (Fletching Table)
- **Function**: Holds vanilla Blueprints that are **only** crafted on demand from a Request Terminal,
  never automatically.

---

### Slimefun Request Crafter · `mvn_sf_request_crafter`

```
E C E
L P L
E C E
```

> E = **Ender Pearl** · C = **Purpur Pillar** · L = **Lectern** · P = **Logic Processor**

- **Result**: 1× Slimefun Request Crafter (Purpur Pillar)
- **Function**: Same as the Request Crafter, with Slimefun and vanilla Blueprints. Depends on
  `slimefun-machines.crafters` and `slimefun-machines.enabled`.

---

### Request Terminal · `mvn_request_terminal`

```
G L G
C P C
G G G
```

> G = **Glass** · L = **Lectern** · C = **Crafting Table** · P = **Logic Processor**

- **Result**: 1× Request Terminal (Lectern)
- **Function**: Lists everything the network's Request Crafters can make and crafts it on demand,
  resolving chains with the network's stock (e.g. logs → planks → crafting table). Left-click 1
  batch, shift+left-click 10, right-click 64, shift+right-click asks for an exact number in chat
  (non-numeric or ≤ 0 cancels). A button toggles delivery to your inventory or to the network.
  Auto-Crafters are not listed here.

---

### Network Crafting Grid · `mvn_crafting_grid`

```
C R C
R G R
C P C
```

> C = **Crafting Table** · R = **Redstone** · G = **Cartography Table** · P = **Logic Processor**

- **Result**: 1× Network Crafting Grid (Cartography Table)
- **Function**: A crafting table that pulls ingredients from the network transactionally. The
  template grid is stored in the block; *Craft 1* / *Craft All* hand you the result.

---

## 🐔 GeneticChickengineering

### Genetic Chicken Sorter · `mvn_chicken_sorter`

```
F E F
C P C
F E F
```

> F = **Feather** · E = **Egg** · C = **Comparator** · P = **Advanced Pusher**

- **Result**: 1× Genetic Chicken Sorter (Hay Bale)
- **Function**: Moves **only** the pocket chickens of the GeneticChickengineering addon, chosen by
  their genes; every other item is ignored. **Push** sends matching chickens from the network to the
  block it faces; **Pull** brings them from that block into the network (up to 16 per cycle).
- **Menu** (right-click), top to bottom:
  - **Control bar**: start/stop, direction (Push/Pull), side, a summary book that says in plain
    words which chickens pass right now, and help.
  - **Accepted products**: 18 slots, each showing the product's own item. Click a slot with a pocket
    chicken on the cursor, or shift-click a chicken in your inventory, to add its product; click a
    product to remove it. Empty list = any product.
  - **Gene rules** (all must pass), in three pairs: tier (min/max), genes (minimum DNA strength, pure
    genes only) and identity (sequenced/unsequenced DNA, adult/baby). Each rule shows its value in
    its name and stack size and glows while active: left-click +1, right-click −1, shift-click
    resets it. The tier range can never be left empty.
  - **Bottom row**: clear products, reset gene rules, close.
- It starts **stopped** so it cannot empty a network before you configure it.

---

## 🧰 Tools

### Configuration Wrench · `mvn_configurator`

```
I · I
· C ·
· I ·
```

> I = **Iron Ingot** · C = **Network Circuit**

- **Result**: 1× Configuration Wrench (hand item)
- **Function**: Shift+right-click a filterable device to copy its filter (exact templates, materials
  and whitelist/blacklist mode); right-click another one to paste it, replacing its filter.

---

### Network Rake · `mvn_rake`

```
D · D
· S ·
· S ·
```

> D = **Dead Bush** · S = **Stick**

- **Result**: 1× Network Rake (hand item, 250 uses — `rake.uses`)
- **Function**: Dismantles a node instantly and gives it back to you with its state (filter,
  Blueprints, link…). It refuses controllers and storage that still holds items or fluid.

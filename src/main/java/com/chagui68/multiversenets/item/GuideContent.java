package com.chagui68.multiversenets.item;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * [EN] Text of the in-game guide (the {@code /mvnets guide} menu), in English and Spanish. Recipes are
 * not written here: the menu reads them from the recipes actually registered, so the guide can never
 * show a recipe the server does not have.
 *
 * [ES] Texto de la guía del juego (el menú de {@code /mvnets guide}), en inglés y español. Las recetas
 * no se escriben aquí: el menú las lee de las recetas registradas de verdad, así la guía nunca muestra
 * una receta que el servidor no tiene.
 */
public final class GuideContent {

    private GuideContent() {
    }

    public enum Category {
        CORE(Material.LODESTONE, "Core & Access", "Núcleo y acceso",
                "Controller, cables, terminals and diagnostics.",
                "Controlador, cables, terminales y diagnóstico."),
        STORAGE(Material.TERRACOTTA, "Item Storage", "Almacenamiento de ítems",
                "Quantum Cells, Infinity Barrel and Greedy Cell.",
                "Celdas Cuánticas, Infinity Barrel y Greedy Cell."),
        MEMORY(Material.WAXED_COPPER_BULB, "Memory (DRAM)", "Memoria (DRAM)",
                "DRAM Bay and the memory modules that carry their stock between networks.",
                "DRAM Bay y los módulos de memoria que llevan su stock entre redes."),
        FLUIDS(Material.PRISMARINE_BRICKS, "Fluids", "Fluidos",
                "Fluid cells and the Liquid Pump.",
                "Celdas de fluidos y la Liquid Pump."),
        TRANSPORT(Material.PISTON, "Item Transport", "Transporte de ítems",
                "Grabbers, Pushers, Vacuum, Purger, Limiter and the wireless bridge.",
                "Grabbers, Pushers, Vacuum, Purger, Limitador y el puente inalámbrico."),
        CRAFTING(Material.CRAFTING_TABLE, "Crafting", "Crafteo",
                "Blueprints, encoders, auto-crafters and request crafting.",
                "Blueprints, codificadores, autocrafters y crafteo bajo pedido."),
        TOOLS(Material.COMPARATOR, "Tools", "Herramientas",
                "Configuration Wrench and Network Rake.",
                "Llave de Configuración y Network Rake."),
        CHICKENS(Material.HAY_BLOCK, "GeneticChickengineering", "GeneticChickengineering",
                "Sorting pocket chickens by their genes.",
                "Clasificar pollos de bolsillo por sus genes."),
        COMPONENTS(Material.PRISMARINE_SHARD, "Components", "Componentes",
                "Intermediate parts every device recipe is built from.",
                "Piezas intermedias con las que se fabrican los dispositivos.");

        private final Material icon;
        private final String nameEn;
        private final String nameEs;
        private final String blurbEn;
        private final String blurbEs;

        Category(Material icon, String nameEn, String nameEs, String blurbEn, String blurbEs) {
            this.icon = icon;
            this.nameEn = nameEn;
            this.nameEs = nameEs;
            this.blurbEn = blurbEn;
            this.blurbEs = blurbEs;
        }

        public Material icon() {
            return icon;
        }

        public String name(boolean es) {
            return es ? nameEs : nameEn;
        }

        public String blurb(boolean es) {
            return es ? blurbEs : blurbEn;
        }
    }

    /**
     * EN: One device: what it does and how to use it. {@code nameEs} is the Spanish name; the item
     * itself keeps its English name, so the Spanish guide shows both.
     * ES: Un dispositivo: qué hace y cómo se usa. {@code nameEs} es el nombre en español; el ítem
     * conserva su nombre en inglés, así la guía en español muestra los dos.
     */
    public record Entry(DeviceType type, Category category, String nameEs,
                        String whatEn, String whatEs, String useEn, String useEs) {

        public String name(boolean es) {
            return es ? nameEs : type.display();
        }

        public String what(boolean es) {
            return es ? whatEs : whatEn;
        }

        public String use(boolean es) {
            return es ? useEs : useEn;
        }
    }

    /** A general topic of the "How networks work" page. */
    public record Topic(Material icon, String titleEn, String titleEs, String textEn, String textEs) {

        public String title(boolean es) {
            return es ? titleEs : titleEn;
        }

        public String text(boolean es) {
            return es ? textEs : textEn;
        }
    }

    private static final Map<DeviceType, Entry> ENTRIES = new EnumMap<>(DeviceType.class);
    private static final List<Topic> TOPICS = new ArrayList<>();

    public static Entry of(DeviceType type) {
        return ENTRIES.get(type);
    }

    public static List<Entry> in(Category category) {
        List<Entry> list = new ArrayList<>();
        for (Entry entry : ENTRIES.values()) {
            if (entry.category() == category) {
                list.add(entry);
            }
        }
        return list;
    }

    public static List<Topic> topics() {
        return Collections.unmodifiableList(TOPICS);
    }

    private static void add(DeviceType type, Category category, String nameEs,
                            String whatEn, String whatEs, String useEn, String useEs) {
        ENTRIES.put(type, new Entry(type, category, nameEs, whatEn, whatEs, useEn, useEs));
    }

    private static void topic(Material icon, String titleEn, String titleEs, String textEn, String textEs) {
        TOPICS.add(new Topic(icon, titleEn, titleEs, textEn, textEs));
    }

    static {
        // ------------------------------------------------------------------ general topics
        topic(Material.LODESTONE, "What is a network?", "¿Qué es una red?",
                "A network is one Network Controller plus every MultiverseNets block touching it, face to face. "
                        + "Cables are the cheap way to connect, but every device conducts. Whoever places the "
                        + "Controller owns the network. Add at least one storage (cell, barrel or DRAM Bay) or the "
                        + "network has nowhere to keep items.",
                "Una red es un Controlador de Red más todos los bloques de MultiverseNets que lo tocan, cara con "
                        + "cara. Los cables son la forma barata de conectar, pero todos los dispositivos conducen. "
                        + "Quien coloca el Controlador es el dueño de la red. Añade al menos un almacenamiento "
                        + "(celda, barril o DRAM Bay) o la red no tendrá dónde guardar ítems.");
        topic(Material.CHEST, "Where items are stored", "Dónde se guardan los ítems",
                "Every deposit fills in this order: Greedy Cells whose filter matches, memory modules that "
                        + "already hold the item, Slimefun barrels, cells and barrels that hold it, free memory, "
                        + "empty Slimefun barrels, empty cells (they adopt the item) and finally Greedy Cells without "
                        + "a filter. A Greedy Cell keeps 1 of each item defined in its filter, unless a Pusher whitelists "
                        + "that item: then it goes to the cells. Quota Limiters cap everything. What does not fit goes "
                        + "back; nothing is deleted.",
                "Cada depósito llena en este orden: Greedy Cells cuyo filtro coincide, módulos de memoria que ya "
                        + "tienen el ítem, barriles de Slimefun, celdas y barriles que lo tienen, memoria libre, "
                        + "barriles de Slimefun vacíos, celdas vacías (adoptan el ítem) y por último Greedy Cells sin "
                        + "filtro. Una Greedy Cell conserva 1 de cada ítem definido en su filtro, salvo que un Pusher "
                        + "tenga ese ítem en su whitelist: entonces va a las celdas. Los Quota Limiters ponen tope a "
                        + "todo. Lo que no cabe vuelve; nada se borra.");
        topic(Material.HOPPER, "Filters", "Filtros",
                "Filters decide WHICH items move, never where to. Whitelist: only the listed items. Blacklist: "
                        + "everything except the listed items (they stay in the network). An empty whitelist on a "
                        + "Pusher or bridge does nothing, so a fresh device never empties a network. Put the exact "
                        + "item in the filter: Slimefun items are matched by their id.",
                "Los filtros deciden QUÉ ítems se mueven, nunca a dónde. Whitelist: solo lo listado. Blacklist: "
                        + "todo menos lo listado (eso se queda en la red). Una whitelist vacía en un Pusher o en el "
                        + "puente no hace nada, así un dispositivo nuevo nunca vacía una red. Pon el ítem exacto en "
                        + "el filtro: los ítems de Slimefun se comparan por su id.");
        topic(Material.BLAST_FURNACE, "Feeding machines", "Alimentar máquinas",
                "Use an Advanced Pusher and select the machine's face. A whitelist of several ingredients gives "
                        + "each one its share of the machine's slots, served in turn, so one ingredient cannot fill "
                        + "it. Never more than one stack per slot. Furnaces: fuel goes to the fuel slot, the rest to "
                        + "the input. For machines that return the product in the same block (a Smeltery without "
                        + "output chest) add an output chest and point the Advanced Grabber at it.",
                "Usa un Advanced Pusher y elige la cara de la máquina. Una whitelist de varios ingredientes da a "
                        + "cada uno su parte de las ranuras de la máquina, por turnos, así un ingrediente no la "
                        + "llena. Nunca más de un stack por ranura. Hornos: el combustible a su ranura, lo demás a "
                        + "la entrada. Para máquinas que devuelven el producto en el mismo bloque (una Fundición sin "
                        + "cofre de salida) pon un cofre de salida y apunta el Advanced Grabber a él.");
        topic(Material.ENDER_EYE, "Between networks", "Entre redes",
                "Networks never merge through the air. Items only cross with a Transmitter/Receiver link, and "
                        + "the end that holds the link moves items through its own filter. A memory module taken out "
                        + "of a DRAM Bay also carries its whole stock to another network.",
                "Las redes nunca se fusionan por el aire. Los ítems solo cruzan con un enlace Transmisor/Receptor, "
                        + "y mueve el extremo que guarda el enlace, con su propio filtro. Un módulo de memoria "
                        + "sacado de un DRAM Bay también lleva todo su stock a otra red.");
        topic(Material.SHIELD, "Safety rules", "Reglas de seguridad",
                "Breaking a device keeps its state in the item (cargo, filters, Blueprints...). Hoppers never "
                        + "interact with network blocks. Devices are immune to pistons and explosions. On claimed "
                        + "land a network only works inside its owner's land, and players cannot open devices on "
                        + "land they cannot use.",
                "Romper un dispositivo guarda su estado en el ítem (carga, filtros, Blueprints...). Las tolvas "
                        + "nunca interactúan con bloques de la red. Los dispositivos son inmunes a pistones y "
                        + "explosiones. En terreno protegido una red solo trabaja dentro del terreno de su dueño, y "
                        + "los jugadores no pueden abrir dispositivos en terreno que no pueden usar.");
        topic(Material.SPYGLASS, "Diagnostics & commands", "Diagnóstico y comandos",
                "Right-click a cable to see if it reaches a controller. The Network Probe and /mvnets doctor "
                        + "explain scan errors. Admins can use /mvnets give (Tab completes the ids), "
                        + "/mvnets stats (networks and storage) and /mvnets save; /mvnets help lists every command.",
                "Clic derecho en un cable muestra si llega a un controlador. La Network Probe y /mvnets doctor "
                        + "explican los errores del escaneo. Los admins pueden usar /mvnets give (Tab completa los ids), "
                        + "/mvnets stats (redes y almacenamiento) y /mvnets save; /mvnets help lista todos los comandos.");
        topic(Material.BOOKSHELF, "Building big", "Construir a lo grande",
                "There is no limit on network blocks per chunk: device data lives in region files inside the "
                        + "world folder, not in the chunk, and a cable or an unconfigured device only stores its "
                        + "type. Fill a chunk with cables and machines if you like. Idle devices cost almost "
                        + "nothing: a Grabber or Pusher that finds nothing checks less often, and storage only "
                        + "looks at the cells of the item it needs. Data is saved in the background and travels "
                        + "with the world.",
                "No hay límite de bloques de red por chunk: los datos de los dispositivos viven en archivos de "
                        + "región dentro de la carpeta del mundo, no en el chunk, y un cable o un dispositivo sin "
                        + "configurar solo guarda su tipo. Llena un chunk de cables y máquinas si quieres. Los "
                        + "dispositivos ociosos casi no cuestan: un Grabber o Pusher que no encuentra nada mira "
                        + "menos a menudo, y el almacenamiento solo mira las celdas del ítem que necesita. Los "
                        + "datos se guardan en segundo plano y viajan con el mundo.");

        // ------------------------------------------------------------------ core
        add(DeviceType.MVN_CONTROLLER, Category.CORE, "Controlador de Red",
                "The root of the network: every scan starts here and walks through the connected blocks. "
                        + "Records its owner (who placed it) and shows a hologram with status, nodes and stored "
                        + "totals. Only one controller per network.",
                "La raíz de la red: cada escaneo empieza aquí y recorre los bloques conectados. Guarda a su "
                        + "dueño (quien lo colocó) y muestra un holograma con estado, nodos y totales. Un solo "
                        + "controlador por red.",
                "Place it first, then connect cables and devices. Right-click: status, router and a button to "
                        + "open the terminal. It does not take memory modules: they go in a DRAM Bay. A module that "
                        + "was inside an old controller waits in the Terminal as a temporary item, with its items.",
                "Colócalo primero y conecta cables y dispositivos. Clic derecho: estado, router y un botón que "
                        + "abre el terminal. No acepta módulos de memoria: van en un DRAM Bay. Un módulo que estaba "
                        + "dentro de un controlador antiguo espera en el Terminal como ítem temporal, con sus ítems.");
        add(DeviceType.MVN_CABLE, Category.CORE, "Cable de Red",
                "Connects devices. It has no logic of its own: it only carries the network.",
                "Conecta dispositivos. No tiene lógica propia: solo lleva la red.",
                "Right-click a cable (empty hand) to see whether it reaches a controller and open its menu. "
                        + "Camouflage: put a block in its slot and the cable looks exactly like it, so a line of "
                        + "cables disappears inside a wall. The block is kept, not used up: take it back and the "
                        + "cable is glass again; breaking the cable drops both. Colours: 4 cables + a dye give 4 "
                        + "cables of that colour (all 16); they connect like the plain cable. Holding a block while "
                        + "right-clicking places the block.",
                "Clic derecho en un cable (mano vacía) para ver si llega a un controlador y abrir su menú. "
                        + "Camuflaje: pon un bloque en su hueco y el cable tiene exactamente su aspecto, así una "
                        + "línea de cables desaparece dentro de una pared. El bloque se guarda, no se gasta: "
                        + "recupéralo y el cable vuelve a ser de cristal; romper el cable suelta los dos. Colores: 4 "
                        + "cables + un tinte dan 4 cables de ese color (los 16); conectan como el cable normal. Con un "
                        + "bloque en la mano, el clic derecho coloca el bloque.");
        add(DeviceType.MVN_TERMINAL, Category.CORE, "Terminal de Red",
                "The storage grid: every item in the network (memory, cells, barrels, greedy cells, Slimefun "
                        + "barrels) plus a page for fluids.",
                "La cuadrícula del almacenamiento: todos los ítems de la red (memoria, celdas, barriles, greedy "
                        + "cells, barriles de Slimefun) y una página de fluidos.",
                "Top row: Network Storage, Purge Mode (items targeted by purgers), Fluids and 5 custom pages. "
                        + "Left-click takes 1, right-click a stack, shift-click to your inventory. Click the grid with "
                        + "an item on the cursor, shift-click your items or leave them in the input slot to store them; "
                        + "Deposit Inventory stores everything. Search, sort, pages and a network overview. Custom "
                        + "pages: hover an item and press 1-5 (or use Pin Mode) to pin it; Q unpins it; right-click a "
                        + "tab to rename it. Buckets and honey bottles go to the fluid storage. Each item shows its "
                        + "total and how much of it is in DRAM, cells, barrels and Greedy Buffers. Recovered modules "
                        + "from an old controller are shown first: click to take them.",
                "Fila superior: Almacenamiento, Modo Purga (ítems que borran los purgers), Fluidos y 5 páginas "
                        + "personalizadas. Clic izquierdo saca 1, clic derecho un stack, shift+clic al inventario. "
                        + "Haz clic en la cuadrícula con un ítem en el cursor, shift+clic en tus ítems o déjalos en la "
                        + "ranura de entrada para guardarlos; Depositar inventario lo guarda todo. Búsqueda, orden, "
                        + "páginas y un resumen de la red. Páginas personalizadas: pasa el ratón por un ítem y pulsa "
                        + "1-5 (o usa el Modo Pin) para fijarlo; Q lo quita; clic derecho en una pestaña la renombra. "
                        + "Cubos y botellas de miel van al almacenamiento de fluidos. Cada ítem muestra su total y "
                        + "cuánto hay en DRAM, celdas, barriles y Greedy Buffers. Los módulos recuperados de un "
                        + "controlador antiguo salen primero: clic para recogerlos.");
        add(DeviceType.MVN_WIRELESS_TERMINAL, Category.CORE, "Terminal Inalámbrico",
                "Opens the Network Terminal from a distance.",
                "Abre el Terminal de Red a distancia.",
                "Shift+right-click a Controller or a Terminal to bind it, then right-click in the air. Without a "
                        + "Network Router it only works in the same world and nearby. Locked for a few seconds after "
                        + "combat.",
                "Shift+clic derecho en un Controlador o Terminal para vincularlo, luego clic derecho al aire. Sin "
                        + "Network Router solo funciona en el mismo mundo y cerca. Se bloquea unos segundos tras un "
                        + "combate.");
        add(DeviceType.MVN_ROUTER, Category.CORE, "Router de Red",
                "Removes the Wireless Terminal limits for its network: any distance, any world.",
                "Quita los límites del Terminal Inalámbrico para su red: cualquier distancia y mundo.",
                "Connect it anywhere in the network.",
                "Conéctalo en cualquier punto de la red.");
        add(DeviceType.MVN_MONITOR, Category.CORE, "Monitor de Red",
                "Live diagnostic panel: nodes by type, storage usage and errors.",
                "Panel de diagnóstico en vivo: nodos por tipo, uso del almacenamiento y errores.",
                "Right-click it; it refreshes while open.",
                "Clic derecho; se actualiza mientras está abierto.");
        add(DeviceType.MVN_PROBE, Category.CORE, "Sonda de Red",
                "Answers \"is this connected?\": network size, controller position, scan errors and land "
                        + "protection cuts for the block you click.",
                "Responde \"¿esto está conectado?\": tamaño de la red, posición del controlador, errores del "
                        + "escaneo y cortes por protección del bloque que pulsas.",
                "Right-click any block with it.",
                "Clic derecho en cualquier bloque.");
        add(DeviceType.MVN_STATUS_LIGHT, Category.CORE, "Luz de Estado de Red",
                "Shows from afar whether the block under it reaches a Controller: green when connected, red "
                        + "when not. It is not part of the network and never links two networks.",
                "Muestra desde lejos si el bloque de debajo llega a un Controlador: verde si está conectado, "
                        + "roja si no. No forma parte de la red y nunca une dos redes.",
                "Place it on top of any network block. It checks once a second.",
                "Colócala encima de cualquier bloque de red. Se comprueba una vez por segundo.");

        // ------------------------------------------------------------------ storage
        String cellWhatEn = "Stores ONE item type in large amounts. An empty cell adopts the first item type that "
                + "has nowhere else to go. Breaking it keeps the cargo inside the item.";
        String cellWhatEs = "Guarda UN tipo de ítem en grandes cantidades. Una celda vacía adopta el primer tipo que "
                + "no tenga otro sitio. Al romperla la carga queda dentro del ítem.";
        String cellUseEn = "Connect it to the network. Right-click to see or manage its contents. Upgrade it one tier "
                + "(cell + 8 diamonds) in a crafting table or in the Quantum Workbench: the cargo is kept.";
        String cellUseEs = "Conéctala a la red. Clic derecho para ver o gestionar su contenido. Súbela un nivel "
                + "(celda + 8 diamantes) en la mesa de crafteo o en la Quantum Workbench: la carga se conserva.";
        DeviceType[] cells = {DeviceType.MVN_CELL_T1, DeviceType.MVN_CELL_T2, DeviceType.MVN_CELL_T3,
                DeviceType.MVN_CELL_T4, DeviceType.MVN_CELL_T5, DeviceType.MVN_CELL_T6};
        for (int i = 0; i < cells.length; i++) {
            add(cells[i], Category.STORAGE, "Celda Cuántica T" + (i + 1), cellWhatEn, cellWhatEs, cellUseEn, cellUseEs);
        }
        add(DeviceType.MVN_INFINITY_BARREL, Category.STORAGE, "Barril Infinito",
                "Like a cell (one item type) with a huge capacity, but it STAYS registered to its item when it "
                        + "empties. Items from Slimefun and other plugins are recognised by their id, so whatever "
                        + "you take out always goes back in.",
                "Como una celda (un tipo de ítem) con una capacidad enorme, pero SIGUE registrado a su ítem al "
                        + "vaciarse. Los ítems de Slimefun y otros plugins se reconocen por su id, así lo que sacas "
                        + "siempre vuelve a entrar.",
                "Right-click: click Set Item with an item on the cursor to register it; right-click Set Item with "
                        + "an empty cursor clears it (only when empty). Quick deposit / take out buttons. Shift+click "
                        + "Set Item toggles Void excess: when full, the network destroys extra items of its type. "
                        + "Hoppers do not interact with it. Wired to the network (cable or any device except a "
                        + "Grabber) it is network storage. Next to a Grabber of a network it does not belong to, the "
                        + "Grabber empties it like a chest.",
                "Clic derecho: clic en Set Item con un ítem en el cursor para registrarlo; clic derecho en Set Item "
                        + "con el cursor vacío lo borra (solo vacío). Botones de depósito y retirada rápida. "
                        + "Mayús+clic en Set Item activa Void excess: lleno, la red destruye lo que sobre de su "
                        + "ítem. Las tolvas no interactúan con él. Conectado a la red (por cable o cualquier "
                        + "dispositivo salvo un Grabber) es almacenamiento de la red. Junto a un Grabber de una red "
                        + "a la que no pertenece, el Grabber lo vacía como a un cofre.");
        add(DeviceType.MVN_GREEDY_CELL, Category.STORAGE, "Celda Codiciosa",
                "With a filter: a priority sink. Matching items go to it before any other storage, it pulls more "
                        + "from the network every cycle and pushes them into the containers next to it (not into "
                        + "network blocks). Holds several item types. Without a filter: overflow storage.",
                "Con filtro: un sumidero prioritario. Los ítems que coinciden van a ella antes que a cualquier "
                        + "otro almacenamiento, saca más de la red cada ciclo y los empuja a los contenedores de al "
                        + "lado (no a bloques de la red). Guarda varios tipos. Sin filtro: almacenamiento de "
                        + "desbordamiento.",
                "Right-click to set the filter and see its buffer. It is an internal filter: an item defined in it "
                        + "always keeps 1 unit inside, even when Pushers take the rest. A Pusher with that item in its "
                        + "whitelist releases it: the Greedy Cell moves all of it to the cells and stops taking it.",
                "Clic derecho para poner el filtro y ver su búfer. Es un filtro interno: un ítem definido en él "
                        + "siempre deja 1 unidad dentro, aunque los Pushers saquen el resto. Un Pusher con ese ítem en "
                        + "su whitelist lo libera: la Greedy Cell pasa todo a las celdas y deja de tomarlo.");
        add(DeviceType.MVN_QUANTUM_WORKBENCH, Category.STORAGE, "Mesa Cuántica",
                "Upgrades a Quantum Cell T1-T5 to the next tier keeping its cargo.",
                "Sube una Celda Cuántica T1-T5 al siguiente nivel conservando su carga.",
                "Cell in the centre, 8 diamonds around it, press Entangle & Upgrade and take the result. "
                        + "Ingredients left inside are returned when you close it.",
                "Celda en el centro, 8 diamantes alrededor, pulsa Entangle & Upgrade y recoge el resultado. Lo que "
                        + "quede dentro se devuelve al cerrar.");

        // ------------------------------------------------------------------ memory
        add(DeviceType.MVN_DRAM_BAY, Category.MEMORY, "Bahía DRAM",
                "Network block that holds up to 18 memory modules, each with its own stock. The modules are the "
                        + "storage: while installed, their stock is part of the network. Place as many bays as you "
                        + "like.",
                "Bloque de red que aloja hasta 18 módulos de memoria, cada uno con su propio stock. Los módulos "
                        + "son el almacenamiento: mientras están instalados, su stock es parte de la red. Pon tantos "
                        + "bays como quieras.",
                "Right-click the bay with a module in hand to install it in the next free slot, or right-click "
                        + "to open its menu: 18 module slots with their fill bars and item/fluid gauges. Install from the cursor or with "
                        + "shift-click; click an installed module to take it out. A module taken out keeps its whole "
                        + "stock: install it in another network's bay and the stock appears there and leaves the "
                        + "first one. Breaking the bay drops it and every module with its stock.",
                "Clic derecho al bay con un módulo en la mano para instalarlo en el siguiente hueco libre, o clic "
                        + "derecho para abrir su menú: 18 huecos de módulo con su barra de llenado y medidores de ítems y fluidos. Instala desde el "
                        + "cursor o con shift+clic; haz clic en un módulo instalado para sacarlo. Un módulo sacado "
                        + "conserva todo su stock: instálalo en el bay de otra red y el stock aparece allí y sale de "
                        + "la primera. Romper el bay suelta el bay y todos sus módulos con su stock.");
        add(DeviceType.MVN_MODULE_UNLOADER, Category.MEMORY, "Descargador DRAM",
                "Empties memory modules (cards) into the network. Unlike a DRAM Bay, the cards inside are not "
                        + "network storage: their stock flows into the network every cycle. What the network can not "
                        + "store stays on the card and the next item is tried, so the card ends up as empty as "
                        + "possible.",
                "Vacía módulos de memoria (tarjetas) en la red. A diferencia de un DRAM Bay, las tarjetas de "
                        + "dentro no son almacenamiento de la red: su stock pasa a la red en cada ciclo. Lo que la red "
                        + "no puede guardar se queda en la tarjeta y se prueba el siguiente ítem, así la tarjeta queda "
                        + "lo más vacía posible.",
                "Right-click it with a card in hand, or open its menu: 9 card slots with their progress. Click a "
                        + "card to take it out; \"Take empty cards\" collects every finished one. It needs free space "
                        + "in the network.",
                "Clic derecho con una tarjeta en la mano, o abre su menú: 9 huecos con su progreso. Haz clic en "
                        + "una tarjeta para sacarla; \"Take empty cards\" recoge todas las terminadas. Necesita espacio "
                        + "libre en la red.");
        add(DeviceType.MVN_MODULE_LOADER, Category.MEMORY, "Cargador DRAM",
                "Fills one memory module with a single item (or fluid, for a Fluid DRAM) taken from the network, "
                        + "up to the amount you choose. Inserting a card does nothing until you press Start.",
                "Llena un módulo de memoria con un único ítem (o fluido, en un Fluid DRAM) sacado de la red, hasta "
                        + "la cantidad que elijas. Meter una tarjeta no hace nada hasta pulsar Start.",
                "Open it, put an empty card in, click the target slot with the item (a fluid bucket for a Fluid "
                        + "DRAM), set the amount (buttons, or click it and type 1m, 500k, max) and press Start. It "
                        + "stops by itself when the card holds that amount or is full. Example: a Quantum card with "
                        + "500k cobblestone, ready to sell.",
                "Ábrelo, mete una tarjeta vacía, haz clic en el hueco de objetivo con el ítem (un cubo de fluido "
                        + "para un Fluid DRAM), fija la cantidad (botones, o haz clic y escribe 1m, 500k, max) y pulsa "
                        + "Start. Se para solo cuando la tarjeta tiene esa cantidad o está llena. Ejemplo: una "
                        + "tarjeta Quantum con 500k de adoquín, lista para vender.");
        String moduleWhatEn = "Item memory module for a DRAM Bay: stores any mix of item types up to its capacity. "
                + "It keeps its items when taken out of the bay.";
        String moduleWhatEs = "Módulo de memoria de ítems para un DRAM Bay: guarda cualquier mezcla de tipos hasta su "
                + "capacidad. Conserva sus ítems al sacarlo del bay.";
        String moduleUseEn = "Install it in a DRAM Bay. Each tier is crafted from the previous module; upgrading a "
                + "module that holds items keeps them.";
        String moduleUseEs = "Instálalo en un DRAM Bay. Cada nivel se craftea con el módulo anterior; mejorar un "
                + "módulo con ítems los conserva.";
        add(DeviceType.MVN_CACHE_L1, Category.MEMORY, "Módulo de Caché L1", moduleWhatEn, moduleWhatEs, moduleUseEn, moduleUseEs);
        add(DeviceType.MVN_CACHE_L2, Category.MEMORY, "Módulo de Caché L2", moduleWhatEn, moduleWhatEs, moduleUseEn, moduleUseEs);
        add(DeviceType.MVN_CACHE_L3, Category.MEMORY, "Módulo de Caché L3", moduleWhatEn, moduleWhatEs, moduleUseEn, moduleUseEs);
        add(DeviceType.MVN_CACHE_DRAM, Category.MEMORY, "Módulo DRAM", moduleWhatEn, moduleWhatEs, moduleUseEn, moduleUseEs);
        add(DeviceType.MVN_CACHE_QUANTUM, Category.MEMORY, "Matriz de Caché Cuántica", moduleWhatEn, moduleWhatEs, moduleUseEn, moduleUseEs);
        add(DeviceType.MVN_FLUID_DRAM, Category.MEMORY, "Módulo DRAM de Fluidos",
                "Fluid-only memory module: holds several fluids at once up to its total capacity, and adds it to "
                        + "the network's fluid storage.",
                "Módulo de memoria exclusivo para fluidos: guarda varios fluidos a la vez hasta su capacidad total "
                        + "y la suma al almacenamiento de fluidos de la red.",
                "Install it in a DRAM Bay. Taking it out carries its fluids to whichever network you install it "
                        + "in next. The two fluid cells of the recipe must be empty.",
                "Instálalo en un DRAM Bay. Al sacarlo se lleva sus fluidos a la red donde lo instales. Las dos "
                        + "celdas de fluidos de la receta deben estar vacías.");

        // ------------------------------------------------------------------ fluids
        add(DeviceType.MVN_FLUID_CELL, Category.FLUIDS, "Celda Cuántica de Fluidos",
                "Holds one fluid: Water, Lava, Milk, Powder Snow or Honey. Every fluid cell of a network forms its "
                        + "fluid storage, together with Fluid DRAM Modules.",
                "Guarda un fluido: Agua, Lava, Leche, Nieve polvo o Miel. Todas las celdas de fluidos de una red "
                        + "forman su almacenamiento de fluidos, junto con los Fluid DRAM Modules.",
                "Right-click with a filled bucket or honey bottle to pour, with an empty bucket to fill it. Or use "
                        + "the Terminal's fluid page. A bucket is only used up if the whole bucket fits.",
                "Clic derecho con un cubo lleno o botella de miel para verter, con un cubo vacío para llenarlo. O "
                        + "usa la página de fluidos del Terminal. Un cubo solo se gasta si cabe entero.");
        add(DeviceType.MVN_LIQUID_PUMP, Category.FLUIDS, "Bomba de Líquidos",
                "Each cycle drains one SOURCE block of water or lava directly below it (1,000 mB). The block is "
                        + "only removed if the whole 1,000 mB fit in the network.",
                "Cada ciclo drena un bloque FUENTE de agua o lava justo debajo (1.000 mB). El bloque solo "
                        + "desaparece si los 1.000 mB caben enteros en la red.",
                "Place it on top of the liquid. Right-click to choose ANY / WATER / LAVA.",
                "Colócala encima del líquido. Clic derecho para elegir ANY / WATER / LAVA.");

        // ------------------------------------------------------------------ transport
        add(DeviceType.MVN_GRABBER, Category.TRANSPORT, "Grabber Simple",
                "Imports from adjacent containers and machines into the network through ALL six faces, one item "
                        + "type per cycle. From a furnace it only takes the result. What the network cannot take "
                        + "waits in its buffer.",
                "Importa de los contenedores y máquinas de al lado a la red por LAS SEIS caras, un tipo de ítem "
                        + "por ciclo. De un horno solo saca el resultado. Lo que la red no admite espera en su búfer.",
                "Right-click for the filter (whitelist / blacklist). An empty filter takes everything. It does not "
                        + "carry the network into a Quantum Cell or Infinity Barrel next to it: it empties it like a "
                        + "chest. An item the network refuses goes back to its slot and the next one is tried.",
                "Clic derecho para el filtro (whitelist / blacklist). Un filtro vacío lo toma todo. No lleva la red "
                        + "a una Celda Cuántica o Infinity Barrel de al lado: la vacía como a un cofre. Un ítem que la "
                        + "red rechaza vuelve a su ranura y se prueba el siguiente.");
        add(DeviceType.MVN_GRABBER_HT, Category.TRANSPORT, "Grabber Avanzado",
                "Same as the Simple Grabber but 8 times faster, and it can be limited to ONE face.",
                "Igual que el Grabber Simple pero 8 veces más rápido, y se puede limitar a UNA cara.",
                "Filter menu plus face selector. Point it at the machine's OUTPUT (or its output chest).",
                "Menú de filtro más selector de cara. Apúntalo a la SALIDA de la máquina (o a su cofre de salida).");
        add(DeviceType.MVN_PUSHER, Category.TRANSPORT, "Pusher Simple",
                "Exports from the network into adjacent containers and machines through ALL six faces (no face "
                        + "selector). Never into another network block, never more than one stack per slot.",
                "Exporta de la red a los contenedores y máquinas de al lado por LAS SEIS caras (sin selector de "
                        + "cara). Nunca a otro bloque de la red, nunca más de un stack por ranura.",
                "The filter decides what leaves: empty whitelist = idle, blacklist = everything except the listed "
                        + "items. A whitelist of several items shares the target's slots between them. It can take "
                        + "from Greedy Cells (they keep 1 of each defined item); an item in its whitelist is released "
                        + "from Greedy Cells to the cells.",
                "El filtro decide qué sale: whitelist vacía = inactivo, blacklist = todo menos lo listado. Una "
                        + "whitelist de varios ítems reparte las ranuras del destino entre ellos. Puede sacar de las "
                        + "Greedy Cells (conservan 1 de cada ítem definido); un ítem de su whitelist se libera de las "
                        + "Greedy Cells hacia las celdas.");
        add(DeviceType.MVN_PUSHER_HT, Category.TRANSPORT, "Pusher Avanzado",
                "Same as the Simple Pusher but 8 times faster, and with a face selected it delivers ONLY to that "
                        + "side. The right tool to feed one machine with its ingredients.",
                "Igual que el Pusher Simple pero 8 veces más rápido, y con una cara elegida entrega SOLO a ese "
                        + "lado. La herramienta para alimentar una máquina con sus ingredientes.",
                "Filter menu plus face selector: pick the machine's face and whitelist its ingredients.",
                "Menú de filtro más selector de cara: elige la cara de la máquina y pon sus ingredientes en la "
                        + "whitelist.");
        add(DeviceType.MVN_VACUUM, Category.TRANSPORT, "Aspiradora de Red",
                "Picks up dropped items around it into the network.",
                "Recoge al almacenamiento los ítems tirados a su alrededor.",
                "Optional filter. Items that do not fit stay on the ground.",
                "Filtro opcional. Lo que no cabe se queda en el suelo.");
        add(DeviceType.MVN_PURGER, Category.TRANSPORT, "Purgador de Red",
                "Deletes items that match its filter from the network every cycle.",
                "Borra de la red cada ciclo los ítems que pasan su filtro.",
                "Without a filter it deletes nothing, on purpose. Use it for machine waste.",
                "Sin filtro no borra nada, a propósito. Úsalo para los residuos de las máquinas.");
        add(DeviceType.MVN_LIMITER, Category.TRANSPORT, "Limitador de Cuota",
                "Caps how much of one item the network may hold. Automatic imports (Grabbers, Vacuums, "
                        + "Crafters...) stop at the cap; a player depositing by hand in a Terminal or a storage's "
                        + "menu is not limited.",
                "Limita cuánto de un ítem puede guardar la red. La importación automática (Grabbers, Vacuums, "
                        + "Crafters...) se detiene en el tope; un jugador que deposita a mano en un Terminal o en el "
                        + "menú de un almacén no tiene límite.",
                "Right-click: target item, limit (buttons or chat) and on/off. With several limiters on the same "
                        + "item the lowest wins.",
                "Clic derecho: ítem objetivo, límite (botones o chat) y activar/desactivar. Con varios limitadores "
                        + "sobre el mismo ítem gana el más bajo.");
        add(DeviceType.MVN_QUOTA_MANAGER, Category.TRANSPORT, "Gestor de Cuotas",
                "One screen for every Quota Limiter of its network: each limited item with its limit, the "
                        + "network's stock and a usage bar, plus how many limiters are still free.",
                "Una pantalla para todos los Quota Limiters de su red: cada ítem limitado con su límite, el stock "
                        + "de la red y una barra de uso, y cuántos limitadores quedan libres.",
                "Right-click it. Add an item with Add (item on the cursor), on a free limiter, or with "
                        + "Shift-Click from your inventory: it uses one free Quota Limiter, so place more limiters "
                        + "to limit more items. On a limited item: left-click types the limit, Shift+right-click "
                        + "turns it on/off, right-click removes the item (the limiter is free again).",
                "Clic derecho. Añade un ítem con Add (ítem en el cursor), sobre un limitador libre o con "
                        + "Shift+clic desde tu inventario: ocupa un Quota Limiter libre, así que coloca más "
                        + "limitadores para limitar más ítems. En un ítem limitado: clic izquierdo escribe el "
                        + "límite, Shift+clic derecho lo activa/desactiva, clic derecho quita el ítem (el limitador "
                        + "queda libre).");
        add(DeviceType.MVN_TRANSMITTER, Category.TRANSPORT, "Transmisor Inalámbrico",
                "One end of the wireless bridge. If it holds the link to a Receiver it PUSHES the items that pass "
                        + "its filter into the receiver's network, at any distance and across worlds.",
                "Un extremo del puente inalámbrico. Si guarda el enlace a un Receptor, EMPUJA los ítems que pasan "
                        + "su filtro a la red del receptor, a cualquier distancia y entre mundos.",
                "Shift+right-click a placed Receiver holding the Transmitter item to link it. Right-click: filter "
                        + "and a button that opens its own network's terminal.",
                "Shift+clic derecho a un Receptor colocado con el Transmisor en la mano para enlazarlo. Clic "
                        + "derecho: filtro y un botón que abre el terminal de su propia red.");
        add(DeviceType.MVN_RECEIVER, Category.TRANSPORT, "Receptor Inalámbrico",
                "The other end. If it holds the link to a Transmitter it PULLS the items that pass its filter "
                        + "from the transmitter's network into its own. An empty whitelist moves nothing.",
                "El otro extremo. Si guarda el enlace a un Transmisor, TRAE los ítems que pasan su filtro desde la "
                        + "red del transmisor a la suya. Una whitelist vacía no mueve nada.",
                "Shift+right-click a placed Transmitter holding the Receiver item to link it. Its menu can open the "
                        + "REMOTE network's terminal.",
                "Shift+clic derecho a un Transmisor colocado con el Receptor en la mano para enlazarlo. Su menú "
                        + "puede abrir el terminal de la red REMOTA.");

        // ------------------------------------------------------------------ crafting
        add(DeviceType.MVN_BLUEPRINT, Category.CRAFTING, "Plano",
                "A recipe: the 3x3 grid and its result. Crafting never consumes it.",
                "Una receta: la cuadrícula 3x3 y su resultado. Craftear nunca lo gasta.",
                "Craft blank ones, write a recipe on them in a Recipe Encoder and install them in a crafter.",
                "Craftea planos en blanco, escribe una receta en un Recipe Encoder e instálalos en un crafter.");
        add(DeviceType.MVN_ENCODER, Category.CRAFTING, "Codificador de Recetas",
                "Writes a vanilla recipe onto a Blueprint. Clicking the template only marks slots, no items are "
                        + "spent.",
                "Escribe una receta vanilla en un Blueprint. Hacer clic en la plantilla solo marca huecos, no "
                        + "gasta ítems.",
                "Fill the template, put a Blueprint in the blue slot and press Encode. Blueprints left in its "
                        + "slots stay stored in the block. Clicking an encoded Blueprint loads its recipe.",
                "Rellena la plantilla, pon un Blueprint en la ranura azul y pulsa Encode. Los Blueprints que dejes "
                        + "en sus ranuras se guardan en el bloque. Clic en un Blueprint codificado carga su receta.");
        add(DeviceType.MVN_SF_ENCODER, Category.CRAFTING, "Codificador de Slimefun",
                "Same as the Recipe Encoder for Slimefun recipes. Needs Slimefun.",
                "Igual que el Recipe Encoder para recetas de Slimefun. Necesita Slimefun.",
                "Blueprints left in its slots stay stored in the block, so you do not need to bring blank "
                        + "Blueprints every time.",
                "Los Blueprints que dejes en sus ranuras se quedan guardados en el bloque, así no hace falta traer "
                        + "planos en blanco cada vez.");
        add(DeviceType.MVN_CRAFTER, Category.CRAFTING, "Autocrafter",
                "Every second it tries each installed vanilla Blueprint once with the network's stock. "
                        + "All-or-nothing: a missing ingredient touches nothing, and a result that does not fit "
                        + "undoes the craft.",
                "Cada segundo intenta una vez cada Blueprint vanilla instalado con el stock de la red. Todo o "
                        + "nada: si falta un ingrediente no se toca nada, y si el resultado no cabe se deshace.",
                "Right-click and click Blueprints in (up to 18). Slimefun Blueprints are refused.",
                "Clic derecho y clic en los Blueprints para instalarlos (hasta 18). Rechaza los de Slimefun.");
        add(DeviceType.MVN_SF_CRAFTER, Category.CRAFTING, "Autocrafter de Slimefun",
                "Same as the Auto-Crafter and it accepts BOTH Slimefun and vanilla Blueprints in the same machine.",
                "Igual que el Autocrafter y acepta Blueprints de Slimefun Y vanilla en la misma máquina.",
                "Install Blueprints like in the Auto-Crafter.",
                "Instala Blueprints igual que en el Autocrafter.");
        add(DeviceType.MVN_REQUEST_CRAFTER, Category.CRAFTING, "Crafter bajo Pedido",
                "Holds vanilla Blueprints that are ONLY crafted on demand from a Request Terminal.",
                "Guarda Blueprints vanilla que SOLO se craftean bajo pedido desde un Request Terminal.",
                "Install Blueprints like in an Auto-Crafter; order from the Request Terminal.",
                "Instala Blueprints igual que en un Autocrafter; pide desde el Request Terminal.");
        add(DeviceType.MVN_SF_REQUEST_CRAFTER, Category.CRAFTING, "Crafter bajo Pedido de Slimefun",
                "Same as the Request Crafter, with Slimefun and vanilla Blueprints.",
                "Igual que el Crafter bajo Pedido, con Blueprints de Slimefun y vanilla.",
                "Install Blueprints and order from the Request Terminal.",
                "Instala Blueprints y pide desde el Request Terminal.");
        add(DeviceType.MVN_REQUEST_TERMINAL, Category.CRAFTING, "Terminal de Pedidos",
                "Lists everything the network's Request Crafters can make and crafts it on demand, solving chains "
                        + "(logs, planks, crafting table) with the network's stock.",
                "Lista todo lo que pueden fabricar los Request Crafters de la red y lo craftea bajo pedido, "
                        + "resolviendo cadenas (troncos, tablones, mesa) con el stock de la red.",
                "Left-click 1 batch, shift-left 10, right-click 64, shift-right asks for an amount in chat. Toggle "
                        + "delivery to your inventory or to the network.",
                "Clic izquierdo 1 lote, shift+izquierdo 10, clic derecho 64, shift+derecho pide una cantidad por "
                        + "chat. Alterna la entrega a tu inventario o a la red.");
        add(DeviceType.MVN_CRAFTING_GRID, Category.CRAFTING, "Mesa de Crafteo de Red",
                "A crafting table that takes the ingredients from the network. Its template is saved in the block.",
                "Una mesa de crafteo que saca los ingredientes de la red. Su plantilla queda guardada en el bloque.",
                "Right-click, set the template and craft one or all.",
                "Clic derecho, monta la plantilla y craftea uno o todos.");

        // ------------------------------------------------------------------ tools
        add(DeviceType.MVN_CONFIGURATOR, Category.TOOLS, "Llave de Configuración",
                "Copies a filter (exact templates, materials and whitelist/blacklist mode) from one device to "
                        + "another.",
                "Copia un filtro (plantillas exactas, materiales y modo whitelist/blacklist) de un dispositivo a "
                        + "otro.",
                "Shift+right-click a device to copy its filter, right-click another one to paste it.",
                "Shift+clic derecho en un dispositivo para copiar su filtro, clic derecho en otro para pegarlo.");
        add(DeviceType.MVN_RAKE, Category.TOOLS, "Rastrillo de Red",
                "Dismantles a device instantly and gives it back with its state (filter, Blueprints, link). A "
                        + "DRAM Bay, Unloader or Loader comes back together with every module inside it, and a "
                        + "camouflaged cable with its block.",
                "Desmonta un dispositivo al instante y te lo devuelve con su estado (filtro, Blueprints, enlace). "
                        + "Un DRAM Bay, Unloader o Loader vuelve junto con todos sus módulos, y un cable camuflado con "
                        + "su bloque.",
                "Right-click a device. It refuses controllers, and cells, barrels, Greedy Cells or fluid cells "
                        + "that still hold something. Limited uses.",
                "Clic derecho en un dispositivo. Rechaza controladores, y celdas, barriles, Greedy Cells o celdas "
                        + "de fluidos que aún guarden algo. Usos limitados.");

        // ------------------------------------------------------------------ chickens
        add(DeviceType.MVN_CHICKEN_SORTER, Category.CHICKENS, "Clasificador Genético de Pollos",
                "Moves ONLY GeneticChickengineering pocket chickens, chosen by their genes; every other item is "
                        + "ignored. Push: from the network into the block it faces. Pull: from that block into the "
                        + "network. Up to 16 chickens per cycle.",
                "Mueve SOLO los pollos de bolsillo de GeneticChickengineering, elegidos por sus genes; cualquier "
                        + "otro ítem se ignora. Push: de la red al bloque al que mira. Pull: de ese bloque a la red. "
                        + "Hasta 16 pollos por ciclo.",
                "Right-click. Top row: start/stop, Push/Pull, side, a summary of what passes, help. Middle: "
                        + "accepted products (click a slot with a chicken on the cursor or Shift-click one in your "
                        + "inventory; click a product to remove it; empty = any). Below: gene rules, all must pass: "
                        + "min/max tier, min DNA strength, pure genes, DNA, age. Left +1, right -1, Shift resets; "
                        + "an active rule glows. It starts STOPPED: turn it on when it is set up.",
                "Clic derecho. Fila superior: arrancar/parar, Push/Pull, lado, un resumen de lo que pasa, ayuda. "
                        + "En medio: productos aceptados (clic en un hueco con un pollo en el cursor o Shift+clic a "
                        + "uno del inventario; clic en un producto para quitarlo; vacío = cualquiera). Debajo: reglas "
                        + "de genes, todas deben cumplirse: nivel mín/máx, fuerza de ADN mínima, genes puros, ADN, "
                        + "edad. Izquierdo +1, derecho -1, Shift reinicia; una regla activa brilla. Empieza PARADO: "
                        + "actívalo cuando esté listo.");

        // ------------------------------------------------------------------ components
        String componentUseEn = "Only an ingredient: it can not be placed or used, and no vanilla recipe accepts it.";
        String componentUseEs = "Solo es un ingrediente: no se coloca ni se usa, y ninguna receta vanilla lo acepta.";
        add(DeviceType.MVN_SILICON_WAFER, Category.COMPONENTS, "Oblea de Silicio",
                "The first component, made from quartz and sand. Network Circuits, Storage Matrices and "
                        + "Blueprints are built on it.",
                "El primer componente, de cuarzo y arena. Los Network Circuits, las Storage Matrix y los "
                        + "Blueprints se fabrican con él.",
                componentUseEn, componentUseEs);
        add(DeviceType.MVN_NETWORK_CIRCUIT, Category.COMPONENTS, "Circuito de Red",
                "Basic network logic. The Controller, cables, Terminal, Simple Grabber and Pusher, Vacuum, "
                        + "Purger, Quota Limiter, Monitor, Probe, Encoders and the Wrench use it, and it is the base "
                        + "of the Logic Processor and the Storage Matrix.",
                "Lógica básica de red. La usan el Controlador, los cables, el Terminal, el Grabber y el Pusher "
                        + "simples, el Vacuum, el Purger, el Limitador, el Monitor, la Sonda, los Encoders y la "
                        + "Llave, y es la base del Logic Processor y la Storage Matrix.",
                componentUseEn, componentUseEs);
        add(DeviceType.MVN_LOGIC_PROCESSOR, Category.COMPONENTS, "Procesador Lógico",
                "The brain of the machines that decide: Controller, Auto-Crafters, Request Crafters, Request "
                        + "Terminal and Crafting Grid. Two of them make a Quantum Core.",
                "El cerebro de las máquinas que deciden: Controlador, Auto-Crafters, Request Crafters, Request "
                        + "Terminal y Crafting Grid. Con dos se hace un Quantum Core.",
                componentUseEn, componentUseEs);
        add(DeviceType.MVN_STORAGE_MATRIX, Category.COMPONENTS, "Matriz de Almacenamiento",
                "The core of every storage: Quantum Cell T1, Infinity Barrel, Greedy Cell, Quantum Fluid Cell, "
                        + "DRAM Bay and the L1 module.",
                "El núcleo de todo almacén: Quantum Cell T1, Infinity Barrel, Greedy Cell, Quantum Fluid Cell, "
                        + "DRAM Bay y el módulo L1.",
                componentUseEn, componentUseEs);
        add(DeviceType.MVN_TRANSFER_COIL, Category.COMPONENTS, "Bobina de Transferencia",
                "Moves things: Grabbers, Pushers (simple and advanced), Vacuum and Liquid Pump.",
                "Mueve cosas: Grabbers, Pushers (simples y avanzados), Vacuum y Liquid Pump.",
                componentUseEn, componentUseEs);
        add(DeviceType.MVN_QUANTUM_CORE, Category.COMPONENTS, "Núcleo Cuántico",
                "Links far places: Wireless Terminal, Transmitter, Receiver, Network Router and Quantum Workbench.",
                "Une lugares lejanos: Terminal Inalámbrico, Transmisor, Receptor, Network Router y Quantum "
                        + "Workbench.",
                componentUseEn, componentUseEs);
    }
}

package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.compat.ProtectionBridge;
import com.chagui68.multiversenets.compat.SlimefunBridge;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
import com.chagui68.multiversenets.util.Settings;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * [EN] Network Graph Topology & State
 * Represents a single connected network of nodes centered on a Network Controller.
 *
 * Scans connected blocks via BFS without triggering synchronous chunk loads, and maintains
 * type-indexed lookups ({@code byType}) to optimize ticking operations for 4000+ nodes.
 *
 * [ES] Topología y Estado del Grafo de Red
 * Representa una red física de nodos conectados centrada en un Controlador de Red.
 * Escanea bloques adyacentes por BFS sin forzar la carga de chunks y mantiene un índice
 * optimizado por tipo de dispositivo.
 */
public class Network {

    private final com.chagui68.multiversenets.MultiverseNets plugin;
    private final org.bukkit.World world;
    private final long controllerPos;
    // Colecciones de long primitivo (fastutil, incluido en Paper): con max-nodes altos, los Long
    // en caja de HashMap/HashSet eran decenas de miles de objetos por escaneo.
    private final Long2ObjectOpenHashMap<DeviceType> nodes = new Long2ObjectOpenHashMap<>();
    private final Map<DeviceType, LongOpenHashSet> byType = new EnumMap<>(DeviceType.class);
    private final LongOpenHashSet sfBarrels = new LongOpenHashSet();
    private final NetworkStorage storage = new NetworkStorage(this);
    private final NetworkFluidStorage fluidStorage = new NetworkFluidStorage(this);
    private final NetworkThroughputTracker throughput = new NetworkThroughputTracker();
    private volatile long version = 0;
    private long lastScanMs = 0;
    private volatile boolean dirty = true;
    /** Something near the network may have changed (chunk load, foreign block); rescan soon. */
    private volatile boolean stale;
    /** {@link NetworkTicker#clock()} when the last scan finished / Hora del ticker del último escaneo. */
    private volatile long lastScanClock;
    /** Owner of the network, taken from the Controller blob. Null for legacy controllers. */
    private volatile java.util.UUID owner;
    /** How many links the last scan refused because the land was somebody else's. */
    private volatile int blockedByProtection;
    /** True when the last scan ran into another network's controller, i.e. both share cables. */
    private volatile boolean touchesForeignController;
    public String error;

    public Network(com.chagui68.multiversenets.MultiverseNets plugin, org.bukkit.World world, long controllerPos) {
        this.plugin = plugin;
        this.world = world;
        this.controllerPos = controllerPos;
    }

    public org.bukkit.World world() {
        return world;
    }

    public long controllerPos() {
        return controllerPos;
    }

    public Map<Long, DeviceType> nodes() {
        return nodes;
    }

    public int size() {
        return nodes.size();
    }

    public Set<Long> slimefunBarrels() {
        return sfBarrels;
    }

    public NetworkStorage storage() {
        return storage;
    }

    public NetworkFluidStorage fluidStorage() {
        return fluidStorage;
    }

    public NetworkThroughputTracker throughput() {
        return throughput;
    }

    public long versionSnapshot() {
        return version;
    }

    /**
     * EN: UUID of the player who placed this network's Controller, used to decide whether the
     * network may operate inside claimed land. Null means "unknown", which is treated as a
     * stranger, never as an owner.
     *
     * ES: UUID del jugador que colocó el Controlador de esta red, usado para decidir si la red
     * puede operar dentro de tierra reclamada. Null significa "desconocido", y se trata como
     * extraño, nunca como dueño.
     */
    public java.util.UUID ownerUuid() {
        return owner;
    }

    /**
     * EN: Links the last scan refused because of land protection. Non-zero means the network is
     * smaller than the player built it, which is the case that used to be silent.
     *
     * ES: Enlaces que el último escaneo rechazó por protección de tierras. Distinto de cero
     * significa que la red es más pequeña de lo que el jugador construyó, el caso que antes era
     * silencioso.
     */
    public int linksBlockedByProtection() {
        return blockedByProtection;
    }

    /**
     * EN: True when this network's cables reach another Controller. Both networks then index the
     * same cables and devices, and the ticker must make sure each device still works only once.
     *
     * ES: True si los cables de esta red alcanzan otro Controlador. Ambas redes indexan entonces los
     * mismos cables y dispositivos, y el ticker debe asegurar que cada dispositivo trabaje una vez.
     */
    public boolean touchesForeignController() {
        return touchesForeignController;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    /**
     * EN: The topology may have changed (a chunk loaded, a block that is not a node was placed or
     * broken next to it). Unlike {@link #markDirty()}, the rescan waits until scan-interval-ticks
     * have passed since the last one, so a player flying over a base does not rescan every cycle.
     *
     * ES: La topología quizá cambió (cargó un chunk, se colocó o rompió al lado un bloque que no es
     * nodo). A diferencia de {@link #markDirty()}, el reescaneo espera a que pasen scan-interval-ticks
     * desde el anterior.
     */
    public void markStale() {
        this.stale = true;
    }

    public boolean isStale() {
        return stale;
    }

    public long lastScanClock() {
        return lastScanClock;
    }

    // Chunks covered by the last scan, for chunk-load events / Chunks que cubrió el último escaneo.
    private volatile int minCx = Integer.MAX_VALUE;
    private volatile int maxCx = Integer.MIN_VALUE;
    private volatile int minCz = Integer.MAX_VALUE;
    private volatile int maxCz = Integer.MIN_VALUE;

    /**
     * EN: Whether a chunk touches this network or borders it: only then can its loading change the
     * topology.
     * ES: Si un chunk toca esta red o linda con ella: solo entonces su carga puede cambiar la
     * topología.
     */
    public boolean nearChunk(int cx, int cz) {
        // El chunk del controlador cuenta siempre: si estaba descargado, el escaneo no llego a
        // registrar nada y su carga es justo lo que tiene que despertar la red.
        int ctrlCx = PosUtil.unpackX(controllerPos) >> 4;
        int ctrlCz = PosUtil.unpackZ(controllerPos) >> 4;
        if (Math.abs(cx - ctrlCx) <= 1 && Math.abs(cz - ctrlCz) <= 1) {
            return true;
        }
        return cx >= minCx - 1 && cx <= maxCx + 1 && cz >= minCz - 1 && cz <= maxCz + 1;
    }

    /**
     * Fixed per-network offset (0..gap/4) so networks loaded together do not all run their periodic
     * full rescan on the same tick / Desfase fijo por red para no escanear todas en el mismo tick.
     */
    long rescanJitter(int gap) {
        int spread = Math.max(1, gap / 4);
        return Math.floorMod(Long.hashCode(controllerPos) ^ world.getUID().hashCode(), spread);
    }

    public boolean contains(long pos) {
        return nodes.containsKey(pos);
    }

    public DeviceType typeAt(long pos) {
        return nodes.get(pos);
    }

    public void forEach(DeviceType type, BiConsumer<Long, DeviceType> consumer) {
        LongOpenHashSet matching = byType.get(type);
        if (matching == null || matching.isEmpty()) {
            return;
        }
        // Copia defensiva: el consumidor puede romper un bloque y modificar el indice mientras
        // se recorre. Solo se copian los de ESE tipo, no la red entera.
        for (long pos : matching.toLongArray()) {
            consumer.accept(pos, type);
        }
    }

    /** Cuantos dispositivos de un tipo tiene la red. Util para diagnosticos sin recorrer nada. */
    public int count(DeviceType type) {
        LongOpenHashSet matching = byType.get(type);
        return matching == null ? 0 : matching.size();
    }

    public Block block(long pos) {
        return world.getBlockAt(PosUtil.unpackX(pos), PosUtil.unpackY(pos), PosUtil.unpackZ(pos));
    }

    public void scan() {
        Long2ObjectOpenHashMap<DeviceType> found = new Long2ObjectOpenHashMap<>();
        LongOpenHashSet visited = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        List<String> errors = new ArrayList<>();
        sfBarrels.clear();
        blockedByProtection = 0;
        boolean foreignController = false;

        // No cargar chunks a la fuerza: si el controlador esta en uno sin cargar, la red se queda
        // como estaba y el proximo scan (o la carga del chunk) lo resuelve. Antes el BFS llamaba
        // a getBlockAt a ciegas y el servidor cargaba trozos enteros de disco en pleno tick.
        int ctrlCx = PosUtil.unpackX(controllerPos) >> 4;
        int ctrlCz = PosUtil.unpackZ(controllerPos) >> 4;
        if (!world.isChunkLoaded(ctrlCx, ctrlCz)) {
            return;
        }

        // canonical(): get() decodificaba una copia del controlador en cada escaneo; si se migra la
        // cache antigua, el put de abajo la reescribe igualmente.
        NodeBlob ctrlBlob = NodeStore.canonical(block(controllerPos));
        if (ctrlBlob == null) {
            // El registro dice que aqui hubo un controlador pero el chunk ya no lo tiene. Dejar
            // la topologia vieja viva haria trabajar a la red sobre bloques que ya no existen.
            synchronized (nodes) {
                nodes.clear();
                byType.clear();
            }
            error = "controller missing";
            version++;
            storage.invalidate();
            return;
        }

        // El dueno de la red sale del propio controlador. La semilla no se comprueba contra
        // proteccion (el jugador que la coloco paso por su plugin al hacerlo), pero todo lo que
        // cuelgue de ella si: la red solo tiende cable por tierra que sea suya.
        this.owner = ctrlBlob.owner();

        // Los modulos de memoria ya no van en el Controlador: uno instalado antes del DRAM Bay sale
        // con su stock y espera en el Terminal como item temporal.
        if (MemoryModules.migrateControllerCache(ctrlBlob)) {
            NodeStore.put(block(controllerPos), ctrlBlob);
            storage.invalidate();
        }

        found.put(controllerPos, DeviceType.MVN_CONTROLLER);
        visited.add(controllerPos);
        queue.enqueue(controllerPos);

        // Un solo buffer reutilizado para los seis vecinos: antes cada nodo asignaba una List y
        // seis Long boxeados, que con max-nodes altos es la mayor fuente de basura del escaneo.
        long[] neighborBuffer = new long[6];
        // Ajustes leidos una vez por escaneo, no una vez por vecino.
        int maxNodes = Settings.maxNodes();
        boolean checkProtection = Settings.protectionBlocksNetworkLinking();
        boolean slimefun = Settings.compatSlimefun() && SlimefunBridge.isAvailable();
        while (!queue.isEmpty()) {
            if (found.size() >= maxNodes) {
                errors.add("node limit reached (" + maxNodes + ")");
                break;
            }
            long pos = queue.dequeueLong();
            fillNeighbors(pos, neighborBuffer);
            for (int neighborIndex = 0; neighborIndex < neighborBuffer.length; neighborIndex++) {
                long next = neighborBuffer[neighborIndex];
                if (!visited.add(next)) {
                    continue;
                }
                if (!world.isChunkLoaded(PosUtil.unpackX(next) >> 4, PosUtil.unpackZ(next) >> 4)) {
                    continue;
                }
                // No tender cable por tierra ajena: cortar aqui es lo que impide que dos redes a
                // lados opuestos de una region protegida acaben fusionadas en un mismo bus de
                // items. El controlador es la semilla y no se comprueba; sus transferencias si
                // pasan por el chequeo de bloque de NetworkTicker.
                if (checkProtection
                        && !ProtectionBridge.mayActorUse(world, PosUtil.unpackX(next), PosUtil.unpackY(next),
                                PosUtil.unpackZ(next), owner)) {
                    blockedByProtection++;
                    continue;
                }
                Block block = block(next);
                // Una busqueda en memoria por vecino: sin nodo ni Slimefun, se descarta aqui mismo.
                DeviceType type = NodeStore.getType(block);
                if (type == null) {
                    if (slimefun && !block.getType().isAir()) {
                        // Un solo getId por vecino (antes isNetworkCable e isBarrel lo pedian cada
                        // uno) y nada para el aire, que es la mayoria de los vecinos de un cable.
                        String sfId = SlimefunBridge.getId(block);
                        if (sfId == null) {
                            continue;
                        }
                        if (SlimefunBridge.isNetworkCableId(sfId)) {
                            found.put(next, DeviceType.MVN_CABLE);
                            queue.enqueue(next);
                        } else if (SlimefunBridge.isBarrel(block, sfId)) {
                            sfBarrels.add(next);
                            queue.enqueue(next);
                        }
                    }
                    continue;
                }
                if (type == DeviceType.MVN_CONTROLLER && next != controllerPos) {
                    errors.add("foreign controller at " + coordString(next));
                    foreignController = true;
                    continue;
                }
                found.put(next, type);
                queue.enqueue(next);
            }
        }

        int loCx = Integer.MAX_VALUE;
        int hiCx = Integer.MIN_VALUE;
        int loCz = Integer.MAX_VALUE;
        int hiCz = Integer.MIN_VALUE;
        synchronized (nodes) {
            nodes.clear();
            nodes.putAll(found);
            byType.clear();
            for (var entry : found.long2ObjectEntrySet()) {
                long pos = entry.getLongKey();
                byType.computeIfAbsent(entry.getValue(), key -> new LongOpenHashSet()).add(pos);
                int cx = PosUtil.unpackX(pos) >> 4;
                int cz = PosUtil.unpackZ(pos) >> 4;
                loCx = Math.min(loCx, cx);
                hiCx = Math.max(hiCx, cx);
                loCz = Math.min(loCz, cz);
                hiCz = Math.max(hiCz, cz);
            }
        }
        for (long pos : sfBarrels.toLongArray()) {
            int cx = PosUtil.unpackX(pos) >> 4;
            int cz = PosUtil.unpackZ(pos) >> 4;
            loCx = Math.min(loCx, cx);
            hiCx = Math.max(hiCx, cx);
            loCz = Math.min(loCz, cz);
            hiCz = Math.max(hiCz, cz);
        }
        this.minCx = loCx;
        this.maxCx = hiCx;
        this.minCz = loCz;
        this.maxCz = hiCz;
        if (blockedByProtection > 0) {
            errors.add(blockedByProtection + " link(s) stopped at protected land");
        }
        this.error = String.join("; ", errors);
        this.touchesForeignController = foreignController;
        this.version++;
        this.lastScanMs = System.currentTimeMillis();
        this.lastScanClock = NetworkTicker.clock();
        this.dirty = false;
        this.stale = false;
        storage.invalidate();
        throughput.retainNodes(this::contains);
        NetworkManager manager = plugin == null ? null : plugin.networks();
        if (manager != null) {
            manager.indexScan(this);
        }
    }

    /**
     * Escribe los seis vecinos ortogonales de {@code pos} en {@code out}. Sin allocaciones: este
     * metodo corre una vez por nodo y por escaneo, y era el punto mas caliente del BFS.
     */
    static void fillNeighbors(long pos, long[] out) {
        int x = PosUtil.unpackX(pos);
        int y = PosUtil.unpackY(pos);
        int z = PosUtil.unpackZ(pos);
        out[0] = PosUtil.pack(x + 1, y, z);
        out[1] = PosUtil.pack(x - 1, y, z);
        out[2] = PosUtil.pack(x, y + 1, z);
        out[3] = PosUtil.pack(x, y - 1, z);
        out[4] = PosUtil.pack(x, y, z + 1);
        out[5] = PosUtil.pack(x, y, z - 1);
    }

    private static String coordString(long pos) {
        return PosUtil.unpackX(pos) + "," + PosUtil.unpackY(pos) + "," + PosUtil.unpackZ(pos);
    }

    public boolean needsScan(long intervalMs) {
        return System.currentTimeMillis() - lastScanMs >= intervalMs;
    }
}
